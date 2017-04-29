'use strict';

var configMod = angular.module('mdwConfigurator', ['mdw']);

configMod.factory('Configurator', ['$http', 'mdw', 'util', 'Assets', 'Workgroups', 'Compatibility', 'DOCUMENT_TYPES',
                  function($http, mdw, util, Assets, Workgroups, Compatibility, DOCUMENT_TYPES) {
  
  var Configurator = function(tab, workflowType, workflowObj, diagramObj, template) {
    this.tab = tab;
    this.workflowType = workflowType;
    this.workflowObj = workflowObj;
    this.diagramObj = diagramObj;
    this.process = diagramObj.diagram.process;
    this.template = template;
    this.initValues();
  };
  
  Configurator.prototype.initValues = function() {

    // we need workgroups populated
    if (!Workgroups.groupList)
      Workgroups.groupList = Workgroups.get();
    
    // help link
    this.helpLink = this.getHelpLink();
    
    this.filterWidgets();
    
    var labelWidth = 10;
    for (let i = 0; i < this.template.pagelet.widgets.length; i++) {
      var widget = this.template.pagelet.widgets[i];
      
      // label
      if (!widget.label)
        widget.label = widget.name;
      if (widget.label.length > labelWidth)
        labelWidth = widget.label.length;

      // options source
      if (widget.source) {
        if (widget.source === 'Variables')
          widget.options = this.getVariableNames();
        else if (widget.source === 'DocumentVariables')
          widget.options = this.getVariableNames(true);
        else if (widget.source === 'UserGroup')
          widget.options = this.getWorkgroups();
        else if (widget.type === 'asset')
          this.initAssetOptions(widget);
        
        // TODO: parameterized, UserGroup, TaskCategory
      }
      
      // value
      if (this.template.category === 'object') {
        widget.value = this.workflowObj[widget.name];
      }
      else if (this.template.category === 'process') {
        if (widget.name === '_isService')
          widget.value = this.workflowObj.attributes.PROCESS_VISIBILITY === 'SERVICE' ? 'true' : 'false';
        else
          widget.value = this.workflowObj.attributes[widget.name];
      }
      else {
        widget.value = this.workflowObj.attributes[widget.name];
      }
      if (!widget.value && widget.default)
        widget.value = widget.default;

      if (widget.type === 'checkbox' && widget.value) {
        widget.value = widget.value.toLowerCase();
      }
      else if (widget.type === 'edit') {
        if (widget.value)
          widget.label += '*';
      }
      else if (widget.type === 'picklist') {
        if (widget.value)
          widget.value = Compatibility.getArray(widget.value);
        widget.unselected = [];
        for (let j = 0; j < widget.options.length; j++) {
          let option = widget.options[j];
          if (widget.value.indexOf(option) == -1)
            widget.unselected.push(option);
        }
//        widget.add = function() {
//          console.log('adding...');
//        };
//        widget.remove = function() {
//          widget.sel.forEach(function(sel)) {
//            widget.unselected.push(sel);
//          };
//          var newVal = [];
//          widget.value.forEach(function(val)) {
//            if (widget.sel.indexOf(val) == -1)
//              newVal.push(val);
//          };
//          widget.value = newVal;
//        };
      }
      
      // width && height
      widget.width = widget.vw;
      if (!widget.width)
        widget.width = 400;
      widget.height = 30;
      if (widget.multiline) {
        widget.rows = widget.rows ? widget.rows : 8;
        widget.height = widget.rows * 17.5 + 10;
      }
      
    }
    
    // padding
    this.template.pagelet.widgets.forEach(function(widget) {
      widget.pad = util.padTrailing('', labelWidth - widget.label.length);
    });    
  };

  Configurator.prototype.getTemplate = function() {
    return this.template;
  };
  
  Configurator.prototype.getWidgets = function() {
    return this.template.pagelet.widgets;
  };
  
  Configurator.prototype.getVariableNames = function(onlyDoc) {
    var varNames = [];
    var varsObj = this.process.variables;
    util.getProperties(varsObj).forEach(function(varName) {
      if (!onlyDoc || DOCUMENT_TYPES[varsObj[varName].type]) {
        varNames.push(varName);
      }
    });
    return varNames;
  };
  
  Configurator.prototype.getWorkgroups = function() {
    return Workgroups.groupList;
  };
    
  Configurator.prototype.initAssetOptions = function(widget) {
    widget.options = [];
    var selectAsset;
    if (widget.value) {
      var spaceV = widget.value.lastIndexOf(' v');
      if (spaceV > 0)
        selectAsset = widget.value.substring(0, spaceV);
      else
        selectAsset = widget.value;
      if (widget.source == 'proc')
        selectAsset += '.proc';  // process attrs saved without ext 
    }
    $http.get(mdw.roots.services + '/services/Assets?extension=' + widget.source + "&app=mdw-admin").then(function(res) {
      if (res.data.packages) {
        res.data.packages.forEach(function(pkg) {
          pkg.assets.forEach(function(asset) {
            var base = pkg.name + '/' + asset.name;
            var option = base + ' v' + asset.version;
            widget.options.push(option);
            if (base === selectAsset) {
              widget.value = option;
              widget.assetUrl = mdw.roots.hub + '/#/asset/' + base;
            }
          });
        });
      }
    });
  };
  
  Configurator.prototype.filterWidgets = function() {
    if (this.template.category === 'object')
      return;
    
    var widgets = [];
    var tab = this.tab;
    this.template.pagelet.widgets.forEach(function(widget) {
      if (!widget.hidden) {
        if ((widget.section && tab === widget.section) || (!widget.section && tab === 'Design'))
          widgets.push(widget);
      }
    });
    this.template.pagelet.widgets = widgets;
  };
  
  Configurator.prototype.getHelpLink = function() {
    var helpWidgetIndex = -1;
    for (let i = 0; i < this.template.pagelet.widgets.length; i++) {
      var widget = this.template.pagelet.widgets[i];
      if (widget.type == 'link' && widget.url && widget.url.startsWith('/MDWWeb/doc')) {
        helpWidgetIndex = i;
        break;
      }
    }
    
    if (helpWidgetIndex > -1) {
      var widg = this.template.pagelet.widgets[helpWidgetIndex];
      this.template.pagelet.widgets.splice(helpWidgetIndex, 1);
      return {
        name: widg.name,
        url: mdw.roots.docs + '/help' + widg.url.substring(11)
      };
    }
    else {
      return null;
    }
  };
  
  Configurator.prototype.edit = function(widget) {
  };
  
  Configurator.prototype.valueChanged = function(widget) {
    if (this.template.category === 'object') {
      this.workflowObj[widget.name] = widget.value;
    }
    else if (this.template.category === 'process') {
      if (widget.name === '_isService')
        this.workflowObj.attributes.PROCESS_VISIBILITY = widget.value === 'true' ? 'SERVICE' : 'PUBLIC';
      else
        this.workflowObj.attributes[widget.name] = widget.value;
    }
    else {
      if (widget.type === 'asset')
        this.setAssetValue(widget);
      else
        this.workflowObj.attributes[widget.name] = widget.value;
    }
  };
  
  Configurator.prototype.setAssetValue = function(widget) {
    var asset = widget.value;
    var version;
    var spaceV = widget.value.lastIndexOf(' v');
    if (spaceV > 0) {
      var minVer = asset.substring(spaceV + 2);
      var dot = minVer.indexOf('.');
      var major = dot > 0 ? parseInt(minVer.substring(0, dot)) : 0;
      version = '[' + minVer + ',' + (++major) + ")";
      asset = asset.substring(0, spaceV);
    }

    if (asset.endsWith('.proc'))
      asset = asset.substring(0, asset.length - 5);
    
    this.workflowObj.attributes[widget.name] = asset;
    if (version) {
      if (widget.name === 'processname')
        this.workflowObj.attributes.processversion = version;
      else
        this.workflowObj.attributes[widget.name + '_assetVersion'] = version;
    }
  };
  
  return Configurator;
}]);