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

      if ($scope.implementors == null) {
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

workflowMod.factory('Diagram', ['$document', 'mdw', 'util', function($document, mdw, util) {
  var Diagram = function(canvas, process, implementors, instance) {
    this.canvas = canvas;
    this.process = process;
    this.implementors = implementors;
    this.instance = instance ? instance : null;
    
    this.defaultFontSize = 12;
    this.defaultFont =  this.defaultFontSize + 'px sans-serif';
    this.titleFontSize = 18;
    this.titleFont = 'bold ' + this.titleFontSize + 'px sans-serif';
    
    this.defaultColor = 'black';
    this.linkColor = '#1565c0'
    this.boxOutlineColor = '#a9a9a9';
    this.metaColor = 'gray';
    this.boxRoundingRadius = 12;
    
    this.context = this.canvas.getContext("2d");    
  };
  
  Diagram.prototype.draw = function() {
    this.context.clearRect(0, 0, this.canvas.width, this.canvas.height);
    var canvasDisplay = this.prepareDisplay();
    this.canvas.width = canvasDisplay.w;
    this.canvas.height = canvasDisplay.h;

    this.drawTitle();
    
    if (this.process.activities) {
      var diagram = this;
      this.process.activities.forEach(function(activity) {
        diagram.drawActivity(activity);
      });
    }
  };
  
  Diagram.prototype.drawTitle = function() {
    this.context.font = this.titleFont;
    this.context.fillStyle = this.titleLinkHover ? this.linkColor : this.defaultColor;
    this.context.clearRect(this.process.title.x, this.process.title.y, this.process.title.w, this.process.title.h);
    this.context.fillText(this.process.name, this.process.title.x, this.process.title.y + this.process.title.h);
    this.context.font = this.defaultFont;
    this.context.fillStyle = this.defaultColor;
  }
  
  Diagram.prototype.drawActivity = function(activity) {
    var diagram = this;
    var ty = activity.y + activity.h / 2 + activity.title.h / 2 - 2;
    if (activity.implementor.icon) {
      if (activity.implementor.icon.startsWith('shape:')) {
        var shape = activity.implementor.icon.substring(6);
        if ('start' == shape) {
          this.drawOval(activity.x, activity.y, activity.w, activity.h, 'green', 'white');
        }
        else if ('stop' == shape) {
          this.drawOval(activity.x, activity.y, activity.w, activity.h, 'red', 'white');
        }
        else if ('decision' == shape) {
          this.drawDiamond(activity.x, activity.y, activity.w, activity.h);
          ty -= 5; // why?
        }
        else if ('activity' == shape) {
          this.drawRoundedBox(activity.x, activity.y, activity.w, activity.h);
        }
      }
      else {
        this.drawRoundedBox(activity.x, activity.y, activity.w, activity.h);
        var iconImg = new Image();
        iconImg.src = mdw.roots.hub + '/asset/' + activity.implementor.icon;
        console.log("IMG SRC: " + iconImg.src);
        var iconx = activity.x + activity.w / 2 - 12;
        var icony = activity.y + 5;
        iconImg.onload = function() {
          diagram.context.drawImage(iconImg, iconx, icony);
        };
        ty += 9;
      }
    }
    else {
      this.drawRoundedBox(activity.x, activity.y, activity.w, activity.h);
    }

    // title
    activity.title.lines.forEach(function(line) {
      var textMetrics = diagram.context.measureText(line);
      var tx = activity.x + activity.w / 2 - textMetrics.width / 2;
      diagram.context.fillText(line, tx, ty);
      ty += diagram.defaultFontSize;
    });
    
    // logical id
    this.context.fillStyle = this.metaColor;
    this.context.fillText(activity.id, activity.x + 2, activity.y - 2);
    this.context.fillStyle = this.defaultColor;
    
  };
  
  Diagram.prototype.drawOval = function(x, y, w, h, fill, fadeTo) {
    var kappa = .5522848;
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
    this.context.fillStyle = this.defaultColor;
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
  
  Diagram.prototype.drawRoundedBox = function(x, y, w, h) {
    this.drawBox(x, y, w, h, this.boxRoundingRadius);
  };
  
  Diagram.prototype.drawBox = function(x, y, w, h, r) {
    this.context.fillStyle = this.boxOutlineColor;
    if (typeof r === 'undefined') {
      this.context.strokeRect(x, y, w, h);
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
    }
    this.context.fillStyle = this.defaultColor;
  };
  
  // sets display fields and returns a display with w and h for canvas size
  Diagram.prototype.prepareDisplay = function() {
    var canvasDisplay = { w: 460, h: 460 };
    
    // process title
    var title = { 
        text: this.process.name, 
        attributes: { WORK_DISPLAY_INFO: this.process.attributes.WORK_DISPLAY_INFO } 
    };
    this.setDisplay(title);
    this.context.font = this.titleFont;
    var textMetrics = this.context.measureText(title.text);
    title.w = textMetrics.width;
    title.h = this.titleFontSize;
    if (title.x + title.w > canvasDisplay.w)
      canvasDisplay.w = title.x + title.w;
    if (title.y + title.h > canvasDisplay.h)
      canvasDisplay.h = title.y + title.h;
    this.process.title = title;
    this.context.font = this.defaultFont;
    
    // activities
    if (this.process.activities) {
      var diagram = this;
      this.process.activities.forEach(function(activity) {
        diagram.setDisplay(activity);
        if (activity.x + activity.w > canvasDisplay.w)
          canvasDisplay.w = activity.x + activity.w;
        if (activity.y + activity.h > canvasDisplay.h)
          canvasDisplay.h = activity.y + activity.h;
        
        // activity title
        var title = { text: activity.name, lines: activity.name.getLines(), w: 0 };
        title.lines.forEach(function(line) {
          var textMetrics = diagram.context.measureText(line);
          if (textMetrics.width > title.w)
            title.w = textMetrics.width;
        });
        title.h = diagram.defaultFontSize;
        if (title.x + title.w > canvasDisplay.w)
          canvasDisplay.w = title.x + title.w;
        if (title.y + title.lines.length * title.h > canvasDisplay.h)
          canvasDisplay.h = title.y + title.lines.length * title.h;
        activity.title = title;    
        // associate full-blown implementor
        activity.implementor = diagram.getImplementor(activity.implementor);
      });
    }
    
    canvasDisplay.w += 2; // why
    return canvasDisplay;
  };
  
  Diagram.prototype.setDisplay = function(item) {
    var displayAttr = item.attributes.WORK_DISPLAY_INFO;
    if (displayAttr) {
      var vals = displayAttr.split(',');
      var display = {};
      vals.forEach(function(val) {
        if (val.startsWith('x='))
          item.x = parseInt(val.substring(2));
        else if (val.startsWith('y='))
          item.y = parseInt(val.substring(2));
        else if (val.startsWith('w='))
          item.w = parseInt(val.substring(2));
        else if (val.startsWith('h='))
          item.h = parseInt(val.substring(2));
      });
    }
  };

  Diagram.prototype.setTransitionDisplay = function(transition) {
    var displayAttr = transition.attributes.TRANSITION_DISPLAY_INFO;
    if (displayAttr) {
      var vals = displayAttr.split(',');
      var display = {};
      vals.forEach(function(val) {
        if (val.startsWith('lx='))
          transition.lx = parseInt(val.substring(3));
        else if (val.startsWith('ly='))
          transition.ly = parseInt(val.substring(3));
        else if (val.startsWith('xs=')) {
          transition.xs = [];
          val.substring(3).split('&').forEach(function(x) {
            transition.xs.push(parseInt(x));
          });
        }
        else if (val.startsWith('ys=')) {
          transition.ys = [];
          val.substring(3).split('&').forEach(function(y) {
            transition.ys.push(parseInt(y));
          });
        }
        else if (val.startsWith('type='))
          transition.type = val.substring(5);
      });
    }
  };
  
  Diagram.prototype.getImplementor = function(className) {
    if (this.implementors) {
      for (var i = 0; i < this.implementors.length; i++) {
        var implementor = this.implementors[i];
        if (implementor.implementorClass == className)
          return implementor;
      };
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
    if (x > this.process.title.x && x < this.process.title.x + this.process.title.w &&
          y > this.process.title.y && y < this.process.title.y + this.process.title.h) {
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