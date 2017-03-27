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
        mode: $scope.asset.language,
        onChange: function() {
          // first call happens on load
          if ($scope.dirty === undefined)
            $scope.dirty = false;
          else
            $scope.dirty = true;
        }
      };
      
      $scope.initVersion();
      
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
      });
    }
  );
  
  $scope.initVersion = function() {
    $scope.version = {
        current: $scope.asset.version,
        nextMinor: util.nextMinor($scope.asset.version),
        nextMajor: util.nextMajor($scope.asset.version),
        selected: util.nextMinor($scope.asset.version),
        comment: null
      };
  };
  
  $scope.isSaveEnabled = function() {
    // TODO: not if git pull needed
    var gitPullNeeded = false;
    return $scope.dirty && $scope.version.comment && !gitPullNeeded;
  };
  
  $scope.cancelSave = function() {
    $scope.closePopover();
    $scope.initVersion();
    $scope.message = null;
  };
  
  $scope.save = function() {
    console.log('saving: ' + $scope.asset.packageName + '/' + $scope.asset.name + ' v' + $scope.version.selected);
    Asset.put({
      packageName: $scope.asset.packageName,
      assetName: $scope.asset.name,
      version: $scope.version.selected,
      comment: $scope.version.comment
    }, 
    $scope.asset.content, 
    function success(response) {
      $scope.message = null;
      $scope.dirty = false;
      $scope.asset.version = $scope.version.selected;
      var commitMsg = $scope.version.comment;
      GitVcs.push({
        pkgPath: $scope.asset.packageName,
        asset: $scope.asset.name,
        gitAction: 'push'
      },
      { comment: commitMsg },
      function success(response) {
        $scope.closePopover();
      },
      function error(response) {
        if (response.data.status)
          $scope.message = response.data.status.message;
        else if (response.statusText)
          $scope.message = response.status + ': ' + response.statusText;
        else
          $scope.message = "Error saving asset";
      });
      
      $scope.initVersion();
    }, 
    function error(response) {
      if (response.data.status)
        $scope.message = response.data.status.message;
      else if (response.statusText)
        $scope.message = response.status + ': ' + response.statusText;
      else
        $scope.message = "Error saving asset";
    });
  };
  
}]);
