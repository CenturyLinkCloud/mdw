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
    }
  };
}]);