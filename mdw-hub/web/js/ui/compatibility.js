'use strict';

var compatMod = angular.module('mdwCompatibility', ['mdw']);

compatMod.factory('Compatibility', ['mdw', 'util',
                  function(mdw, util) {
  return {
    getArray: function(value) {
      if (value.startsWith('['))
        return JSON.parse(value);
      else
        return value.split('#');
    },
    getMap: function(value) {
      if (value.startsWith('{')) {
        return JSON.parse(value);
      }
      else {
        var map = {};
        value.split(';').forEach(function(entry) {
          var eq = entry.indexOf('=');
          map[entry.substring(0, eq)] = entry.substring(eq + 1);
        });
        return map;
      }
    }
  };
}]);