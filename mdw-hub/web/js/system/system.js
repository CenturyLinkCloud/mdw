// Copyright (c) 2017 CenturyLink, Inc. All Rights Reserved.
'use strict';

var sysMod = angular.module('system', ['ngResource', 'mdw']);

sysMod.controller('SystemController', ['$scope', '$routeParams', '$location', 'WorkflowCache', 'mdw', 'System',
                                        function($scope, $routeParams, $location, WorkflowCache, mdw, System) {

  $scope.sysInfoType = $routeParams.sysInfoType;
  if (typeof $scope.sysInfoType === 'undefined') {
    $scope.sysInfoType = 'System';
  }
  $scope.sysInfoCategories = System.get({sysInfoType: $scope.sysInfoType});
  $scope.filepanelUrl = mdw.roots.webTools + '/system/filepanel/index.jsf?user=' + $scope.authUser.cuid;
  
  $scope.cacheRefresh = function(refreshType) {
    $scope.refreshMessage = '';
      // leave cache error logging to the server side
      WorkflowCache.refresh({}, { distributed: refreshType}).$promise.then(function success(response) {
        $scope.refreshMessage = response.status.message;
        mdw.messages = 'Cache refresh complete'
      }, function error(error) {
        if (error.data.status)
          mdw.messages = error.data.status.message;
      });
  };
  
  $scope.findClass = function(className) {
    $scope.classInfo = System.get({sysInfoType: 'Class', className: className})
  };
}]);

sysMod.factory('System', ['$resource', 'mdw', function($resource, mdw) {
  return $resource(mdw.roots.services + '/Services/System/:sysInfoType', mdw.serviceParams(), {
    get: { method: 'GET', isArray: true }
  });  
}]);
