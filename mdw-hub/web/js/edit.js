'use strict';

var editMod = angular.module('edit', ['ngResource', 'mdw']);

editMod.controller('EditorController', ['$scope', '$routeParams', 'mdw', 'util', 'Assets', 'Asset', 'GitVcs',
                                         function($scope, $routeParams, mdw, util, Assets, Asset, GitVcs) {
  
  $scope.packageName = $routeParams.packageName;
  $scope.assetName = $routeParams.assetName;

  $scope.asset = Assets.get({
      packageName: $routeParams.packageName,
      assetName: $routeParams.assetName
    },
    function(assetsData) {
      $scope.asset.language = util.getLanguage($scope.asset.name);
      if ($scope.asset.language == 'proc') {
        $scope.process = {packageName: $scope.packageName, name: $scope.asset.name.substring(0, $scope.asset.name.length - 5)};
      }
      
      $scope.aceOptions = {
        theme: 'eclipse', 
        mode: $scope.asset.language
      };
      
      $scope.version = {
        current: $scope.asset.version,
        nextMinor: util.nextMinor($scope.asset.version),
        nextMajor: util.nextMajor($scope.asset.version),
        selected: util.nextMinor($scope.asset.version)
      };
      
      $scope.asset.url = mdw.roots.hub + '/asset/' + $scope.packageName + '/' +  $scope.asset.name;
      
      $scope.asset.view = 'content';
      $scope.asset.packageName = $scope.packageName;
      
      Asset.get({
          packageName: $scope.packageName,
          assetName: $scope.asset.name,
          gitRemote: true
        },
        function(assetData) {
          $scope.asset.content = assetData.rawResponse;
        }
      );
    }
  );
  
  $scope.save = function() {
    console.log('saving: ' + $scope.asset.packageName + '/' + $scope.asset.name + ' v' + $scope.version.selected);
    console.log('comment: ' + $scope.version.comment);
    $scope.closePopover();
  };
  
  
}]);
