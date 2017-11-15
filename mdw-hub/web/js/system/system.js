'use strict';

var sysMod = angular.module('system', ['ngResource', 'mdw']);

sysMod.controller('SystemController', ['$scope', '$routeParams', '$location', 'WorkflowCache', 'mdw', 'System', 'Assets',
                                        function($scope, $routeParams, $location, WorkflowCache, mdw, System, Assets) {

  $scope.sysInfoType = $routeParams.sysInfoType;
  if (typeof $scope.sysInfoType === 'undefined') {
    $scope.sysInfoType = 'System';
  }
  
  $scope.sysInfoCategories = System.get({sysInfoType: $scope.sysInfoType}, function() {
    $scope.defaultClassLoader = $scope.sysInfoCategories.find(function(cat) {
      return cat.name === 'System Details';
    }).sysInfos.find(function (sysInfoCat) {
      return sysInfoCat.name === 'Default ClassLoader';
    }).value;
    $scope.classLoader = $scope.defaultClassLoader;
  });
  
  $scope.packageList = Assets.get({});
  
  $scope.filepanelUrl = mdw.roots.webTools + '/system/filepanel/index.jsf?user=' + $scope.authUser.cuid;
  
  $scope.cacheRefresh = function(refreshType) {
    $scope.refreshMessage = '';
      // leave cache error logging to the server side
      WorkflowCache.refresh({}, { distributed: refreshType}).$promise.then(function success(response) {
        $scope.refreshMessage = response.status.message;
        mdw.messages = 'Cache refresh complete';
      }, function error(response) {
        if (response.data.status)
          mdw.messages = response.data.status.message;
      });
  };
  
  $scope.setClassLoader = function(classLoader) {
    $scope.classLoader = classLoader;
  };
  
  $scope.findClass = function(className) {
    $scope.classInfo = System.get({sysInfoType: 'Class', className: className, classLoader: $scope.classLoader});
  };
  
  $scope.runGit = function(gitCommand) {
    $scope.gitCommandOutput = System.get({sysInfoType: 'Git', command: gitCommand});
  };
}]);

sysMod.factory('System', ['$resource', 'mdw', function($resource, mdw) {
  return $resource(mdw.roots.services + '/Services/System/:sysInfoType', mdw.serviceParams(), {
    get: { method: 'GET', isArray: true }
  });  
}]);
