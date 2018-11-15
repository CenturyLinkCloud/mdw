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

  Subflow.create = function(diagram, idNum, startActivityId, startTransitionId, type, x, y) {

    var subprocess = Subflow.newSubprocess(diagram, idNum, type, x, y);
    var subflow = new Subflow(diagram, subprocess);
    subflow.steps = [];
    subflow.links = [];
    subflow.display = {x: x, y: y};
    
    var activityId = startActivityId;
    var activityX = x + 40;
    var activityY = y + 40;
    var transitionId = startTransitionId;
    
    var start = Step.create(diagram, activityId, diagram.getImplementor(Step.START_IMPL), activityX, activityY);
    subprocess.activities.push(start.activity);
    subflow.steps.push(start);

    activityId++;
    
    var task;
    if (type == 'Exception Handler') {
      activityX = x + 170;
      activityY = y + 30;
      task = Step.create(diagram, activityId, diagram.getImplementor(Step.TASK_IMPL), activityX, activityY);
      task.activity.attributes.TASK_PAGELET = Step.TASK_PAGELET;
      task.activity.attributes.STATUS_AFTER_EVENT = 'Cancelled';
      task.activity.name = diagram.process.name + ' Fallout';
      subprocess.activities.push(task.activity);
      subflow.steps.push(task);
      let link = Link.create(diagram, transitionId, start, task);
      subflow.links.push(link);
    }
    
    activityId++;
    activityX = x + 340;
    activityY = y + 40;
    var stop = Step.create(diagram, activityId, diagram.getImplementor(Step.STOP_IMPL), activityX, activityY);
    subprocess.activities.push(stop.activity);
    subflow.steps.push(stop);
    let link = Link.create(diagram, transitionId, task ? task : start, stop);
    subflow.links.push(link);
    
    return subflow;
  };
  
  Subflow.newSubprocess = function(diagram, idNum, type, x, y) {
    var w = 440;
    var h = 120;
    var subprocess = { activities: [],
      attributes: {
        EMBEDDED_PROCESS_TYPE: type,
        PROCESS_VISIBILITY: 'EMBEDDED',
        WORK_DISPLAY_INFO: 'x=' + x + ',y=' + y + ',w=' + w + ',h=' + h
      },
      id: 'P' + idNum,
      name: type
    };
    return subprocess;
  };
  
  Subflow.prototype.draw = function(animate) {

    // runtime state first
    if (this.instances && !animate) {
      this.diagram.drawState(this.display, this.instances, true);
    }
    
    this.diagram.roundedRect(this.display.x, this.display.y, this.display.w, this.display.h, Subflow.BOX_OUTLINE_COLOR);
    this.diagram.context.clearRect(this.title.x - 1, this.title.y, this.title.w + 2, this.title.h);
    this.diagram.context.font = DC.DEFAULT_FONT.FONT;
    this.diagram.context.fillText(this.title.text, this.title.x, this.title.y + DC.DEFAULT_FONT.SIZE);

    // animation sequence controlled by diagram 
    if (!animate) {
      this.steps.forEach(function(step) {
        step.draw();
      });
      this.links.forEach(function(link) {
        link.draw();
      });
    }
    
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
  
  Subflow.prototype.getStart = function() {
    for (var i = 0; i < this.steps.length; i++) {
      if (this.steps[i].activity.implementor == Step.START_IMPL)
        return this.steps[i];
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
  
  Subflow.prototype.getOutLinks = function(step) {
    var links = [];
    for (let i = 0; i < this.links.length; i++) {
      if (step.activity.id == this.links[i].from.activity.id)
        links.push(this.links[i]);
    }
    return links;
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

  Subflow.prototype.deleteStep = function(step) {
    var idx = -1;
    for (let i = 0; i < this.steps.length; i++) {
      var s = this.steps[i];
      if (step.activity.id === s.activity.id) {
        idx = i;
        break;
      }
    }
    if (idx >= 0) {
      this.subprocess.activities.splice(idx, 1);
      this.steps.splice(idx, 1);
      for (let i = 0; i < this.links.length; i++) {
        var link = this.links[i];
        if (link.to.activity.id === step.activity.id) {
          this.deleteLink(link);
        }
      }
    }
  };

  Subflow.prototype.deleteLink = function(link) {
    var idx = -1;
    for (let i = 0; i < this.links.length; i++) {
      var l = this.links[i];
      if (l.transition.id === link.transition.id) {
        idx = i;
        break;
      }
    }
    if (idx >= 0) {
      this.links.splice(idx, 1);
      var tidx = -1;
      for (let i = 0; i < link.from.activity.transitions.length; i++) {
        if (link.from.activity.transitions[i].id === link.transition.id) {
          tidx = i;
          break;
        }
      }
      if (tidx >= 0) {
        link.from.activity.transitions.splice(tidx, 1);
      }
    }
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
              // needed for subprocess & task instance retrieval
              actInst.processInstanceId = procInstId;
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
  
  Subflow.prototype.getTransitionInstances = function(id) {
    if (this.instances) {
      var transInsts = [];
      this.instances.forEach(function(inst) {
        if (inst.transitions) {
          inst.transitions.forEach(function(transInst) {
            if ('T' + transInst.transitionId == id)
              transInsts.push(transInst);
          });
        }
      });
      transInsts.sort(function(t1, t2) {
        return t2.id - t1.id;
      });
      return transInsts;
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