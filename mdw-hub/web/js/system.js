// Copyright (c) 2017 CenturyLink, Inc. All Rights Reserved.
'use strict';

var sysMod = angular.module('system', ['ngResource', 'mdw']);

sysMod.controller('SystemController', ['$scope', '$routeParams', 'WorkflowCache', 'mdw', 'System',
                                        function($scope, $routeParams, WorkflowCache, mdw, System) {
  
  $scope.sysInfoType = $routeParams.sysInfoType;
  if (typeof $scope.sysInfoType === 'undefined') {
    $scope.sysInfoType = 'System';
  }
  $scope.sysInfoCategories = System.get({sysInfoType: $scope.sysInfoType});
  $scope.filepanelUrl = mdw.roots.webTools + '/system/filepanel/index.jsf?user=' + $scope.authUser.cuid;
  
  $scope.cacheRefresh = function(refreshType) {
    // leave cache error logging to the server side
      WorkflowCache.refresh({}, { distributed: refreshType});
    $location.path('/packages');
  };
}]);

sysMod.factory('System', ['$resource', 'mdw', function($resource, mdw) {
  return $resource(mdw.roots.services + '/Services/System/:sysInfoType', mdw.serviceParams(), {
    get: { method: 'GET', isArray: true }
  });  
}]);
