// Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
'use strict';

var inspectMod = angular.module('mdwInspector', ['mdw']);

inspectMod.factory('Inspector', ['mdw', 'util', function(mdw, util) {
  return {
    setObj: function(workflowType, workflowObj) {
      this.workflowType = workflowType;
      this.workflowObj = workflowObj;
      if (this.listener) {
        this.listener(workflowType, workflowObj);
      }
    },
    listen: function(listener) {
      this.listener = listener;
    },
    unlisten: function() {
      this.listener = null;
    }
  };
}]);

inspectMod.directive('mdwInspector', ['$window', 'Inspector', function($window, Inspector) {
  return {
    restrict: 'A',
    controller: 'MdwWorkflowController',
    link: function link(scope, elem, attrs, ctrls) {
      
      // value binding
      Inspector.listen(function(workflowType, workflowObj) {
        console.log(workflowType + ": " + JSON.stringify(workflowObj));
      });
      
      // scroll handling
      var scrollHandler = function() {
        console.log("ON SCROLL");
      };
      
      angular.element($window).bind('scroll', scrollHandler);
      
      scope.$on('$destroy', function() {
        Inspector.unlisten();
        angular.element($window).unbind('scroll', scrollHandler);
      });
    }
  };
}]);