'use strict';

var assetMod = angular.module('assets', ['ngResource', 'mdw']);

assetMod.controller('PackagesController', ['$scope', '$location', '$http', '$cookieStore', 'mdw', 'Assets', 'GitVcs', 'WorkflowCache', 'JSON_DOWNLOAD',
                                           function($scope, $location, $http, $cookieStore, mdw, Assets, GitVcs, WorkflowCache, JSON_DOWNLOAD) {
  $scope.pkgList = Assets.get({}, 
    function(data) {
      if (!$scope.pkgList.packages || $scope.pkgList.packages.length === 0)
        mdw.messages = 'No packages found in Asset Root: ' + $scope.pkgList.assetRoot;
      else if ($scope.pkgList.vcsBranch !== $scope.pkgList.gitBranch)
        mdw.messages = 'VCS branch: "' + $scope.pkgList.vcsBranch + '" does not match Git branch: "' + $scope.pkgList.gitBranch + "'"; 
      else {
        $scope.pkgList.selectedState = { all: false };
        $scope.pkgList.toggleAll = function() {
          $scope.pkgList.packages.forEach(function(pkg) {
            pkg.selected = $scope.pkgList.selectedState.all;
          });
        };
        $scope.pkgList.notAllSelected = function() {
          $scope.pkgList.selectedState.all = false;
        };
        $scope.pkgList.getSelected = function() {
          var selected = [];
          if ($scope.pkgList.packages) {
            $scope.pkgList.packages.forEach(function(pkg) {
              if (pkg.selected)
                selected.push(pkg);
            });
          }
          return selected;
        };
      }      
    },
    function(error) {
      if (error.data.status)
        mdw.messages = error.data.status.message;
    }
  );
  
  $scope.gitImportMessage = 'Do you want to import assets from Git?';
  $scope.fileImportMessage = 'Select a JSON or ZIP file to import.';
  $scope.fileImportUploading = false;
  $scope.packageImportFile = null;
  $scope.distributedImport = true;
  $scope.cacheRefresh = true;
  $scope.deleteTempBackups = true;
  
  $scope.cancel = function() {
    $location.path('/packages');
  };
  
  $scope.gitImport = function() {
    GitVcs.import({pkgPath: '*', gitAction: 'pull'}, { distributed: $scope.distributedImport, deleteTempBackups: $scope.deleteTempBackups }, function(data) {
      if (data.status && data.status.code !== 0) {
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
  
  $scope.fileImport = function() {
    if (!$scope.packageImportFile) {
      $scope.fileImportMessage = 'No file selected for import';
    }
    else {
      console.log("file: " + $scope.packageImportFile.name);
      $scope.fileImportMessage = 'Importing ' + $scope.packageImportFile.name + '...';
      $scope.fileImportUploading = true;
      $http({
        url: mdw.roots.hub + '/asset/packages?app=mdw-admin',
        method: 'PUT',
        headers: {'Content-Type': $scope.packageImportFile.name.endsWith('.zip') ? 'application/zip' : 'application/json'},
        data: $scope.packageImportFile.content,
        transformRequest: []
      }).then(function success(response) {
          $scope.fileImportUploading = false;
          console.log('package file uploaded');
          // leave cache error logging to the server side
          if ($scope.cacheRefresh)
            WorkflowCache.refresh({}, { distributed: $scope.distributedImport });
          $location.path('/packages');
        }, function error(response) {
          $scope.fileImportUploading = false;
          $scope.fileImportMessage = 'Upload failed: ' + response.statusText;
        }
      );
    }
  };
  
  $scope.getExportPackagesParam = function() {
    var pkgParam = '[';
    for (var i = 0; i < $scope.pkgList.getSelected().length; i++) {
      var pkg = $scope.pkgList.getSelected()[i];
      pkgParam += pkg.name;
      if (i < $scope.pkgList.getSelected().length - 1)
        pkgParam += ',';
    }
    pkgParam += ']';
    return pkgParam;
  };
  
  $scope.exportJson = function() {
    window.location = mdw.roots.services + '/services/Packages?app=mdw-admin&packages=' + 
        $scope.getExportPackagesParam() + '&' + JSON_DOWNLOAD;
  };

  $scope.exportZip = function() {
    window.location = mdw.roots.hub + '/asset/packages?app=mdw-admin&packages=' + 
        $scope.getExportPackagesParam();
  };

  $scope.discoveryUrl = $cookieStore.get('discoveryUrl');
  $scope.discover = function() {
    $cookieStore.put('discoveryUrl', $scope.discoveryUrl);
    $scope.discoveredPkgList = null;
    $scope.pkgList = Assets.get({discoveryUrl: $scope.discoveryUrl}, 
      function(data) {
        $scope.discoveryMessage = null;
        $scope.discoveredPkgList = data;
        $scope.discoveredPkgList.selectedState = { all: false };
        $scope.discoveredPkgList.toggleAll = function() {
          $scope.discoveredPkgList.packages.forEach(function(pkg) {
            pkg.selected = $scope.discoveredPkgList.selectedState.all;
          });
        };
        $scope.discoveredPkgList.notAllSelected = function() {
          $scope.discoveredPkgList.selectedState.all = false;
        };
        $scope.discoveredPkgList.getSelected = function() {
          var selected = [];
          if ($scope.discoveredPkgList.packages) {
            $scope.discoveredPkgList.packages.forEach(function(pkg) {
              if (pkg.selected)
                selected.push(pkg);
            });
          }
          return selected;
        };
      },
      function(error) {
        if (error.data.status)
          $scope.discoveryMessage = 'Discovery failed: ' + error.data.status.message;
      }
    );    
  };
  
  $scope.importDiscovered = function() {
    var pkgsObj = { packages: [] };
    
    $scope.discoveredPkgList.getSelected().forEach(function(pkg) {
      pkgsObj.packages.push(pkg.name);
    });
    
    $scope.pkgList = Assets.put({discoveryUrl: $scope.discoveryUrl}, pkgsObj, 
      function(data) {
        $scope.discoveryMessage = null;
        // leave cache error logging to the server side
        if ($scope.cacheRefresh)
          WorkflowCache.refresh({}, { distributed: true });
        $location.path('/packages');
      },
      function(error) {
        if (error.data.status)
          $scope.discoveryMessage = 'Import failed: ' + error.data.status.message;
      }
    );
  };

  $scope.refresh = function() {
    WorkflowCache.refresh({}, { distributed: true });
  };  
}]);

assetMod.controller('PackageController', ['$scope', '$routeParams', 'mdw', 'Assets', 'Asset', 
                                          function($scope, $routeParams, mdw, Assets, Asset) {
  mdw.message = null;
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
    },
    function(error) {
      if (error.data.status)
        mdw.messages = error.data.status.message;
    }    
  );
}]);

assetMod.controller('AssetController', ['$scope', '$routeParams', 'mdw', 'util', 'Assets', 'Asset', 
                                       function($scope, $routeParams, mdw, util, Assets, Asset) {
  
  $scope.packageName = $routeParams.packageName;
  $scope.assetName = $routeParams.assetName;
  if ($scope.assetName.endsWith('.proc')) {
    $scope.process = {packageName: $scope.packageName, name: $scope.assetName.substring(0, $scope.assetName.length - 5)};
  }

  $scope.asset = Assets.get({
      packageName: $routeParams.packageName,
      assetName: $routeParams.assetName
    },
    function(assetsData) {
      $scope.asset.language = util.getLanguage($scope.asset.name);
      
      $scope.asset.url = mdw.roots.hub + '/asset/' + $scope.packageName + '/' +  $scope.asset.name;
      $scope.asset.view = 'content';
      $scope.asset.packageName = $scope.packageName;
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
    get: { method: 'GET', isArray: false },
    put: { method: 'PUT'}
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
    },
    put: {
      method: 'PUT'
    }
  });
}]);

assetMod.factory('GitVcs', ['$resource', 'mdw', function($resource, mdw) {
  return $resource(mdw.roots.services + '/Services/GitVcs/:pkgPath/:asset', mdw.serviceParams(), {
    import: { method: 'POST' },
    push: { method: 'POST'}
  });
}]);

assetMod.factory('WorkflowCache', ['$resource', 'mdw', function($resource, mdw) {
  return $resource(mdw.roots.services + '/Services/WorkflowCache', mdw.serviceParams(), {
    refresh: { method: 'POST' }
  });
}]);
