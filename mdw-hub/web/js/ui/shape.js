'use strict';

var shapeMod = angular.module('mdwShape', ['mdw']);

// prototype for rectangular diagram elements
shapeMod.factory('Shape', ['mdw', 'util', 'DC',
                         function(mdw, util, DC) {
  
  var Shape = function() {
  };
  
  // get a display object from an attribute value
  Shape.prototype.getDisplay = function(displayAttr) {
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
  
  Shape.prototype.getDisplayAttr = function(x, y, w, h) {
    var attr = 'x=' + x + ',y=' + y;
    if (w)
      attr += ',w=' + w + ',h=' + h;
    return attr;
  };
  
  Shape.prototype.getAttr = function(display) {
    var attr = 'x=' + display.x + ',y=' + display.y;
    if (display.w)
      attr += ',w=' + display.w + ',h=' + display.h;
    return attr;
  };
  
  Shape.prototype.getAnchor = function(x, y) {
    if (Math.abs(this.display.x - x) <= DC.ANCHOR_W && Math.abs(this.display.y - y) <= DC.ANCHOR_W)
      return 0;
    else if (Math.abs(this.display.x + this.display.w - x) <= DC.ANCHOR_W && Math.abs(this.display.y - y) <= DC.ANCHOR_W)
      return 1;
    else if (Math.abs(this.display.x + this.display.w - x) <= DC.ANCHOR_W && Math.abs(this.display.y + this.display.h - y) <= DC.ANCHOR_W)
      return 2;
    else if (Math.abs(this.display.x - x) <= DC.ANCHOR_W && Math.abs(this.display.y + this.display.h - y) <= DC.ANCHOR_W)
      return 3;
    else 
      return -1;
  };
  
  Shape.prototype.resizeDisplay = function(x, y, deltaX, deltaY, min) {
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
    return display;
  };
  
  
  return Shape;
  
}]);