'use strict';

var subflowMod = angular.module('mdwSubflow', ['mdw']);

subflowMod.factory('Subflow', ['$document', 'mdw', 'util', 'Shape', 'DC', 'Step', 'Link',
                                function($document, mdw, util, Shape, DC, Step, Link) {
  var Subflow = function(diagram, subprocess) {
    Shape.call(this, diagram, subprocess);
    this.diagram = diagram;
    this.subprocess = subprocess;
    this.workflowType = 'subprocess';
    this.isSubflow = true;
  };
  
  Subflow.prototype = new Shape();

  Subflow.BOX_OUTLINE_COLOR = '#337ab7';
  Subflow.HIT_WIDTH = 7;
  
  Subflow.prototype.draw = function() {

    // runtime state first
    if (this.instances) {
      this.diagram.drawState(this.display, this.instances, true);
    }
    
    this.diagram.roundedRect(this.display.x, this.display.y, this.display.w, this.display.h, Subflow.BOX_OUTLINE_COLOR);
    this.diagram.context.clearRect(this.title.x - 1, this.title.y, this.title.w + 2, this.title.h);

    this.diagram.context.fillText(this.title.text, this.title.x, this.title.y + DC.DEFAULT_FONT.SIZE);
    this.steps.forEach(function(step) {
      step.draw();
    });
    this.links.forEach(function(link) {
      link.draw();
    });
    
    // logical id
    this.diagram.context.fillStyle = DC.META_COLOR;
    this.diagram.context.fillText('[' + this.subprocess.id + ']', this.display.x + 10, this.display.y + this.display.h + 4);
    this.diagram.context.fillStyle = DC.DEFAULT_COLOR;
  };
  
  Subflow.prototype.prepareDisplay = function() {
    var maxDisplay = { w: 0, h: 0 };
    this.display = this.getDisplay();
    
    // title
    var title = { 
        subflow: this,
        text: this.subprocess.name,
        x: this.display.x + 10, 
        y: this.display.y + 4 - DC.DEFAULT_FONT.SIZE,        
        isHover: function(x, y) {
          var hov = x >= this.x && x <= this.x + this.w &&
              y >= this.y && y <= this.y + this.h;
          if (!hov) {
            var context = subflow.diagram.context;
            context.lineWidth = Subflow.HIT_WIDTH;
            var display = this.subflow.display; 
            var r = DC.BOX_ROUNDING_RADIUS;
            context.beginPath();
            context.moveTo(display.x + r, display.y);
            context.lineTo(display.x + display.w - r, display.y);
            context.quadraticCurveTo(display.x + display.w, display.y, x + display.w, display.y + r);
            context.lineTo(display.x + display.w, display.y + display.h - r);
            context.quadraticCurveTo(display.x + display.w, display.y + display.h, display.x + display.w - r, display.y + display.h);
            context.lineTo(display.x + r, display.y + display.h);
            context.quadraticCurveTo(display.x, display.y + display.h, display.x, display.y + display.h - r);
            context.lineTo(display.x, display.y + r);
            context.quadraticCurveTo(display.x, display.y, display.x + r, display.y);
            context.closePath();
            hov = context.isPointInStroke(x, y);
            context.lineWidth = DC.DEFAULT_LINE_WIDTH;
          }
          return hov;
        }
    };
    var textMetrics = this.diagram.context.measureText(title.text);
    title.w = textMetrics.width;
    title.h = DC.DEFAULT_FONT.SIZE;
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
        var step = new Step(subflow.diagram, activity);
        step.implementor = subflow.diagram.getImplementor(activity.implementor);
        step.prepareDisplay();
        subflow.steps.push(step);
      });
    }
    subflow.links = [];
    subflow.steps.forEach(function(step) {
      if (step.activity.transitions) {
        step.activity.transitions.forEach(function(transition) {
          var link = new Link(subflow.diagram, transition, step, subflow.getStep(transition.to));
          var display = link.prepareDisplay();
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
      if (this.steps[i].activity.id == activityId)
        return this.steps[i];
    }
  };
  
  Subflow.prototype.getLink = function(transitionId) {
    for (var i = 0; i < this.links.length; i++) {
      if (this.links[i].transition.id == transitionId)
        return this.links[i];
    }
  };
  
  Subflow.prototype.getLinks = function(step) {
    var links = [];
    for (var i = 0; i < this.links.length; i++) {
      if (step == this.links[i].to || step == this.links[i].from)
        links.push(this.links[i]);
    }
    return links;
  };
  
  Subflow.prototype.get = function(id) {
    if (id.startsWith('A'))
      return this.getStep(id);
    else if (id.startsWith('T'))
      return this.getLink(id);
  };
  
  Subflow.prototype.getActivityInstances = function(id) {
    if (this.instances) {
      var actInsts = [];
      var mainProcessInstanceId = this.mainProcessInstanceId;
      this.instances.forEach(function(inst) {
        if (inst.activities) {
          var procInstId = mainProcessInstanceId;
          inst.activities.forEach(function(actInst) {
            if ('A' + actInst.activityId == id) {
              actInsts.push(actInst);
              actInst.processInstanceId = procInstId; // needed for subprocess & task instance retrieval
            }
          });
        }
      });
      actInsts.sort(function(a1, a2) {
        return a2.id - a1.id;
      });
      return actInsts;
    }
  };
  
  Subflow.prototype.move = function(deltaX, deltaY) {
    var x = this.display.x + deltaX;
    var y = this.display.y + deltaY;
    this.setDisplayAttr(x, y, this.display.w, this.display.h);
    
    this.steps.forEach(function(step) {
      step.move(deltaX, deltaY);
    });
    this.links.forEach(function(link) {
      link.move(deltaX, deltaY);
    });
  };
  
  Subflow.prototype.resize = function(x, y, deltaX, deltaY) {
    var display = this.resizeDisplay(x, y, deltaX, deltaY, Step.MIN_SIZE);
    this.setDisplayAttr(display.x, display.y, display.w, display.h);
  };
  
  return Subflow;
}]);