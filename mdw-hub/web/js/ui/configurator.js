'use strict';

var configMod = angular.module('mdwConfigurator', ['mdw']);

configMod.factory('Configurator', ['$injector', '$http', 'mdw', 'util', 'Assets', 'Workgroups', 'Compatibility', 'DOCUMENT_TYPES',
                  function($injector, $http, mdw, util, Assets, Workgroups, Compatibility, DOCUMENT_TYPES) {
  
  var Configurator = function(tab, workflowType, workflowObj, diagramObj, template) {
    this.tab = tab;
    this.workflowType = workflowType;
    this.workflowObj = workflowObj;
    this.diagramObj = diagramObj;
    if (diagramObj.diagram.process)
      this.process = diagramObj.diagram.process;
    this.template = template;
  };
  
  Configurator.prototype.initValues = function(editCallback) {

    // we need workgroups populated
    if (!Workgroups.groupList)
      Workgroups.groupList = Workgroups.get();
    
    // help link
    this.helpLink = this.getHelpLink();
    
    this.filterWidgets();
    
    var labelWidth = 10;
    for (let i = 0; i < this.template.pagelet.widgets.length; i++) {
      var widget = this.template.pagelet.widgets[i];
      widget.configurator = this;
      
      if (!this.diagramObj.diagram.editable)
        widget.readonly = true;
      
      // label
      if (!widget.label)
        widget.label = widget.name;
      if (widget.label.length > labelWidth && widget.type != 'note')
        labelWidth = widget.label.length;

      // options source
      if (widget.source) {
        if (widget.source === 'Variables')
          widget.options = this.getVariableNames();
        else if (widget.source === 'DocumentVariables')
          widget.options = this.getVariableNames(true);
        else if (widget.source === 'UserGroup')
          widget.options = this.getWorkgroups();
        // asset options are handled after setting value
        // TODO: parameterized
      }
      
      // value
      if (this.template.category === 'object') {
        if (widget.converter) {
          var converter = widget.converter;
          if (typeof converter !== 'object')
            converter = $injector.get(converter);
          widget.value = converter.toWidgetValue(widget, this.workflowObj[widget.name]);
        }
        else {
          widget.value = this.workflowObj[widget.name];
        }
      }
      else if (this.template.category === 'attributes') {
        if (widget.name === '_isService')
          widget.value = this.workflowObj.attributes.PROCESS_VISIBILITY === 'SERVICE' ? 'true' : 'false';
        else if (widget.name === '_linkType')
          widget.value = this.diagramObj.getDisplay().type;
        else if (widget.name === '_controlPoints')
          widget.value = this.diagramObj.getDisplay().xs.length;
        else
          widget.value = this.workflowObj.attributes[widget.name];
        if (widget.type === 'asset')
          this.initAssetOptions([widget]);
      }
      else {
        widget.value = this.workflowObj.attributes[widget.name];
        if (widget.type === 'asset')
          this.initAssetOptions([widget]);
      }
      if (!widget.value && widget.default) {
        widget.value = widget.default;
        this.valueChanged(widget);
      }

      // value handling for some widget types
      if (widget.type === 'checkbox' && widget.value) {
        widget.value = widget.value.toLowerCase();
      }
      else if (widget.type === 'edit') {
        if (widget.value)
          widget.label += '*';
      }
      else if (widget.type === 'link' && widget.name === 'implementor' && widget.value) {
        var implName = widget.value;
        if (implName.startsWith('com.centurylink.mdw.workflow.')) {
          // mdw built-in (GitHub)
          var filePath = implName.replace(/\./g,"/");
          widget.url = mdw.sourceRepoUrl + '/blob/master/mdw-workflow/src/' + filePath + '.java';     
        }
        else {
          // hopefully a java asset
          var lastSlash = implName.lastIndexOf('.');
          var pkgName = implName.substring(0, lastSlash);
          var assetName = implName.substring(lastSlash + 1);
          widget.url = mdw.roots.hub + '/#/asset/' + pkgName + "/" + assetName + '.java';
        }
      }
      else if (widget.type === 'picklist') {
        if (widget.value)
          widget.value = Compatibility.getArray(widget.value);
        this.setUnselected(widget);
      }
      else if (widget.type === 'mapping') {
        if (widget.value)
          widget.value = Compatibility.getMap(widget.value);
        if (widget.source === 'Subprocess')
          this.initSubprocBindings(widget, this.workflowObj.attributes.processname);
        else
          this.initBindings(widget, this.process.variables);
      }
      else if (widget.type === 'table') {
        if (widget.value)
          widget.value = Compatibility.getTable(widget.value);
        this.initTableValues(widget);
      }
      else if (widget.type === 'editor') {
        editCallback(widget); // callback should check if editable
      }
      else if (widget.type === 'datetime') {
        if (widget.value) {
          widget.units = this.workflowObj.attributes[widget.name + '_UNITS'];
          if (!widget.units)
            widget.units = 'Hours';
          if (widget.units == 'Minutes')
            widget.value = parseInt(widget.value) / 60;
          else if (widget.units == 'Hours')
            widget.value = parseInt(widget.value) / 3600;
          else if (widget.units == 'Days')
            widget.value = parseInt(widget.value) / 86400;
        }
      }
      
      // width && height
      widget.width = widget.vw;
      if (!widget.width)
        widget.width = 450;
      if (widget.type != 'mapping' && widget.type != 'picklist' && widget.type != 'table') {
        widget.height = 30;
        if (widget.multiline) {
          widget.rows = widget.rows ? widget.rows : 8;
          widget.height = widget.rows * 17.5 + 10;
        }
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
    var groups = [];
    Workgroups.groupList.workgroups.forEach(function(workgroup) {
      groups.push(workgroup.name);
    });
    return groups;
  };
  
  // init the unselected options based on value
  Configurator.prototype.setUnselected = function(widget) {
    if (!widget.value) {
      widget.unselected = widget.options.slice();
    }
    else {
      widget.unselected = [];
      for (let i = 0; i < widget.options.length; i++) {
        let option = widget.options[i];
        if (!widget.value || widget.value.indexOf(option) == -1)
          widget.unselected.push(option);
      }
    }
  };
  
  Configurator.prototype.initSubprocBindings = function(widget, subproc) {
    var spaceV = subproc.lastIndexOf(' v');
    if (spaceV > 0)
      subproc = subproc(0, spaceV);

    var configurator = this;
    $http.get(mdw.roots.services + '/services/Workflow/' + subproc + "?app=mdw-admin").then(function(res) {
      if (res.data.variables) {
        configurator.initBindings(widget, res.data.variables, true);
      }
    });
  };
  
  // init bindings
  Configurator.prototype.initBindings = function(widget, vars, includeOuts) {
    widget.bindingVars = [];
    util.getProperties(vars).forEach(function(varName) {
      var variable = vars[varName];
      if (variable.category === 'INPUT' || variable.category === 'INOUT' || (includeOuts && variable.category === 'OUTPUT')) {
        variable.name = varName;
        widget.bindingVars.push(variable);
      }
    });
    widget.bindingVars.sort(function(v1, v2) {
      if (widget.value) {
        if (widget.value[v1.name] && !widget.value[v2.name])
          return -1;
        else if (widget.value[v2.name] && !widget.value[v1.name])
          return 1;
      }
      return v1.name.localeCompare(v2.name);
    });
  };
  
  Configurator.prototype.initTableValues = function(tblWidget, assetOptions) {
    tblWidget.widgetRows = [];
    var assetWidgets = []; // must be uniform source
    if (!tblWidget.value || tblWidget.value.length === 0) {
      tblWidget.value = [];
      tblWidget.value.push([]);
      tblWidget.widgets.forEach(function(widget) {
        if (widget.source !== 'ProcessVersion' && widget.source !== 'AssetVersion')
          tblWidget.value[0].push('');
      });
    }
    for (let i = 0; i < tblWidget.value.length; i++) {
      var widgetRow = [];
      for (let j = 0; j < tblWidget.widgets.length; j++) {
        var widget = tblWidget.widgets[j];
        if (widget.source !== 'ProcessVersion' && widget.source !== 'AssetVersion') {
          var rowWidget = {
            type: widget.type,
            value: tblWidget.value[i][j],
            parent: tblWidget,
            configurator: this
          };
          if (!rowWidget.value && widget.default)
            rowWidget.value = widget.default;
          if (widget.readonly)
            rowWidget.readonly = widget.readonly;
          else if (tblWidget.readonly)
            rowWidget.readonly = tblWidget.readonly;          
          if (widget.type === 'asset') {
            rowWidget.source = widget.source;
            assetWidgets.push(rowWidget);
          }
          if (widget.options)
            rowWidget.options = widget.options;
          if (widget.vw)
            rowWidget.width = widget.vw;
          widgetRow.push(rowWidget);
        }
      }
      tblWidget.widgetRows.push(widgetRow);
    }
    if (assetWidgets.length > 0)
      this.initAssetOptions(assetWidgets, assetOptions);
  };

  Configurator.prototype.initAssetOptions = function(widgets, assetOptions) {
    var selectAssets = [];
    for (let i = 0; i < widgets.length; i++) {
      let widget = widgets[i];
      widget.options = [];
      var selectAsset = null;
      if (widget.value) {
        var spaceV = widget.value.lastIndexOf(' v');
        if (spaceV > 0)
          selectAsset = widget.value.substring(0, spaceV);
        else
          selectAsset = widget.value;
        if (widget.source == 'proc')
          selectAsset += '.proc';  // process attrs saved without ext
      }
      selectAssets.push(selectAsset);
    }
    if (assetOptions) {
      for (let i = 0; i < widgets.length; i++) {
        let widget = widgets[i];
        widget.options = assetOptions;
        for (let j = 0; j < widget.options.length; j++) {
          var assetVer = this.getAssetVersion(widget.options[j]);
          if (assetVer.asset === selectAssets[i]) {
            widget.value = widget.options[j];
            widget.assetUrl = mdw.roots.hub + '/#/asset/' + assetVer.asset;
          }
        }
      }
    }
    else {
      var ext = widgets[0].source;
      if (ext.startsWith('[')) {
        ext = ext.substring(1, ext.length - 1);
      }
      if (ext.indexOf(',') > 0) {
        ext = '%5B' + ext + '%5D';
      }
      $http.get(mdw.roots.services + '/services/Assets?extension=' + ext + "&app=mdw-admin").then(function(res) {
        if (res.data.packages) {
          res.data.packages.forEach(function(pkg) {
            pkg.assets.forEach(function(asset) {
              var base = pkg.name + '/' + asset.name;
              var option = base + ' v' + asset.version;
              for (let i = 0; i < widgets.length; i++) {
                let widget = widgets[i];
                widget.options.push(option);
                if (base === selectAssets[i]) {
                  widget.value = option;
                  widget.assetUrl = mdw.roots.hub + '/#/asset/' + base;
                }
              }
            });
          });
        }
      });
    }
  };
  
  Configurator.prototype.filterWidgets = function() {
    if (this.template.category === 'object' || this.template.category === 'attributes')
      return;
    
    var widgets = [];
    var tab = this.tab;
    this.template.pagelet.widgets.forEach(function(widget) {
      if (!widget.hidden) {
        // TODO unsupported sections: Bindings (LdapAdapter.impl and OAuthServiceAdapter.impl)
        if ((widget.section && tab === widget.section) || (!widget.section && (tab == 'Design' || tab == 'General')))
          widgets.push(widget);
      }
    });
    this.template.pagelet.widgets = widgets;
  };
  
  Configurator.prototype.getHelpLink = function() {
    var helpWidgetIndex = -1;
    for (let i = 0; i < this.template.pagelet.widgets.length; i++) {
      var widget = this.template.pagelet.widgets[i];
      if (widget.type == 'link' && widget.url && (widget.url.startsWith('/MDWWeb/doc') || widget.url.startsWith('help/')) && 
          ((!widget.section && (this.tab == 'Design' || this.tab == 'General')) || this.tab === widget.section)) {
        helpWidgetIndex = i;
        break;
      }
    }
    
    if (helpWidgetIndex > -1) {
      var widg = this.template.pagelet.widgets[helpWidgetIndex];
      this.template.pagelet.widgets.splice(helpWidgetIndex, 1);
      return {
        name: widg.name,
        url: mdw.roots.docs + '/help' + (widg.url.startsWith('/MDWWeb/doc') ? widg.url.substring(11) : widg.url.substring(4))
      };
    }
    else {
      return null;
    }
  };
  
  Configurator.prototype.valueChanged = function(widget, evt) {
    if (this.template.category === 'object' && !widget.parent && widget.type !== 'table') {
      if (widget.converter) {
        let converter = widget.converter;
        if (typeof converter !== 'object')
          converter = $injector.get(converter);
        this.workflowObj[widget.name] = converter.fromWidgetValue(widget, widget.value);
      }
      else {
        this.workflowObj[widget.name] = widget.value;
      }
    }
    else if (this.template.category === 'attributes') {
      if (widget.name === '_isService') {
        this.workflowObj.attributes.PROCESS_VISIBILITY = widget.value === 'true' ? 'SERVICE' : 'PUBLIC';
      }
      else if (widget.name === '_linkType') {
        this.diagramObj.display.type = widget.value;
        this.workflowObj.attributes.TRANSITION_DISPLAY_INFO = this.diagramObj.getAttr(this.diagramObj.display);
      }
      else if (widget.name === '_controlPoints') {
        this.diagramObj.calc(widget.value);
      }
      else if (widget.parent && widget.parent.type === 'table') {
        var tblWdg = widget.parent;
        tblWdg.value = [];
        for (let i = 0; i < tblWdg.widgetRows.length; i++) {
          var wdgRow = tblWdg.widgetRows[i];
          if (wdgRow[0].value) {
            var valRow = [];
            for (let j = 0; j < wdgRow.length; j++) {
              if (wdgRow[j].source === 'proc' || wdgRow[j].type === 'asset') {
                var assetVer = this.getAssetVersion(wdgRow[j].value, true);
                if (assetVer) {
                  valRow.push(assetVer.asset);
                  if (assetVer.version)
                    valRow.push(assetVer.version);
                }
              }
              else {
                valRow.push(wdgRow[j].value ? wdgRow[j].value : '');  
              }
            }
            tblWdg.value.push(valRow);
          }
        }
        if (tblWdg.converter && this.template.category === 'object') {
          let converter = $injector.get(tblWdg.converter);
          this.workflowObj[tblWdg.name] = converter.fromWidgetValue(tblWdg, this.removeEmptyRows(tblWdg.value));
        }
        else {
          this.workflowObj.attributes[tblWdg.name] = JSON.stringify(this.removeEmptyRows(tblWdg.value));
        }
      }
      else {
        this.workflowObj.attributes[widget.name] = widget.value;
      }
    }
    else if (widget.type === 'table') {
      // only evts are handled at this level
      if (evt) {
        if (evt === 'new') {
          widget.value.push([]);
          widget.widgets.forEach(function(w) {
            widget.value[0].push('');
          });
          this.initTableValues(widget, this.getAssetOptions(widget));
        }
        else if (evt.startsWith('del_')) {
          widget.value.splice(parseInt(evt.substring(4)), 1);
          this.initTableValues(widget, this.getAssetOptions(widget));
        }
        if (widget.converter && this.template.category === 'object') {
          let converter = widget.converter;
          if (typeof converter !== 'object')
            converter = $injector.get(converter);
          this.workflowObj[widget.name] = converter.fromWidgetValue(widget, this.removeEmptyRows(widget.value));
        }
        else {
          this.workflowObj.attributes[widget.name] = JSON.stringify(this.removeEmptyRows(widget.value));
        }
      }
    }
    else if (widget.parent) {
      if (widget.parent.type === 'table') {
        var tblWidget = widget.parent;
        tblWidget.value = [];
        for (let i = 0; i < tblWidget.widgetRows.length; i++) {
          var widgetRow = tblWidget.widgetRows[i];
          if (widgetRow[0].value) {
            var valueRow = [];
            for (let j = 0; j < widgetRow.length; j++) {
              if (widgetRow[j].source === 'proc' || widgetRow[j].type === 'asset') {
                var assetVersion = this.getAssetVersion(widgetRow[j].value, true);
                if (assetVersion) {
                  valueRow.push(assetVersion.asset);
                  if (assetVersion.version)
                    valueRow.push(assetVersion.version);
                }
              }
              else {
                valueRow.push(widgetRow[j].value ? widgetRow[j].value : '');  
              }
            }
            tblWidget.value.push(valueRow);
          }
        }
        if (tblWidget.converter && this.template.category === 'object') {
          let converter = $injector.get(tblWidget.converter);
          this.workflowObj[tblWidget.name] = converter.fromWidgetValue(tblWidget, this.removeEmptyRows(tblWidget.value));
        }
        else {
          this.workflowObj.attributes[tblWidget.name] = JSON.stringify(this.removeEmptyRows(tblWidget.value));
        }
      }
    }
    else if (widget.type === 'asset') {
        this.setAssetValue(widget);
    }
    else if (widget.type === 'picklist') {
      if (evt === 'add' && widget.unsel) {
        if (!widget.value)
          widget.value = [];
        widget.unsel.forEach(function(unsel) {
          widget.value.push(unsel);
        });
        this.setUnselected(widget);
        widget.unsel = widget.sel = null;
        this.valueChanged(widget);
      }
      else if (evt === 'remove' && widget.sel) {
        let newVal = [];
        widget.value.forEach(function(val) {
          if (widget.sel.indexOf(val) == -1)
            newVal.push(val);
        });
        widget.value = newVal;
        this.setUnselected(widget);
        widget.sel = widget.unsel = null;
        this.valueChanged(widget);
      }
      else {
        return false;
      }
      this.workflowObj.attributes[widget.name] = widget.value ? JSON.stringify(widget.value) : widget.value;
    }
    else if (widget.type === 'mapping') {
      if (widget.value) {
        util.getProperties(widget.value).forEach(function(name) {
          if (!widget.value[name].trim()) {
            delete widget.value[name];
          }
        });
        this.workflowObj.attributes[widget.name] = JSON.stringify(widget.value);
      }
    }
    else if (widget.type === 'editor') {
      if (widget.value)
        this.workflowObj.attributes[widget.name] = JSON.stringify(widget.value);
    }
    else {
      if (widget.type === 'datetime') {
        if (widget.value) {
          this.workflowObj.attributes[widget.name + '_UNITS'] = widget.units;
          if (widget.units == 'Minutes')
            this.workflowObj.attributes[widget.name] = '' + (widget.value * 60);
          else if (widget.units == 'Hours')
            this.workflowObj.attributes[widget.name] = '' + (widget.value * 3600);
          else if (widget.units == 'Days')
            this.workflowObj.attributes[widget.name] = '' + (widget.value * 86400);
        }
      }
      else {
        this.workflowObj.attributes[widget.name] = widget.value;
      }
    }
    return true;
  };
  
  // avoid re-retrieving asset select options
  Configurator.prototype.getAssetOptions = function(widget) {
    if (widget.type === 'table' && widget.widgetRows) {
      for (let i = 0; i < widget.widgetRows.length; i++) {
        var widgetRow = widget.widgetRows[i];
        for (let j = 0; j < widget.widgetRows[i].length; j++) {
          var w = widget.widgetRows[i][j];
          if (w.type === 'asset' && w.options && w.options.length > 0)
            return w.options;
        }
      }
    }
  };
  
  // returns an obj with asset, version props
  Configurator.prototype.getAssetVersion = function(value, stripProc) {
    if (value) {
      var asset = value;
      var version;
      var spaceV = value.lastIndexOf(' v');
      if (spaceV > 0) {
        var minVer = asset.substring(spaceV + 2);
        var dot = minVer.indexOf('.');
        var major = dot > 0 ? parseInt(minVer.substring(0, dot)) : 0;
        version = '[' + minVer + ',' + (++major) + ")";
        asset = asset.substring(0, spaceV);
      }
  
      if (stripProc && asset.endsWith('.proc'))
        asset = asset.substring(0, asset.length - 5);
  
      var assetVersion = { asset: asset};
      if (version)
        assetVersion.version = version;
      return assetVersion;
    }
  };
  
  Configurator.prototype.setAssetValue = function(widget) {
    var assetVersion = this.getAssetVersion(widget.value, true);
    this.workflowObj.attributes[widget.name] = assetVersion.asset;
    if (assetVersion.version) {
      if (widget.name === 'processname')
        this.workflowObj.attributes.processversion = assetVersion.version;
      else
        this.workflowObj.attributes[widget.name + '_assetVersion'] = assetVersion.version;
    }
  };
  
  Configurator.prototype.removeEmptyRows = function(tableValue) {
    if (tableValue) {
      var ret = [];
      for (let i = 0; i < tableValue.length; i++) {
        var hasAnyValue = false;
        for (let j = 0; j < tableValue[i].length; j++) {
          if (tableValue[i][j]) {
            hasAnyValue = true;
            break;
          }
        }
        if (hasAnyValue)
          ret.push(tableValue[i]);
      }
      if (ret.length !== 0)
        return ret;
    }
  };
  
  return Configurator;
}]);