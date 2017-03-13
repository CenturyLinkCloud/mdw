// Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
'use strict';

var processMod = angular.module('processes', ['mdw']);

processMod.controller('ProcessesController', ['$scope', '$http', '$routeParams', 'mdw', 'util', 'PROCESS_STATUSES',
                                              function($scope, $http, $routeParams, mdw, util, PROCESS_STATUSES) {
  // two-way bound to/from directive
  $scope.processList = {};
  $scope.processFilter = { 
      master: true,
      status: '[Active]',
      sort: 'startDate',
      descending: true
  };
  
  // pseudo-status [Active] means non-final
  $scope.allStatuses = ['[Active]'].concat(PROCESS_STATUSES);
  
  // preselected procDef
  if ($routeParams.procPkg && $routeParams.proc) {
    $scope.processFilter.master = false;
    $scope.processFilter.status = null;
    var workflowSpec = $routeParams.procPkg + '/' + $routeParams.proc;
    $scope.typeaheadMatchSelection = $routeParams.proc;
    if ($routeParams.procVer) {
      workflowSpec += ' v' + $routeParams.procVer;
      $scope.processFilter.definition = workflowSpec;
      $scope.typeaheadMatchSelection += ' v' + $routeParams.procVer;
    }
    else {
      $scope.typeaheadMatchSelection = null;
    }
  }
    
  
  $scope.$on('page-retrieved', function(event, processList) {
    // start date and end date, adjusted for db offset
    var dbDate = new Date(processList.retrieveDate);
    processList.processInstances.forEach(function(processInstance) {
      processInstance.startDate = util.formatDateTime(util.correctDbDate(new Date(processInstance.startDate), dbDate));
      if (processInstance.endDate)
        processInstance.endDate = util.formatDateTime(util.correctDbDate(new Date(processInstance.endDate), dbDate));
    });
  });  
  
  // instanceId, masterRequestId, processName, packageName
  $scope.findTypeaheadMatches = function(typed) {
    return $http.get(mdw.roots.services + '/services/Processes' + '?app=mdw-admin&find=' + typed).then(function(response) {
      // service matches on instanceId or masterRequestId
      var procInsts = response.data.processInstances;
      if (procInsts.length > 0) {
        var matches = [];
        procInsts.forEach(function(procInst) {
          if (procInst.id.toString().startsWith(typed))
            matches.push({type: 'instanceId', value: procInst.id.toString()});
          else
            matches.push({type: 'masterRequestId', value: procInst.masterRequestId});
        });
        return matches;
      }
      else {
        return $http.get(mdw.roots.services + '/services/Processes/definitions' + '?app=mdw-admin&find=' + typed).then(function(response) {
          // services matches on process or package name
          if (response.data.length > 0) {
            var matches2 = [];
            response.data.forEach(function(procDef) {
              if (typed.indexOf('.') > 0)
                matches2.push({type: 'processId', value: procDef.packageName + '/' + procDef.name + ' v' + procDef.version, id: procDef.processId});
              else
                matches2.push({type: 'processId', value: procDef.name + ' v' + procDef.version, id: procDef.processId});
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
    ['$scope', '$route', '$routeParams', '$filter', 'mdw', 'util', 'Process', 'ProcessSummary', 'DOCUMENT_TYPES', 'WORKFLOW_STATUSES',
     function($scope, $route, $routeParams, $filter, mdw, util, Process, ProcessSummary, DOCUMENT_TYPES, WORKFLOW_STATUSES) {
  
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
  
  $scope.valuesEdit = false;
  $scope.editValues = function(edit) {
    $scope.valuesEdit = edit;
  };
  $scope.saveValues = function() {
    console.log('saving process values for instance: ' + $scope.process.id);
    var newValues = {};
    util.getProperties($scope.values).forEach(function(name) {
      var value = $scope.values[name];
      if (value.dirty && value.value) {
        if (value.type === 'java.util.Date') {
          var timezoneAbbr = 'MST'; // TODO
          newValues[value.name] = $filter('date')(value.value, 'EEE MMM dd HH:mm:ss ') + timezoneAbbr + $filter('date')(value.value, ' yyyy');
        }    
        else {
          newValues[value.name] = value.value;
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
      $scope.values = Process.retrieve({instanceId: $routeParams.instanceId, extra: 'values'});
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
    if (!$scope.exception)
      $scope.exception = '';
    
  };
}]);

processMod.factory('Process', ['$resource', 'mdw', function($resource, mdw) {
  return $resource(mdw.roots.services + '/Services/Processes/:instanceId/:extra/:valueName', mdw.serviceParams(), {
    retrieve: { method: 'GET', isArray: false },
    update: {method: 'PUT'}
  });
}]);

processMod.controller('ProcessDefsController', ['$scope', '$cookieStore', 'mdw', 'util', 'Assets',
                                               function($scope, $cookieStore, mdw, util, Assets) {
  $scope.definitionList = Assets.get({extension: 'proc'}, function success() {
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

processMod.controller('ProcessDefController', ['$scope', '$routeParams', '$route', '$filter', 'mdw', 'util', 'ProcessDef', 'ProcessSummary', 'ProcessRun',
                                              function($scope, $routeParams, $route, $filter, mdw, util, ProcessDef, ProcessSummary, ProcessRun) {
  $scope.process = { 
    packageName: $routeParams.packageName,
    name: $routeParams.processName,
    version: $routeParams.version
  };
  
  $scope.isRun = $route.current.loadedTemplateUrl === 'workflow/run.html';
  
  $scope.runProcess = function() {
    console.log('running process: ' + $scope.run.definitionId);
    var inValues = {};
    util.getProperties($scope.run.values).forEach(function(name) {
      var value = $scope.run.values[name];
      if (value.value) {
        if (value.type === 'java.util.Date') {
          var timezoneAbbr = 'MST'; // TODO
          value.value = $filter('date')(value.value, 'EEE MMM dd HH:mm:ss ') + timezoneAbbr + $filter('date')(value.value, ' yyyy');
        }    
        else {
          value.value = value.value;
        }
        inValues[name] = value;
      }
    });
    var toRun = { masterRequestId: $scope.run.masterRequestId, definitionId: $scope.run.definitionId, values: inValues};
    ProcessRun.run({definitionId: $scope.run.definitionId}, toRun,
      function(data) {
        if (data.status && data.status.code !== 0) {
          mdw.messages = data.status.message;
        }
      }, 
      function(error) {
        mdw.messages = error.data.status.message;
      }
    );
    // don't wait before going to live view
  };
  
  var summary = ProcessSummary.get();
  if (summary) {
    $scope.process.id = summary.id;
    $scope.process.masterRequestId = summary.masterRequestId;
    $scope.process.definitionId = summary.definitionId;
    if ($scope.isRun)
      $scope.run = ProcessRun.retrieve({definitionId: $scope.process.definitionId});
  }
  else {
    var defSum = ProcessDef.retrieve({packageName: $scope.process.packageName, processName: $scope.process.name, processVersion: $scope.process.version, summary: true}, function() {
        $scope.process.definitionId = defSum.id;
        if ($scope.isRun)
          $scope.run = ProcessRun.retrieve({definitionId: $scope.process.definitionId});
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

processMod.factory('ProcessRun', ['$resource', 'mdw', function($resource, mdw) {
  return $resource(mdw.roots.services + '/Services/Processes/run/:definitionId', mdw.serviceParams(), {
    retrieve: { method: 'GET', isArray: false },
    run: {method: 'POST'}
  });
}]);
