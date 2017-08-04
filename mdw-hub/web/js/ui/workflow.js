'use strict';

var workflowMod = angular.module('mdwWorkflow', ['mdw', 'mdwDiagram', 'mdwSelection', 'drawingConstants']);

workflowMod.controller('MdwWorkflowController', 
    ['$scope', '$http', '$document', 'mdw', 'util', 'uiUtil', 'mdwImplementors', 'Diagram', 'Inspector', 'Toolbox',
    function($scope, $http, $document, mdw, util, uiUtil, mdwImplementors, Diagram, Inspector, Toolbox) {
  
  $scope.init = function(canvas) {
    if ($scope.serviceBase.endsWith('/'))
      $scope.serviceBase = $scope.serviceBase.substring(0, $scope.serviceBase.length - 1);
    if ($scope.hubBase) {
      if ($scope.hubBase.endsWith('/'))
        $scope.hubBase = $scope.hubBase.substring(0, $scope.hubBase.length - 1);
    }
    else {
      $scope.hubBase = $scope.serviceBase.substring(0, $scope.serviceBase.length - 9);
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
        $scope.canvas.bind('mouseover', $scope.mouseOver);
        $scope.canvas.bind('mouseout', $scope.mouseOut);
        $scope.canvas.bind('dblclick', $scope.mouseDoubleClick);
        if ($scope.editable)
          $document.bind('keydown', $scope.keyDown);        
      }, function(error) {
        mdw.messages = error;
      });
    }
    else {
      $scope.renderProcess();
      $scope.canvas.bind('mousemove', $scope.mouseMove);
      $scope.canvas.bind('mousedown', $scope.mouseDown);
      $scope.canvas.bind('mouseup', $scope.mouseUp);
      $scope.canvas.bind('mouseover', $scope.mouseOver);
      $scope.canvas.bind('mouseout', $scope.mouseOut);
      $scope.canvas.bind('dblclick', $scope.mouseDoubleClick);
      if ($scope.editable)
        $document.bind('keydown', $scope.keyDown);        
    }
  };
  
  $scope.dest = function() {
    $scope.canvas.unbind('mousemove', $scope.mouseMove);
    $scope.canvas.unbind('mousedown', $scope.mouseDown);
    $scope.canvas.unbind('mouseup', $scope.mouseUp);
    $scope.canvas.unbind('mouseover', $scope.mouseOver);
    $scope.canvas.unbind('mouseout', $scope.mouseOut);
    $scope.canvas.unbind('dblclick', $scope.mouseDoubleClick);
    if ($scope.editable)
      $document.unbind('keydown', $scope.keyDown);        
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
    if ($scope.editable)
      workflowUrl += '?forUpdate=true'; // TODO: honor forUpdate
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
    if (typeof $scope.renderState === 'string')
      $scope.renderState = 'true' == $scope.renderState.toLowerCase();
    if (typeof $scope.animate === 'string')
      $scope.animate = 'true' == $scope.animate.toLowerCase();
    
    if ($scope.renderState && $scope.process.id) {
      $http({ method: 'GET', url: $scope.serviceBase + '/Processes/' + $scope.process.id })
        .then(function success(response) {
          $scope.instance = response.data;
            $scope.diagram = new Diagram($scope.canvas[0], uiUtil, $scope.process, $scope.implementors, $scope.hubBase, $scope.editable, $scope.instance);
            $scope.diagram.draw($scope.animate);
        }, function error(response) {
          mdw.messages = response.statusText;
      });
    }
    else {
      $scope.diagram = new Diagram($scope.canvas[0], uiUtil, $scope.process, $scope.implementors, $scope.hubBase, $scope.editable, $scope.instance);
      $scope.diagram.draw($scope.animate);
      if ($scope.editable) {
        $scope.toolbox = Toolbox.getToolbox();
        $scope.toolbox.init($scope.implementors, $scope.hubBase);
      }
    }
  };
  
  $scope.down = false;
  $scope.dragging = false;
  $scope.dragIn = null;
  $scope.mouseMove = function(e) {
    if ($scope.dragIn && $scope.editable) {
      $document[0].body.style.cursor = 'copy';
    }
    else {
      if ($scope.down && $scope.editable)
        $scope.dragging = true;
      if ($scope.diagram) {
        if ($scope.dragging) {
          if ($scope.diagram.onMouseDrag(e)) {
            $scope.handleChange();
          }
        }
        else {
          $scope.diagram.onMouseMove(e);
        }
      }
    }
  };
  $scope.mouseDown = function(e) {
    $scope.down = true;
    if ($scope.diagram) {
      $scope.diagram.onMouseDown(e);
      var selObj = $scope.diagram.selection.getSelectObj();
      if (selObj && selObj.isLabel)
        selObj = selObj.owner;
      if (selObj) {
        Inspector.setObj(selObj, !$scope.editable);
      }
      else {
        var bgObj = $scope.diagram.getBackgroundObj(e);
        if (bgObj)
          Inspector.setObj(bgObj, !$scope.editable);
      }
    }
  };
  $scope.mouseUp = function(e) {
    $scope.down = false;
    $scope.dragging = false;
    if ($scope.diagram) {
      if ($scope.dragIn) {
        if ($scope.diagram.onDrop(e, $scope.dragIn)) {
          $scope.handleChange();
        }
      }
      else {
        $scope.diagram.onMouseUp(e);
      }
    }
    $scope.dragIn = null;
  };
  $scope.mouseOver = function(e) {
    if (e.buttons === 1 && $scope.toolbox && $scope.toolbox.getSelected()) {
      $scope.dragIn = $scope.toolbox.getSelected();
    }
  };
  $scope.mouseOut = function(e) {
    $scope.down = false;
    $scope.dragging = false;
    $scope.dragIn = null;
    if ($scope.diagram)
      $scope.diagram.onMouseOut(e);
  };
  $scope.mouseDoubleClick = function(e) {
    if ($scope.diagram && $scope.editable) {
      var selObj = $scope.diagram.selection.getSelectObj();
      if (selObj && selObj.isLabel)
        selObj = selObj.owner;
      if (selObj)
        Inspector.setObj(selObj, true);
    }
  };
  $scope.keyDown = function(e) {
    if (e.keyCode == 46 && $scope.diagram && $scope.editable) {
      $scope.diagram.onDelete(e, $scope.handleChange);
    }
  };
  
  $scope.handleChange = function() {
    if ($scope.onChange)
      $scope.onChange($scope.process);
  };
}]);

// cache implementors
workflowMod.factory('mdwImplementors', ['mdw', function(mdw) {
  return {
    set: function(implementors) {
      implementors = implementors.concat($mdwUi.pseudoImplementors);
      implementors.sort(function(impl1, impl2) {
        return impl1.label.localeCompare(impl2.label);
      });
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
//   - on-process-change: handler for process change event
//   - service-base: endpoint url root
//   - hub-base: MDWHub url root
workflowMod.directive('mdwWorkflow', [function() {
  return {
    restrict: 'E',
    templateUrl: 'ui/workflow.html',
    scope: {
      process: '=process',
      onChange: '=onProcessChange',
      renderState: '@renderState',
      editable: '@editable',
      serviceBase: '@serviceBase',
      hubBase: '@hubBase',
      animate: '@animate'
    },
    controller: 'MdwWorkflowController',
    controllerAs: 'mdwWorkflow',
    link: function link(scope, elem, attrs, ctrls) {
      var canvas = angular.element(elem[0].getElementsByClassName('mdw-canvas')[0]);
      scope.init(canvas);
      scope.$on('$destroy', function() {
        scope.dest();
      });
    }
  };
}]);
