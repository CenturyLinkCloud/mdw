'use strict';

var customMod = angular.module('mdwCustom', ['mdw']);

customMod.controller('CustomController', ['$scope', 'mdw',
  function($scope, mdw) {
    console.log("CUSTOM CONTROLLER: " + $mdwUi.routesMap['demo/Bug.jsx']);
    
    $scope.getJsxAsset = function() {
      return 'demo/Bug.jsx';
    };
  }
]);