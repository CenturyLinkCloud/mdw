// Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
'use strict';

var workflowMod = angular.module('mdwWorkflow', ['mdw']);

workflowMod.controller('MdwWorkflowController', 
    ['$scope', '$http', '$routeParams', 'mdw', 'util', 'Diagram', 'Inspector',
    function($scope, $http, $routeParams, mdw, util, Diagram, Inspector) {
  
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
    if ($scope.hubBase.endsWith('/MDWHub'))
      $scope.adminBase = $scope.hubBase.substring(0, $scope.hubBase.length - 7) + '/mdw-admin';
    else
      $scope.adminBase = $scope.hubBase.substring(0, $scope.hubBase.length - 4) + '/mdw-admin'; 
    Inspector.setAdminBase($scope.adminBase); 
    
    $scope.canvas = canvas;
    if ($scope.process.$promise) {
      // wait until resolved
      $scope.process.$promise.then(function(data) {
        $scope.process = data;
        $scope.renderProcess();
        $scope.canvas.bind('mousemove', $scope.mouseMove);
        $scope.canvas.bind('click', $scope.mouseClick);
      }, function(error) {
        mdw.messages = error;
      });
    }
    else {
      $scope.renderProcess();
      $scope.canvas.bind('mousemove', $scope.mouseMove);
      $scope.canvas.bind('click', $scope.mouseClick);
    }
  };
  
  $scope.dest = function() {
    $scope.canvas.bind('mousemove', $scope.mouseMove);
    $scope.canvas.bind('click', $scope.mouseClick);
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
        $http({ method: 'GET', url: $scope.serviceBase + '/Implementors?pageletAsJson=true' })
          .then(function success(response) {
            $scope.implementors = response.data;
            if ($scope.process.id) {
              $http({ method: 'GET', url: $scope.serviceBase + '/Processes/' + $scope.process.id })
                .then(function success(response) {
                  $scope.instance = response.data;
                  $scope.diagram = new Diagram($scope.canvas[0], $scope.process, $scope.implementors, $scope.instance);
                  $scope.diagram.draw();
                }, function error(response) {
                  mdw.messages = response.statusText;
              });
            }
            else {
              $scope.diagram = new Diagram($scope.canvas[0], $scope.process, $scope.implementors);
              $scope.diagram.draw();
            }
          }, function error(response) {
            mdw.messages = response.statusText;
        });
    });
  };
  
  $scope.mouseMove = function(e) {
    if ($scope.diagram)
      $scope.diagram.onMouseMove(e);
  };
  $scope.mouseClick = function(e) {
    if ($scope.diagram) {
      var prevSelectObj = $scope.diagram.selectObj;
      $scope.diagram.onMouseClick(e);
      if ($scope.diagram.selectObj !== prevSelectObj) {
        var selObj = $scope.diagram.selectObj;
        if (selObj)
          Inspector.setObj(selObj);
        else
          Inspector.setObj($scope.diagram);
      }
    }
  };
}]);

workflowMod.factory('Diagram', ['$document', 'mdw', 'util', 'Step', 'Link', 'Subflow', 'Note',
                                function($document, mdw, util, Step, Link, Subflow, Note) {
  var Diagram = function(canvas, process, implementors, instance) {
    this.canvas = canvas;
    this.process = process;
    this.implementors = implementors;
    this.instance = instance;
    this.workflowType = 'process';
    this.context = this.canvas.getContext("2d");
  };

  Diagram.DEFAULT_FONT_SIZE = 12;
  Diagram.DEFAULT_FONT =  Diagram.DEFAULT_FONT_SIZE + 'px sans-serif';
  Diagram.TITLE_FONT_SIZE = 18;
  Diagram.TITLE_FONT = 'bold ' + Diagram.TITLE_FONT_SIZE + 'px sans-serif';
  Diagram.DEFAULT_COLOR = 'black';
  Diagram.HYPERLINK_COLOR = '#1565c0';
  Diagram.BOX_BOUNDING_RADIUS = 12;  
  
  Diagram.prototype.draw = function() {

    this.context.clearRect(0, 0, this.canvas.width, this.canvas.height);
    // TODO: grid when diagram is editable
    // TODO: status info when instance
    var canvasDisplay = this.prepareDisplay();

    this.canvas.width = canvasDisplay.w;
    this.canvas.height = canvasDisplay.h;

    var diagram = this;
    diagram.drawTitle();
    diagram.steps.forEach(function(step) {
      step.draw(diagram);
    });
    diagram.links.forEach(function(link) {
      link.draw(diagram);
    });
    diagram.subflows.forEach(function(subflow) {
      subflow.draw(diagram);
    });
    diagram.notes.forEach(function(note) {
      note.draw(diagram);
    });
  };
  
  // sets display fields and returns a display with w and h for canvas size
  // (for performance reasons, also initializes steps/links arrays and activity impls)
  Diagram.prototype.prepareDisplay = function() {
    var canvasDisplay = { w: 460, h: 460 };
    
    // process title
    this.display = this.getDisplay(this.process.attributes.WORK_DISPLAY_INFO);
    var title = { text: this.process.name, x: this.display.x, y: this.display.y };
    this.context.font = Diagram.TITLE_FONT;
    var textMetrics = this.context.measureText(title.text);
    title.w = textMetrics.width;
    title.h = Diagram.TITLE_FONT_SIZE;
    if (title.x + title.w > canvasDisplay.w)
      canvasDisplay.w = title.x + title.w;
    if (title.y + title.h > canvasDisplay.h)
      canvasDisplay.h = title.y + title.h;    
    this.title = title;
    this.context.font = Diagram.DEFAULT_FONT;

    this.drawBoxes = this.process.attributes.NodeStyle == 'BoxIcon'; // TODO: we should make this the default
    
    var diagram = this;
    diagram.steps = [];
    if (this.process.activities) {
      this.process.activities.forEach(function(activity) {
        var step = new Step(activity);
        step.implementor = diagram.getImplementor(activity.implementor);
        diagram.makeRoom(canvasDisplay, step.prepareDisplay(diagram));
        if (diagram.instance)
          step.applyState(diagram.getActivityInstances(activity.id));
        diagram.steps.push(step);
      });
    }
    diagram.links = [];
    diagram.steps.forEach(function(step) {
      if (step.activity.transitions) {
        step.activity.transitions.forEach(function(transition) {
          var link = new Link(transition, step, diagram.getStep(transition.to));
          diagram.makeRoom(canvasDisplay, link.prepareDisplay(diagram));
          diagram.links.push(link);
        });
      }
    });
    diagram.subflows = [];
    if (this.process.subprocesses) {
      this.process.subprocesses.forEach(function(subproc) {
        var subflow = new Subflow(subproc);
        diagram.makeRoom(canvasDisplay, subflow.prepareDisplay(diagram));
        if (diagram.instance)
          subflow.applyState(diagram.getSubflowInstances(subflow.subprocess.id));
        diagram.subflows.push(subflow);
      });
    }

    diagram.notes = [];
    if (this.process.textNotes) {
      this.process.textNotes.forEach(function(textNote) {
        var note = new Note(textNote);
        diagram.makeRoom(canvasDisplay, note.prepareDisplay(diagram));
        diagram.notes.push(note);
      });
    }
    
    canvasDisplay.w += 2; // TODO why?
    canvasDisplay.h += 2;
    
    return canvasDisplay;
  };
  
  Diagram.prototype.makeRoom = function(canvasDisplay, display) {
    if (display.w > canvasDisplay.w)
      canvasDisplay.w = display.w;
    if (display.h > canvasDisplay.h)
      canvasDisplay.h = display.h;
  };
  
  Diagram.prototype.getStep = function(activityId) {
    for (var i = 0; i < this.steps.length; i++) {
      if (this.steps[i].activity.id === activityId)
        return this.steps[i];
    }
  };
  
  Diagram.prototype.getDisplay = function(displayAttr) {
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
  
  Diagram.prototype.drawTitle = function() {
    this.context.font = Diagram.TITLE_FONT;
    this.context.fillStyle = this.titleLinkHover ? Diagram.HYPERLINK_COLOR : Diagram.DEFAULT_COLOR;
    this.context.clearRect(this.title.x, this.title.y, this.title.w, this.title.h);
    this.context.fillText(this.title.text, this.title.x, this.title.y + this.title.h);
    this.context.font = Diagram.DEFAULT_FONT;
    this.context.fillStyle = Diagram.DEFAULT_COLOR;
  };
  
  Diagram.prototype.getImplementor = function(className) {
    if (this.implementors) {
      for (var i = 0; i < this.implementors.length; i++) {
        var implementor = this.implementors[i];
        if (implementor.implementorClass == className)
          return implementor;
      }
    }
    // not found -- return placeholder
    return { implementorClass: className };
  };
  
  Diagram.prototype.getActivityInstances = function(id) {
    if (this.instance && this.instance.activities) {
      var insts = [];
      this.instance.activities.forEach(function(actInst) {
        if ('A' + actInst.activityId == id)
          insts.push(actInst);
      });
      return insts;
    }
  };

  Diagram.prototype.getSubflowInstances = function(id) {
    if (this.instance && this.instance.subprocesses) {
      var insts = [];
      this.instance.subprocesses.forEach(function(subInst) {
        if ('P' + subInst.processId == id)
          insts.push(subInst);
      });
      return insts;
    }
  };
  
  Diagram.prototype.drawRoundedBox = function(context, x, y, w, h, color) {
    this.drawBox(context, x, y, w, h, color, Diagram.BOX_BOUNDING_RADIUS);
  };
  
  Diagram.prototype.drawBox = function(context, x, y, w, h, color, r) {
    if (color)
      context.strokeStyle = color;
    
    if (typeof r === 'undefined') {
      context.strokeRect(x, y, w, h);
    }
    else {
      // rounded corners
      context.beginPath();
      context.moveTo(x + r, y);
      context.lineTo(x + w - r, y);
      context.quadraticCurveTo(x + w, y, x + w, y + r);
      context.lineTo(x + w, y + h - r);
      context.quadraticCurveTo(x + w, y + h, x + w - r, y + h);
      context.lineTo(x + r, y + h);
      context.quadraticCurveTo(x, y + h, x, y + h - r);
      context.lineTo(x, y + r);
      context.quadraticCurveTo(x, y, x + r, y);
      context.closePath();
      context.stroke();
    }
    context.strokeStyle = Step.DEFAULT_COLOR;
  };
  
  Diagram.prototype.onMouseClick = function(e) {
    var rect = this.canvas.getBoundingClientRect();
    var x = e.clientX - rect.left;
    var y = e.clientY - rect.top;
    
    var prevSelect = this.selectObj;
    if (this.isHover(x, y, this.title)) {
      this.selectObj = this;
      if (prevSelect && prevSelect !== this.selectObj)
        this.unselect(prevSelect);
      this.select(this.selectObj);
    }
    else {
      this.selectObj = this.getHoverObj(x, y);
      if (this.selectObj) {
        if (prevSelect && prevSelect !== this.selectObj)
          this.unselect(prevSelect);
        this.select(this.selectObj);
      }
      else {
        if (prevSelect)
          this.unselect(prevSelect);
        this.selectObj = null;
      }
    }
  };
  
  // TODO better select indication
  Diagram.prototype.select = function(obj) {
    this.selectedObj = obj;
    var display = obj.display;
    this.context.fillStyle = 'red';
    var s = 2;
    this.context.fillRect(display.x - s, display.y - s, s * 2, s * 2);
    this.context.fillRect(display.x + display.w - s, display.y - s, s * 2, s * 2);
    this.context.fillRect(display.x + display.w - 2, display.y + display.h - s, s * 2, s * 2);
    this.context.fillRect(display.x - 2, display.y + display.h - s, s * 2, s * 2);
    this.context.fillStyle = Step.DEFAULT_COLOR;
  };

  Diagram.prototype.unselect = function(obj) {
    var display = obj.display;
    var s = 2;
    this.context.clearRect(display.x - s, display.y - s, s * 2, s * 2);
    this.context.clearRect(display.x + display.w - s, display.y - s, s * 2, s * 2);
    this.context.clearRect(display.x + display.w - 2, display.y + display.h - s, s * 2, s * 2);
    this.context.clearRect(display.x - 2, display.y + display.h - s, s * 2, s * 2);
    this.selectedObj = null;
  };
  
  Diagram.prototype.onMouseMove = function(e) {
    var rect = this.canvas.getBoundingClientRect();
    var x = e.clientX - rect.left;
    var y = e.clientY - rect.top;

    var wasTitleLinkHover = this.titleLinkHover ? this.titleLinkHover : false;
    if (this.isHover(x, y, this.title)) {
      this.hoverObj = this;
      $document[0].body.style.cursor = 'pointer';
      this.titleLinkHover = true;
    }
    else {
      this.hoverObj = this.getHoverObj(x, y);
      if (this.hoverObj) {
        $document[0].body.style.cursor = 'pointer';
      }
      else {
        $document[0].body.style.cursor = '';
        this.titleLinkHover = false;
      }
    }
    
    if (this.titleLinkHover != wasTitleLinkHover) {
      this.drawTitle();
    }
  };
  
  Diagram.prototype.getHoverObj = function(x, y) {
    for (var i = 0; i < this.steps.length; i++) {
      if (this.isHover(x, y, this.steps[i].display))
        return this.steps[i];
    }
    // TODO: links
    for (i = 0; i < this.subflows.length; i++) {
      var subflow = this.subflows[i];
      if (this.isHover(x, y, subflow.title))
        return subflow;
      for (var j = 0; j < subflow.steps.length; j++) {
        if (this.isHover(x, y, subflow.steps[j].display))
          return subflow.steps[j];
      }
    }
  };
  
  Diagram.prototype.isHover = function(x, y, display) {
    return x >= display.x && x <= display.x + display.w &&
        y >= display.y && y <= display.y + display.h;
  };
  
  return Diagram;
  
}]);

// attributes
//   - process (object): packageName and name must be populated
//     optional process fields
//       - version: for non-latest process version
//       - id: if populated, then retrieve instance data
//   - service-base: endpoint url root
//   - hub-base: MDWHub url root
workflowMod.directive('mdwWorkflow', [function() {
  return {
    restrict: 'E',
    templateUrl: 'ui/workflow.html',
    scope: {
      process: '=process',
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