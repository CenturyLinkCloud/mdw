'use strict';

var diagramMod = angular.module('mdwDiagram', ['mdw', 'drawingConstants']);

diagramMod.factory('Diagram', 
    ['$document', 'mdw', 'util', 'Label', 'Shape', 'Step', 'Link', 'Subflow', 'Note', 'Marquee', 'Selection', 'Toolbox', 'DC',
     function($document, mdw, util, Label, Shape, Step, Link, Subflow, Note, Marquee, Selection, Toolbox, DC) {

    var Diagram = function(canvas, dialog, process, implementors, imgBase, editable, instance) {
    Shape.call(this, this, process);
    this.canvas = canvas;
    this.dialog = dialog;
    this.process = process;
    this.implementors = implementors;
    this.imgBase = imgBase;
    this.editable = editable;
    this.instance = instance;
    this.workflowType = 'process';
    this.isDiagram = true;
    this.context = this.canvas.getContext("2d");
    this.anchor = -1;
    this.drawBoxes = process.attributes.NodeStyle == 'BoxIcon';
    this.selection = new Selection(this);
  };

  Diagram.prototype = new Shape();

  Diagram.BOUNDARY_DIM = 25;

  Diagram.prototype.draw = function(reveal) {

    this.context.clearRect(0, 0, this.canvas.width, this.canvas.height);

    this.prepareDisplay();
    
    if (reveal) {
      this.label.draw();
      var st = this.getStart();
      st.draw();
    }
    else {
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
    }
    
    if (this.marquee) {
      this.marquee.draw();
    }
  };

  // sets display fields and returns a display with w and h for canvas size
  // (for performance reasons, also initializes steps/links arrays and activity impls)
  Diagram.prototype.prepareDisplay = function() {
    var canvasDisplay = { w: 640, h: 480 };
    
    var diagram = this; // forEach inner access
    
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
    
    // marquee
    if (this.marquee)
      diagram.makeRoom(canvasDisplay, this.marquee.prepareDisplay());
    
    // allow extra room
    canvasDisplay.w += Diagram.BOUNDARY_DIM;
    canvasDisplay.h += Diagram.BOUNDARY_DIM;
    
    if (this.editable) {
      var toolbox = Toolbox.getToolbox();
      // fill available
      var parentWidth = this.canvas.parentElement.offsetWidth;
      if (toolbox)
        parentWidth -= toolbox.getWidth();
      if (canvasDisplay.w < parentWidth)
        canvasDisplay.w = parentWidth;
      if (toolbox && canvasDisplay.h < toolbox.getHeight())
        canvasDisplay.h = toolbox.getHeight();
    }
    
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
  
  Diagram.prototype.getStart = function() {
    for (var i = 0; i < this.steps.length; i++) {
      if (this.steps[i].activity.implementor == Step.START_IMPL)
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
      if (step.activity.id == this.links[i].to.activity.id || step.activity.id == this.links[i].from.activity.id)
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
    return { implementorClass: className, category: 'com.centurylink.mdw.activity.types.GeneralActivity', icon: 'shape:activity', label: 'Unknown Implementer' };
  };

  Diagram.prototype.addStep = function(impl, x, y) {
    var implementor = this.getImplementor(impl);
    var step = Step.create(this, this.genId(this.steps, 'activity'), implementor, x, y);
    this.process.activities.push(step.activity);
    this.steps.push(step);
  };

  Diagram.prototype.addLink = function(from, to) {
    var link = Link.create(this, this.genId(this.links, 'transition'), from, to);
    this.links.push(link);
  };

  Diagram.prototype.addSubflow = function(type, x, y) {
    var startActivityId = this.genId(this.steps, 'activity');
    var startTransitionId = this.genId(this.links, 'transition');
    var subprocId = this.genId(this.subflows, 'subprocess');
    var subflow = Subflow.create(this, subprocId, startActivityId, startTransitionId, type, x, y);
    if (!this.process.subprocesses)
      this.process.subprocesses = [];
    this.process.subprocesses.push(subflow.subprocess);
    this.subflows.push(subflow);
  };

  Diagram.prototype.addNote = function(x, y) {
    var note = Note.create(this, this.genId(this.notes, 'textNote'), x, y);
    this.process.textNotes.push(note.textNote);
    this.notes.push(note);
  };

  Diagram.prototype.genId = function(items, workflowType) {
    var maxId = 0;
    if (items) {
      items.forEach(function(item) {
        var itemId = parseInt(item[workflowType].id.substring(1));
        if (itemId > maxId)
          maxId = itemId;
      });
    }
    return maxId + 1;
  };

  Diagram.prototype.deleteStep = function(step) {
    var idx = -1;
    for (let i = 0; i < this.steps.length; i++) {
      var s = this.steps[i];
      if (step.activity.id === s.activity.id) {
        idx = i;
        break;
      }
    }
    if (idx >= 0) {
      this.process.activities.splice(idx, 1);
      this.steps.splice(idx, 1);
    }
    for (let i = 0; i < this.links.length; i++) {
      var link = this.links[i];
      if (link.to.activity.id === step.activity.id) {
        this.deleteLink(link);
      }
    }
  };

  Diagram.prototype.deleteLink = function(link) {
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

  Diagram.prototype.deleteSubflow = function(subflow) {
    var idx = -1;
    for (let i = 0; i < this.subflows.length; i++) {
      var s = this.subflows[i];
      if (s.subprocess.id === subflow.subprocess.id) {
        idx = i;
        break;
      }
    }
    if (idx >= 0) {
      this.process.subprocesses.splice(idx, 1);
      this.subflows.splice(idx, 1);
    }
  };

  Diagram.prototype.deleteNote = function(note) {
    var idx = -1;
    for (let i = 0; i < this.notes.length; i++) {
      var n = this.notes[i];
      if (n.textNote.id === note.textNote.id) {
        idx = i;
        break;
      }
    }
    if (idx >= 0) {
      this.process.textNotes.splice(idx, 1);
      this.notes.splice(idx, 1);
    }
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
                  status.color, status.color, rounding);
            } 
            else {
              this.rect(
                  display.x - Step.OLD_INST_W * rem,
                  display.y - Step.OLD_INST_W * rem,
                  display.w + Step.OLD_INST_W * 2 * rem,
                  display.h + Step.OLD_INST_W * 2 * rem,
                  status.color, status.color, 0);
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
                  status.color, status.color, rounding);
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
                this.rect(x1, y1, w1, h1, status.color, status.color);
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
    src = this.imgBase + '/' + src;
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
    
    var selObj = this.getHoverObj(x, y);
    
    if (this.editable && e.ctrlKey) {
      if (selObj) {
        if (this.selection.includes(selObj))
          this.selection.remove(selObj);
        else
          this.selection.add(selObj);
        selObj.select();
      }
    }
    else {
      if (!this.selection.includes(selObj)) {
        // normal single select
        this.selection.setSelectObj(selObj);
        this.unselect();
        if (this.selection.getSelectObj()) {
          this.selection.getSelectObj().select();
          if (this.editable && e.shiftKey && this.selection.getSelectObj().isStep)
            this.shiftDrag = true;
        }
      }
    }
  };

  Diagram.prototype.onMouseUp = function(e) {
    if (this.shiftDrag && this.dragX && this.dragY) {
      if (this.selection.getSelectObj() && this.selection.getSelectObj().isStep) {
        var rect = this.canvas.getBoundingClientRect();
        var x = e.clientX - rect.left;
        var y = e.clientY - rect.top;
        var destObj = this.getHoverObj(x, y);
        if (destObj && destObj.isStep) {
          this.addLink(this.selection.getSelectObj(), destObj);
          this.draw();
        }
      }
    }
    this.shiftDrag = false;
    
    if (this.marquee) {
      this.selection.setSelectObj(null);
      var selObjs = this.marquee.getSelectObjs();
      for (let i = 0; i < selObjs.length; i++) {
        selObjs[i].select();
        this.selection.add(selObjs[i]);
      }
      this.marquee = null;
    }
    else {
      this.selection.reselect();
    }
  };

  Diagram.prototype.onMouseOut = function(e) {
    $document[0].body.style.cursor = 'default';
  };

  Diagram.prototype.onMouseMove = function(e) {
    var rect = this.canvas.getBoundingClientRect();
    var x = e.clientX - rect.left;
    var y = e.clientY - rect.top;
    this.anchor = -1;
    this.hoverObj = this.getHoverObj(x, y);
    if (this.hoverObj) {
      if (this.editable && (this.hoverObj == this.selection.getSelectObj())) {
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
    if (this.editable && this.dragX && this.dragY && !e.ctrlKey) {
      var rect = this.canvas.getBoundingClientRect();
      var x = e.clientX - rect.left;
      var y = e.clientY - rect.top;
      var deltaX = x - this.dragX;
      var deltaY = y - this.dragY;

      if (Math.abs(deltaX) > DC.MIN_DRAG || Math.abs(deltaY) > DC.MIN_DRAG) {
        
        if (x > rect.right - Diagram.BOUNDARY_DIM)
          this.canvas.width = this.canvas.width + Diagram.BOUNDARY_DIM;
        if (y > rect.bottom - Diagram.BOUNDARY_DIM)
          this.canvas.height = this.canvas.height + Diagram.BOUNDARY_DIM;

        if (this.selection.getSelectObj()) {
          var diagram = this;
          if (this.shiftDrag) {
            if (this.selection.getSelectObj().isStep) {
              this.draw();
              this.drawLine(this.dragX, this.dragY, x, y, DC.LINE_COLOR);
              return true;
            }
          }
          else if (this.anchor >= 0) {
            if (this.selection.getSelectObj().isLink) {
              let link = this.selection.getSelectObj();
              link.moveAnchor(this.anchor, x - this.dragX, y - this.dragY);
              if (this.anchor === 0) {
                let hovStep = this.getHoverStep(x, y);
                if (hovStep && link.from.activity.id != hovStep.activity.id)
                  link.setFrom(hovStep);
              }
              else if (this.anchor == this.selection.getSelectObj().display.xs.length - 1) {
                var hovStep = this.getHoverStep(x, y);
                if (hovStep && link.to.activity.id != hovStep.activity.id)
                  link.setTo(hovStep);
              }
              this.draw();
            }
            if (this.selection.getSelectObj().resize) {
              if (this.selection.getSelectObj().isStep) {
                let activityId = this.selection.getSelectObj().activity.id;
                let step = this.getStep(activityId);
                if (step) {
                  this.selection.getSelectObj().resize(this.dragX, this.dragY, x - this.dragX, y - this.dragY);
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
                      diagram.selection.getSelectObj().resize(diagram.dragX, diagram.dragY, x - diagram.dragX, y - diagram.dragY, subflow.display);
                      subflow.getLinks(step).forEach(function(link) {
                        link.recalc(step);
                      });
                    }
                  });
                }
              }
              else {
                this.selection.getSelectObj().resize(this.dragX, this.dragY, x - this.dragX, y - this.dragY);
              }
              this.draw();
              var obj = this.getHoverObj(x, y);
              if (obj)
                obj.select();
              return true;
            }
          }
          else {
            this.selection.move(this.dragX, this.dragY, deltaX, deltaY);
            // non-workflow selection may not be reselected after move
            var hovObj = this.diagram.getHoverObj(x, y);
            if (hovObj)
              hovObj.select();
            return true;
          }
        }
        else {
          if (this.marquee) {
            this.marquee.resize(this.dragX, this.dragY, x - this.dragX, y - this.dragY);
          }
          else {
            this.marquee = new Marquee(this);
            this.marquee.start(this.dragX, this.dragY);
          }
          this.draw();
        }
      }
    }
  };

  Diagram.prototype.onDrop = function(e, item) {
    var rect = this.canvas.getBoundingClientRect();
    var x = e.clientX - rect.left;
    var y = e.clientY - rect.top;
    if (item.category == 'subflow') {
      this.addSubflow(item.implementorClass, x, y);
    }
    else if (item.category == 'note') {
      this.addNote(x, y);
    }
    else {
      this.addStep(item.implementorClass, x, y);
    }
    this.draw();
    return true;
  };

  Diagram.prototype.onDelete = function(e, onChange) {
    var selection = this.selection;
    var selObj = this.selection.getSelectObj();
    if (selObj && !selObj.isLabel) {
      var msg = this.selection.isMulti ? 'Delete selected items?' : 'Delete ' + selObj.workflowType + '?';
      this.dialog.confirm('Confirm Delete', msg, function(res) {
        if (res) {
          selection.doDelete();
          selection.diagram.draw();
          onChange();
        }
      });
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

  Diagram.prototype.getHoverStep = function(x, y) {
    for (let i = 0; i < this.subflows.length; i++) {
      var subflow = this.subflows[i];
      if (subflow.isHover(x, y)) {
        for (var j = 0; j < subflow.steps.length; j++) {
          if (subflow.steps[j].isHover(x, y))
            return subflow.steps[j];
        }
      }
    }
    for (let i = 0; i < this.steps.length; i++) {
      if (this.steps[i].isHover(x, y))
        return this.steps[i];
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
      this.label.select();
    else
      bgObj.select();
    
    return bgObj;
  };

  // removes anchors from currently selected obj, if any
  Diagram.prototype.unselect = function() {
    this.draw();
  };

  Diagram.prototype.getAnchor = function(x, y) {
    return -1; // not applicable
  };
  
  return Diagram;  
}]);