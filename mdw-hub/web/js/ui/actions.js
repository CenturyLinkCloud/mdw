'use strict';

var actionsMod = angular.module('mdwActions', ['mdw']);

actionsMod.controller('MdwActionsController', ['$scope', '$window',
                                              function($scope, $window) {
  $scope.popPlace = 'left';
  
  this.getScope = function() {
    return $scope;
  };
  
  this.setPlace = function() {
    var minWidth = $scope.wrapWidth;
    if (minWidth && !$window.matchMedia('(min-width: ' + minWidth + ')').matches)
      $scope.popPlace = 'right-top';
    else
      $scope.popPlace = 'left-top';
  };
}]);

// device-specific wrap
actionsMod.directive('mdwActions', [function() {
  return {
    restrict: 'E',
    transclude: true,
    templateUrl: 'ui/mdw-actions.html',
    controller: 'MdwActionsController',
    controllerAs: 'mdwActions', 
    link: {
      pre: function(scope, elem, attrs) {
        scope.wrapWidth = attrs.wrap;
      },
      post: function(scope, elem, attrs) {
      }
    }
  };
}]);

// device-specific wrap
actionsMod.directive('mdwActionPopButton', ['$window', '$compile', function($window, $compile) {
  return {
    restrict: 'A',
    templateUrl: 'ui/action-pop-button.html',
    scope: true,
    transclude: true,
    require: '^mdwActions',
    link: {
      pre: function(scope, elem, attrs, ctrl) {
        ctrl.setPlace();
        attrs.$set("popoverPlacement", scope.popPlace);
      },
      post: function(scope, elem, attrs, ctrl) {

        if (typeof attrs.selectPop === 'undefined') { // don't automatically close these
          // try multiple generations
          var parentScope = scope.$parent;
          while (!parentScope.setPopElem && parentScope.$parent)
            parentScope = parentScope.$parent;
  
          if (parentScope.setPopElem) {
            elem.bind('click', function() {
              if (parentScope.popElem === elem) {
                // clicked to close
                parentScope.setPopElem(null);
              }
              else {
                parentScope.closePopover();
                parentScope.setPopElem(elem);
              }
            });
          }
        }
  
        var resizeHandler = function() {
          ctrl.setPlace();
          attrs.$set("popoverPlacement", scope.popPlace);
          scope.$apply();
        };
        angular.element($window).bind('resize', resizeHandler);
        scope.$on('$destroy', function() {
          angular.element($window).unbind('resize', resizeHandler);
        });
      }
    }
  };
}]);

