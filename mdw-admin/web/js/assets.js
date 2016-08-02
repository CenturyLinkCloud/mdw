// Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
'use strict';

var assetMod = angular.module('assets', ['ngResource', 'mdw']);

assetMod.controller('PackagesController', ['$scope', '$location', 'Assets', 'GitVcs', 'WorkflowCache', 
                                           function($scope, $location, Assets, GitVcs, WorkflowCache) {
  $scope.pkgList = Assets.get();
  
  $scope.gitImportMessage = 'Please confirm that you want to perform "git checkout" to update this Asset Root.';
  $scope.distributedImport = true;
  $scope.cacheRefresh = true;
  
  $scope.cancel = function() {
    $location.path("/packages");
  };
  
  $scope.gitImport = function() {
    GitVcs.import({assetPath: '*', gitAction: 'pull'}, { distributed: $scope.distributedImport}, function(data) {
      if (data.status.code !== 0) {
        $scope.gitImportMessage = data.status.message;
      }
      else {
        $scope.gitImportMessage = null;
        // leave cache error logging to the server side
        if ($scope.cacheRefresh)
          WorkflowCache.refresh({}, { distributed: $scope.distributedImport });
        $location.path('/packages');
      }
    }, 
    function(error) {
      $scope.gitImportMessage = error.data.status.message;
    });
  };
  
  $scope.refresh = function() {
    WorkflowCache.refresh({}, { distributed: true });
  };  
}]);

assetMod.controller('PackageController', ['$scope', '$routeParams', 'Assets', 'Asset', 
                                          function($scope, $routeParams, Assets, Asset) {
  $scope.pkg = Assets.get({
    packageName: $routeParams.packageName},
    function(pkgData) {
      for (var i = 0; i < pkgData.assets.length; i++) {
        var asset = pkgData.assets[i];
        if (asset.name == "README.md" || asset.name == "readme.md")
          $scope.pkg.readmeAsset = asset;
      }
      if ($scope.pkg.readmeAsset) {
        Asset.get({
          packageName: $routeParams.packageName,
          assetName: $scope.pkg.readmeAsset.name
        },
        function(assetData) {
          $scope.pkg.readmeAsset.content = assetData.rawResponse;
        });
      }
    });
}]);

assetMod.controller('AssetController', ['$scope', '$routeParams', 'mdw', 'Assets', 'Asset', 
                                       function($scope, $routeParams, mdw, Assets, Asset) {
  
  $scope.packageName = $routeParams.packageName;

  $scope.asset = Assets.get({
      packageName: $routeParams.packageName,
      assetName: $routeParams.assetName
    },
    function(assetsData) {
      var lastDot = $scope.asset.name.lastIndexOf('.');
      if (lastDot > 0 && lastDot < $scope.asset.name.length - 2)
        $scope.asset.language = $scope.asset.name.substring(lastDot + 1);

      if ($scope.asset.language == 'proc')
        $scope.asset.workflowImageUrl = mdw.roots.hub + '/workflowImage?processId=' + $scope.asset.id; 
      $scope.asset.url = mdw.roots.hub + '/asset/' + $scope.packageName + '/' +  $scope.asset.name;
      $scope.asset.view = 'content';      
      if (!$scope.asset.isBinary && !$scope.asset.isImage) {
        if ($scope.asset.vcsDiff != 'MISSING') {
          Asset.get({
              packageName: $scope.packageName,
              assetName: $scope.asset.name
            },
            function(assetData) {
              $scope.asset.content = assetData.rawResponse.removeCrs();
              $scope.asset.lineNums = $scope.asset.content.lineNumbers();
              // TODO process image
            }
          );
        }
        if ($scope.asset.vcsDiff && $scope.asset.vcsDiff != 'EXTRA') {
          Asset.get({
              packageName: $scope.packageName,
              assetName: $scope.asset.name,
              gitRemote: true
            },
            function(assetData) {
              $scope.asset.remoteContent = assetData.rawResponse.removeCrs();
              $scope.asset.remoteLineNums = $scope.asset.remoteContent.lineNumbers();
            }
          );
        }
      }
    }
  );
}]);

assetMod.factory('Assets', ['$resource', 'mdw', function($resource, mdw) {
  return $resource(mdw.roots.services + '/Services/Assets/:packageName/:assetName', mdw.serviceParams(), {
    get: { method: 'GET', isArray: false }
  });
}]);

assetMod.factory('Asset', ['$resource', 'mdw', function($resource, mdw) {
  return $resource(mdw.roots.hub + '/asset/:packageName/:assetName', mdw.hubParams(), {
    get: { 
      method: 'GET', 
      transformResponse: function(data, headers) {
        var assetData = {
            rawResponse: data
        };
        return assetData;
      }
    }
  });
}]);

assetMod.factory('GitVcs', ['$resource', 'mdw', function($resource, mdw) {
  return $resource(mdw.roots.services + '/Services/GitVcs/:assetPath', mdw.serviceParams(), {
    import: { method: 'POST' }
  });
}]);

assetMod.factory('WorkflowCache', ['$resource', 'mdw', function($resource, mdw) {
  return $resource(mdw.roots.services + '/Services/WorkflowCache', mdw.serviceParams(), {
    refresh: { method: 'POST' }
  });
}]);
