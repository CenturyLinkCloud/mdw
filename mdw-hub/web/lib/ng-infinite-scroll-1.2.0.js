/*! ng-infinite-scroll - v1.2.0 - 2015-02-14 
 * - modified for horizontal scrolling: https://github.com/sroze/ngInfiniteScroll/issues/69
 * - plus fixes applied to support horizontal scrolling with window as container */
var mod;

mod = angular.module('infinite-scroll', []);

mod.value('THROTTLE_MILLISECONDS', null);

mod.directive('infiniteScroll', [
  '$rootScope', '$window', '$interval', 'THROTTLE_MILLISECONDS', function($rootScope, $window, $interval, THROTTLE_MILLISECONDS) {
    return {
      scope: {
        infiniteScroll: '&',
        infiniteScrollContainer: '=',
        infiniteScrollDistance: '=',
        infiniteScrollDisabled: '=',
        infiniteScrollUseDocumentBottom: '=',
        infiniteScrollListenForEvent: '@',
        infiniteScrollHorizontal: '='
      },
      link: function(scope, elem, attrs) {
        var changeContainer, checkWhenEnabled, container, handleInfiniteScrollContainer, handleInfiniteScrollDisabled, handleInfiniteScrollDistance, handleInfiniteScrollUseDocumentBottom, handleInfiniteScrollHorizontal, handler, height, immediateCheck, offsetTop, offsetLeft, pageYOffset, pageXOffset, scrollDistance, scrollEnabled, throttle, unregisterEventListener, useDocumentBottom, windowElement;
        windowElement = angular.element($window);
        scrollDistance = null;
        scrollEnabled = null;
        checkWhenEnabled = null;
        container = null;
        immediateCheck = true;
        useDocumentBottom = false;
        unregisterEventListener = null;
        height = function(elem) {
          elem = elem[0] || elem;
          if (isNaN(elem.offsetHeight)) {
            return elem.document.documentElement.clientHeight;
          } else {
            return elem.offsetHeight;
          }
        };
        width = function(elem) {
          elem = elem[0] || elem;
          if (isNaN(elem.offsetWidth)) {
            return elem.document.documentElement.clientWidth;
          } else {
            return elem.offsetWidth;
          }
        };        
        scrollWidth = function(elem) {
          elem = elem[0] || elem;
          return elem.scrollWidth;
        };        
        offsetTop = function(elem) {
          if (!elem[0].getBoundingClientRect || elem.css('none')) {
            return;
          }
          return elem[0].getBoundingClientRect().top + pageYOffset(elem);
        };
        offsetLeft = function(elem) {
          if (!elem[0].getBoundingClientRect || elem.css('none')) {
            return;
          }
          return elem[0].getBoundingClientRect().left + pageXOffset(elem);
        };
        pageYOffset = function(elem) {
          elem = elem[0] || elem;
          if (isNaN(window.pageYOffset)) {
            return elem.document.documentElement.scrollTop;
          } else {
            return elem.ownerDocument.defaultView.pageYOffset;
          }
        };
        pageXOffset = function(elem) {
          elem = elem[0] || elem;
          if (isNaN(window.pageXOffset)) {
            return elem.document.documentElement.scrollLeft;
          } else {
            return elem.ownerDocument.defaultView.pageXOffset;
          }
        };
        handler = function() {
          var containerBottom, containerTopOffset, elementBottom, remaining, shouldScroll;
          if (container === windowElement) {
            if (scrollHorizontal) {
              containerBottom = width(container) + pageXOffset(container[0].document.documentElement);
              elementBottom = offsetLeft(elem) + scrollWidth(elem);
            } else {
              containerBottom = height(container) + pageYOffset(container[0].document.documentElement);
              elementBottom = offsetTop(elem) + height(elem);
            }
          } else {
            if (scrollHorizontal) {
              containerBottom = width(container);
            } else {
              containerBottom = height(container);
            }
            containerTopOffset = 0;
            if (scrollHorizontal) {
              if (offsetLeft(container) !== void 0)
                containerTopOffset = offsetLeft(container);
            }
            else {
              if (offsetTop(container) !== void 0)
                containerTopOffset = offsetTop(container);
            }
            if (scrollHorizontal) {
              elementBottom = offsetLeft(elem) - containerTopOffset + scrollWidth(elem);
            } else {
              elementBottom = offsetTop(elem) - containerTopOffset + height(elem);
            }
          }
          if (useDocumentBottom) {
            if (scrollHorizontal) {
              elementBottom = width((elem[0].ownerDocument || elem[0].document).documentElement);
            } else {
              elementBottom = height((elem[0].ownerDocument || elem[0].document).documentElement);
            }
          }
          remaining = elementBottom - containerBottom;
          if (scrollHorizontal) {
            shouldScroll = remaining <= width(container) * scrollDistance + 1;
          } else {
            shouldScroll = remaining <= height(container) * scrollDistance + 1;
          }
          if (shouldScroll) {
            checkWhenEnabled = true;
            if (scrollEnabled) {
              if (scope.$$phase || $rootScope.$$phase) {
                return scope.infiniteScroll();
              } else {
                return scope.$apply(scope.infiniteScroll);
              }
            }
          } else {
            return checkWhenEnabled = false;
          }
        };
        throttle = function(func, wait) {
          var later, previous, timeout;
          timeout = null;
          previous = 0;
          later = function() {
            var context;
            previous = new Date().getTime();
            $interval.cancel(timeout);
            timeout = null;
            func.call();
            return context = null;
          };
          return function() {
            var now, remaining;
            now = new Date().getTime();
            remaining = wait - (now - previous);
            if (remaining <= 0) {
              clearTimeout(timeout);
              $interval.cancel(timeout);
              timeout = null;
              previous = now;
              return func.call();
            } else {
              if (!timeout) {
                return timeout = $interval(later, remaining, 1);
              }
            }
          };
        };
        if (THROTTLE_MILLISECONDS != null) {
          handler = throttle(handler, THROTTLE_MILLISECONDS);
        }
        scope.$on('$destroy', function() {
          container.unbind('scroll', handler);
          if (unregisterEventListener != null) {
            unregisterEventListener();
            return unregisterEventListener = null;
          }
        });
        handleInfiniteScrollHorizontal = function(v) {
          scrollHorizontal = v;
        };
        scope.$watch('infiniteScrollHorizontal', handleInfiniteScrollHorizontal);
        handleInfiniteScrollHorizontal(scope.infiniteScrollHorizontal);
        handleInfiniteScrollDistance = function(v) {
          return scrollDistance = parseFloat(v) || 0;
        };
        scope.$watch('infiniteScrollDistance', handleInfiniteScrollDistance);
        handleInfiniteScrollDistance(scope.infiniteScrollDistance);
        handleInfiniteScrollDisabled = function(v) {
          scrollEnabled = !v;
          if (scrollEnabled && checkWhenEnabled) {
            checkWhenEnabled = false;
            return handler();
          }
        };
        scope.$watch('infiniteScrollDisabled', handleInfiniteScrollDisabled);
        handleInfiniteScrollDisabled(scope.infiniteScrollDisabled);
        handleInfiniteScrollUseDocumentBottom = function(v) {
          return useDocumentBottom = v;
        };
        scope.$watch('infiniteScrollUseDocumentBottom', handleInfiniteScrollUseDocumentBottom);
        handleInfiniteScrollUseDocumentBottom(scope.infiniteScrollUseDocumentBottom);
        changeContainer = function(newContainer) {
          if (container != null) {
            container.unbind('scroll', handler);
          }
          container = newContainer;
          if (newContainer != null) {
            return container.bind('scroll', handler);
          }
        };
        changeContainer(windowElement);
        if (scope.infiniteScrollListenForEvent) {
          unregisterEventListener = $rootScope.$on(scope.infiniteScrollListenForEvent, handler);
        }
        handleInfiniteScrollContainer = function(newContainer) {
          if ((newContainer == null) || newContainer.length === 0) {
            return;
          }
          if (newContainer instanceof HTMLElement) {
            newContainer = angular.element(newContainer);
          } else if (typeof newContainer.append === 'function') {
            newContainer = angular.element(newContainer[newContainer.length - 1]);
          } else if (typeof newContainer === 'string') {
            newContainer = angular.element(document.querySelector(newContainer));
          }
          if (newContainer != null) {
            return changeContainer(newContainer);
          } else {
            throw new Exception("invalid infinite-scroll-container attribute.");
          }
        };
        scope.$watch('infiniteScrollContainer', handleInfiniteScrollContainer);
        handleInfiniteScrollContainer(scope.infiniteScrollContainer || []);
        if (attrs.infiniteScrollParent != null) {
          changeContainer(angular.element(elem.parent()));
        }
        if (attrs.infiniteScrollImmediateCheck != null) {
          immediateCheck = scope.$eval(attrs.infiniteScrollImmediateCheck);
        }
        return $interval((function() {
          if (immediateCheck) {
            return handler();
          }
        }), 0, 1);
      }
    };
  }
]);
