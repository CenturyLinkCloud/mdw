// Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
'use strict';

var subflowMod = angular.module('mdwSubflow', ['mdw']);

subflowMod.factory('Subflow', ['$document', 'mdw', 'util', 'DC', 'Step', 'Link',
                                function($document, mdw, util, DC, Step, Link) {
  var Subflow = function(subprocess) {
    this.subprocess = subprocess;
    this.workflowType = 'subprocess';
  };

  Subflow.BOX_OUTLINE_COLOR = '#337ab7';
  
  Subflow.prototype.draw = function(diagram) {

    // runtime state first
    if (this.instances) {
      diagram.drawState(this.display, this.instances, true);
    }
    
    diagram.roundedRect(this.display.x, this.display.y, this.display.w, this.display.h, Subflow.BOX_OUTLINE_COLOR);
    diagram.context.clearRect(this.title.x - 1, this.title.y, this.title.w + 2, this.title.h);

    diagram.context.fillText(this.title.text, this.title.x, this.title.y + DC.DEFAULT_FONT_SIZE);
    this.steps.forEach(function(step) {
      step.draw(diagram);
    });
    this.links.forEach(function(link) {
      link.draw(diagram);
    });
    
    // logical id
    diagram.context.fillStyle = DC.META_COLOR;
    diagram.context.fillText('[' + this.subprocess.id + ']', this.display.x + 10, this.display.y + this.display.h + 4);
    diagram.context.fillStyle = DC.DEFAULT_COLOR;
  };
  
  Subflow.prototype.prepareDisplay = function(diagram) {
    var maxDisplay = { w: 0, h: 0 };
    this.display = diagram.getDisplay(this.subprocess.attributes.WORK_DISPLAY_INFO);
    
    // title
    var title = { text: this.subprocess.name, x: this.display.x + 10, y: this.display.y + 4 - DC.DEFAULT_FONT_SIZE };
    var textMetrics = diagram.context.measureText(title.text);
    title.w = textMetrics.width;
    title.h = DC.DEFAULT_FONT_SIZE;
    if (title.x + title.w > maxDisplay.w)
      maxDisplay.w = title.x + title.w;
    if (title.y + title.h > maxDisplay.h)
      maxDisplay.h = title.y + title.h;
    this.title = title;
    
    // boundaries
    if (this.display.x + this.display.w > maxDisplay.w)
      maxDisplay.w = this.display.x + this.display.w;
    if (this.display.y + this.display.h > maxDisplay.h)
      maxDisplay.h = this.display.y + this.display.h;
    
    var subflow = this;
    // just prepare activities -- assume boundaries account for size
    subflow.steps = [];
    if (this.subprocess.activities) {
      this.subprocess.activities.forEach(function(activity) {
        var step = new Step(activity);
        step.implementor = diagram.getImplementor(activity.implementor);
        step.prepareDisplay(diagram);
        subflow.steps.push(step);
      });
    }
    subflow.links = [];
    subflow.steps.forEach(function(step) {
      if (step.activity.transitions) {
        step.activity.transitions.forEach(function(transition) {
          var link = new Link(transition, step, subflow.getStep(transition.to));
          var display = link.prepareDisplay(diagram);
          subflow.links.push(link);
        });
      }
    });
    return maxDisplay;
  };
  
  Subflow.prototype.applyState = function(subprocessInstances) {
    this.instances = subprocessInstances;
    if (this.instances) {
      var subflow = this;
      this.instances.forEach(function(instance) {
        subflow.steps.forEach(function(step) {
          step.applyState(subflow.getActivityInstances(step.activity.id));
        });
      });
    }
  };
  
  Subflow.prototype.getStep = function(activityId) {
    for (var i = 0; i < this.steps.length; i++) {
      if (this.steps[i].activity.id === activityId)
        return this.steps[i];
    }
  };

  Subflow.prototype.getActivityInstances = function(id) {
    if (this.instances) {
      var actInsts = [];
      this.instances.forEach(function(inst) {
        if (inst.activities) {
          inst.activities.forEach(function(actInst) {
            if ('A' + actInst.activityId == id)
              actInsts.push(actInst);
          });
        }
      });
      actInsts.sort(function(a1, a2) {
        return a2.id - a1.id;
      });
      return actInsts;
    }
  };  
    
  return Subflow;
}]);