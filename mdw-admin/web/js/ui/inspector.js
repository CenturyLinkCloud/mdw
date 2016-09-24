// Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
'use strict';

var inspectMod = angular.module('mdwInspector', ['mdw']);

inspectMod.controller('MdwInspectorController', ['$scope', 'mdw', 'util', 'Inspector', 'InspectorTabs',
                                                 function($scope, mdw, util, Inspector, InspectorTabs) {
  
  $scope.setWorkflow = function(obj) {
    $scope.diagramObject = obj;
    $scope.workflowType = obj.workflowType;
    $scope.workflowObject = obj[obj.workflowType];
    
    $scope.tabs = InspectorTabs.definition[$scope.workflowType];
    var sameActiveTab = $scope.tabs[$scope.activeTabName];
    if (sameActiveTab)
      $scope.setActiveTab($scope.activeTabName);
    else
      $scope.setActiveTab('Definition'); // instance something else
  };
  
  $scope.setActiveTab = function(tabName) {
    $scope.drilledValue = null;
    $scope.activeTabName = tabName;
    $scope.activeTab = $scope.tabs[tabName];
    $scope.activeTabValues = [];
    if (typeof $scope.activeTab === 'object') {
      for (var prop in $scope.activeTab) {
        if ($scope.activeTab.hasOwnProperty(prop)) {
          $scope.activeTabValues.push({
            name: prop,
            value: $scope.workflowObject[$scope.activeTab[prop]]
          });
        }
      }
    }
    else {
      var tabObj = $scope.workflowObject[$scope.activeTab];
      for (var prop in tabObj) {
        if (tabObj.hasOwnProperty(prop)) {
          var spec = InspectorTabs[$scope.activeTab] ? InspectorTabs[$scope.activeTab][prop] : null;
          if (!spec || !spec.exclude) {
            var val = {
              name: prop,
              value: tabObj[prop]
            };
            if (val.value.indexOf('\n') >= 0)
              val.multiline = true;
            if (spec) {
              if (spec.alias)
                val.name = spec.alias;
              // for attributes with languages
              if (spec.langAttr)
                val.language = tabObj[spec.langAttr] ? tabObj[spec.langAttr].toLowerCase() : null;
              else
                val.language = spec.language;
            }
            else {
              // asset attrs
              if (prop.endsWith("_assetVersion")) {
                val.name = prop.substring(0, prop.length - 13);
                val.asset = { 
                  path: tabObj[val.name], 
                  version: 'v' + tabObj[prop]
                };
              }
              else if (prop == 'processname') {
                val.asset = { 
                  path: val.value + '.proc', 
                  version: tabObj['processversion']
                };
              }
              else if (prop == 'processmap') {
                // TODO
              }
              if (InspectorTabs.assetAttrs.indexOf(prop) != -1) {
                val.asset = { path: tabObj[prop] };
              }
              if (val.asset && val.asset.path) {
                val.value = val.asset.path;
                if (val.asset.version)
                  val.value += ' ' + val.asset.version;
                val.asset.url = $scope.adminBase + '#/asset/' + val.asset.path;
              }
            }
            if (!tabObj[prop + "_assetVersion"])
              $scope.activeTabValues.push(val);
          }          
        }
      }
    }
  };
  
  $scope.drillIn = function(tabValue) {
    $scope.drilledValue = tabValue;
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
    setAdminBase: function(baseUrl) {
      this.adminBase = baseUrl;
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
        scope.setWorkflow(obj);
        scope.$apply();
        
        if (elem[0].style.display == 'none') {
          // set inspector left position
          elem[0].style.left = workflowElem[0].getBoundingClientRect().left + 'px';
          
          // show inspector
          elem[0].style.display = 'block';
        }
        
        // set workflow element height to accommodate inspector
        workflowElem[0].style.height = (canvasElem[0].offsetHeight + panelElem[0].offsetHeight - 50) + 'px';

        if (obj.workflowType != 'process') {
          // scroll into view
          var objBtmY = canvasElem[0].getBoundingClientRect().top + obj.display.y + obj.display.h;
          var inspTopY = elem[0].getBoundingClientRect().top;
          if (objBtmY > inspTopY)
            $window.scrollBy(0, objBtmY - inspTopY + 10);
        }
      });
      
      scope.$on('$destroy', function() {
        Inspector.unlisten();
      });
    }
  };
}]);