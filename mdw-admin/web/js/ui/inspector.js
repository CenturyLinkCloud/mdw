// Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
'use strict';

var inspectMod = angular.module('mdwInspector', ['mdw']);

inspectMod.controller('MdwInspectorController', ['$scope', '$parse', 'mdw', 'util', 'Inspector', 'InspectorTabs',
                                                 function($scope, $parse, mdw, util, Inspector, InspectorTabs) {
  
  $scope.setWorkflow = function(obj) {
    $scope.diagramObject = obj;
    $scope.workflowType = obj.workflowType;
    $scope.workflowObject = obj[obj.workflowType];
    $scope.runtimeInfo = null;  // can be object or array
    
    if ($scope.workflowType == 'process')
      $scope.runtimeInfo = $scope.diagramObject.instance;
    else
      $scope.runtimeInfo = $scope.diagramObject.instances;
    
    if ($scope.runtimeInfo)
      $scope.tabs = InspectorTabs.instance[$scope.workflowType];
    else
      $scope.tabs = InspectorTabs.definition[$scope.workflowType];
    
    var filteredTabs = {};
    util.getProperties($scope.tabs).forEach(function(tabName) {
      var tab = $scope.tabs[tabName];
      if (!tab.condition || $parse(tab.condition)($scope)) {
        filteredTabs[tabName] = tab;
      }
    });
    $scope.tabs = filteredTabs;
    
    var sameActiveTab = $scope.tabs[$scope.activeTabName];
    if (sameActiveTab) {
      $scope.setActiveTab($scope.activeTabName);
    }
    else {
      // first tab
      $scope.setActiveTab(util.getProperties($scope.tabs)[0]);
    }
  };
  
  $scope.setActiveTab = function(tabName) {
    $scope.drilledValue = null;
    $scope.activeTabName = tabName;
    $scope.activeTab = $scope.tabs[tabName];
    $scope.activeTabValues = [];

    var tabInfo = $scope.runtimeInfo ? $scope.runtimeInfo : $scope.workflowObject;
    
    // check for array type
    var tabArr;
    if ($scope.activeTab instanceof Array) {
      // array is runtime obj itself (eg: instance.activity.Instances)
      tabArr = $scope.activeTab;
    }
    else if (typeof $scope.activeTab === 'object') {
      var tabProps = util.getProperties($scope.activeTab);
      if (tabProps[0]) {
        if ($scope.activeTab[tabProps[0]] instanceof Array) {
          // array is first object property (eg: instance.process.Values)
          tabArr = $scope.activeTab[tabProps[0]];
          tabInfo = tabInfo[tabProps[0]];
          if ($scope.runtimeInfo && $scope.workflowObject[tabProps[0]]) {
            // special handling in case workflowObj specifies values not in runtime
            var wfArrObj = $scope.workflowObject[tabProps[0]];
            var wfArrObjProps = util.getProperties(wfArrObj);
            wfArrObjProps.forEach(function(wfArrObjProp) {
              var found;
              for (var n = 0; n < tabInfo.length; n++) {
                if (tabInfo[n].name == wfArrObjProp) {
                  found = true;
                  break;
                }
              }
              if (!found) {
                var toAdd = wfArrObj[wfArrObjProp];
                toAdd.name = wfArrObjProp;
                tabInfo.push(toAdd);
              }
            });
          }
        }
        else if (typeof $scope.activeTab[tabProps[0]] === 'object') {
          // named object equates to array (eg: process.definition.Variables)
          var arrObj = $scope.activeTab[tabProps[0]];
          var arrObjProps = util.getProperties(arrObj);
          $scope.idProp = null;
          for (var m = 0; m < arrObjProps.length; m++) {
            if (arrObj[arrObjProps[m]] == '=') {
              $scope.idProp = arrObjProps[m];
              break;
            }
          }
          if ($scope.idProp)
            arrObj[$scope.idProp] = $scope.idProp;
          tabArr = [arrObj];
          var tabInfoObj = tabInfo[tabProps[0]];
          tabInfo = []; // to populate from object
          var tabInfoObjProps = util.getProperties(tabInfoObj);
          tabInfoObjProps.forEach(function(tabInfoObjProp) {
            tabInfo.push(tabInfoObj[tabInfoObjProp]);
            if ($scope.idProp) {
              tabInfo[tabInfo.length-1][$scope.idProp] = tabInfoObjProp;
            }
          });
        }
      }
    }
    
    if (tabArr) {
      // indicates columnar display and tabInfo is array
      var maxColWidth = 50;
      var colSpacing = 3;
      var props = util.getProperties(tabArr[0]);
      
      var colWidths = [];
      var labels = [];
      props.forEach(function(prop) {
        labels.push(prop);
        colWidths.push(prop.length);
      });

      var values = []; // 2d
      tabInfo.forEach(function(rowObj) {
        var valueRow = [];
        for (var h = 0; h < props.length; h++) {
          var value = rowObj[tabArr[0][props[h]]];
          if (!value)
            value = '';
          value = '' + value; // convert to str
          if (value.length > maxColWidth)
            value = value.substring(0, maxColWidth - 5) + '...';
          if (value.length > colWidths[h])
            colWidths[h] = value.length;
          valueRow.push(value);
        };
        values.push(valueRow);
      });

      
      // column labels
      var names = [];
      for (var k = 0; k < labels.length; k++) {
        var name = { name: labels[k]};
        if (k < labels.length - 1)
          name.pad = util.padTrailing('', colWidths[k] - labels[k].length + colSpacing);
        names.push(name);
      }
      // row values
      for (var i = 0; i < values.length; i++) {
        var fields = [];
        for (var j = 0; j < values[i].length; j++) {
          var field = {value: values[i][j] };
          if (j < values[i].length - 1)
            field.pad = util.padTrailing('', colWidths[j] - values[i][j].length + colSpacing);
          fields.push(field);
        }
        if (i == 0) // only first element needs names
          $scope.activeTabValues.push({names: names, values: fields});
        else
          $scope.activeTabValues.push({values: fields});
      }
    }
    else if (typeof $scope.activeTab === 'object') {
      // evaluate each object prop against tabInfo object
      var tabObj = tabInfo;
      for (var prop in $scope.activeTab) {
        if ($scope.activeTab.hasOwnProperty(prop)) {
          $scope.activeTabValues.push({
            name: prop,
            value: tabObj[$scope.activeTab[prop]]
          });
        }
      }
    }
    else {
      // string indicates value is a object property name on tabInfo (object)
      tabInfo = tabInfo[$scope.activeTab];
      for (var prop in tabInfo) {
        if (tabInfo.hasOwnProperty(prop)) {
          var spec = InspectorTabs[$scope.activeTab] ? InspectorTabs[$scope.activeTab][prop] : null;
          if (!spec || !spec.exclude) {
            var val = {
              name: prop,
              value: tabInfo[prop]
            };
            if (typeof val.value === 'object')
              val.value = JSON.stringify(val.value);
            if (val.value.indexOf('\n') >= 0)
              val.multiline = true;
            if (spec) {
              if (spec.alias)
                val.name = spec.alias;
              // for attributes with languages
              if (spec.langAttr)
                val.language = tabInfo[spec.langAttr] ? tabInfo[spec.langAttr].toLowerCase() : null;
              else
                val.language = spec.language;
            }
            else {
              // asset attrs
              if (prop.endsWith("_assetVersion")) {
                val.name = prop.substring(0, prop.length - 13);
                val.asset = { 
                  path: tabInfo[val.name], 
                  version: 'v' + tabInfo[prop]
                };
              }
              else if (prop == 'processname') {
                val.asset = { 
                  path: val.value + '.proc', 
                  version: tabInfo['processversion']
                };
              }
              else if (prop == 'processmap') {
                // TODO
              }
              if (InspectorTabs.assetAttrs.indexOf(prop) != -1) {
                val.asset = { path: tabInfo[prop] };
              }
              if (val.asset && val.asset.path) {
                val.value = val.asset.path;
                if (val.asset.version)
                  val.value += ' ' + val.asset.version;
                val.asset.url = $scope.adminBase + '#/asset/' + val.asset.path;
              }
            }
            if (!tabInfo[prop + "_assetVersion"])
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