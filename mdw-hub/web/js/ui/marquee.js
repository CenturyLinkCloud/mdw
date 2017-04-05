'use strict';

var marqueeMod = angular.module('mdwMarquee', ['mdw']);

marqueeMod.factory('Marquee', ['$document', 'mdw', 'util', 'Shape', 'DC',
                         function($document, mdw, util, Shape, DC) {
  var Marquee = function(diagram) {
    Shape.call(this, diagram);
    this.diagram = diagram;
    this.isMarquee = true;
  };
  
  Marquee.prototype = new Shape();
  
  Marquee.BOX_OUTLINE_COLOR = 'cyan';
  Marquee.BOX_ROUNDING_RADIUS = 2;

  Marquee.prototype.draw = function() {
    this.diagram.rect(this.display.x, this.display.y, this.display.w, this.display.h, null, Marquee.BOX_ROUNDING_RADIUS);
  };
  
  Marquee.prototype.move = function(deltaX, deltaY) {
    var x = this.display.x + deltaX;
    var y = this.display.y + deltaY;
    // this.setDisplayAttr(x, y, this.display.w, this.display.h);
    return true;
  };
  
  Marquee.prototype.resize = function(x, y, deltaX, deltaY) {
    var display = this.resizeDisplay(x, y, deltaX, deltaY, Shape.MIN_SIZE);
    this.setDisplayAttr(display.x, display.y, display.w, display.h);
  };
  
  return Marquee;
}]);