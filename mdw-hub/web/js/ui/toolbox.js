'use strict';

var toolboxMod = angular.module('mdwToolbox', ['mdw']);

toolboxMod.factory('Toolbox', ['$document', 'mdw', 'util',
                         function($document, mdw, util) {
  
  var Toolbox = function() {
  };
  
  Toolbox.prototype.getImplementors = function() {
    return this.implementors;
  };
  
  Toolbox.prototype.getImplementor = function(className) {
    for (let i = 0; i < this.implementors.length; i++) {
      if (this.implementors[i].implementorClass === className)
        return this.implementors[i];
    }
  };
  
  Toolbox.prototype.init = function(implementors, hubBase) {
    this.implementors = implementors;
    this.hubBase = hubBase;
    this.implementors.forEach(function(impl) {
      if (impl.icon.startsWith('shape:'))
        impl.iconUrl = hubBase + '/asset/com.centurylink.mdw.base/' + impl.icon.substring(6) + '.png';
      else
        impl.iconUrl = hubBase + '/asset/' + impl.icon;
    });
    this.implementors.push({
      category: 'subflow',
      label: 'Embedded Subprocess',
      iconUrl: hubBase + '/asset/com.centurylink.mdw.base/subflow.png',
      implementorClass: 'subflow'
    });
    this.implementors.push({
      category: 'note',
      label: 'Text Note',
      iconUrl: hubBase + '/asset/com.centurylink.mdw.base/note.png',
      implementorClass: 'note'
    });
    this.implementors.sort(function(impl1, impl2) {
      return impl1.label.localeCompare(impl2.label);
    });
  };
  
  Toolbox.prototype.setSelected = function(className) {
    if (className)
      this.selected = this.getImplementor(className);
    else
      this.selected = null;
  };
  
  Toolbox.prototype.getSelected = function() {
    if (this.selected && document.activeElement && this.selected.implementorClass === document.activeElement.id)
      return this.selected;
  };
  
  Toolbox.prototype.getWidth = function() {
    return this.view[0].offsetWidth;
  };

  Toolbox.prototype.getHeight = function() {
    return this.view[0].offsetHeight;
  };
  
  Toolbox.prototype.isOpen = function() {
    return this.open;
  };
  
  Toolbox.getToolbox = function() {
    if (!Toolbox.toolbox)
      Toolbox.toolbox = new Toolbox();
    return Toolbox.toolbox;
  };

  return Toolbox;
}]);

toolboxMod.directive('mdwToolbox', ['$window', '$timeout', 'Toolbox', function($window, $timeout, Toolbox) {
  return {
    restrict: 'A',
    controller: 'MdwWorkflowController',
    link: function link(scope, elem, attrs, ctrls) {
      
      var mouseDown = function(e) {
        var el = e.target;
        if (el.tagName !== 'LI')
          while ((el = el.parentElement) && el.tagName !== 'LI');
        if (el)
          Toolbox.getToolbox().setSelected(el.id);
      };
      
      var mouseUp = function(e) {
        Toolbox.getToolbox().setSelected(null);
      };
      
      var mouseOut = function(e) {
        if (e.buttons !== 1) {
          Toolbox.getToolbox().setSelected(null);
        }
      };

      var toolbox = Toolbox.getToolbox();
      toolbox.view = elem;
      var ul = angular.element(toolbox.view[0].getElementsByTagName('UL'));
      toolbox.open = true;
      
      scope.openToolbox = function() {
        ul[0].style.display = 'block';
        toolbox.view[0].style.bottom = '10px';
        Toolbox.getToolbox().open = true;
      };
      scope.closeToolbox = function() {
        toolbox.view[0].style.bottom = '';
        ul[0].style.display = 'none';
        Toolbox.getToolbox().open = false;
      };
      
      ul.bind('mousedown', mouseDown);
      ul.bind('mouseup', mouseUp);
      ul.bind('mouseout', mouseOut);
      
      scope.$on('$destroy', function() {
        ul.unbind('mousedown', mouseDown);
        ul.unbind('mouseup', mouseUp);
        ul.unbind('mouseout', mouseOut);
      });
    }
  };
}]);  