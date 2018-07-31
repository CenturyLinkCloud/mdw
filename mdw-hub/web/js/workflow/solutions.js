'use strict';

var solutionMod = angular.module('solutions', ['ngResource', 'mdw', 'infinite-scroll']);

solutionMod.controller('SolutionsController', ['$scope', '$http', '$location', 'mdw', 'Solutions', 
                                               function($scope, $http, $location, mdw, Solutions) {
  $scope.solutions = [];
    
  $scope.busy = false;
  $scope.total = 0;
  $scope.selected = null;
  
  $scope.getNext = function() {
    if (!$scope.busy) {
      $scope.busy = true;
      
      if ($scope.selected !== null) {
        // solution has been selected in typeahead
        $scope.solutions = [$scope.selected];
        $scope.busy = false;
      }
      else {
        // retrieve the solution list
        var url = mdw.roots.services + '/Services/Solutions?app=mdw-admin&start=' + $scope.solutions.length;
        $http.get(url).error(function(data, status) {
          console.log('HTTP ' + status + ': ' + url);
          this.busy = false;
        }).success(function(data, status, headers, config) {
          $scope.total = data.total;
          $scope.solutions = $scope.solutions.concat(data.solutions);
          $scope.busy = false;
        });
      }
    }
  };
  
  $scope.hasMore = function() {
    return $scope.solutions.length === 0 || $scope.solutions.length < $scope.total;
  };
  
  $scope.select = function() {
    $scope.solutions = [$scope.selected];
  };
  
  $scope.change = function() {
    if ($scope.selected === null) {
      // repopulate list
      $scope.solutions = [];
      $scope.getNext();
    }
  };
  
  $scope.find = function(typed) {
    return $http.get(mdw.roots.services + '/Services/Solutions?app=mdw-admin&find=' + typed).then(function(response) {
      return response.data.solutions;
    });
  };
  
  $scope.create = false;
  $scope.setCreate = function(create) {
    $scope.create = create;
    $scope.solution = {
        // blank id and name help w/applying error styles
        id: '', 
        name: '',
        // blank default attributes force empty fields to display
        values: { }
    };
  };
  
  $scope.cancel = function() {
    $scope.setCreate(false);
  };
  
  $scope.isSaveEnabled = function() {
    return $scope.solution && $scope.solution.id && $scope.solution.id.length > 0 &&
      $scope.solution.name && $scope.solution.name.length > 0;
  };
  
  $scope.save = function() {
    console.log('creating solution: ' + $scope.solution.id);
    
    Solutions.create({id: $scope.solution.id}, $scope.solution,
      function(data) {
        if (data.status.code >= 300) {
          $scope.solution.message = data.status.message;
        }
        else {
          $scope.setCreate(false);
          $scope.solutions = [];
          $scope.total = 0;          
          $location.path('/solutions');
        }
      }, 
      function(error) {
        $scope.solution.message = error.data.status.message;
      });
  };
  
}]);

solutionMod.controller('SolutionController', ['$scope', '$routeParams', '$location', '$http', 'mdw', 'uiUtil', 'Solutions',
                                              function($scope, $routeParams, $location, $http, mdw, uiUtil, Solutions) {
  $scope.solutionId = $routeParams.solutionId;
  $scope.edit = false;
  $scope.setEdit = function(edit) {
    $scope.edit = edit;
    if (edit) // backup original for cancel
      $scope.uneditedSolution = Solutions.shallowCopy({}, $scope.solution);
  };
  
  $scope.confirm = false;
  $scope.setConfirm = function(confirm) {
    $scope.confirm = confirm;
    $scope.solution.message = confirm ? 'Delete solution "' + $scope.solution.id + '"?' : null;
  };
  
  $scope.cancel = function() {
    if ($scope.edit)
      $scope.solution = Solutions.shallowCopy($scope.solution, $scope.uneditedSolution);
    $scope.setEdit(false);
    $scope.setConfirm(false);
  };
  
  $scope.isSaveEnabled = function() {
    return $scope.solution && $scope.solution.id && $scope.solution.id.length > 0 &&
      $scope.solution.name && $scope.solution.name.length > 0;
  };
  
  $scope.save = function() {
    console.log('saving solution: ' + $scope.solution.id);
    Solutions.update({id: $scope.uneditedSolution.id}, Solutions.shallowCopy({}, $scope.solution),
      function(data) {
        if (data.status.code >= 300) {
          $scope.solution.message = data.status.message;
        }
        else {
          $scope.solutionName = $scope.solution.name;
          $scope.setEdit(false);
        }
      }, 
      function(error) {
        $scope.solution.message = error.data.status.message;
      });
  };
  
  $scope.deleteSolution = function() {
    console.log('deleting solution: ' + $scope.solution.id);
    
    Solutions.remove({id: $scope.solution.id},
      function(data) {
        if (data.status.code >= 300) {
          $scope.solution.message = data.status.message;
        }
        else {
          $scope.setConfirm(false);
          $location.path('/solution');
        }
      }, 
      function(error) {
        $scope.solution.message = error.data.status.message;
      });
  };
  
  $scope.solution = Solutions.get({id: $routeParams.solutionId},
    function() {
      $scope.solutionName = $scope.solution.name;
    });
  
  $scope.findTask = function(typed) {
    return $http.get(mdw.roots.services + '/Services/Tasks?app=mdw-admin&status=Active&find=' + typed).then(function(response) {
      return response.data.tasks;
    });
  };
  
  $scope.findMasterRequest = function(typed) {
    return $http.get(mdw.roots.services + '/Services/Requests?app=mdw-admin&type=masterRequests&find=' + typed).then(function(response) {
      return response.data.requests;
    });
  };

  $scope.findProcess = function(typed) {
      return $http.get(mdw.roots.services + '/services/Processes?app=mdw-admin&find=' + typed).then(function(response) {
        return response.data.processInstances;
      });
  };
  
  $scope.addSelectedTask = function(selTask) {
    console.log('adding task: ' + selTask.id + 'to solution: ' + $scope.solution.id);
    Solutions.create({id: $scope.solution.id, rel: 'tasks', relId: selTask.id }, Solutions.shallowCopy({}, $scope.solution),
        function(data) {
          if (data.status.code >= 300) {
            $scope.solution.message = data.status.message;
          }
          else {
            if (!$scope.solution.members.tasks)
              $scope.solution.members.tasks = [];
            $scope.solution.members.tasks.push(selTask);
            $scope.solution.members.tasks.sort(function(t1, t2) {
              return t1.id - t2.id;
            });
          }
        },
        function(error) {
          $scope.solution.message = error.data.status.message;
        });

    $scope.closePopover();
  };
  
  $scope.addSelectedMasterRequest = function(selRequest) {
    console.log('adding Request: ' + selRequest.masterRequestId + 'to solution: ' + $scope.solution.id);
    Solutions.create({id: $scope.solution.id, rel: 'requests', relId: selRequest.masterRequestId }, Solutions.shallowCopy({}, $scope.solution),
        function(data) {
          if (data.status.code >= 300) {
            $scope.solution.message = data.status.message;
          }
          else {
            if (!$scope.solution.members.requests)
              $scope.solution.members.requests = [];
            $scope.solution.members.requests.push(selRequest);
            $scope.solution.members.requests.sort(function(mr1, mr2) {
              return mr1.masterRequestId - mr2.masterRequestId;
            });
          }
        },
        function(error) {
          $scope.solution.message = error.data.status.message;
        });

    $scope.closePopover();
  };
  
  $scope.addSelectedProcess = function(selProcess) {
      console.log('adding Process: ' + selProcess.id + 'to solution: ' + $scope.solution.id);
      Solutions.create({id: $scope.solution.id, rel: 'processes', relId: selProcess.id }, Solutions.shallowCopy({}, $scope.solution),
          function(data) {
            if (data.status.code >= 300) {
              $scope.solution.message = data.status.message;
            }
            else {
              if (!$scope.solution.members.processes)
                $scope.solution.members.processes = [];
              $scope.solution.members.processes.push(selProcess);
              $scope.solution.members.processes.sort(function(mr1, mr2) {
                return mr1.id - mr2.id;
              });
            }
          },
          function(error) {
            $scope.solution.message = error.data.status.message;
          });

      $scope.closePopover();
  };
  
  $scope.removeTask = function(task) {
    Solutions.remove({id: $scope.solution.id, rel: 'tasks', relId: task.id }, Solutions.shallowCopy({}, $scope.solution),
        function(data) {
          if (data.status.code >= 300) {
            $scope.solution.message = data.status.message;
          }
          else {
            $scope.solution.members.tasks.splice($scope.solution.members.tasks.indexOf(task), 1);
          }
        },
        function(error) {
          $scope.solution.message = error.data.status.message;
        });
  };
  
  $scope.removeRequest = function(request) {
    Solutions.remove({id: $scope.solution.id, rel: 'requests', relId: request.masterRequestId }, Solutions.shallowCopy({}, $scope.solution),
        function(data) {
          if (data.status.code >= 300) {
            $scope.solution.message = data.status.message;
          }
          else {
            $scope.solution.members.requests.splice($scope.solution.members.requests.indexOf(request), 1);
          }
        },
        function(error) {
          $scope.solution.message = error.data.status.message;
        });
  };
  
  $scope.removeProcess = function(process) {
      Solutions.remove({id: $scope.solution.id, rel: 'processes', relId: process.id }, Solutions.shallowCopy({}, $scope.solution),
          function(data) {
            if (data.status.code >= 300) {
              $scope.solution.message = data.status.message;
            }
            else {
              $scope.solution.members.processes.splice($scope.solution.members.processes.indexOf(process), 1);
            }
          },
          function(error) {
            $scope.solution.message = error.data.status.message;
          });
  };
  $scope.setAdvance = function(advance) {
      $scope.advance = advance;
  };
  $scope.attribute = {
        name: '', 
        value: ''
    };
  $scope.addAttribute = function () {
      if ($scope.solution.values === undefined) {
        $scope.solution.values = {};
      }

    $scope.solution.values[$scope.attribute.name] = $scope.attribute.value;
    
    $scope.attribute = {
        name: '', 
        value: ''
    };
  };
  $scope.del = function(attrName){
    var msg = 'Proceed with deleting ' + attrName + ' attribute!';
    uiUtil.confirm('Confirm Attribute Delete', msg, function(res) {
      if (res) {
        delete  $scope.solution.values[attrName];
         console.log('deleted attribute ' + attrName);
      }
    });
  };  
}]);

solutionMod.factory('Solutions', ['$resource', 'mdw', function($resource, mdw) {
  return angular.extend({}, $resource(mdw.roots.services + '/Services/Solutions/:id/:rel/:relId', mdw.serviceParams(), {
    find: { method: 'GET', isArray: false },
    create: { method: 'POST'},
    update: { method: 'PUT' },
    remove: { method: 'DELETE' }
  }), {
    shallowCopy: function(destSoln, srcSoln) {
      destSoln.id = srcSoln.id;
      destSoln.name = srcSoln.name;
      destSoln.description = srcSoln.description;
      destSoln.ownerType = srcSoln.ownerType;
      destSoln.ownerId = srcSoln.ownerId;
      destSoln.created = srcSoln.created;
      destSoln.createdBy = srcSoln.createdBy;
      destSoln.modified = srcSoln.modified;
      destSoln.modifiedBy = srcSoln.modifiedBy;
      destSoln.values = {};
      for (var val in srcSoln.values) {
        if (srcSoln.values.hasOwnProperty(val))
          destSoln.values[val] = srcSoln.values[val];
      }
      return destSoln;
    }
  });
}]);
