'use strict';

var compatMod = angular.module('mdwCompatibility', ['mdw']);

compatMod.factory('Compatibility', ['mdw', 'util',
                  function(mdw, util) {
  return {
    getArray: function(value) {
      if (value.startsWith('['))
        return JSON.parse(value);
      else
        return this.safeSplit(value, '#');
    },
    getMap: function(value) {
      if (value.startsWith('{')) {
        return JSON.parse(value);
      }
      else {
        var map = {};
        this.safeSplit(value, ';').forEach(function(entry) {
          var eq = entry.indexOf('=');
          map[entry.substring(0, eq)] = entry.substring(eq + 1);
        });
        return map;
      }
    },
    getTable: function(value) {
      if (value.startsWith('[')) {
        return JSON.parse(value);
      }
      else {
        let compat = this;
        var rows = [];
        compat.safeSplit(value, ';').forEach(function(row) {
          var cols = [];
          compat.safeSplit(row, ',').forEach(function(col) {
            cols.push(col);
          });
          rows.push(cols);
        });
        return rows;
      }
    },
    // split a string based on a delimiter except when preceded by \
    safeSplit: function(str, delim) {
      var res = []; 
      var segs = str.split(delim);
      var accum = '';
      for (let i = 0; i < segs.length; i++) {
        var seg = segs[i];
        if (seg.endsWith('\\')) {
          accum += seg.substring(0, seg.length - 1) + delim;
        }
        else {
          accum += seg;
          res.push(accum);
          accum = '';
        }
      }
      return res;
    }
  };
}]);