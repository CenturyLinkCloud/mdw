'use strict';

var processMod = angular.module('processes', ['mdw']);

processMod.controller('ProcessesController', 
    ['$scope', '$http', '$cookieStore', 'mdw', 'util', 'PROCESS_STATUSES',
    function($scope, $http, $cookieStore, mdw, util, PROCESS_STATUSES) {
      
  // two-way bound to/from directive
  $scope.processList = {};
  
  $scope.selectedChart=$cookieStore.get('selectedChart');
  $scope.processFilter = $cookieStore.get('processFilter');
  if (!$scope.processFilter) {
      $scope.processFilter = { 
        master: true,
        status: '[Active]',
        sort: 'startDate',
        descending: true
    };
  }
  else {
    // don't remember these
    $scope.processFilter.instanceId = null;
    $scope.processFilter.masterRequestId = null;
    // fix date format stored in cookieStore
    if ($scope.processFilter.startDate)
      $scope.processFilter.startDate = util.serviceDate(new Date($scope.processFilter.startDate));
    $cookieStore.remove('selectedChart');
  }
  
  // pseudo-status [Active] means non-final
  $scope.allStatuses = ['[Active]','[Any]'].concat(PROCESS_STATUSES);
  
  $scope.setSelectedChart=function(selChart) {
    $scope.selectedChart= selChart;
    $cookieStore.put('selectedChart',$scope.selectedChart);
    if (selChart ==='List') {
      window.location.href='#/workflow/processes';
    }
    else {     
      window.location.href='#/dashboard/processes?chart='+selChart;
    }    
  };
  
  // preselected procDef
  if ($scope.processFilter.processId) {
    $scope.typeaheadMatchSelection = $cookieStore.get('processSpec');
  }
  else {
    $cookieStore.remove('processSpec'); 
  }
  
  $scope.$on('page-retrieved', function(event, processList) {
  $cookieStore.remove('selectedChart');
    // start date and end date, adjusted for db offset
    var dbDate = new Date(processList.retrieveDate);
    processList.processInstances.forEach(function(processInstance) {
      processInstance.startDate = util.formatDateTime(util.correctDbDate(new Date(processInstance.startDate), dbDate));
      if (processInstance.endDate)
        processInstance.endDate = util.formatDateTime(util.correctDbDate(new Date(processInstance.endDate), dbDate));
    });
    if ($scope.processFilter.processId) {
      if (processList.processInstances.length > 0) {
        let procSpec = processList.processInstances[0].processName;
        if (processList.processInstances[0].processVersion)
          procSpec += ' v' + processList.processInstances[0].processVersion;
        $cookieStore.put('processSpec', procSpec);
      }
      else {
        $http.get(mdw.roots.services + '/services/Workflow?id=' + $scope.processFilter.processId + '&summary=true&app=mdw-admin')
          .then(function(response) {
            $cookieStore.put('processSpec', response.data.name + ' v' + response.data.version);
          }
        );
      }
    }
    else {
      $cookieStore.remove('processSpec');
    }
    $cookieStore.put('processFilter', $scope.processFilter);
  });
  
  // instanceId, masterRequestId, processName, packageName
  $scope.findTypeaheadMatches = function(typed) {
    return $http.get(mdw.roots.services + '/services/Processes' + '?app=mdw-admin&find=' + typed).then(function(response) {
      // service matches on instanceId or masterRequestId
      var procInsts = response.data.processInstances;
      if (procInsts.length > 0) {
        var matches = [];
        procInsts.forEach(function(procInst) {
          if (procInst.id.toString().startsWith(typed)) {
            var existProcInst = matches.find(function(match) {
              return match.type === 'instanceId' && match.value === procInst.id.toString();
            });
            if (!existProcInst)
              matches.push({type: 'instanceId', value: procInst.id.toString()});
          }
          if (procInst.masterRequestId.startsWith(typed)) {
            var existMrId = matches.find(function(match) {
              return match.type === 'masterRequestId' && match.value === procInst.masterRequestId;
            });
            if (!existMrId)
              matches.push({type: 'masterRequestId', value: procInst.masterRequestId});
          }
        });
        return matches;
      }
      else {
        return $http.get(mdw.roots.services + '/services/Processes/definitions' + '?app=mdw-admin&find=' + typed).then(function(response) {
          // services matches on process or package name
          if (response.data.length > 0) {
            var matches2 = [];
            response.data.forEach(function(procDef) {
              if (typed.indexOf('.') > 0) {
                matches2.push({type: 'processId', value: procDef.packageName + '/' + procDef.name + ' v' + procDef.version, id: procDef.processId});
              }
              else {
                matches2.push({type: 'processId', value: procDef.name + ' v' + procDef.version, id: procDef.processId});
              }
            });
            return matches2;
          }
        });                     
      }
    });
  };
  
  $scope.clearTypeaheadFilters = function() {
    // check if defined to avoid triggering evaluation
    if ($scope.processFilter.instanceId)
      $scope.processFilter.instanceId = null;
    if ($scope.processFilter.masterRequestId)
      $scope.processFilter.masterRequestId = null;
    if ($scope.processFilter.processId)
      $scope.processFilter.processId = null;
  };
  
  $scope.typeaheadChange = function() {
    if ($scope.typeaheadMatchSelection === null)
      $scope.clearTypeaheadFilters();
  };
  
  $scope.typeaheadSelect = function() {
    $scope.clearTypeaheadFilters();
    if ($scope.typeaheadMatchSelection.id)
      $scope.processFilter[$scope.typeaheadMatchSelection.type] = $scope.typeaheadMatchSelection.id;
    else
      $scope.processFilter[$scope.typeaheadMatchSelection.type] = $scope.typeaheadMatchSelection.value;
  };
  
}]);

processMod.controller('ProcessController', 
    ['$scope', '$route', '$routeParams', '$filter', 'mdw', 'util', 'Process', 'ProcessSummary', 'DOCUMENT_TYPES', 'WORKFLOW_STATUSES', 'ProcessHierarchy',
     function($scope, $route, $routeParams, $filter, mdw, util, Process, ProcessSummary, DOCUMENT_TYPES, WORKFLOW_STATUSES, ProcessHierarchy) {
  
  $scope.activity = util.urlParams().activity; // (will be highlighted in rendering)
  
  $scope.retrieveProcess = function() {
    if ($routeParams.triggerId) {
      $scope.process = Process.retrieve({triggerId: $routeParams.triggerId}, function() {
        ProcessSummary.set($scope.process);
      });    
    }
    else {
      $scope.process = Process.retrieve({instanceId: $routeParams.instanceId, extra: 'summary'}, function() {
        ProcessSummary.set($scope.process);
      });    
    }
  };
    $scope.workflowStatuses = WORKFLOW_STATUSES;
    
    console.log('$routeParams.triggerId='+$routeParams.triggerId+ '$routeParams.instanceId='+$routeParams.instanceId);
    var instancesList = ProcessHierarchy.retrieve({}, function(response) {
    var hierarchies= instancesList.rawResponse; 
    instancesList= util.jsonToArray(hierarchies);
    $scope.instances =JSON.parse(instancesList);
  }); 
  $scope.valuesEdit = false;
  $scope.editValues = function(edit) {
    $scope.valuesEdit = edit;
    if ($scope.valuesEdit)
      $scope.values = $scope.allValues;
    else
      $scope.values = $scope.populatedValues;
  };
  $scope.saveValues = function() {
    console.log('saving process values for instance: ' + $scope.process.id);
    var newValues = {};
    util.getProperties($scope.values).forEach(function(name) {
      var value = $scope.values[name];
      if (value.dirty) {
        if (value.type === 'java.util.Date' && value.value) {
            var timezoneAbbr = 'MST'; // TODO
            newValues[value.name] = $filter('date')(value.value, 'EEE MMM dd HH:mm:ss ') + timezoneAbbr + $filter('date')(value.value, ' yyyy');
        }    
        else {
          newValues[value.name] = value.value === null ? '' : value.value;
        }
      }
    });
    Process.update({instanceId: $scope.process.id, extra: 'values'}, newValues,
      function(data) {
        if (data.status.code !== 0) {
          mdw.messages = data.status.message;
        }
        else {
          $route.reload();
        }
      }, 
      function(error) {
        mdw.messages = error.data.status.message;
      }
    );
  };
  
  if ($route.current.originalPath.endsWith('/values') || $routeParams.name) {
    if ($routeParams.name) {
      $scope.value = Process.retrieve({instanceId: $routeParams.instanceId, extra: 'values', valueName: $routeParams.name}, function() {
        $scope.value.name = $routeParams.name;
        if ($scope.value.type) {
          $scope.value.format = DOCUMENT_TYPES[$scope.value.type];
          if ($scope.value.type === 'java.lang.Exception')
            $scope.value.view = 'exception';
        }
      });
    }
    else {
      $scope.allValues = Process.retrieve({instanceId: $routeParams.instanceId, extra: 'values', includeEmpty: 'true'}, function() {
        $scope.populatedValues = {};
        var emptyValues = {};
        util.getProperties($scope.allValues).forEach(function(name) {
          var value = $scope.allValues[name];
          if (value.value)
            $scope.populatedValues[name] = value;
          else
            emptyValues[name] = value;
        });
        $scope.values = $scope.populatedValues;
      });
    }
    
    $scope.process = ProcessSummary.get();
    if (!$scope.process) {
      $scope.retrieveProcess();
    }
  }
  else {
    $scope.retrieveProcess();
  }
  
  $scope.isException = function(value) {
    return value.type == 'java.lang.Exception';
  };
  
  $scope.asException = function(value) {
    return util.asException(value);
  };
  
}]);

processMod.factory('Process', ['$resource', 'mdw', function($resource, mdw) {
  return $resource(mdw.roots.services + '/Services/Processes/:instanceId/:extra/:valueName', mdw.serviceParams(), {
    retrieve: { method: 'GET', isArray: false },
    update: {method: 'PUT'}
  });
}]);

processMod.controller('ProcessDefsController', ['$scope', '$cookieStore', 'mdw', 'util', 'ProcessDef',
                                               function($scope, $cookieStore, mdw, util, ProcessDef) {
  $scope.definitionList = ProcessDef.retrieve({}, function success() {
    var pkgs = $scope.definitionList.packages;
    pkgs.forEach(function(pkg) {
      pkg.assets.forEach(function(a) {
        a.baseName = a.name.substring(0, a.name.lastIndexOf('.'));
      });
      $scope.applyPkgCollapsedState();
    });
  });
  
  // TODO: copied from tests.js, consider refactoring
  $scope.collapse = function(pkg) {
    pkg.collapsed = true;
    $scope.savePkgCollapsedState();
  };
  $scope.collapseAll = function() {
    $scope.definitionList.packages.forEach(function(pkg) {
      pkg.collapsed = true;
    });
    $scope.savePkgCollapsedState();
  };
  $scope.expand = function(pkg) {
    pkg.collapsed = false;
    $scope.savePkgCollapsedState();
  };
  $scope.expandAll = function() {
    $scope.definitionList.packages.forEach(function(pkg) {
      pkg.collapsed = false;
    });
    $scope.savePkgCollapsedState();
  };
  $scope.savePkgCollapsedState = function() {
    var st = {};
    $scope.definitionList.packages.forEach(function(pkg) {
      if (pkg.collapsed)
        st[pkg.name] = true;
    });
    $cookieStore.put('procsPkgCollapsedState', st);
  };
  $scope.applyPkgCollapsedState = function() {
    var st = $cookieStore.get('procsPkgCollapsedState');
    if (st) {
      util.getProperties(st).forEach(function(pkgName) {
        var col = st[pkgName];
        if (col === true) {
          for (var i = 0; i < $scope.definitionList.packages.length; i++) {
            if (pkgName == $scope.definitionList.packages[i].name) {
              $scope.definitionList.packages[i].collapsed = true;
              break;
            }
          }
        }
      });
    }
  };
}]);

processMod.controller('ProcessDefController', 
    ['$scope', '$routeParams', '$route', '$location', '$filter', '$cookieStore', 'mdw', 'util', 'ProcessDef', 'ProcessSummary',
    function($scope, $routeParams, $route, $location, $filter, $cookieStore, mdw, util, ProcessDef, ProcessSummary) {
      
  $scope.activity = util.urlParams().activity; // (will be highlighted in rendering)
      
  $scope.process = { 
    packageName: $routeParams.packageName,
    name: $routeParams.processName,
    version: $routeParams.version
  };
  $scope.definitionId = null;  // stored at scope level due to vagaries in $scope.process 
  
  $scope.setProcessFilter = function() {
    var procFilter = $cookieStore.get('processFilter');
    if (!procFilter)
      procFilter = {};
    procFilter.processId = $scope.definitionId;
    if (procFilter.processId) {
      var procSpec = $scope.process.name;
      if ($scope.process.version)
        procSpec += ' v' + $scope.process.version;
      else if ($routeParams.version)
        procSpec += ' v' + $routeParams.version;
      procFilter.master = false;
      procFilter.status = null;
      $cookieStore.put('processFilter', procFilter);
      $cookieStore.put('processSpec', procSpec);
    }
  };
  
  var summary = ProcessSummary.get();
  if (summary) {
    $scope.process.id = summary.id;
    $scope.process.masterRequestId = summary.masterRequestId;
    $scope.process.definitionId = summary.definitionId;
    $scope.process.archived = summary.archived;
    if ($scope.process.archived)
      $scope.process.version = summary.version;
    $scope.definitionId = $scope.process.definitionId;
  }
  else {
    var defSum = ProcessDef.retrieve({packageName: $scope.process.packageName, processName: $scope.process.name, processVersion: $scope.process.version, summary: true}, function() {
        $scope.process.definitionId = defSum.id;
        $scope.definitionId = $scope.process.definitionId;
    });
  }
}]);

// retains state for nav
processMod.factory('ProcessSummary', ['mdw', function(mdw) {
  return {
    set: function(process) {
      this.process = process;
    },
    get: function() {
      return this.process;
    }
  };
}]);

processMod.factory('ProcessDef', ['$resource', 'mdw', function($resource, mdw) {
  return $resource(mdw.roots.services + '/Services/Workflow/:packageName/:processName/:processVersion', mdw.serviceParams(), {
    retrieve: { method: 'GET', isArray: false }
  });
}]);

processMod.factory('ProcessHierarchy', ['$resource', 'mdw', '$routeParams',  function($resource, mdw, $routeParams) {
  return $resource(mdw.roots.services + '/Services/Processes/?mdw-app=designer&app=mdw-admin&callHierarchyFor='+$routeParams.instanceId, mdw.serviceParams(), {
    retrieve: { method: 'GET', transformResponse: function(data, headers) {
      var instanceData = {
          rawResponse: data
      };
      return instanceData; 
      }
    }
  });
}]);


processMod.directive('mdwInstanceTree', function() {
  return {
    restrict: 'E', 
    replace: true, //replace <mdwInstanceTree> by the whole template
    scope: {
      data: '=src' 
    },    
    template: '<ul><mdw-tree-branch ng-repeat="c in data.children" src="c"></mdw-tree-branch></ul>',
   };
});

processMod.directive('mdwTreeBranch', function($compile) {
  return {
    restrict: 'E', 
    replace: true, // replace <mdwTreeBranch> by the whole template
    scope: {
      node: '=src'  
    },    
    template: '<li><a href="#/workflow/processes/{{node.processInstance.id}}">{{ node.processInstance.processName }} {{node.processInstance.processVersion }} ({{node.processInstance.id}})</a></li>',
    link: function(scope, element, attrs) {
    var hasChildren = angular.isArray(scope.node.children);   
     if (hasChildren) {        
       element.append('<mdw-instance-tree src="node"></mdw-instance-tree>');    
        // recompile Angular due to manual appending
        $compile(element.contents())(scope); 
      }     
      element.on('click', function(event) {
        event.stopPropagation();          
          if (hasChildren) {
            element.toggleClass('collapsed');
          }
      });      
    }
  };
});
