// Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
'use strict';

var processMod = angular.module('processes', ['mdw']);

processMod.controller('ProcessesController', ['$scope', '$http', 'mdw', 'util', 'PROCESS_STATUSES',
                                              function($scope, $http, mdw, util, PROCESS_STATUSES) {
  
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
  
  $scope.$on('page-retrieved', function(event, processList) {
    // start date and end date, adjusted for db offset
    var dbDate = new Date(processList.retrieveDate);
    processList.processInstances.forEach(function(processInstance) {
      processInstance.startDate = util.formatDateTime(util.correctDbDate(new Date(processInstance.startDate), dbDate));
      if (processInstance.endDate)
        processInstance.endDate = util.formatDateTime(util.correctDbDate(new Date(processInstance.endDate), dbDate));
    });
  });  
  
  $scope.typeaheadMatchSelection = null;
  
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

processMod.controller('ProcessController', ['$scope', '$route', '$routeParams', 'mdw', 'Process',
                                            function($scope, $route, $routeParams, mdw, Process) {
    
  $scope.process = Process.retrieve({instanceId: $routeParams.instanceId}, function() {
    $scope.process.name = $scope.process.processName;
    $scope.item = $scope.process; // for processItem template
  });
   
  $scope.refreshWorkflowImage = function() {
    $route.reload();
  };   
    
}]);

processMod.factory('Process', ['$resource', 'mdw', function($resource, mdw) {
  return $resource(mdw.roots.services + '/Services/Processes/:instanceId', mdw.serviceParams(), {
    retrieve: { method: 'GET', isArray: false }
  });
}]);

processMod.controller('ProcessDefController', ['$scope', '$routeParams', 'mdw',
                                            function($scope, $routeParams, mdw) {
    
  $scope.processDef = { 
    packageName: $routeParams.packageName,
    name: $routeParams.processName,
    version: $routeParams.version
  };    
}]);
