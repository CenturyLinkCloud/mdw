'use strict';

var labelMod = angular.module('mdwLabel', ['mdw']);

labelMod.factory('Label', ['$document', 'mdw', 'util', 'Shape', 'DC',
                   function($document, mdw, util, Shape, DC) {
  
  var Label = function(owner, display, font) {
    Shape.call(this);
    this.owner = owner;
    this.display = display; // just x, y except for process owner
    this.font = font;
    this.isLabel = true;
  };
  
  Label.prototype = new Shape();
  
  Label.prototype.draw = function(diagram) {
    
    if (this.font)
      diagram.context.font = this.font.FONT;
    diagram.context.fillStyle = DC.DEFAULT_COLOR;
    diagram.context.clearRect(this.display.x, this.display.y, this.display.w, this.display.h);
    diagram.context.fillText(this.owner.workflowItem.name, this.display.x, this.display.y + this.display.h / 1.33);
    diagram.context.font = DC.DEFAULT_FONT.FONT;
  };
  
  Label.prototype.prepareDisplay = function(diagram) {
    if (this.font)
      diagram.context.font = this.font.FONT;
    var textMetrics = diagram.context.measureText(this.owner.workflowItem.name);
    this.display.w = textMetrics.width;
    this.display.h = this.font.SIZE;
    var maxDisplay = { w: this.display.w + this.display.x, h: this.display.h + this.display.y };
    diagram.context.font = DC.DEFAULT_FONT.FONT;
    return maxDisplay;
  };
  
  Label.prototype.move = function(deltaX, deltaY) {
    var x = this.display.x + deltaX;
    var y = this.display.y + deltaY;
    this.owner.setDisplayAttr(x, y, this.display.w, this.display.h); 
  };
  
  return Label;
}]);