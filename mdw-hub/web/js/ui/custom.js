'use strict';

var customMod = angular.module('mdwCustom', ['mdw']);

customMod.controller('CustomController', ['$scope', '$route', 'mdw',
  function($scope, $route, mdw) {
  
    $scope.getJsxAsset = function() {
      if ($mdwCustomRoutes && !$mdwCustomRoutes.startsWith('${')) {
          var match = JSON.parse($mdwCustomRoutes).find(function(customRoute) {
            return customRoute.path === $route.current.$$route.originalPath;
          });
          if (match)
            return match.asset;
      }
    };
  }
]);