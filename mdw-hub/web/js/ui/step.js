// Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
'use strict';

var stepMod = angular.module('mdwStep', ['mdw']);

stepMod.factory('Step', ['mdw', 'util', 'DC',
                         function(mdw, util, DC) {
  
  var Step = function(activity) {
    this.activity = activity;
    this.workflowType = 'activity';
  };
  
  Step.INST_W = 8;
  Step.OLD_INST_W = 4;
  Step.MAX_INSTS = 10;
  
  Step.STATUSES = [
    {status: 'Unknown', color: 'transparent'},
    {status: 'Pending', color: 'blue'},
    {status: 'In Progress', color: 'green'},
    {status: 'Failed', color: 'red'},
    {status: 'Completed', color: 'black'},
    {status: 'Canceled', color: 'darkgray'},
    {status: 'Hold', color: 'cyan'},
    {status: 'Waiting', color: 'yellow'}
  ];

  Step.prototype.draw = function(diagram) {
    var activity = this.workflowObj = this.activity;
    var shape;
    if (this.implementor.icon && this.implementor.icon.startsWith('shape:'))
      shape = this.implementor.icon.substring(6);
    
    // runtime state first
    if (this.instances) {
      var adj = 0;
      if (shape == 'start' || shape == 'stop')
        adj = 2;
      diagram.drawState(this.display, this.instances, !diagram.drawBoxes, adj);
    }
    
    if (this.implementor.icon) {
      var yAdjust = -2;
      if (shape) {
        if ('start' == shape) {
          diagram.drawOval(this.display.x, this.display.y, this.display.w, this.display.h, 'green', 'white');
        }
        else if ('stop' == shape) {
          diagram.drawOval(this.display.x, this.display.y, this.display.w, this.display.h, 'red', 'white');
        }
        else if ('decision' == shape) {
          diagram.drawDiamond(this.display.x, this.display.y, this.display.w, this.display.h);
          yAdjust = -8;
        }
        else if ('activity' == shape) {
          diagram.roundedRect(this.display.x, this.display.y, this.display.w, this.display.h, DC.BOX_OUTLINE_COLOR);
        }
      }
      else {
        if (diagram.drawBoxes)
          diagram.roundedRect(this.display.x, this.display.y, this.display.w, this.display.h, DC.BOX_OUTLINE_COLOR);
        var iconSrc = mdw.roots.hub + '/asset/' + this.implementor.icon;
        var iconX = this.display.x + this.display.w / 2 - 12;
        var iconY = this.display.y + 5;
        diagram.drawImage(iconSrc, iconX, iconY);
        yAdjust = +4; 
      }
    }
    else {
      diagram.roundedRect(this.display.x, this.display.y, this.display.w, this.display.h, DC.BOX_OUTLINE_COLOR);
    }

    // title
    this.title.lines.forEach(function(line) {
      diagram.context.fillText(line.text, line.x, line.y + yAdjust);
    });
    
    // logical id
    diagram.context.fillStyle = DC.META_COLOR;
    diagram.context.fillText(activity.id, this.display.x + 2, this.display.y - 2);
    diagram.context.fillStyle = DC.DEFAULT_COLOR;
    
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
      title.h += DC.DEFAULT_FONT_SIZE;
      line.x = display.x + display.w / 2 - textMetrics.width / 2;
      line.y = display.y + display.h / 2 + DC.DEFAULT_FONT_SIZE * (i + 0.5);
      if (line.x + textMetrics.width > maxDisplay.w)
        maxDisplay.w = line.x + textMetrics.width;
      if (line.y + DC.DEFAULT_FONT_SIZE > maxDisplay.h)
        maxDisplay.h = line.y + DC.DEFAULT_FONT_SIZE;
    }

    this.display = display;
    this.title = title;    
    
    return maxDisplay;
  };
  
  Step.prototype.applyState = function(activityInstances) {
    this.instances = activityInstances;
  };
      
  return Step;
    
}]);
