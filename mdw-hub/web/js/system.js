// Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
'use strict';

var sysMod = angular.module('system', ['ngResource', 'mdw']);

sysMod.controller('SystemController', ['$scope', '$routeParams', 'mdw', 'System',
                                        function($scope, $routeParams, mdw, System) {
  
  $scope.sysInfoType = $routeParams.sysInfoType;
  if (typeof $scope.sysInfoType === 'undefined') {
    $scope.sysInfoType = 'System';
  }
  $scope.sysInfoCategories = System.get({sysInfoType: $scope.sysInfoType});
  $scope.filepanelUrl = mdw.roots.webTools + '/system/filepanel/index.jsf?user=' + $scope.authUser.cuid;
  
}]);

sysMod.factory('System', ['$resource', 'mdw', function($resource, mdw) {
  return $resource(mdw.roots.services + '/Services/System/:sysInfoType', mdw.serviceParams(), {
    get: { method: 'GET', isArray: true }
  });  
}]);