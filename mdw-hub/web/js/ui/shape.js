'use strict';

var shapeMod = angular.module('mdwShape', ['mdw']);

// prototype for rectangular diagram elements
shapeMod.factory('Shape', ['mdw', 'util', 'DC',
                         function(mdw, util, DC) {
  
  var Shape = function(diagram, workflowItem) {
    this.diagram = diagram;
    this.workflowItem = workflowItem;
  };
  
  Shape.MIN_SIZE = 4;
  
  // get a display object from an attribute value
  Shape.prototype.getDisplay = function() {
    var displayAttr = this.workflowItem.attributes.WORK_DISPLAY_INFO;
    var display = {};
    if (displayAttr) {
      var vals = displayAttr.split(',');
      vals.forEach(function(val) {
        if (val.startsWith('x='))
          display.x = parseInt(val.substring(2));
        else if (val.startsWith('y='))
          display.y = parseInt(val.substring(2));
        else if (val.startsWith('w='))
          display.w = parseInt(val.substring(2));
        else if (val.startsWith('h='))
          display.h = parseInt(val.substring(2));
      });
    }
    return display;
  };

  Shape.prototype.setDisplayAttr = function(x, y, w, h) {
    var attr = 'x=' + x + ',y=' + y;
    if (w)
      attr += ',w=' + w + ',h=' + h;
    this.workflowItem.attributes.WORK_DISPLAY_INFO = attr;
  };
  
  Shape.prototype.getAttr = function(display) {
    var attr = 'x=' + display.x + ',y=' + display.y;
    if (display.w)
      attr += ',w=' + display.w + ',h=' + display.h;
    return attr;
  };
  
  Shape.prototype.isHover = function(x, y) {
    return x >= this.display.x && x <= this.display.x + this.display.w &&
        y >= this.display.y && y <= this.display.y + this.display.h;
  };
  
  Shape.prototype.select = function() {
    var display = this.display;
    var context = this.diagram.context;
    context.fillStyle = DC.ANCHOR_COLOR;
    var s = DC.ANCHOR_W;
    context.fillRect(display.x - s, display.y - s, s * 2, s * 2);
    context.fillRect(display.x + display.w - s, display.y - s, s * 2, s * 2);
    context.fillRect(display.x + display.w - 2, display.y + display.h - s, s * 2, s * 2);
    context.fillRect(display.x - 2, display.y + display.h - s, s * 2, s * 2);
    context.fillStyle = DC.DEFAULT_COLOR;
  };
  
  Shape.prototype.getAnchor = function(x, y) {
    if (Math.abs(this.display.x - x) <= DC.ANCHOR_HIT_W && Math.abs(this.display.y - y) <= DC.ANCHOR_HIT_W)
      return 0;
    else if (Math.abs(this.display.x + this.display.w - x) <= DC.ANCHOR_HIT_W && Math.abs(this.display.y - y) <= DC.ANCHOR_HIT_W)
      return 1;
    else if (Math.abs(this.display.x + this.display.w - x) <= DC.ANCHOR_HIT_W && Math.abs(this.display.y + this.display.h - y) <= DC.ANCHOR_HIT_W)
      return 2;
    else if (Math.abs(this.display.x - x) <= DC.ANCHOR_HIT_W && Math.abs(this.display.y + this.display.h - y) <= DC.ANCHOR_HIT_W)
      return 3;
    else 
      return -1;
  };
  
  Shape.prototype.resizeDisplay = function(x, y, deltaX, deltaY, min, limDisplay) {
    var anchor = this.getAnchor(x, y);
    var display = {x: this.display.x, y: this.display.y, w: this.display.w, h: this.display.h};
    var t1, t2;
    if (anchor === 0) {
      t1 = display.x + display.w;
      t2 = display.y + display.h;
      display.x = x + deltaX;
      display.y = y + deltaY;
      if (t1 - display.x < min)
        display.x = t1 - min;
      if (t2 - display.y < min) 
        display.y = t2 - min;
      display.w = t1 - display.x;
      display.h = t2 - display.y;
    } 
    else if (anchor == 1) {
      t2 = display.y + display.h;
      display.y = y + deltaY;
      if (t2 - display.y < min) 
        display.y = t2 - min;
      display.w = x - (display.x - deltaX);
      if (display.w < min) 
        display.w = min;
      display.h = t2 - display.y;
    } 
    else if (anchor == 2) {
      display.w = x - (display.x - deltaX);
      display.h = y - (display.y - deltaY);
      if (display.w < min) 
        display.w = min;
      if (display.h < min) 
        display.h = min;
    } 
    else if (anchor == 3) {
      t1 = display.x + display.w;
      display.x = x + deltaX;
      if (t1 - display.x < min)
        display.x = t1 - min;
      display.w = t1 - display.x;
      display.h = y - (display.y - deltaY);
      if (display.h < min) 
        display.h = min;
    }
    if (limDisplay) {
      if (display.x < limDisplay.x)
        display.x = limDisplay.x;
      if (display.x + display.w > limDisplay.x + limDisplay.w)
        display.w = limDisplay.x + limDisplay.w - display.x;
      if (display.y < limDisplay.y)
        display.y = limDisplay.y;
      if (display.y + display.h > limDisplay.y + limDisplay.h)
        display.h = limDisplay.y + limDisplay.h - display.y;
    }
    
    return display;
  };
  
  
  return Shape;
  
}]);