'use strict';

var inspectMod = angular.module('mdwInspector', ['mdw']);

inspectMod.controller('MdwInspectorController', ['$scope', '$http', '$parse', 'mdw', 'util', 'Inspector', 'InspectorTabs', 'Configurator',
                                                 function($scope, $http, $parse, mdw, util, Inspector, InspectorTabs, Configurator) {
  
  $scope.setWorkflow = function(obj) {
    $scope.diagramObject = obj;
    $scope.workflowType = obj.workflowType;
    $scope.workflowObject = obj[obj.workflowType];
    $scope.runtimeInfo = null;  // can be object or array
    $scope.editable = $scope.diagramObject.diagram.editable;
    
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
      
      // editable tabs require a template property
      if (!$scope.editable || ($scope.editable && tab._template)) {
        if (typeof tab === 'object') {
          var tabProps = util.getProperties(tab);
          // when _template is only prop (eg: Design tab)
          var onlyShowForDef = tabProps.length === 1 && tabProps[0] === '_template';
          if (!$scope.runtimeInfo || !onlyShowForDef) {
            if (tabProps[0] && typeof tab[tabProps[1]] == 'function') {
              if (tab[tabProps[1]]($scope.diagramObject, $scope.workflowObject))
                filteredTabs[tabName] = tab;
            }
            else {
              if (obj.workflowType == 'activity' && tab._categories) {
                // only show for specific categories
                var implCat = $scope.diagramObject.diagram.getImplementor($scope.workflowObject.implementor).category;
                if (tab._categories.indexOf(implCat) >= 0)
                  filteredTabs[tabName] = tab;
              }
              else {
                filteredTabs[tabName] = tab;
              }
            }
          }
        }
        else {
          filteredTabs[tabName] = tab;
        }
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
    $scope.configurator = null;
    $scope.editing = null;
    $scope.activeTabName = tabName;
    $scope.activeTab = $scope.tabs[tabName];
    $scope.activeTabValues = [];

    // templated tabs use configurator (TODO: ugly special handling for Documentation)
    if ($scope.activeTab._template && ($scope.editable || $scope.activeTabName !== 'Documentation')) {
      var templateUrl = util.substExpr($scope.activeTab._template, $scope.workflowObject);
      $http.get(templateUrl).then(function(res) {
        var template = res.data;
        $scope.configurator = new Configurator(tabName, $scope.workflowType, $scope.workflowObject, $scope.diagramObject, template);
        $scope.configurator.initValues($scope.edit);
      });
      return;
    }
      
    var tabInfo = $scope.runtimeInfo ? $scope.runtimeInfo : $scope.workflowObject;
    
    // check for array type
    var tabArr;
    if ($scope.activeTab instanceof Array) {
      // array is runtime obj itself (eg: instance.activity.Instances)
      tabArr = angular.copy($scope.activeTab);
      util.getProperties(tabArr[0]).forEach(function(tabProp) {
        var v = tabArr[0][tabProp];
        if (v.startsWith('${') && v.endsWith('}')) {
          $scope.runtimeInfo.forEach(function(rt) {
            var evalsTo = $parse(v.substring(2, v.length - 1))({it: rt});
            rt[tabProp] = evalsTo;
          });
          tabArr[0][tabProp] = tabProp;
        }
      });
    }
    else if (typeof $scope.activeTab == 'function') {
    }
    else if (typeof $scope.activeTab === 'object') {
      var tabProps = util.getProperties($scope.activeTab);
      if (tabProps[0]) {
        if ($scope.activeTab[tabProps[0]] instanceof Array) {
          if (typeof $scope.activeTab[tabProps[1]] == 'function') {
            // array is first prop, function is second (eg: instance.activities.Subprocesses)
            var listName = tabProps[0];
            tabArr = $scope.activeTab[listName];
            var passedTabArr = [{}]; 
            var itsObj = $scope.activeTab[tabProps[0]][0];
            var itsProps = util.getProperties(itsObj);
            var promise = $scope.activeTab[tabProps[1]]($scope.diagramObject, $scope.workflowObject, $scope.runtimeInfo);
            if (promise) {
              promise.then(function(res) {
                var resArr;
                if (res instanceof Array)
                  resArr = res;
                else
                  resArr = [res];
                var tabInfo = [];
                resArr.forEach(function(result) {
                  result.data[listName].forEach(function(it) {
                    var item = {};
                    itsProps.forEach(function(itsProp) {
                      var itsSpec = itsObj[itsProp];
                      if (itsSpec.startsWith('${') && itsSpec.endsWith('}')) {
                        var evalsTo = $parse(itsSpec.substring(2, itsSpec.length - 1))({it: it});
                        item[itsProp] = evalsTo;
                        passedTabArr[0][itsProp] = itsProp;
                      }
                      else {
                        // straight prop
                        item[itsSpec] = it[itsSpec];
                        passedTabArr[0][itsProp] = itsSpec;
                      }
                    });
                    tabInfo.push(item);
                  });
                });
                $scope.applyTabArray(passedTabArr, tabInfo);
              });
            }
            return;
          }
          else {
            // array is only object property (eg: instance.process.Values)
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
        }
        else if (typeof $scope.activeTab[tabProps[0]] === 'object') {
          if (tabProps[0] === '_attribute') {
            var attrname = $scope.activeTab[tabProps[0]].name;
            $scope.activeTabValues.push({
              name: attrname,
              value: tabInfo.attributes[attrname],
              isMarkdown: $scope.activeTab[tabProps[0]].markdown
            });
          }
          else {
            // named object equates to array (eg: process.definition.Variables)
            var arrObj = angular.copy($scope.activeTab[tabProps[0]]);
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
    }
    
    if (tabArr) {
      $scope.applyTabArray(tabArr, tabInfo);
    }
    else if (typeof $scope.activeTab === 'object' && !$scope.activeTab._attribute) {
      // evaluate each object prop against tabInfo object
      var tabObj = tabInfo;
      var atProps = util.getProperties($scope.activeTab);
      for (var i = 0; i < atProps.length; i++) {
        var atProp = atProps[i];
        var v = $scope.activeTab[atProp];
        if (v.startsWith('${') && v.endsWith('}')) {
          var evalsTo = $parse(v.substring(2, v.length - 1))({it: tabObj});
          if (evalsTo !== null)
            tabObj[$scope.activeTab[atProp]] = evalsTo;
        }
        if (atProp == '_url' && tabObj[$scope.activeTab[atProp]]) {
          $scope.activeTabValues[i-1].url = tabObj[$scope.activeTab[atProp]];
        }
        else if (!atProp.startsWith('_')) {
          $scope.activeTabValues.push({
            name: atProp,
            value: tabObj[$scope.activeTab[atProp]]
          });
        }
      }
    }
    else {
      // string indicates value is an object property name on tabInfo (object)
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
            if (val.value.indexOf('\n') >= 0) {
              val.full = val.value;
              val.value = val.full.getLines()[0] + ' ...';
              val.extended = true;
            }
            else if (val.value.indexOf('${props[') >= 0 || val.value.indexOf('#{props[') >= 0) {
              val.extended = true;
            }
            else if (val.value.startsWith('[') || val.value.startsWith('{')) {
              try {
                let parsed = JSON.parse(val.value);
                val.full = JSON.stringify(parsed, null, 2);
                val.extended = true;
              }
              catch(ex) {}
            }
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
              if (prop.endsWith('_assetVersion')) {
                val.name = prop.substring(0, prop.length - 13);
                val.asset = { 
                  path: tabInfo[val.name], 
                  version: 'v' + tabInfo[prop]
                };
              }
              else if (prop == 'processname') {
                val.name = 'Process';
                val.asset = {
                  path: val.value + '.proc', 
                  version: 'v' + tabInfo.processversion
                };
              }
              else if (prop == 'processmap') {
                // TODO
              }
              else if (prop == 'Java') {
                val.language = 'java';
              }
              if (InspectorTabs.assetAttrs.indexOf(prop) != -1) {
                val.asset = { path: tabInfo[prop] };
              }
              if (val.asset && val.asset.path) {
                val.value = val.asset.path;
                if (val.asset.version)
                  val.value += ' ' + val.asset.version;
                val.asset.url = '#/asset/' + val.asset.path;
              }
            }
            if (!tabInfo[prop + '_assetVersion'] && prop != 'processversion' && !prop.startsWith('_'))
              $scope.activeTabValues.push(val);
          }          
        }
      }
    }
  };
  
  $scope.applyTabArray = function(tabArr, tabInfo) {
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
        var value = { value: rowObj[tabArr[0][props[h]]] };
        if (!value.value)
          value.value = '';
        value.value = '' + value.value; // convert to str
        if (value.value.length > maxColWidth) {
          value.full = value.value;
          value.value = value.full.substring(0, maxColWidth - 5) + ' ...';
          value.extended = true;
        }
        if (value.value.length > colWidths[h])
          colWidths[h] = value.value.length;
        valueRow.push(value);
      }
      values.push(valueRow);
    });

    
    // column labels
    var names = [];
    for (var k = 0; k < labels.length; k++) {
      if (!labels[k].startsWith('_')) {
        var name = { name: labels[k]};
        if (k < labels.length - 1)
          name.pad = util.padTrailing('', colWidths[k] - labels[k].length + colSpacing);
        names.push(name);
      }
    }
    // row values
    for (var i = 0; i < values.length; i++) {
      var fields = [];
      for (var j = 0; j < values[i].length; j++) {
        var field = values[i][j];
        if (field.value.startsWith('DOCUMENT:')) {
          field.url = '#/workflow/processes/' + $scope.workflowObject.id + '/values/' + fields[j-1].value;
        }
        if (labels[j] == '_url') {
          // applies to previous field
          fields[j-1].url = field.value;
        }
        else {
          if (j < values[i].length - 1)
            field.pad = util.padTrailing('', colWidths[j] - field.value.length + colSpacing);
          fields.push(field);
        }
      }
      if (i === 0) // only first element needs names
        $scope.activeTabValues.push({names: names, values: fields});
      else
        $scope.activeTabValues.push({values: fields});
    }
  };
  
  $scope.drillIn = function(tabValue) {
    $scope.drilledValue = tabValue;
    if (tabValue && tabValue.value) {
      var propStart = tabValue.value.indexOf('${props[');
      if (propStart < 0)
        propStart = tabValue.value.indexOf('#{props[');
      if (propStart >= 0) {
        var propEnd = tabValue.value.indexOf('}', 9);
        if (propEnd > propStart + 10) {
          $scope.drilledValue.full = tabValue.value + ' --> ';
          var propName = tabValue.value.substring(propStart + 9, propEnd - 2);
          var mdwProps = util.getMdwProperties();
          if (mdwProps) {
            if (propStart > 0)
              $scope.drilledValue.full += tabValue.value.substring(0, propStart);
            $scope.drilledValue.full += mdwProps[propName] + tabValue.value.substring(propEnd + 1);
          }
          else {
            util.loadMdwProperties().then(function(response) {
              mdwProps = util.getMdwProperties();
              if (propStart > 0)
                $scope.drilledValue.full += tabValue.value.substring(0, propStart);
              $scope.drilledValue.full += mdwProps[propName] + tabValue.value.substring(propEnd + 1);
            });
          }
        }
      }
    }
  };
  
  $scope.edit = function(widget) {
    var lang = widget.name === 'Java' ? 'java' : (widget.language ? widget.language.toLowerCase() : 'groovy');
    if ($scope.editable) {
      $scope.editing = widget;
      $scope.editOptions = {
        theme: 'eclipse', 
        mode: lang,
        onChange: function() {
          // first call happens on load
          if ($scope.editDirty === undefined) {
            $scope.editDirty = false;
          }
          else {
            if ($scope.configurator.valueChanged(widget))
              $scope.onChange($scope.process);
            $scope.$parent.setDirty($scope.$parent.process);
          }
        },
        basePath: '/mdw/lib/ace-builds/src-min-noconflict',
      };
    }
    else {
      $scope.drilledValue = {
        full: widget.value,
        language: lang
      };
    }
  };
  
  $scope.valueChanged = function(widget, evt) {
    if ($scope.configurator.valueChanged(widget, evt)) {
      $scope.onChange($scope.process);
    }
  };  
}]);

inspectMod.factory('Inspector', ['mdw', 'util', function(mdw, util) {
  return {
    setObj: function(obj, show) {
      this.obj = obj;
      if (this.listener) {
        this.listener(obj, show);
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

inspectMod.directive('mdwInspector', ['$window', '$document', 'Inspector', 
                                      function($window, $document, Inspector) {
  return {
    restrict: 'A',
    controller: 'MdwInspectorController',
    link: function link(scope, elem, attrs, ctrls) {
      
      var workflowElem = elem.parent();
      var canvasElem = angular.element(workflowElem[0].getElementsByClassName('mdw-canvas'));
      var panelElem = angular.element(elem[0].getElementsByClassName('mdw-inspector-panel'));
      var contentElem = angular.element(elem[0].getElementsByClassName('mdw-inspector-content'));
      var contentHeight;

      scope.openInspector = function() {
        elem[0].style.left = workflowElem[0].getBoundingClientRect().left + 'px';
        scope.initInspector();
        elem[0].style.display = 'block';
      };
      scope.closeInspector = function() {
        elem[0].style.display = 'none';
        workflowElem[0].style.height = canvasElem[0].offsetHeight + 'px';
      };
      scope.maxInspector = function() {
        contentElem[0].style.height = '100%';        
        elem[0].style.height = '80%';
        elem[0].style.top = '100px';
        panelElem[0].style.height = '90%';
      };
      // removes extra styling added by max or close
      scope.initInspector = function() {
        contentElem[0].style.height = '';        
        elem[0].style.height = '';
        elem[0].style.top = '';
        panelElem[0].style.height = '';
        contentHeight = 168;  // must agree with mdw-inspector-content        
      };
      
      var mouseDown = function(e) {
        var y = e.clientY - elem[0].getBoundingClientRect().top;
        scope.resizing = y <= 3;
        if (scope.resizing) {
          scope.startY = e.clientY;
          if (contentElem[0].style.height)
            contentHeight = parseInt(contentElem[0].style.height.substring(0, contentElem[0].style.height.length - 2));
        }
      };
      var mouseUp = function(e) {
        scope.resizing = false;
        $document[0].body.style.cursor = 'default';
      };
      var mouseOut = function(e) {
        if (!scope.resizing)
          $document[0].body.style.cursor = 'default';
      };
      var mouseMove = function(e) {
        if (scope.resizing) {
          contentElem[0].style.height = (contentHeight + (scope.startY - e.clientY)) + 'px';
        }
        if (e.target == elem[0]) {
          var y = e.clientY - elem[0].getBoundingClientRect().top;
          if (y <= 3 || scope.resizing)
            $document[0].body.style.cursor = 'ns-resize';
          else
            $document[0].body.style.cursor = 'default';
        }
        else {
          if (scope.resizing)
            $document[0].body.style.cursor = 'ns-resize';
        }
      };
      
      elem.bind('mousedown', mouseDown);
      elem.bind('mouseup', mouseUp);
      elem.bind('mouseout', mouseOut);
      workflowElem.bind('mousemove', mouseMove);
      workflowElem.bind('mouseup', mouseUp);
      
      // show
      Inspector.listen(function(obj, show) {
        scope.setWorkflow(obj);
                
        scope.$apply();

        if (show) {
          if (elem[0].style.display == 'none') {
            scope.openInspector();
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
        }
      });
      
      scope.$on('$destroy', function() {
        Inspector.unlisten();
        elem.unbind('mousedown', mouseDown);
        elem.unbind('mouseup', mouseUp);        
        elem.unbind('mouseout', mouseOut);        
        workflowElem.unbind('mousemove', mouseMove);
        workflowElem.unbind('mouseup', mouseUp);
      });
    }
  };
}]);