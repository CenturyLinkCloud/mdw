// Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
'use strict';

var inspectMod = angular.module('mdwInspector', ['mdw']);

inspectMod.controller('MdwInspectorController', ['$scope', 'mdw', 'util', 'Inspector', 'InspectorTabs',
                                                 function($scope, mdw, util, Inspector, InspectorTabs) {
  
  $scope.setWorkflow = function(type, workflowObj) {
    $scope.workflowType = type;
    $scope.workflowObject = workflowObj;
    
    $scope.tabs = InspectorTabs.definition[$scope.workflowType];
    $scope.setActiveTab('Definition');
    
  };
  
  $scope.setActiveTab = function(tabName) {
    $scope.activeTab = $scope.tabs[tabName];
    $scope.activeTabValues = {};
    if (typeof $scope.activeTab === 'object') {
      for (var prop in $scope.activeTab) {
        if ($scope.activeTab.hasOwnProperty(prop)) {
          $scope.activeTabValues[prop] = $scope.workflowObject[$scope.activeTab[prop]];
        }
      }
    }
    else {
      var tabObj = $scope.workflowObject[$scope.activeTab];
      for (var prop in tabObj) {
        if (tabObj.hasOwnProperty(prop)) {
          if (InspectorTabs.exclusions.indexOf(prop) == -1) {
            if (prop == 'Rule')
              $scope.activeTabValues['Script'] = '<link>';
            else if (prop == 'SCRIPT')
              $scope.activeTabValues['Language'] = tabObj[prop];
            else
              $scope.activeTabValues[prop] = tabObj[prop];
          }
        }
      }
    }
  };
  
  
}]);

inspectMod.factory('Inspector', ['mdw', 'util', function(mdw, util) {
  return {
    setObj: function(obj) {
      this.obj = obj;
      if (this.listener) {
        this.listener(obj);
      }
    },
    getObj: function() {
      return this.obj;
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
      
      var workflowElem = elem.parent();
      var canvasElem = angular.element(workflowElem[0].getElementsByClassName('mdw-canvas'));
      var panelElem = angular.element(elem[0].getElementsByClassName('mdw-inspector-panel'));

      scope.closeInspector = function() {
        elem[0].style.display = 'none';
        workflowElem[0].style.height = canvasElem[0].offsetHeight + 'px';
      };
      
      // show
      Inspector.listen(function(obj) {
        var obj = Inspector.getObj();
        scope.setWorkflow(obj.workflowType, obj[obj.workflowType]);
        scope.$apply();
        
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
      });
      
      scope.$on('$destroy', function() {
        Inspector.unlisten();
      });
    }
  };
}]);