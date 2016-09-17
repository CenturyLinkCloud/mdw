// Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
'use strict';

var workflowMod = angular.module('mdwWorkflow', ['mdw']);

workflowMod.controller('MdwWorkflowController', ['$scope', '$http', '$routeParams', 'mdw', 'util', 'Workflow',
                                             function($scope, $http, $routeParams, mdw, util, Workflow) {
  
  $scope.init = function(canvas) {
    $scope.canvas = canvas;
    $scope.renderProcess();
  };
  
  $scope.renderProcess = function() {
    var packageName = $scope.process.packageName;
    $scope.process = Workflow.retrieve({pkg: packageName, process: $scope.process.name}, function() {
      $scope.process.packageName = packageName;  // not returned in JSON

      // draw something
      var c = $scope.canvas[0];
      var ctx = c.getContext("2d");
      ctx.moveTo(0,0);
      ctx.lineTo(200,100);
      ctx.stroke();      
      
    });
  };
  
}]);

workflowMod.factory('Workflow', ['$resource', 'mdw', function($resource, mdw) {
  return $resource(mdw.roots.services + '/services/Workflow/:pkg/:process', mdw.serviceParams(), {
    retrieve: { method: 'GET', isArray: false }
  });
}]);

// packageName and name must be populated on model object
// version is optional
workflowMod.directive('mdwWorkflow', function() {
  return {
    restrict: 'A',
    templateUrl: 'ui/list.html',
    scope: {
      process: '=mdwWorkflow'
    },
    controller: 'MdwWorkflowController',
    controllerAs: 'mdwWorkflow',
    link: function link(scope, elem, attrs, ctrls) {
      scope.init(elem);
    }
  };
});