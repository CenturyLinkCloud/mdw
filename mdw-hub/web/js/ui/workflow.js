'use strict';

var workflowMod = angular.module('mdwWorkflow', ['mdw', 'drawingConstants']);

workflowMod.controller('MdwWorkflowController', 
    ['$scope', '$http', '$routeParams', 'mdw', 'util', 'mdwImplementors', 'Diagram', 'Inspector',
    function($scope, $http, $routeParams, mdw, util, mdwImplementors, Diagram, Inspector) {
  
  $scope.init = function(canvas) {
    if ($scope.serviceBase.endsWith('/'))
      $scope.serviceBase = $scope.serviceBase.substring(0, $scope.serviceBase.length - 1);
    if ($scope.hubBase) {
      if ($scope.hubBase.endsWith('/'))
        $scope.hubBase = $scope.hubBase.substring(0, $scope.hubBase.length - 1);
    }
    else {
      $scope.hubBase = $scope.serviceBase;
    }
    
    $scope.canvas = canvas;
    if ($scope.process.$promise) {
      // wait until resolved
      $scope.process.$promise.then(function(data) {
        $scope.process = data;
        $scope.renderProcess();
        $scope.canvas.bind('mousemove', $scope.mouseMove);
        $scope.canvas.bind('mousedown', $scope.mouseDown);
        $scope.canvas.bind('mouseup', $scope.mouseUp);
        $scope.canvas.bind('mouseout', $scope.mouseOut);
      }, function(error) {
        mdw.messages = error;
      });
    }
    else {
      $scope.renderProcess();
      $scope.canvas.bind('mousemove', $scope.mouseMove);
      $scope.canvas.bind('mousedown', $scope.mouseDown);
      $scope.canvas.bind('mouseup', $scope.mouseUp);
      $scope.canvas.bind('mouseout', $scope.mouseOut);
    }
  };
  
  $scope.dest = function() {
    $scope.canvas.bind('mousemove', $scope.mouseMove);
    $scope.canvas.bind('mousedown', $scope.mouseDown);
    $scope.canvas.bind('mouseup', $scope.mouseUp);
    $scope.canvas.bind('mouseout', $scope.mouseOut);
  };
  
  $scope.renderProcess = function() {
    var packageName = $scope.process.packageName;
    var processName = $scope.process.name;
    var processVersion = null; // TODO: version
    var instanceId = $scope.process.id;
    var masterRequestId = $scope.process.masterRequestId;
    var workflowUrl = $scope.serviceBase + '/Workflow/' + packageName + '/' + processName;
    if (processVersion)
      workflowUrl += '/v' + processVersion;
    $http({ method: 'GET', url: workflowUrl })
      .then(function success(response) {
        $scope.process = response.data;
        $scope.process.packageName = packageName; // not returned in JSON
        // restore summary instance data
        $scope.process.id = instanceId;
        $scope.process.masterRequestId = masterRequestId; 
        $scope.implementors = mdwImplementors.get();
        if ($scope.implementors) {
          $scope.doRender();
        }
        else {
          $http({ method: 'GET', url: $scope.serviceBase + '/Implementors' })
          .then(function success(response) {
            $scope.implementors = response.data;
            mdwImplementors.set($scope.implementors);
            $scope.doRender();
          }, function error(response) {
            mdw.messages = response.statusText;
          });
        }
    });
  };
  
  $scope.doRender = function() {
    if ($scope.renderState && $scope.process.id) {
      $http({ method: 'GET', url: $scope.serviceBase + '/Processes/' + $scope.process.id })
        .then(function success(response) {
          $scope.instance = response.data;
          $scope.diagram = new Diagram($scope.canvas[0], $scope.process, $scope.implementors, $scope.instance, $scope.editable);
          $scope.diagram.draw();
        }, function error(response) {
          mdw.messages = response.statusText;
      });
    }
    else {
      $scope.diagram = new Diagram($scope.canvas[0], $scope.process, $scope.implementors, $scope.instance, $scope.editable);
      $scope.diagram.draw();
    }
  };
  
  $scope.down = false;
  $scope.dragging = false;
  $scope.mouseMove = function(e) {
    if ($scope.down && $scope.editable)
      $scope.dragging = true;
    if ($scope.diagram) {
      if ($scope.dragging) {
        if ($scope.diagram.onMouseDrag(e)) {
          $scope.onChange($scope.process);
        }
      }
      else {
        $scope.diagram.onMouseMove(e);
      }
    }
  };
  $scope.mouseDown = function(e) {
    $scope.down = true;
    if ($scope.diagram) {
      $scope.diagram.onMouseDown(e);
      var selObj = $scope.diagram.selectObj;
      if (selObj && selObj.isLabel)
        selObj = selObj.owner;
      if (selObj) {
        Inspector.setObj(selObj);
      }
      else {
        var bgObj = $scope.diagram.getBackgroundObj(e);
        if (bgObj)
          Inspector.setObj(bgObj);
      }
    }
  };
  $scope.mouseUp = function(e) {
    $scope.down = false;
    $scope.dragging = false;
    if ($scope.diagram) {
      $scope.diagram.onMouseUp(e);
    }
  };
  $scope.mouseOut = function(e) {
    $scope.down = false;
    $scope.dragging = false;
  };
}]);

workflowMod.factory('Diagram', 
    ['$document', 'mdw', 'util', 'DC', 'Label', 'Shape', 'Step', 'Link', 'Subflow', 'Note',
     function($document, mdw, util, DC, Label, Shape, Step, Link, Subflow, Note) {
  var Diagram = function(canvas, process, implementors, instance, editable) {
    Shape.call(this, this, process);
    this.canvas = canvas;
    this.process = process;
    this.implementors = implementors;
    this.instance = instance;
    this.editable = editable;
    this.workflowType = 'process';
    this.isDiagram = true;
    this.context = this.canvas.getContext("2d");
    this.anchor = -1;
    this.drawBoxes = process.attributes.NodeStyle == 'BoxIcon';
  };

  Diagram.prototype = new Shape();
  
  Diagram.prototype.draw = function() {

    this.context.clearRect(0, 0, this.canvas.width, this.canvas.height);

    this.prepareDisplay();

    this.label.draw();
    this.steps.forEach(function(step) {
      step.draw();
    });
    this.links.forEach(function(link) {
      link.draw();
    });
    this.subflows.forEach(function(subflow) {
      subflow.draw();
    });
    this.notes.forEach(function(note) {
      note.draw();
    });
  };
  
  // sets display fields and returns a display with w and h for canvas size
  // (for performance reasons, also initializes steps/links arrays and activity impls)
  Diagram.prototype.prepareDisplay = function() {
    var canvasDisplay = { w: 460, h: 460 };
    
    var diagram = this; // convenient/inner access
    
    // label
    diagram.label = new Label(this, this.process.name, this.getDisplay(), DC.TITLE_FONT);
    diagram.makeRoom(canvasDisplay, diagram.label.prepareDisplay());
    
    // activities
    diagram.steps = [];
    if (this.process.activities) {
      this.process.activities.forEach(function(activity) {
        var step = new Step(diagram, activity);
        step.implementor = diagram.getImplementor(activity.implementor);
        diagram.makeRoom(canvasDisplay, step.prepareDisplay());
        if (diagram.instance)
          step.applyState(diagram.getActivityInstances(activity.id));
        diagram.steps.push(step);
      });
    }
    
    // transitions
    diagram.links = [];
    diagram.steps.forEach(function(step) {
      if (step.activity.transitions) {
        step.activity.transitions.forEach(function(transition) {
          var link = new Link(diagram, transition, step, diagram.getStep(transition.to));
          diagram.makeRoom(canvasDisplay, link.prepareDisplay());
          if (diagram.instance)
            link.applyState(diagram.getTransitionInstances(link.transition.id));
          diagram.links.push(link);
        });
      }
    });
    
    // embedded subprocesses
    diagram.subflows = [];
    if (this.process.subprocesses) {
      this.process.subprocesses.forEach(function(subproc) {
        var subflow = new Subflow(diagram, subproc);
        diagram.makeRoom(canvasDisplay, subflow.prepareDisplay());
        if (diagram.instance) {
          subflow.mainProcessInstanceId = diagram.instance.processInstanceId; // needed for subprocess & task instance retrieval          
          subflow.applyState(diagram.getSubflowInstances(subflow.subprocess.id));
        }
        diagram.subflows.push(subflow);
      });
    }

    // notes
    diagram.notes = [];
    if (this.process.textNotes) {
      this.process.textNotes.forEach(function(textNote) {
        var note = new Note(diagram, textNote);
        diagram.makeRoom(canvasDisplay, note.prepareDisplay());
        diagram.notes.push(note);
      });
    }
    
    // allow for external transition instances, etc
    canvasDisplay.w += 25;
    canvasDisplay.h += 25;
    
    this.canvas.width = canvasDisplay.w;
    this.canvas.height = canvasDisplay.h;
  };
  
  Diagram.prototype.makeRoom = function(canvasDisplay, display) {
    if (display.w > canvasDisplay.w)
      canvasDisplay.w = display.w;
    if (display.h > canvasDisplay.h)
      canvasDisplay.h = display.h;
  };
  
  Diagram.prototype.getStep = function(activityId) {
    for (var i = 0; i < this.steps.length; i++) {
      if (this.steps[i].activity.id == activityId)
        return this.steps[i];
    }
  };

  Diagram.prototype.getLink = function(transitionId) {
    for (var i = 0; i < this.links.length; i++) {
      if (this.links[i].transition.id == transitionId)
        return this.links[i];
    }
  };
  
  Diagram.prototype.getLinks = function(step) {
    var links = [];
    for (var i = 0; i < this.links.length; i++) {
      if (step == this.links[i].to || step == this.links[i].from)
        links.push(this.links[i]);
    }
    return links;
  };

  Diagram.prototype.getSubflow = function(subprocessId) {
    for (var i = 0; i < this.subflows.length; i++) {
      if (this.subflows[i].subprocess.id == subprocessId)
        return this.subflows[i];
    }
  };

  Diagram.prototype.getNote = function(textNoteId) {
    for (var i = 0; i < this.notes.length; i++) {
      if (this.notes[i].textNote.id == textNoteId)
        return this.notes[i];
    }
  };
  
  Diagram.prototype.get = function(id) {
    if (id.startsWith('A'))
      return this.getStep(id);
    else if (id.startsWith('T'))
      return this.getLink(id);
    else if (id.startsWith('P'))
      return this.getSubflow(id);
    else if (id.startsWith('N'))
      return this.getNote(id);
  };
  
  Diagram.prototype.addLink = function(from, to) {
    var maxId = 0;
    this.links.forEach(function(link) {
      var linkId = parseInt(link.transition.id.substring(1));
      if (linkId > maxId)
        maxId = linkId;
    });
    var transition = {
        id: 'T' + (maxId + 1),
        event: 'FINISH',
        to: to.activity.id
    };
    from.activity.transitions.push(transition);
    var link = new Link(this, transition, from, to);
    link.display = {type: Link.LINK_TYPES.ELBOW, lx: 0, ly: 0, xs: [0,0], ys: [0,0]};
    link.calc();
    this.links.push();
  };

  Diagram.prototype.getImplementor = function(className) {
    if (this.implementors) {
      for (var i = 0; i < this.implementors.length; i++) {
        var implementor = this.implementors[i];
        if (implementor.implementorClass == className) {
          return implementor;
        }
      }
    }
    // not found -- return placeholder
    return { implementorClass: className };
  };
  
  Diagram.prototype.getActivityInstances = function(id) {
    if (this.instance) {
      var insts = [];  // should always return something, even if empty
      if (this.instance.activities) {
        var procInstId = this.instance.id;
        this.instance.activities.forEach(function(actInst) {
          if ('A' + actInst.activityId == id) {
            actInst.processInstanceId = procInstId; // needed for subprocess & task instance retrieval
            insts.push(actInst);
          }
        });
      }
      insts.sort(function(a1, a2) {
        return a2.id - a1.id;
      });
      return insts;
    }
  };

  Diagram.prototype.getTransitionInstances = function(id) {
    if (this.instance) {
      var insts = [];  // should always return something, even if empty
      if (this.instance.transitions) {
        this.instance.transitions.forEach(function(transInst) {
          if ('T' + transInst.transitionId == id)
            insts.push(transInst);
        });
      }
      insts.sort(function(t1, t2) {
        return t2.id - t1.id;
      });
      return insts;
    }
  };

  Diagram.prototype.getSubflowInstances = function(id) {
    if (this.instance) {
      var insts = [];  // should always return something, even if empty
      if (this.instance.subprocesses) {
        this.instance.subprocesses.forEach(function(subInst) {
          if ('P' + subInst.processId == id)
            insts.push(subInst);
        });
      }
      insts.sort(function(s1, s2) {
        return s2.id - s1.id;
      });
      return insts;
    }
  };
  
  Diagram.prototype.drawState = function(display, instances, ext, adj) {
    if (instances) {
      var count = instances.length > Step.MAX_INSTS ? Step.MAX_INSTS : instances.length;
      for (var i = 0; i < count; i++) {
        var instance = instances[i];
        var rounding = DC.BOX_ROUNDING_RADIUS;
        if (instance.statusCode) {
          var status = Step.STATUSES[instance.statusCode];
          instance.status = status.status;
          var del = Step.INST_W - Step.OLD_INST_W;
          if (ext) {
            var rem = count - i;
            if (i === 0) {
              this.rect(
                  display.x - Step.OLD_INST_W * rem - del, 
                  display.y - Step.OLD_INST_W * rem - del,
                  display.w + Step.OLD_INST_W * 2* rem + 2 * del,
                  display.h + Step.OLD_INST_W * 2 * rem + 2 * del,
                  status.color, true, rounding);
            } 
            else {
              this.rect(
                  display.x - Step.OLD_INST_W * rem,
                  display.y - Step.OLD_INST_W * rem,
                  display.w + Step.OLD_INST_W * 2 * rem,
                  display.h + Step.OLD_INST_W * 2 * rem,
                  status.color, true, 0);
            }
            rem--;
            this.context.clearRect(
                display.x - Step.OLD_INST_W * rem - 1,
                display.y - Step.OLD_INST_W * rem - 1,
                display.w + Step.OLD_INST_W * 2 * rem + 2,
                display.h + Step.OLD_INST_W * 2 * rem + 2);
          }
          else {
            var x1, y1, w1, h1;
            if (i === 0) {
              this.rect(
                  display.x - adj, 
                  display.y - adj, 
                  display.w + 2 * adj, 
                  display.h + 2* adj, 
                  status.color, true, rounding);
              x1 = display.x + del;
              y1 = display.y + del;
              w1 = display.w - 2 * del;
              h1 = display.h - 2 * del;
            } 
            else {
              x1 = display.x + Step.OLD_INST_W * i + del;
              y1 = display.y + Step.OLD_INST_W * i + del;
              w1 = display.w - Step.OLD_INST_W * 2 * i - 2 * del;
              h1 = display.h - Step.OLD_INST_W * 2 * i - 2 * del;
              if (w1 > 0 && h1 > 0)
                this.rect(x1, y1, w1, h1, status.color, true);
            }
            x1 += Step.OLD_INST_W - 1;
            y1 += Step.OLD_INST_W - 1;
            w1 -= 2 * Step.OLD_INST_W - 2;
            h1 -= 2 * Step.OLD_INST_W - 2;
            if (w1 > 0 && h1 > 0)
              this.context.clearRect(x1, y1, w1, h1);
          }
        }
      }
    }    
  };
  
  Diagram.prototype.roundedRect = function(x, y, w, h, border, fill) {
    this.rect(x, y, w, h, border, fill, DC.BOX_ROUNDING_RADIUS);
  };
  
  Diagram.prototype.rect = function(x, y, w, h, border, fill, r) {
    if (border)
      this.context.strokeStyle = border;
    if (fill)
      this.context.fillStyle = fill;

    if (!r) {
      this.context.strokeRect(x, y, w, h);
      if (fill)
        this.context.fillRect(x, y, w, h);
    }
    else {
      // rounded corners
      this.context.beginPath();
      this.context.moveTo(x + r, y);
      this.context.lineTo(x + w - r, y);
      this.context.quadraticCurveTo(x + w, y, x + w, y + r);
      this.context.lineTo(x + w, y + h - r);
      this.context.quadraticCurveTo(x + w, y + h, x + w - r, y + h);
      this.context.lineTo(x + r, y + h);
      this.context.quadraticCurveTo(x, y + h, x, y + h - r);
      this.context.lineTo(x, y + r);
      this.context.quadraticCurveTo(x, y, x + r, y);
      this.context.closePath();
      
      this.context.stroke();
      if (fill)
        this.context.fill();
    }
    
    this.context.fillStyle = DC.DEFAULT_COLOR;
    this.context.strokeStyle = DC.DEFAULT_COLOR;
  };
  
  Diagram.prototype.drawOval = function(x, y, w, h, fill, fadeTo) {
    var kappa = 0.5522848;
    var ox = (w / 2) * kappa; // control point offset horizontal
    var oy = (h / 2) * kappa; // control point offset vertical
    var xe = x + w; // x-end
    var ye = y + h; // y-end
    var xm = x + w / 2; // x-middle
    var ym = y + h / 2; // y-middle

    this.context.beginPath();
    this.context.moveTo(x, ym);
    this.context.bezierCurveTo(x, ym - oy, xm - ox, y, xm, y);
    this.context.bezierCurveTo(xm + ox, y, xe, ym - oy, xe, ym);
    this.context.bezierCurveTo(xe, ym + oy, xm + ox, ye, xm, ye);
    this.context.bezierCurveTo(xm - ox, ye, x, ym + oy, x, ym);
    this.context.closePath(); // not used correctly? (use to close off open path)
    if (typeof fill === 'undefined') {
      this.context.stroke();
    }
    else {
      if (typeof fadeTo === 'undefined') {
        this.context.fillStyle = fill;
      }
      else {
        var gradient = this.context.createLinearGradient(x, y, x + w, y + h);
        gradient.addColorStop(0, fill);
        gradient.addColorStop(1, 'white');
        this.context.fillStyle = gradient;
      }
      this.context.fill();
      this.context.stroke();
    }
    this.context.fillStyle = DC.DEFAULT_COLOR;
  };
  
  Diagram.prototype.drawLine = function(x1, y1, x2, y2, color) {
    if (color)
      this.context.strokeStyle = color;
    this.context.beginPath();
    this.context.moveTo(x1, y1);
    this.context.lineTo(x2, y2);
    this.context.stroke();
    this.context.strokeStyle = DC.DEFAULT_COLOR;
  };
  
  Diagram.prototype.drawDiamond = function(x, y, w, h) {
    var xh = x + w / 2;
    var yh = y + h / 2;
    this.context.beginPath();
    this.context.moveTo(x, yh);
    this.context.lineTo(xh, y);
    this.context.lineTo(x + w, yh);
    this.context.lineTo(xh, y + h);
    this.context.lineTo(x, yh);
    this.context.closePath();
    this.context.stroke();
  };
  
  Diagram.prototype.drawImage = function(src, x, y) {
    if (!this.images)
      this.images = {};
    var img = this.images[src];
    if (!img) {
      img = new Image();
      img.src = src;
      var context = this.context;
      var images = this.images;
      img.onload = function() {
        context.drawImage(img, x, y);
        images[src] = img;
      };
    }
    else {
      this.context.drawImage(img, x, y);
    }
  };
  
  Diagram.prototype.onMouseDown = function(e) {
    var rect = this.canvas.getBoundingClientRect();
    var x = e.clientX - rect.left;
    var y = e.clientY - rect.top;
    // starting points for drag
    this.dragX = x;
    this.dragY = y;
    
    this.selectObj = this.getHoverObj(x, y);
    this.unselect();
    if (this.selectObj) {
      this.select(this.selectObj);
      if (this.editable && e.shiftKey && this.selectObj.isStep)
        this.shiftDrag = true;
    }
  };
  
  Diagram.prototype.onMouseUp = function(e) {
    if (this.shiftDrag && this.dragX && this.dragY) {
      if (this.selectObj && this.selectObj.isStep) {
        var rect = this.canvas.getBoundingClientRect();
        var x = e.clientX - rect.left;
        var y = e.clientY - rect.top;
        var destObj = this.getHoverObj(x, y);
        if (destObj && destObj.isStep) {
          this.addLink(this.selectObj, destObj);
          this.draw();
        }
      }
    }
    this.shiftDrag = false;
    if (this.selectObj)
      this.reselect(this.selectObj);
  };
  
  Diagram.prototype.onMouseMove = function(e) {
    var rect = this.canvas.getBoundingClientRect();
    var x = e.clientX - rect.left;
    var y = e.clientY - rect.top;
    this.anchor = -1;
    this.hoverObj = this.getHoverObj(x, y);
    if (this.hoverObj) {
      if (this.editable && (this.hoverObj == this.selectObj)) {
        this.anchor = this.hoverObj.getAnchor(x, y);
        if (this.anchor >= 0) {
          if (this.hoverObj.isLink) {
            $document[0].body.style.cursor = 'crosshair';
          }
          else {
            if (this.anchor === 0 || this.anchor == 2)
              $document[0].body.style.cursor = 'nw-resize';
            else if (this.anchor == 1 || this.anchor == 3)
              $document[0].body.style.cursor = 'ne-resize';
          }
        }
        else {
          $document[0].body.style.cursor = 'pointer';
        }
      }
      else {
        $document[0].body.style.cursor = 'pointer';
      }
    }
    else {
      $document[0].body.style.cursor = '';
    }
    
  };
  
  Diagram.prototype.onMouseDrag = function(e) {
    if (this.selectObj && this.dragX && this.dragY) {
      var rect = this.canvas.getBoundingClientRect();
      var x = e.clientX - rect.left;
      var y = e.clientY - rect.top;
      var diagram = this;
      if (this.shiftDrag) {
        if (this.selectObj.isStep) {
          this.draw();
          this.drawLine(this.dragX, this.dragY, x, y, 'green');
        }
      }
      else if (this.anchor >= 0) {
        if (this.selectObj.isLink) {
          this.selectObj.moveAnchor(this.anchor, x - this.dragX, y - this.dragY);
          this.draw();
        }
        if (this.selectObj.resize) {
          if (this.selectObj.isStep) {
            let activityId = this.selectObj.activity.id;
            let step = this.getStep(activityId);
            if (step) {
              this.selectObj.resize(this.dragX, this.dragY, x - this.dragX, y - this.dragY);
              this.getLinks(step).forEach(function(link) {
                link.recalc(step);
              });
            }
            else {
              // try subflows
              this.subflows.forEach(function(subflow) {
                let step = subflow.getStep(activityId);
                if (step) {
                  // only within bounds of subflow
                  diagram.selectObj.resize(diagram.dragX, diagram.dragY, x - diagram.dragX, y - diagram.dragY, subflow.display);
                  subflow.getLinks(step).forEach(function(link) {
                    link.recalc(step);
                  });
                }
              });
            }
          }
          else {
            this.selectObj.resize(this.dragX, this.dragY, x - this.dragX, y - this.dragY);
          }
          this.draw();
          var obj = this.getHoverObj(x, y);
          if (obj)
            this.select(obj);
          return true;
        }
      }
      else if (this.selectObj.move) {
        var deltaX = x - this.dragX;
        var deltaY = y - this.dragY;
        if (this.selectObj.isStep) {
          let activityId = this.selectObj.activity.id;
          let step = this.getStep(activityId);
          if (step) {
            this.selectObj.move(deltaX, deltaY);
            this.getLinks(step).forEach(function(link) {
              link.recalc(step);
            });
          }
          else {
            // try subflows
            this.subflows.forEach(function(subflow) {
              let step = subflow.getStep(activityId);
              if (step) {
                // only within bounds of subflow
                diagram.selectObj.move(deltaX, deltaY, subflow.display);
                subflow.getLinks(step).forEach(function(link) {
                  link.recalc(step);
                });
              }
            });
          }
        }
        else if (this.selectObj.isLink) {
          var link = this.selectObj;
          if (link.label && link.label.isHover(this.dragX, this.dragY)) {
            link.moveLabel(deltaX, deltaY);
          }
        }
        else {
          this.selectObj.move(deltaX, deltaY);
        }
        this.draw();
        var hovObj = this.getHoverObj(x, y);
        if (hovObj)
          this.select(hovObj);
        return true;
      }
    }
  };
  
  Diagram.prototype.getHoverObj = function(x, y) {
    if (this.label.isHover(x, y))
      return this.label;
    // links checked before steps for better anchor selectability
    for (i = 0; i < this.subflows.length; i++) {
      var subflow = this.subflows[i];
      if (subflow.title.isHover(x, y))
        return subflow;
      if (subflow.isHover(x, y)) {
        for (j = 0; j < subflow.links.length; j++) {
          if (subflow.links[j].isHover(x, y))
            return subflow.links[j];
        }
        for (var j = 0; j < subflow.steps.length; j++) {
          if (subflow.steps[j].isHover(x, y))
            return subflow.steps[j];
        }
      }
    }
    for (i = 0; i < this.links.length; i++) {
      if (this.links[i].isHover(x, y))
        return this.links[i];
    }
    for (var i = 0; i < this.steps.length; i++) {
      if (this.steps[i].isHover(x, y))
        return this.steps[i];
    }
    for (i = 0; i < this.notes.length; i++) {
      if (this.notes[i].isHover(x, y))
        return this.notes[i];
    }
  };

  // when nothing selectable is hovered
  Diagram.prototype.getBackgroundObj = function(e) {
    var rect = this.canvas.getBoundingClientRect();
    var x = e.clientX - rect.left;
    var y = e.clientY - rect.top;
    var bgObj = this;
    for (var i = 0; i < this.subflows.length; i++) {
      if (this.subflows[i].isHover(x, y)) {
        bgObj = this.subflows[i];
        break;
      }
    }
    
    if (bgObj == this)
      this.select(this.label);
    else
      this.select(bgObj);
    
    return bgObj;
  };
  
  Diagram.prototype.select = function(obj) {
    if (obj)
      obj.select();
  };

  // removes anchors from currently selected obj, if any
  Diagram.prototype.unselect = function() {
    this.draw();
  };
  
  Diagram.prototype.reselect = function(selObj) {
    if (selObj) {
      var id = selObj.workflowItem ? selObj.workflowItem.id : null;
      if (id) {
        this.selectObj = this.get(id);
        if (!this.selectObj) {
          for (var i = 0; i < this.subflows.length; i++) {
            this.selectObj = this.subflows[i].get(id);
            if (this.selectObj)
              break;
          }
        }
      }
      else {
        this.selectObj = this.title;
      }
    }
  };
  
  Diagram.prototype.getAnchor = function(x, y) {
    return -1;
  };
  
  return Diagram;
  
}]);

// cache implementors
workflowMod.factory('mdwImplementors', ['mdw', function(mdw) {
  return {
    set: function(implementors) {
      this.implementors = implementors;
    },
    get: function() {
      return this.implementors;
    }
  };
}]);

// attributes
//   - process (object): packageName and name must be populated
//     optional process fields
//       - version: for non-latest process version
//       - renderState: if true, display runtime overlay
//       - editable: if true, workflow can be modified
//   - service-base: endpoint url root
//   - hub-base: MDWHub url root
workflowMod.directive('mdwWorkflow', [function() {
  return {
    restrict: 'E',
    templateUrl: 'ui/workflow.html',
    scope: {
      process: '=process',
      onChange: '=onchange',
      renderState: '@renderState',
      editable: '@editable',
      serviceBase: '@serviceBase',
      hubBase: '@hubBase'
    },
    controller: 'MdwWorkflowController',
    controllerAs: 'mdwWorkflow',
    link: function link(scope, elem, attrs, ctrls) {
      scope.init(angular.element(elem[0].getElementsByClassName('mdw-canvas')[0]));
      scope.$on('$destroy', function() {
        scope.dest();
      });
    }
  };
}]);