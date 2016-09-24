// Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
'use strict';

var stepMod = angular.module('mdwStep', ['mdw']);

stepMod.factory('Step', ['mdw', 'util', function(mdw, util) {
  
  var Step = function(activity) {
    this.activity = activity;
    this.workflowType = 'activity';
  };
  
  Step.DEFAULT_FONT_SIZE = 12;
  Step.DEFAULT_COLOR = 'black';
  Step.BOX_OUTLINE_COLOR = 'black';
  Step.META_COLOR = 'gray';

  Step.prototype.draw = function(diagram) {
    var activity = this.workflowObj = this.activity;

    if (this.implementor.icon) {
      var yAdjust = -2;
      if (this.implementor.icon.startsWith('shape:')) {
        var shape = this.implementor.icon.substring(6);
        if ('start' == shape) {
          this.drawOval(diagram.context, this.display.x, this.display.y, this.display.w, this.display.h, 'green', 'white');
        }
        else if ('stop' == shape) {
          this.drawOval(diagram.context, this.display.x, this.display.y, this.display.w, this.display.h, 'red', 'white');
        }
        else if ('decision' == shape) {
          this.drawDiamond(diagram.context, this.display.x, this.display.y, this.display.w, this.display.h);
          yAdjust = -8;
        }
        else if ('activity' == shape) {
          diagram.drawRoundedBox(diagram.context, this.display.x, this.display.y, this.display.w, this.display.h, Step.BOX_OUTLINE_COLOR);
        }
      }
      else {
        diagram.drawRoundedBox(diagram.context, this.display.x, this.display.y, this.display.w, this.display.h, Step.BOX_OUTLINE_COLOR);
        var iconImg = new Image();
        iconImg.src = mdw.roots.hub + '/asset/' + this.implementor.icon;
        var iconx = this.display.x + this.display.w / 2 - 12;
        var icony = this.display.y + 5;
        iconImg.onload = function() {
          diagram.context.drawImage(iconImg, iconx, icony);
        };
        yAdjust = +4; 
      }
    }
    else {
      diagram.drawRoundedBox(diagram.context, this.display.x, this.display.y, this.display.w, this.display.h, Step.BOX_OUTLINE_COLOR);
    }

    // title
    this.title.lines.forEach(function(line) {
      diagram.context.fillText(line.text, line.x, line.y + yAdjust);
    });
    
    // logical id
    diagram.context.fillStyle = Step.META_COLOR;
    diagram.context.fillText(activity.id, this.display.x + 2, this.display.y - 2);
    diagram.context.fillStyle = Step.DEFAULT_COLOR;
  };
  
  // sets display/title and returns an object with w and h for required size
  Step.prototype.prepareDisplay = function(diagram) {
    var maxDisplay = { w: 0, h: 0};
    var display = diagram.getDisplay(this.activity.attributes.WORK_DISPLAY_INFO);

    if (display.x + display.w > maxDisplay.w)
      maxDisplay.w = display.x + display.w;
    if (display.y + display.h > maxDisplay.h)
      maxDisplay.h = display.y + display.h;
    
    // step title
    var titleLines = [];
    this.activity.name.getLines().forEach(function(line) {
      titleLines.push({ text: line });
    });
    var title = { text: this.activity.name, lines: titleLines, w: 0, h:0 };
    for (var i = 0; i < title.lines.length; i++) {
      var line = title.lines[i];
      var textMetrics = diagram.context.measureText(line.text);
      if (textMetrics.width > title.w)
        title.w = textMetrics.width;
      title.h += Step.DEFAULT_FONT_SIZE;
      line.x = display.x + display.w / 2 - textMetrics.width / 2;
      line.y = display.y + display.h / 2 + Step.DEFAULT_FONT_SIZE * (i + 0.5);
      if (line.x + textMetrics.width > maxDisplay.w)
        maxDisplay.w = line.x + textMetrics.width;
      if (line.y + Step.DEFAULT_FONT_SIZE > maxDisplay.h)
        maxDisplay.h = line.y + Step.DEFAULT_FONT_SIZE;
    }

    this.display = display;
    this.title = title;    
    
    return maxDisplay;
  };
  
  Step.prototype.drawOval = function(context, x, y, w, h, fill, fadeTo) {
    var kappa = 0.5522848;
    var ox = (w / 2) * kappa; // control point offset horizontal
    var oy = (h / 2) * kappa; // control point offset vertical
    var xe = x + w; // x-end
    var ye = y + h; // y-end
    var xm = x + w / 2; // x-middle
    var ym = y + h / 2; // y-middle

    context.beginPath();
    context.moveTo(x, ym);
    context.bezierCurveTo(x, ym - oy, xm - ox, y, xm, y);
    context.bezierCurveTo(xm + ox, y, xe, ym - oy, xe, ym);
    context.bezierCurveTo(xe, ym + oy, xm + ox, ye, xm, ye);
    context.bezierCurveTo(xm - ox, ye, x, ym + oy, x, ym);
    context.closePath(); // not used correctly? (use to close off open path)
    if (typeof fill === 'undefined') {
      context.stroke();
    }
    else {
      if (typeof fadeTo === 'undefined') {
        context.fillStyle = fill;
      }
      else {
        var gradient = context.createLinearGradient(x, y, x + w, y + h);
        gradient.addColorStop(0, fill);
        gradient.addColorStop(1, 'white');
        context.fillStyle = gradient;
      }
      context.fill();
      context.stroke();
    }
    context.fillStyle = Step.DEFAULT_COLOR;
  };
  
  Step.prototype.drawDiamond = function(context, x, y, w, h) {
    var xh = x + w / 2;
    var yh = y + h / 2;
    context.beginPath();
    context.moveTo(x, yh);
    context.lineTo(xh, y);
    context.lineTo(x + w, yh);
    context.lineTo(xh, y + h);
    context.lineTo(x, yh);
    context.closePath();
    context.stroke();
  };
    
  return Step;
    
}]);
