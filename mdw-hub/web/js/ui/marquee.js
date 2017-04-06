'use strict';

var marqueeMod = angular.module('mdwMarquee', ['mdw']);

marqueeMod.factory('Marquee', ['$document', 'mdw', 'util', 'Shape', 'DC',
                         function($document, mdw, util, Shape, DC) {
  var Marquee = function(diagram) {
    Shape.call(this, diagram);
    this.diagram = diagram;
    this.workflowItem = { attributes: { WORK_DISPLAY_INFO: {x: 0, y: 0, w: 0, h: 0} } }; // dummy to hold attrs
    this.isMarquee = true;
  };
  
  Marquee.prototype = new Shape();
  
  Marquee.BOX_OUTLINE_COLOR = 'cyan';
  Marquee.BOX_ROUNDING_RADIUS = 2;

  Marquee.prototype.draw = function() {
    this.diagram.rect(this.display.x, this.display.y, this.display.w, this.display.h, Marquee.BOX_OUTLINE_COLOR, null, Marquee.BOX_ROUNDING_RADIUS);
  };
  
  Marquee.prototype.prepareDisplay = function() {
    this.display = this.getDisplay();
    return this.display;
  };
  
  Marquee.prototype.start = function(x, y) {
    this.setDisplayAttr(x, y, 2, 2);
    this.display = this.getDisplay();
  };
  
  Marquee.prototype.resize = function(x, y, deltaX, deltaY) {
    var newX = deltaX > 0 ? x : x + deltaX;
    if (newX < 0)
      newX = 0;
    var newY = deltaY > 0 ? y : y + deltaY;
    if (newY < 0)
      newY = 0;
    var newW = deltaX > 0 ? deltaX : -deltaX;
    var newH = deltaY > 0 ? deltaY : -deltaY;
    
    this.setDisplayAttr(newX, newY, newW, newH);
  };
  
  Marquee.prototype.getAnchor = function(x, y) {
    return -1;
  };
  
  Marquee.prototype.getSelectObjs = function() {
    var selObjs = [];
    for (let i = 0; i < this.diagram.steps.length; i++) {
      var step = this.diagram.steps[i];
      if (this.isContained(step))
        selObjs.push(step);
    }
    for (let i = 0; i < this.diagram.subflows.length; i++) {
      var subflow = this.diagram.subflows[i];
      if (this.isContained(subflow))
        selObjs.push(subflow);
    }
    for (let i = 0; i < this.diagram.notes.length; i++) {
      var note = this.diagram.notes[i];
      if (this.isContained(note))
        selObjs.push(note);
    }
    return selObjs;
  };
  
  Marquee.prototype.isContained = function(shape) {
    return shape.display.x >= this.display.x && shape.display.y > this.display.y &&
        shape.display.x + shape.display.w <= this.display.x + this.display.w && 
        shape.display.y + shape.display.h <= this.display.y + this.display.h;
  };
  
  return Marquee;
}]);