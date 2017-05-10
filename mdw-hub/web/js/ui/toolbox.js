'use strict';

var toolboxMod = angular.module('mdwToolbox', ['mdw']);

toolboxMod.factory('Toolbox', ['$document', 'mdw', 'util',
                         function($document, mdw, util) {
  var Toolbox = function(diagram, implementors, hubBase) {
    this.diagram = diagram;
    this.implementors = implementors;
    this.hubBase = hubBase;
    this.implementors.forEach(function(impl) {
      if (impl.icon.startsWith('shape:'))
        impl.iconUrl = hubBase + '/asset/com.centurylink.mdw.base/' + impl.icon.substring(6) + '.png';
      else
        impl.iconUrl = hubBase + '/asset/' + impl.icon;
    });
  };
  
  Toolbox.prototype.getImplementors = function() {
    return this.implementors;
  };
  
  return Toolbox;
}]);