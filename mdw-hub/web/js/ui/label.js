'use strict';

var labelMod = angular.module('mdwLabel', ['mdw']);

labelMod.factory('Label', ['$document', 'mdw', 'util', 'Shape', 'DC',
                   function($document, mdw, util, Shape, DC) {
  
  var Label = function(owner, text, display, font) {
    Shape.call(this);
    this.owner = owner;
    this.text = text;
    this.display = display; // just x, y except for process owner
    this.font = font;
    this.workflowItem = owner.workflowItem;
    this.isLabel = true;
  };
  
  Label.prototype = new Shape();
  
  Label.SEL_COLOR = '#e91e63';
  Label.SEL_PAD = 4;
  Label.SEL_ROUNDING_RADIUS = 4;
  
  Label.prototype.draw = function(diagram, color) {
    
    if (this.font)
      diagram.context.font = this.font.FONT;
    diagram.context.fillStyle = color ? color : DC.DEFAULT_COLOR;
    diagram.context.clearRect(this.display.x, this.display.y, this.display.w, this.display.h);
    diagram.context.fillText(this.text, this.display.x, this.display.y + this.display.h / 1.33);
    diagram.context.font = DC.DEFAULT_FONT.FONT;
    diagram.context.fillStyle = DC.DEFAULT_COLOR;
  };
  
  Label.prototype.prepareDisplay = function(diagram) {
    if (this.font)
      diagram.context.font = this.font.FONT;
    var textMetrics = diagram.context.measureText(this.text);
    this.display.w = textMetrics.width;
    this.display.h = this.font.SIZE;
    var maxDisplay = { w: this.display.w + this.display.x, h: this.display.h + this.display.y };
    diagram.context.font = DC.DEFAULT_FONT.FONT;
    return maxDisplay;
  };
  
  Label.prototype.select = function(diagram) {
    var x = this.display.x - Label.SEL_PAD;
    var y = this.display.y - Label.SEL_PAD;
    var w = this.display.w + Label.SEL_PAD + 2;
    var h = this.display.h + Label.SEL_PAD;
    diagram.rect(x, y, w, h, Label.SEL_COLOR, null, Label.SEL_ROUNDING_RADIUS);
  };
  
  Label.prototype.move = function(deltaX, deltaY) {
    var x = this.display.x + deltaX;
    var y = this.display.y + deltaY;
    this.setDisplayAttr(x, y, this.display.w, this.display.h); 
  };
  
  return Label;
}]);