'use strict';

var configMod = angular.module('mdwConfigurator', ['mdw']);

configMod.factory('Configurator', ['mdw',
                                      function(mdw) {
  
  var Configurator = function(workflowType, workflowObj, template) {
    this.workflowType = workflowType;
    this.workflowObj = workflowObj;
    this.template = template;
  };
  
  Configurator.prototype.x = function() {
    console.log('x');
    if (this.template)
      return JSON.stringify(this.template, null, 2);
    else
      return '{}';
  };
  
  return Configurator;
}]);