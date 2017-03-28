'use strict';

var editMod = angular.module('edit', ['ngResource', 'mdw']);

editMod.controller('EditorController', ['$scope', '$routeParams', 'mdw', 'util', 'Assets', 'Asset', 'WorkflowCache', 'GitVcs',
                                         function($scope, $routeParams, mdw, util, Assets, Asset, WorkflowCache, GitVcs) {
  
  $scope.packageName = $routeParams.packageName;
  $scope.assetName = $routeParams.assetName;
  if ($scope.assetName.endsWith('.proc')) {
    $scope.process = {packageName: $scope.packageName, name: $scope.assetName.substring(0, $scope.assetName.length - 5)};
    $scope.onProcessChange = function(proc) {
      var wasDirty = $scope.procDirty;
      $scope.procDirty = true;
      if (!wasDirty)
        $scope.$digest();
      $scope.process = proc;
      // console.log(JSON.stringify($scope.process, null, '  '));
    };
  }

  $scope.asset = Assets.get({
      packageName: $routeParams.packageName,
      assetName: $routeParams.assetName
    },
    function(assetsData) {
      $scope.asset.language = util.getLanguage($scope.asset.name);
      
      $scope.aceOptions = {
        theme: 'eclipse', 
        mode: $scope.asset.language,
        onChange: function() {
          // first call happens on load
          if ($scope.aceDirty === undefined)
            $scope.aceDirty = false;
          else
            $scope.aceDirty = true;
        }
      };
      
      $scope.initVersion();
      $scope.initOptions();
      
      $scope.asset.url = mdw.roots.hub + '/asset/' + $scope.packageName + '/' +  $scope.asset.name;
      
      $scope.asset.view = 'content';
      $scope.asset.packageName = $scope.packageName;
      
      Asset.get({
        packageName: $scope.packageName,
        assetName: $scope.asset.name
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
  
  $scope.initOptions = function() {
    $scope.options = {
      distributedSave: true,
      cacheRefresh: true,
      commitAndPush: true
    };    
  };
  
  $scope.isDirty = function() {
    if ($scope.process)
      return $scope.procDirty;
    else
      return $scope.aceDirty;
  };
  
  $scope.isSaveEnabled = function() {
    return $scope.isDirty() && (!$scope.commitAndSave || $scope.version.comment);
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
      comment: $scope.version.comment,
      distributedSave: $scope.options.distributedSave
    }, 
    $scope.asset.content, 
    function success(response) {
      $scope.message = null;
      $scope.aceDirty = false;
      $scope.asset.version = $scope.version.selected;
      var commitMsg = $scope.version.comment;
      if ($scope.options.cacheRefresh) {
        WorkflowCache.refresh({}, { distributed: $scope.options.distributedSave });
      }
      if ($scope.options.commitAndPush) {
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
      }
      else {
        $scope.closePopover();
      }
      
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
