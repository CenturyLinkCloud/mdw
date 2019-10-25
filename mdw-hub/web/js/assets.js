'use strict';

var assetMod = angular.module('assets', ['ngResource', 'mdw']);

assetMod.controller('PackagesController', ['$scope', '$location', '$route', '$http', '$uibModal', 'mdw', 'util', 'uiUtil', 'Assets', 'GitVcs', 'WorkflowCache',
                                           function($scope, $location, $route, $http, $uibModal, mdw, util, uiUtil, Assets, GitVcs, WorkflowCache) {
  $scope.pkgList = Assets.get({}, 
    function(data) {
      if (!$scope.pkgList.packages || $scope.pkgList.packages.length === 0) {
        mdw.messages = 'No packages found in Asset Root: ' + $scope.pkgList.assetRoot;
      }
      else if ($scope.pkgList.vcsBranch !== $scope.pkgList.gitBranch && !$scope.pkgList.vcsTag) {
        mdw.messages = 'VCS branch: "' + $scope.pkgList.vcsBranch + '" does not match Git branch: "' + $scope.pkgList.gitBranch + "'";
      }
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
  $scope.fileImportMessage = 'Select an asset package ZIP file to import.';
  $scope.fileImportUploading = false;
  $scope.packageImportFile = null;
  $scope.distributedImport = false;
  $scope.cacheRefresh = false;
  $scope.deleteTempBackups = true;
  $scope.gitHardReset = false;
  
  $scope.cancel = function() {
    $location.path('/packages');
  };
  
  $scope.gitImport = function() {
    GitVcs.import({pkgPath: '*', gitAction: 'pull'}, { distributed: $scope.distributedImport, deleteTempBackups: $scope.deleteTempBackups, gitHard: $scope.gitHardReset }, function(data) {
      if (data.status && data.status.code >= 300) {
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
        headers: {'Content-Type': 'application/zip'},
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
    var pkgParam = '%5B';
    for (var i = 0; i < $scope.pkgList.getSelected().length; i++) {
      var pkg = $scope.pkgList.getSelected()[i];
      pkgParam += pkg.name;
      if (i < $scope.pkgList.getSelected().length - 1)
        pkgParam += ',';
    }
    pkgParam += '%5D';
    return pkgParam;
  };
  
  $scope.exportZip = function() {
    window.location = mdw.roots.hub + '/asset/packages?app=mdw-admin&packages=' + 
        $scope.getExportPackagesParam();
  };

  $scope.createPackage = function() {
    uiUtil.enter('Create Package', 'New package name:', function(res) {
      if (res) {
        Assets.post({packageName: res}, {name: res},
          function(data) {
            $scope.mdwMessages = null;
            console.log('created package: ' + res);
            $route.reload();
          },
          function(error) {
            if (error.data.status)
              $scope.mdwMessages = 'Package create failed: ' + error.data.status.message;
          }
        ); 
      }
    });
  };
  
  $scope.refresh = function() {
    WorkflowCache.refresh({}, { distributed: true });
  };
  
}]);

assetMod.controller('PackageController', ['$scope', '$routeParams', '$route', '$location', 'mdw', 'uiUtil', 'Assets', 'Asset', 'ASSET_TYPES', 
                                          function($scope, $routeParams, $route, $location, mdw, uiUtil, Assets, Asset, ASSET_TYPES) {
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
  
  $scope.getAssetTypes = function() {
    return ASSET_TYPES;    
  };

  $scope.newAsset = function(type) {
    var ext = ASSET_TYPES[type];
    $scope.closePopover();
    uiUtil.enter('Create Asset', 'New asset name (.' + ext  + '):', function(res) {
      if (res) {
        var assetName = res;
        var lastDot = res.lastIndexOf('.');
        if (lastDot <= 0)
          assetName += '.' + ext;
        if (!assetName.endsWith('.' + ext)) {
          mdw.messages = 'Invalid ' + type + ' asset name: ' + assetName;
        }
        else {
          Assets.post({packageName: $routeParams.packageName, assetName: assetName}, {name: assetName},
            function(data) {
              $scope.mdwMessages = null;
              console.log('created asset: ' + $routeParams.packageName + '/' + assetName);
              $location.path('/edit/' + $routeParams.packageName + '/' + assetName);
            },
            function(error) {
              if (error.data.status)
                $scope.mdwMessages = 'Asset create failed: ' + error.data.status.message;
            }
          ); 
        }
      }
    });
  };
  
  $scope.deletePackage = function() {
    var msg = 'Delete: ' + $scope.pkg.name + '?  All its assets will be deleted.';
    uiUtil.confirm('Confirm Delete Package', msg, function(res) {
      if (res) {
        Assets.del({packageName: $scope.pkg.name},
          function(data) {
            $scope.mdwMessages = null;
            console.log('deleted package: ' + $scope.pkg.name);
            $location.path('/packages');
          },
          function(error) {
            if (error.data.status)
              $scope.mdwMessages = 'Package delete failed: ' + error.data.status.message;
          }
        ); 
      }
    });
  };
}]);

assetMod.controller('AssetController', ['$scope', '$cookieStore', '$routeParams', '$route', '$location', '$http', 'mdw', 'util', 'uiUtil', 'Assets', 'Asset', 'AssetVersions', 'Staging',
                                       function($scope, $cookieStore, $routeParams, $route, $location, $http, mdw, util, uiUtil, Assets, Asset, AssetVersions, Staging) {

  $scope.packageName = $routeParams.packageName;
  $scope.assetName = $routeParams.assetName;
  $scope.version = $routeParams.version;
  if ($scope.assetName.endsWith('.proc')) {
    $scope.process = {packageName: $scope.packageName, name: $scope.assetName.substring(0, $scope.assetName.length - 5)};
    if ($scope.version && $scope.version !== $scope.process.version) {
      $scope.process.version = $scope.version;
      $scope.process.archived = true;
    }
  }
  else if ($scope.assetName.endsWith('.task')) {
    $scope.task = true;
  }
 
  $scope.asset = Assets.get({
      packageName: $routeParams.packageName,
      assetName: $routeParams.assetName,
      version: $routeParams.version
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
              assetName: $scope.asset.name,
              version: $scope.version
            },
            function(assetData) {
              if (assetData.rawResponse) {
                $scope.asset.content = assetData.rawResponse.removeCrs();
                $scope.asset.lineNums = $scope.asset.content.lineNumbers();
                // TODO process image
              }
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

      // history
      if ($route.current && $route.current.templateUrl && $route.current.templateUrl.endsWith('/history.html')) {
        AssetVersions.retrieve({
            packageName: $scope.packageName,
            assetName: $scope.assetName,
            withCommitInfo: true
          }, function(data) {
          $scope.versions = data.versions;
          $scope.versions.forEach(function(version) {
            if (version.commitInfo && version.commitInfo.date) {
                version.commitInfo.date = new Date(version.commitInfo.date);
            }
          });
        });
      }
    }
  );

  $scope.isEditAllowed = function() {
    return $scope.authUser.hasRole('Process Design') && !$scope.version;
  };

  $scope.stageAsset = function() {
    var assetPath = $scope.packageName + '/' + $scope.asset.name;
    Staging.stageAsset($scope.authUser.cuid, assetPath);
  };

  $scope.rollbackAsset = function(version) {
    var stagingCuid = $scope.authUser.cuid;
    var assetVersions = {};
    assetVersions[$scope.packageName + '/' + $scope.asset.name] = version;
    $http({
      url: mdw.roots.services + '/services/com/centurylink/mdw/staging/' + stagingCuid + '/assetVersions?app=mdw-admin',
      method: 'PUT',
      headers: {'Content-Type': 'application/json'},
      data: JSON.stringify({assetVersions: assetVersions}, null, 2),
      transformRequest: []
    }).then(function success(response) {
        $location.path('/staging/' + stagingCuid);
      }, function error(response) {
        if (response.status === 404) {
          $location.path('/staging/' + stagingCuid);
        }
        else {
          uiUtil.error('Asset Rollback Error', response.data.status.message);
        }
      }
    );
  };

  $scope.viewInstances=function(){
    $cookieStore.put('taskId', $scope.asset.id);
    $location.path('/tasks/');
  };
  $scope.deleteAsset = function() {
    var msg = 'Delete: ' + $scope.asset.name + '?';
    uiUtil.confirm('Confirm Delete', msg, function(res) {
      if (res) {
        Assets.del({packageName: $scope.packageName, assetName: $scope.asset.name},
          function(data) {
            $scope.mdwMessages = null;
            console.log('deleted asset: ' + $scope.packageName + '/' + $scope.asset.name);
            $location.path('/packages/' + $scope.packageName);
          },
          function(error) {
            if (error.data.status)
              $scope.mdwMessages = 'Asset delete failed: ' + error.data.status.message;
          }
        ); 
      }
    });
  };
  
}]);

assetMod.controller('ArchiveController', ['$scope', '$route', 'mdw', 'uiUtil', 'Assets',
                                  function($scope, $route, mdw, uiUtil, Assets, Asset) {
  $scope.archiveDirs = Assets.get({archiveDirs: true});
  
  $scope.deleteArchive = function() {
    var msg = 'Proceed with delete?\nArchive cannot be recovered!';
    uiUtil.confirm('Confirm Archive Delete', msg, function(res) {
      if (res) {
        Assets.del({packageName: 'Archive'},
          function(data) {
            $scope.mdwMessages = null;
            console.log('deleted asset archive');
            $route.reload();
          },
          function(error) {
            if (error.data.status)
              $scope.mdwMessages = 'Archive delete failed: ' + error.data.status.message;
          }
        ); 
      }
    });
  };
}]);

assetMod.controller('DiscoveryController', ['$scope', '$route', '$location', 'mdw', 'uiUtil', 'Assets', 'WorkflowCache',
                                  function($scope, $route, $location, mdw, uiUtil, Assets, WorkflowCache) {

  $scope.discoveryUrls = mdw.discoveryUrls;

  $scope.repositoriesList = Assets.get({discoveryUrls: $scope.discoveryUrls, discoveryType: 'git'},
    function(data) {
      $scope.discoveryMessage = null;
      $scope.repositoriesList.repositories.forEach(function(repository) {
        var url = repository.url;
        if (url.indexOf('?') != -1)
          url = url.substr(0, url.indexOf('?'));
        var lines = url.split('@');
        if (lines[1] != null)
           url = lines[0].substr(0, lines[0].indexOf("//") + 2) + lines[1];
        repository.repoUrl = url;
      });
    },
    function(error) {
      if (error.data.status)
        $scope.discoveryMessage = 'Discovery failed: ' + error.data.status.message;
    }
  );

  $scope.collapse = function(repository) {
    repository.collapsed = true;
  };

  $scope.expand = function(repository) {
    repository.collapsed = false;
  };

  $scope.collapseBranch = function(branch) {
    branch.collapsed = true;
  };

  $scope.expandBranch = function(branch) {
    branch.collapsed = false;
  };

  $scope.collapseTag = function(tag) {
    tag.collapsed = true;
  };

  $scope.expandTag = function(tag) {
    tag.collapsed = false;
  };

  $scope.clear = function() {
    $scope.discoveredPkgList = null;
  };

  $scope.discover = function() {
    $scope.discoveredPkgList = null;
    $scope.pkgList = Assets.get({discoveryUrls: $scope.discoveryUrl, branch:$scope.branch , discoveryType: 'git'},
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

  $scope.discoverFromGit = function(repoUrl, branch) {
    $scope.discoveryUrl = repoUrl;
    $scope.branch = branch;
    $scope.closePopover();
    $scope.discover();
  };

  $scope.importDiscovered = function() {
    var pkgsObj = { packages: [] };

    $scope.discoveredPkgList.getSelected().forEach(function(pkg) {
        pkgsObj.packages.push(pkg.name);
    });

    $scope.pkgList = Assets.put({discoveryUrl: $scope.discoveryUrl, branch:$scope.branch, discoveryType: 'git'}, pkgsObj,
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

  $scope.stopPropagation = function($event) {
    $event.stopPropagation();
    $event.preventDefault();
  };

}]);

assetMod.factory('Assets', ['$resource', 'mdw', function($resource, mdw) {
  return $resource(mdw.roots.services + '/Services/Assets/:packageName/:assetName/:version', mdw.serviceParams(), {
    get: { method: 'GET', isArray: false },
    put: { method: 'PUT'},
    post: { method: 'POST'},
    del: { method: 'DELETE'}
  });
}]);

assetMod.factory('Asset', ['$resource', 'mdw', function($resource, mdw) {
  return $resource(mdw.roots.hub + '/asset/:packageName/:assetName/:instanceId/:version', mdw.hubParams(), {
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

assetMod.factory('AssetVersions', ['$resource', 'mdw', function($resource, mdw) {
  return $resource(mdw.roots.services + '/Services/Versions/:packageName/:assetName', mdw.serviceParams(), {
    retrieve: { method: 'GET', isArray: false }
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

assetMod.factory('Staging', ['$http', '$location', '$route', 'mdw', 'uiUtil', function($http, $location, $route, mdw, uiUtil) {
  return {
    stageAsset: function(stagingCuid, assetPath, callback) {
      var assets = { assets: [assetPath] };
      $http({
        url: mdw.roots.services + '/services/com/centurylink/mdw/staging/' + stagingCuid + '/assets?app=mdw-admin',
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        data: JSON.stringify(assets, null, 2),
        transformRequest: []
      }).then(function success(response) {
          $location.path('/staging/' + stagingCuid);
          if (callback) {
            callback(response.status);
          }
        }, function error(response) {
          if (response.status === 404) {
            $location.path('/staging/' + stagingCuid);
          }
          else {
            uiUtil.error('Cannot Stage Asset', response.data.status.message);
          }
          if (callback) {
            callback(response.status);
          }
        }
      );
    }
  };
}]);

