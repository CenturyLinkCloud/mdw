// Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
'use strict';

var workflowMod = angular.module('mdwWorkflow', ['mdw']);

workflowMod.controller('MdwWorkflowController', 
    ['$scope', '$http', '$routeParams', 'mdw', 'util', 'Workflow', 'Diagram',
    function($scope, $http, $routeParams, mdw, util, Workflow, Diagram) {
  
  $scope.init = function(canvas) {
    $scope.canvas = canvas;
    $scope.renderProcess();
  };
  
  $scope.renderProcess = function() {
    var packageName = $scope.process.packageName;
    $scope.process = Workflow.retrieve({pkg: packageName, process: $scope.process.name}, function() {
      $scope.process.packageName = packageName;  // not returned in JSON

      $scope.diagram = new Diagram($scope.canvas[0], $scope.process);
      $scope.diagram.draw();
    });
  };
  
  $scope
  
}]);

workflowMod.factory('Workflow', ['$resource', 'mdw', function($resource, mdw) {
  return $resource(mdw.roots.services + '/services/Workflow/:pkg/:process', mdw.serviceParams(), {
    retrieve: { method: 'GET', isArray: false }
  });
}]);

workflowMod.factory('Diagram', ['mdw', 'util', function(mdw, util) {
  var Diagram = function(canvas, process, instance) {
    this.canvas = canvas;
    this.process = process;
    this.instance = instance ? instance : null;
  };
  
  Diagram.prototype.draw = function() {
    // draw something
    var ctx = this.canvas.getContext("2d");
    ctx.moveTo(0,0);
    ctx.lineTo(200,100);
    ctx.stroke();      
  };
  
  return Diagram;
  
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