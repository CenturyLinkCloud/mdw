'use strict';

var configMod = angular.module('mdwConfigurator', ['mdw']);

configMod.factory('Configurator', ['$http', 'mdw', 'util', 'Assets', 'DOCUMENT_TYPES',
                  function($http, mdw, util, Assets, DOCUMENT_TYPES) {
  
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
      
      // value
      if (this.template.category === 'object') {
        widget.value = this.workflowObj[widget.name];
      }
      else {
        widget.value = this.workflowObj.attributes[widget.name];
      }
      if (!widget.value && widget.default)
        widget.value = widget.default;
      
      // width && height
      widget.width = widget.vw;
      if (!widget.width)
        widget.width = 400;
      widget.height = 30;
      if (widget.multiline) {
        widget.rows = widget.rows ? widget.rows : 8;
        widget.height = widget.rows * 17.5 + 10;
      }
      
      // options source
      if (widget.source) {
        if (widget.source === 'Variables')
          widget.options = this.getVariableNames();
        else if (widget.source === 'DocumentVariables')
          widget.options = this.getVariableNames(true);
        else if (widget.source === 'Assets')
            this.setSourceAssetOptions(widget);
        
        // TODO: parameterized, UserGroup, 
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
    
  Configurator.prototype.setSourceAssetOptions = function(widget) {
    $http.get(mdw.roots.services + '/services/Assets?extension=' + widget.ext + "&app=mdw-admin").then(function(res) {
      widget.options = [];
      if (res.data.packages) {
        res.data.packages.forEach(function(pkg) {
          pkg.assets.forEach(function(asset) {
            widget.options.push(pkg.name + '/' + asset.name + ' v' + asset.version);
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
      if ((widget.section && tab === widget.section) || (!widget.section && tab === 'Design'))
        widgets.push(widget);
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
  
  return Configurator;
}]);