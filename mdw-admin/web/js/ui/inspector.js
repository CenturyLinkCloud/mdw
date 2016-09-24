// Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
'use strict';

var inspectMod = angular.module('mdwInspector', ['mdw']);

inspectMod.controller('MdwInspectorController', ['$scope', 'mdw', 'util', 'Inspector',
                                                 function($scope, mdw, util, Inspector) {
}]);

inspectMod.factory('Inspector', ['mdw', 'util', function(mdw, util) {
  return {
    setObj: function(obj) {
      this.obj = obj;
      if (this.listener) {
        this.listener(obj);
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
    controller: 'MdwInspectorController',
    link: function link(scope, elem, attrs, ctrls) {
      
      scope.closeInspector = function() {
        elem[0].style.display = 'none';
      };
      
      // value binding
      Inspector.listen(function(obj) {
        var workflowElem = elem.parent();
        var canvasElem = angular.element(workflowElem[0].getElementsByClassName('mdw-canvas'));
        var panelElem = angular.element(elem[0].getElementsByClassName('mdw-inspector-panel'));

        if (elem[0].style.display == 'none') {
          // set inspector left position
          elem[0].style.left = workflowElem[0].getBoundingClientRect().left + 'px';
          
          // show inspector
          elem[0].style.display = 'block';
        }
        
        // set workflow element height to accommodate inspector
        workflowElem[0].style.height = (canvasElem[0].offsetHeight + panelElem[0].offsetHeight - 50) + 'px';

        // scroll into view
        var objBtmY = canvasElem[0].getBoundingClientRect().top + obj.display.y + obj.display.h;
        var inspTopY = elem[0].getBoundingClientRect().top;
        if (objBtmY > inspTopY)
          $window.scrollBy(0, objBtmY - inspTopY + 10);
        
        
        // display details
        var workflowObject = obj[obj.workflowType];
        console.log(obj.workflowType + ": " + JSON.stringify(workflowObject));
      });
      
      scope.$on('$destroy', function() {
        Inspector.unlisten();
      });
    }
  };
}]);