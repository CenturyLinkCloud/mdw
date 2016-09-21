// Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
'use strict';

var workflowMod = angular.module('mdwWorkflow', ['mdw']);

workflowMod.controller('MdwWorkflowController', 
    ['$scope', '$http', '$routeParams', 'mdw', 'util', 'Workflow', 'Diagram',
    function($scope, $http, $routeParams, mdw, util, Workflow, Diagram) {
  
  $scope.init = function(canvas) {
    $scope.canvas = canvas;
    $scope.renderProcess();
    $scope.canvas.bind('mousemove', $scope.mouseMove);
  };
  
  $scope.dest = function() {
    $scope.canvas.bind('mousemove', $scope.mouseMove);
  };
  
  $scope.renderProcess = function() {
    var packageName = $scope.process.packageName;
    $scope.process = Workflow.retrieve({pkg: packageName, process: $scope.process.name}, function() {
      $scope.process.packageName = packageName;  // not returned in JSON

      if (!$scope.implementors) {
        $http( {method: 'GET', url: mdw.roots.services + '/services/Implementors' })
          .then(function success(response) {
            $scope.implementors = response.data;
            $scope.diagram = new Diagram($scope.canvas[0], $scope.process, $scope.implementors);
            $scope.diagram.draw();
          }, function error(response) {
            mdw.messages = response.statusText;
        });
      }
      else {
        $scope.diagram = new Diagram($scope.canvas[0], $scope.process, $scope.implementors);
        $scope.diagram.draw();
      }
    });
  };
  
  $scope.mouseMove = function(e) {
    if ($scope.diagram)
      $scope.diagram.onMouseMove(e);
  };
}]);

workflowMod.factory('Workflow', ['$resource', 'mdw', function($resource, mdw) {
  return $resource(mdw.roots.services + '/services/Workflow/:pkg/:process', mdw.serviceParams(), {
    retrieve: { method: 'GET', isArray: false }
  });
}]);

workflowMod.factory('Diagram', ['$document', 'mdw', 'util', 'Step', 'Link',
                                function($document, mdw, util, Step, Link) {
  var Diagram = function(canvas, process, implementors, instance) {
    this.canvas = canvas;
    this.process = process;
    this.implementors = implementors;
    this.instance = instance ? instance : null;
    
    this.context = this.canvas.getContext("2d");
  };

  Diagram.DEFAULT_FONT_SIZE = 12;
  Diagram.DEFAULT_FONT =  Diagram.DEFAULT_FONT_SIZE + 'px sans-serif';
  Diagram.TITLE_FONT_SIZE = 18;
  Diagram.TITLE_FONT = 'bold ' + Diagram.TITLE_FONT_SIZE + 'px sans-serif';
  
  Diagram.DEFAULT_COLOR = 'black';
  Diagram.HYPERLINK_COLOR = '#1565c0';
  
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
  };
  
  // sets display fields and returns a display with w and h for canvas size
  // (for performance reasons, also initializes steps/links arrays and activity impls)
  Diagram.prototype.prepareDisplay = function() {
    var canvasDisplay = { w: 460, h: 460 };
    
    // process title
    var display = this.getDisplay(this.process.attributes.WORK_DISPLAY_INFO);
    var title = { text: this.process.name, x: display.x, y: display.y };
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
    
    var diagram = this;
    diagram.steps = [];
    if (this.process.activities) {
      this.process.activities.forEach(function(activity) {
        var step = new Step(activity);
        var display = step.prepareDisplay(diagram);
        if (display.w > canvasDisplay.w)
          canvasDisplay.w = display.w;
        if (display.h > canvasDisplay.h)
          canvasDisplay.h = display.h;
        diagram.steps.push(step);
        activity.implementor = diagram.getImplementor(activity.implementor);
        
      });
    }
    diagram.links = [];
    diagram.steps.forEach(function(step) {
      if (step.activity.transitions) {
        step.activity.transitions.forEach(function(transition) {
          var link = new Link(transition, step, diagram.getStep(transition.to));
          var display = link.prepareDisplay(diagram);
          if (display.w > canvasDisplay.w)
            canvasDisplay.w = display.w;
          if (display.h > canvasDisplay.h)
            canvasDisplay.h = display.h;
          diagram.links.push(link);
        });
      }
    });
    
    // TODO subprocesses
    
    canvasDisplay.w += 2; // TODO why?
    canvasDisplay.h += 2;
    
    return canvasDisplay;
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
  
  Diagram.prototype.onMouseMove = function(e) {
    var rect = this.canvas.getBoundingClientRect();
    var x = e.clientX - rect.left;
    var y = e.clientY - rect.top;
    
    // title link
    var wasTitleLinkHover = this.titleLinkHover ? this.titleLinkHover : false;
    if (x > this.title.x && x < this.title.x + this.title.w &&
          y > this.title.y && y < this.title.y + this.title.h) {
      $document[0].body.style.cursor = 'pointer';
      this.titleLinkHover = true; // to be used in onMouseClick
    }
    else {
      $document[0].body.style.cursor = '';
      this.titleLinkHover = false;
    }
    if (this.titleLinkHover != wasTitleLinkHover) {
      this.drawTitle();
    }
  };
  
  return Diagram;
  
}]);

// packageName and name must be populated on model object
// version is optional
workflowMod.directive('mdwWorkflow', function() {
  return {
    restrict: 'A',
    templateUrl: 'ui/list.html',
    scope: {
      process: '=mdwWorkflow'
    },
    controller: 'MdwWorkflowController',
    controllerAs: 'mdwWorkflow',
    link: function link(scope, elem, attrs, ctrls) {
      scope.init(elem);
      scope.$on('$destroy', function() {
        scope.dest();
      });
    }
  };
});