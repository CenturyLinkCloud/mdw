'use strict';

var requestMod = angular.module('requests', ['mdw']);

requestMod.controller('RequestsController', ['$scope', '$http', 'mdw', 'util', 'REQUEST_STATUSES',
                                             function($scope, $http, mdw, util, REQUEST_STATUSES) {

  // two-way bound to/from directive
  $scope.requestList = {};
  $scope.requestFilter = { 
      master: true,
      status: '[Active]',
      sort: 'receivedDate',
      type: 'masterRequests',
      descending: true
  }; 

$scope.requestTypes = {
      masterRequests: 'Master Requests', 
      inboundRequests: 'Inbound', 
      outboundRequests: 'Outbound'
  };
  
  $scope.requestType = 'masterRequests';
  $scope.setRequestType = function(requestType) {
    $scope.requestType = requestType;
    if (requestType !== 'masterRequests')
      $scope.requestFilter.master = false;
    $scope.requestFilter.type = requestType;
  };
 
  
  // pseudo-status [Active] means non-final
  $scope.allStatuses = ['[Active]'].concat(REQUEST_STATUSES);  
  
  $scope.$on('page-retrieved', function(event, requestList) {
    // received date and end date, adjusted for db offset
    var dbDate = new Date(requestList.retrieveDate);
    requestList.requests.forEach(function(requestInstances) {
      requestInstances.receivedDate = util.formatDateTime(util.correctDbDate(new Date(requestInstances.receivedDate), dbDate));
      if (requestInstances.endDate)
        requestInstances.endDate = util.formatDateTime(util.correctDbDate(new Date(requestInstances.endDate), dbDate));
    });
  });   
  
  
  $scope.typeaheadMatchSelection = null;
  
  // instanceId, masterRequestId, processName, packageName
  $scope.findTypeaheadMatches = function(typed) {
    return $http.get(mdw.roots.services + '/services/Requests' + '?app=mdw-admin&find=' + typed).then(function(response) {
      // service matches on instanceId or masterRequestId
      var procInsts = response.data.requestInstances;
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
        return $http.get(mdw.roots.services + '/services/Requests/definitions' + '?app=mdw-admin&find=' + typed).then(function(response) {
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
    if ($scope.requestFilter.instanceId)
      $scope.requestFilter.instanceId = null;
    if ($scope.requestFilter.masterRequestId)
      $scope.requestFilter.masterRequestId = null;
    if ($scope.requestFilter.processId)
      $scope.requestFilter.processId = null;
  };
  
  $scope.typeaheadChange = function() {
    if ($scope.typeaheadMatchSelection === null)
      $scope.clearTypeaheadFilters();
  };
  
  $scope.typeaheadSelect = function() {
    $scope.clearTypeaheadFilters();
    if ($scope.typeaheadMatchSelection.id)
      $scope.requestFilter[$scope.typeaheadMatchSelection.type] = $scope.typeaheadMatchSelection.id;
    else
      $scope.requestFilter[$scope.typeaheadMatchSelection.type] = $scope.typeaheadMatchSelection.value;
  };
  
}]);

requestMod.controller('WorkflowRequestController', ['$scope', '$route', '$routeParams', 'mdw', 'Request',
                                                    function($scope, $route, $routeParams, mdw, Request) {
  $scope.request = Request.retrieve({requestId: $routeParams.requestId}, function() {
    var trimmed = $scope.request.content.trim();
    if (trimmed.startsWith('{'))
      $scope.request.format = 'json';
    else if (trimmed.startsWith('<'))
      $scope.request.format = 'xml';
    });
}]);

requestMod.factory('Request', ['$resource', 'mdw', function($resource, mdw) {
  return $resource(mdw.roots.services + '/services/Requests/:requestId', mdw.serviceParams(), {
    retrieve: { method: 'GET', isArray: false }
  });
}]);
        


