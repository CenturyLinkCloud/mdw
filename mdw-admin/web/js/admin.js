// Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
'use strict';

var adminApp = angular.module('adminApp', ['ngRoute', 'ngAnimate', 'ngWebSocket', 'ui.bootstrap', 'chart.js', 'authUser', 'mdw', 'util', 'constants', 
                                           'mdwChart', 'mdwActions', 'mdwList', 'mdwPanel', 'routes', 'users', 'groups', 'roles', 'assets', 'testing', 
                                           'tasks', 'task', 'taskTemplates', 'processes', 'activities', 'requests', 'services', 'solutions', 'history',
                                           'dashboardProcesses', 'dashboardRequests', 'dashboardTasks', 'dashboardActivities',
                                           'blv', 'ngSanitize','blvProcessSvc', 'blvDrawingSvc', 'blvSharedData', 'blvtypes', 'mdwAttributesSvc']);

adminApp.config(function($httpProvider) {
  $httpProvider.defaults.headers.get = { 'Accept': 'application/json' };
  $httpProvider.defaults.headers.post = { 'Accept': 'application/json' };
  $httpProvider.interceptors.push(function($q, mdw) {
    return {
      'request': function(config) {
        if (config.url.startsWith(mdw.roots.services)) {
          mdw.hubLoading(true);
          // config.headers['Authorization'] = "Bearer yoyo";
        }
        return config;
      },
      'requestError': function(rejection) {
        mdw.hubLoading(false);
        return $q.reject(rejection);
      },
      'response': function(response) {
        if (response.config.url.startsWith(mdw.roots.services))
          mdw.hubLoading(false);
        return response;
      },
      'responseError': function(rejection) {
        mdw.hubLoading(false);
        return $q.reject(rejection);
      }
    };
  });
});

adminApp.config(['$routeProvider', function($routeProvider) {
  
  if (theRoutes) {
    var allRoutes = theRoutes.def;
    for (var i = 0; i < allRoutes.length; i++) {
      var route = allRoutes[i];
      $routeProvider.when(route.path, {
        templateUrl: route.templateUrl,
        controller: route.controller
      });
    }
  }
  $routeProvider.otherwise({
    redirectTo: $mdwTasksUi == 'mdw-admin' ? '/tasks' : '/users'
  });
}]);

adminApp.controller('AdminController', ['$rootScope', '$scope', '$window', '$timeout', '$location', '$anchorScroll', 'mdw', 'authUser', 'util',
                                        function($rootScope, $scope, $window, $timeout, $location, $anchorScroll, mdw, authUser, util) {
  $scope.mdw = mdw;
  var qs = $location.search();
  console.log ('access_token=', qs.access_token);

  $scope.authUser = theUser;
  $scope.authUser.setActiveTab($location.url());
  
  $scope.$on("$locationChangeStart", function(event, next, current) {
    if ($scope.authUser.cuid == 'guest' && !authUser.guestAccessAllowed($location.url())) {
      document.cookie = 'mdw.redirect=' + encodeURIComponent(window.location.href) + ';path=/';
      window.location.href = $mdwHubRoot + $mdwLoginPage;
      event.preventDefault();
    }
  });  

  // one popover at a time
  $scope.popElem = null;
  $scope.setPopElem = function(elem) {
    $scope.popElem = elem;
  };
  // for programmatic access
  $scope.closePopover = function() {
    if ($scope.popElem !== null) {
      $scope.popElem[0].click();
      $scope.popElem = null;
    }
  };
  
  $scope.isDescendant = function(parent, child) {
    var node = child.parentNode;
    while (node !== null) {
        if (node == parent)
            return true;
        node = node.parentNode;
    }
    return false;
  };
  
  $scope.popHide = function(e) {
    if (e.target && e.target.getAttribute && e.target.getAttribute('popover-stay-open') !== null) // enable popovers to stay open
      return;
    var ignoreTarg = $scope.popElem ? $scope.popElem[0] : null;
    if (ignoreTarg !== null && ignoreTarg.parentElement)
      ignoreTarg = ignoreTarg.parentElement;
    if ($scope.popElem !== null && ignoreTarg != e.target && !$scope.isDescendant(ignoreTarg, e.target))
      $scope.closePopover();
  };
  
  $scope.isMobile = util.isMobile() || 'true' === util.urlParams().mdwMobile;
  $scope.isDebug = 'true' === util.urlParams().mdwDebug;

  $scope.login = function() {
    window.location.href = $mdwHubRoot + $mdwLoginPage;
  };

  $scope.logout = function() {
    console.log('Logging out user: ' + $scope.authUser.id);
    authUser.logout();
  };
  
  $scope.getAuthUser = function() {
    return $scope.authUser;
  };
}]);

// container for one-at-a-time popovers
adminApp.directive('popContainer', ['$window', function($window) {
  return {
    restrict: 'A',
    link: function link(scope, elem, attrs) {

      var win = angular.element($window); 
      win.bind('click', scope.popHide);
      win.bind('scroll', scope.popHide);
      win.bind('resize', scope.popHide);
      
      scope.$on('$destroy', function() {
        win.unbind('click', scope.popHide);
        win.unbind('scroll', scope.popHide);
        win.unbind('resize', scope.popHide);
      });
    }
  };
}]);

adminApp.directive('popClick', [function() {
  return {
    restrict: 'A',
    link: function link(scope, elem, attrs) {
      elem.bind('click', function() {
        if (scope.popElem === elem) {
          // clicked to close
          scope.setPopElem(null);
        }
        else {
          scope.closePopover();
          scope.setPopElem(elem);
        }
      });
    }
  };
}]);

adminApp.directive('hubLink', ['$window', function($window) {
  return {
    restrict: 'A',
    link: function link(scope, elem, attrs) {
      elem.bind('click', function() {
        $window.location.href = scope.mdw.roots.hub;
      });
    }
  };
}]);

adminApp.directive('tabLink', ['$window', '$location', function($window, $location) {
  return {
    restrict: 'A',
    link: function link(scope, elem, attrs) {
      var url = attrs.tabLink;
      elem.bind('click', function() {
        if (url.startsWith('#')) {
          scope.authUser.setActiveTab(url);
          $location.path(url.substring(1));
          scope.$apply();
        }
        else {
          $window.location.href = url;
        }
      });
    }
  };
}]);

adminApp.directive('navLink', ['$route', '$location', function($route, $location) {
  return {
    restrict: 'A',
    link: function link(scope, elem, attrs) {
      if ($route.current) {
        var active = $route.current.loadedTemplateUrl === attrs.navLink; // includes parameters
        if (!active) {
          if (attrs.navLink.endsWith('*') && $route.current.loadedTemplateUrl.indexOf('?') == -1) {
            // wildcard (except urls with params)
            active = $route.current.templateUrl.startsWith(attrs.navLink.substring(0, attrs.navLink.length - 2));
          }
          else {
            // logical template
            active = $route.current.templateUrl === attrs.navLink + '.html';
          }
        }
        if (active)
          elem.addClass('mdw-active');
      }
    }
  };
}]);

// programmatic route navigation
adminApp.directive('mdwRoute', ['$location', function($location) {
  return {
    restrict: 'A',
    link: function link(scope, elem, attrs) {
      elem.bind('click', function() {
        var path = attrs.mdwRoute;
        if (!path.startsWith('#'))
          path = '#' + path;
        scope.authUser.setActiveTab(path);
        $location.path(path.substring(1));
        scope.$apply();
      });
    }
  };
}]);

adminApp.directive('onEnter', function() {
  return {
    restrict: 'A',
    link: function link(scope, elem, attrs) {
      elem.bind('keypress', function(event) {
        if (event.which === 13) {
          scope.$apply(function() {
            scope.$eval(attrs.onEnter);
          });
          event.preventDefault();
        }
      });
    }
  };
});

adminApp.directive('focusMe', ['$timeout', function($timeout) {
  return {
    restrict: 'A',
    link: function link(scope, elem, attrs) {
      if (attrs.focusMe.length === 0 || scope.$eval(attrs.focusMe)) {
        $timeout(function() {
          elem[0].focus();
        });
      }
    }
  };
}]);

adminApp.filter('highlight', function($sce) {
  return function(input, lang) {
    if (lang === 'test')
      lang = 'groovy';
    else if (lang === 'spring' || lang === 'camel' || lang === 'proc' || lang === 'task' || lang === 'impl' || lang === 'evth')
      lang = 'xml';
    if (lang && hljs.getLanguage(lang) && input) {
      return hljs.highlight(lang, input.removeCrs()).value;
    }
    else if (input)
      return input.replace(/&/g,'&amp;').replace(/</g,'&lt;');
    else
      return input;
  };
}).filter('unsafe', function($sce) { return $sce.trustAsHtml; });

adminApp.filter('markdown', function($sce) {
  return function(input) {
    if (input)
        return marked(input);
    else
      return input;
  };
}).filter('unsafe', function($sce) { return $sce.trustAsHtml; });

adminApp.filter('lineLimit', ['$filter', function($filter) {
  return function(input, limit) {
    if (input) {
      input = input.getLines()[0]; // first line only
      if (input.length <= limit)
         return input;
      return $filter('limitTo')(input, limit) + '...';
    }
  };
}]);

adminApp.filter('escape', function($sce) {
  return function(input) {
    if (input)
      return input.replace(/&/g,'&amp;').replace(/</g,'&lt;');
    else
      return input;
  };
}).filter('unsafe', function($sce) { return $sce.trustAsHtml; });

adminApp.filter('diff', function($sce) {
  return function(one, two) {
    if (one) {
      one = one.replace(/&/g,'&amp;').replace(/</g,'&lt;');
      if (two) {
        two = two.replace(/&/g,'&amp;').replace(/</g,'&lt;');
  
        var diffs = JsDiff.diffWordsWithSpace(one, two);
        var hlOne = '';
        var pos = 0;
        diffs.forEach(function(diff) {
          if (diff.removed) {
            var lines = diff.value.getLines();
            if (lines.length == 1) {
              hlOne += '<span class="mdw-diff-delta">' + lines[0] + '</span>';
            }
            else {
              for (var i = 0; i < lines.length; i++) {
                if (!(i == lines.length - 1 && lines[i] === ''))
                  hlOne += '<span class="mdw-diff-delta">' + lines[i] + '</span>\n';
              }
            }
          }
          else if (!diff.added) {
            hlOne += diff.value;
          }
        });
        if (hlOne.length > 0) {
          return hlOne;
        }
        else {
          return one + '\n';
        }
      }
      else {
        return one + '\n';
      }
    }
  };
}).filter('unsafe', function($sce) { return $sce.trustAsHtml; });

// wait until the user is loaded to manually bootstrap angularjs
var theUser;
var theRoutes;
angular.element(document).ready(function() {
  var ng = angular.injector(['ng', 'adminApp']);
  var authUser = ng.get('authUser');
  theRoutes = ng.get('routes');
  authUser.getAuthUser($mdwHubUser, window.location.hash).then(function(authUser) {
    theUser = authUser;
    if (theUser.cuid) {
      document.cookie = 'mdw.redirect=; expires=Thu, 01 Jan 1970 00:00:01 GMT;';
      angular.bootstrap(document, ['adminApp']);
    }
    else {
      // redirect to login
      document.cookie = 'mdw.redirect=' + encodeURIComponent(window.location.href) + ';path=/';
      window.location.href = $mdwHubRoot + $mdwLoginPage;
    }
  });
});

// in case js string does not supply startsWith() and endsWith()
if (typeof String.prototype.startsWith != 'function') {
  String.prototype.startsWith = function(prefix) {
    return this.indexOf(prefix) === 0;
  };
}
if (typeof String.prototype.endsWith !== 'function') {
  String.prototype.endsWith = function(suffix) {
      return this.indexOf(suffix, this.length - suffix.length) !== -1;
  };
}
// remove DOS/Windows CR characters
String.prototype.removeCrs = function() {
  return this.replace(/\r/g, '');
};
// split into lines (removing CRs first)
String.prototype.getLines = function() {
  return this.removeCrs().split(/\n/);
};
// line numbers
String.prototype.lineNumbers = function() {
  var lines = this.getLines();
  var lineNums = '';
  for (var i = 1; i < lines.length + 1; i++)
    lineNums += i + '\n';
  return lineNums;
};