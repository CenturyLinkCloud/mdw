'use strict';
var converter = angular.module('objectTableConverter', ['mdw']);

// converts an object to/from a table widget value (eg: variables.json)
// TODO: handle an array of objects in addition to map
converter.factory('ObjectTableConverter', ['mdw', 'util', function(mdw, util) {
  return {
    toWidgetValue: function(widget, objectValue) {
      var rows = [];
      util.getProperties(objectValue).forEach(function(prop) {
        var row = [];
        var val = objectValue[prop];
        widget.widgets.forEach(function(colWidget) {
          if (colWidget.name === '_key')
            row.push(prop);
          else if (val[colWidget.name])
            row.push(val[colWidget.name]);
          else
            row.push('');
        });
        rows.push(row);
      });
      return JSON.stringify(rows);
    },
    fromWidgetValue: function(widget, widgetValue) {
      var obj = {};
      widgetValue.forEach(function(row) {
        var rowObj = {};
        for (let i = 0; i < widget.widgets.length; i++) {
          var colWidget = widget.widgets[i];
          if (colWidget.name === '_key')
            obj[row[i]] = rowObj;
          else if (row[i])
            rowObj[colWidget.name] = row[i];
        }
      });
      return obj;
    }
  };
}]);