'use strict';

var assetMod = angular.module('assets', ['ngResource', 'mdw']);

assetMod.controller('PackagesController', ['$scope', '$location', '$route', '$http', '$cookieStore', '$uibModal', 'mdw', 'util', 'uiUtil', 'Assets', 'GitVcs', 'WorkflowCache',
                                           function($scope, $location, $route, $http, $cookieStore, $uibModal, mdw, util, uiUtil, Assets, GitVcs, WorkflowCache) {
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

  $scope.repositoryUrls = $cookieStore.get('discoveryUrls');
  $scope.discoveryType = 'central';
  $scope.groupId = $cookieStore.get('groupId');
  if (!$scope.discoveryUrls)
    $scope.discoveryUrls = mdw.discoveryUrls;
  if (!$scope.groupId)
    $scope.groupId = 'com.centurylink.mdw.assets';

  $scope.repositoriesList = Assets.get({discoveryUrls: $scope.discoveryUrls, discoveryType: 'git'},
    function(data) {
      $scope.discoveryMessage = null;
      $scope.repositoriesList.repositories.forEach(function(repository) {
        var url = repository.url;
        if (url.indexOf('?') != -1)
          url = url.substr(0, url.indexOf('?'));
        var lines = url.split('@');
        if (lines[1] != null)
           url = lines[0].substr(0, lines[0].indexOf("//")+2) + lines[1];
        repository.repoUrl = url;
      });
      $scope.applyRepoCollapsedState();
      $scope.applyBranchCollapsedState();
      $scope.applyTagCollapsedState();
    },
    function(error) {
      if (error.data.status)
        $scope.discoveryMessage = 'Discovery failed: ' + error.data.status.message;
    }
  );

  $scope.saveRepoCollapsedState = function() {
    var st = {};
    $scope.repositoriesList.repositories.forEach(function(repository) {
    if (repository.collapsed)
      st[repository.url] = true;
    });
    $cookieStore.put('repoCollapsedState', st);
  };

  $scope.saveBranchCollapsedState = function() {
    var st = {};
    $scope.repositoriesList.repositories.forEach(function(repository) {
      if (repository.branches.collapsed)
        st[repository.url+'_branch'] = true;
    });
    $cookieStore.put('branchCollapsedState', st);
  };

  $scope.saveTagCollapsedState = function() {
    var st = {};
    $scope.repositoriesList.repositories.forEach(function(repository) {
      if (repository.tags.collapsed)
        st[repository.url+'_tag'] = true;
    });
    $cookieStore.put('tagCollapsedState', st);
  };

  $scope.applyRepoCollapsedState = function() {
    var st = $cookieStore.get('repoCollapsedState');
    if (st) {
      util.getProperties(st).forEach(function(repoUrl) {
        var col = st[repoUrl];
        if (col === true) {
          if($scope.repositoriesList.repositories) {
            for (var i = 0; i < $scope.repositoriesList.repositories.length; i++) {
              if (repoUrl == $scope.repositoriesList.repositories[i].url) {
                $scope.repositoriesList.repositories[i].collapsed = true;
                break;
              }
            }
          }
        }
      });
    }
  };

  $scope.applyBranchCollapsedState = function() {
    var st = $cookieStore.get('branchCollapsedState');
    if (st) {
      util.getProperties(st).forEach(function(branch) {
        var col = st[branch];
        if (col === true) {
          if ($scope.repositoriesList.repositories) {
            for (var i = 0; i < $scope.repositoriesList.repositories.length; i++) {
              if (branch === $scope.repositoriesList.repositories[i].url+"_branch") {
                $scope.repositoriesList.repositories[i].branches.collapsed = true;
                break;
              }
            }
          }
        }
      });
    }
  };

  $scope.applyTagCollapsedState = function() {
      var st = $cookieStore.get('tagCollapsedState');
      if (st) {
        util.getProperties(st).forEach(function(tag) {
          var col = st[tag];
          if (col === true) {
            if($scope.repositoriesList.repositories) {
              for (var i = 0; i < $scope.repositoriesList.repositories.length; i++) {
                if (tag === $scope.repositoriesList.repositories[i].url+"_tag") {
                  $scope.repositoriesList.repositories[i].tags.collapsed = true;
                  break;
                }
              }
            }
          }
        });
      }
    };

  $scope.collapse = function(repository) {
    repository.collapsed = true;
    $scope.saveRepoCollapsedState();
  };

  $scope.expand = function(repository) {
    repository.collapsed = false;
    $scope.saveRepoCollapsedState();
  };

  $scope.collapseBranch = function(branch) {
    branch.collapsed = true;
    $scope.saveBranchCollapsedState();
  };

  $scope.expandBranch = function(branch) {
    branch.collapsed = false;
    $scope.saveBranchCollapsedState();
  };

  $scope.collapseTag = function(tag) {
    tag.collapsed = true;
    $scope.saveTagCollapsedState();
  };

  $scope.expandTag = function(tag) {
    tag.collapsed = false;
    $scope.saveTagCollapsedState();
  };

  $scope.discover = function() {
    $cookieStore.put('groupId', $scope.groupId);
    $scope.discoveredPkgList = null;
    $scope.pkgList = Assets.get({discoveryUrls: $scope.discoveryUrl, branch:$scope.branch , discoveryType: $scope.discoveryType, groupId: $scope.groupId},
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
  
  $scope.clear = function() {
    $scope.discoveredPkgList = null; 
  };

  $scope.discoverFromGit = function(repoUrl, branch) {
    $cookieStore.put('discoveryUrls', $scope.discoveryUrls);
    $scope.discoveryUrl =  repoUrl;
    $scope.branch = branch;
    $scope.closePopover();
    $scope.discover();
  };

  $scope.stopPropagation = function($event) {
    $event.stopPropagation();
    $event.preventDefault();
  };
  
  $scope.importDiscovered = function() {
    var pkgsObj = { packages: [] };
    
    $scope.discoveredPkgList.getSelected().forEach(function(pkg) {
      if ($scope.discoveryType === 'central')
        pkgsObj.packages.push(pkg.artifact + "-" + pkg.version);
      else
        pkgsObj.packages.push(pkg.name);
    });
    
    $scope.pkgList = Assets.put({discoveryUrl: $scope.discoveryUrl, branch:$scope.branch, discoveryType: $scope.discoveryType, groupId: $scope.groupId}, pkgsObj,
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

  $scope.isEditAllowed = function() {
    return $scope.authUser.hasRole('Process Design') && !mdw.git.tag;
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

assetMod.controller('AssetController', ['$scope', '$cookieStore', '$routeParams', '$location', 'mdw', 'util', 'uiUtil', 'Assets', 'Asset', 
                                       function($scope, $cookieStore, $routeParams, $location, mdw, util, uiUtil, Assets, Asset) {
  
  $scope.packageName = $routeParams.packageName;
  $scope.assetName = $routeParams.assetName;
  if ($scope.assetName.endsWith('.proc')) {
    $scope.process = {packageName: $scope.packageName, name: $scope.assetName.substring(0, $scope.assetName.length - 5)};
  }
  else if($scope.assetName.endsWith('.task')){
    $scope.task = true;
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
    }
  );

  $scope.isEditAllowed = function() {
    return $scope.authUser.hasRole('Process Design') && !$scope.asset.isBinary && !mdw.git.tag;
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

assetMod.factory('Assets', ['$resource', 'mdw', function($resource, mdw) {
  return $resource(mdw.roots.services + '/Services/Assets/:packageName/:assetName', mdw.serviceParams(), {
    get: { method: 'GET', isArray: false },
    put: { method: 'PUT'},
    post: { method: 'POST'},
    del: { method: 'DELETE'}
  });
}]);

assetMod.factory('Asset', ['$resource', 'mdw', function($resource, mdw) {
  return $resource(mdw.roots.hub + '/asset/:packageName/:assetName/:instanceId', mdw.hubParams(), {
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
