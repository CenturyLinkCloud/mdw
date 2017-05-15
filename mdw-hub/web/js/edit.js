'use strict';

var editMod = angular.module('edit', ['ngResource', 'mdw']);

editMod.controller('EditorController', ['$scope', '$cookieStore', '$routeParams', 'mdw', 'util', 'Assets', 'Asset', 'WorkflowCache', 'GitVcs',
                                         function($scope, $cookieStore, $routeParams, mdw, util, Assets, Asset, WorkflowCache, GitVcs) {
  
  $scope.setFullWidth(true);

  $scope.packageName = $routeParams.packageName;
  $scope.assetName = $routeParams.assetName;
  if ($scope.assetName.endsWith('.proc')) {
    $scope.process = {packageName: $scope.packageName, name: $scope.assetName.substring(0, $scope.assetName.length - 5)};
    $scope.onProcessChange = function(proc) {
      var wasDirty = $scope.procDirty;
      $scope.procDirty = true;
      if (!wasDirty) {
        var phase = this.$root.$$phase;
        if (phase !== '$apply' && phase !== '$digest')
          $scope.$digest();
      }
      $scope.process = proc;
    };
  }

  $scope.asset = Assets.get({
      packageName: $routeParams.packageName,
      assetName: $routeParams.assetName
    },
    function(assetData) {
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
        },
        basePath: '/mdw/lib/ace-builds/src-min-noconflict'
      };
      
      $scope.initVersion();
      $scope.initOptions();
      $scope.initGitCredentials();
      
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
  
  $scope.initGitCredentials = function() {
    // for transient values
    $scope.git = {};
    
    // gitCredentials populated from cookie values
    var user = $cookieStore.get('gitUser');
    var password = $cookieStore.get('gitPassword');
    if (user && password) {
      $scope.gitCredentials = {
        user: user,
        password: password
      };
    }
    else {
      $scope.gitCredentials = null;
    }
  };
  
  $scope.isDirty = function() {
    if ($scope.process)
      return $scope.procDirty;
    else
      return $scope.aceDirty;
  };
  
  $scope.setDirty = function(dirty) {
    if ($scope.process)
      $scope.procDirty = dirty;
    else
      $scope.aceDirty = dirty;
  };
  
  $scope.isSaveEnabled = function() {
    if (!$scope.isDirty())
      return false;
    if ($scope.options.commitAndPush) {
      return $scope.version.comment && (!$scope.isShowGitCredentials() || ($scope.git.user && $scope.git.password));
    }
    return true;
  };
  
  $scope.cancelSave = function() {
    $scope.closePopover();
    $scope.initVersion();
    $scope.message = null;
  };
  
  $scope.isShowGitCredentials = function() {
    return $scope.options.commitAndPush && !$scope.gitCredentials;    
  };
  
  $scope.save = function() {
    console.log('saving: ' + $scope.asset.packageName + '/' + $scope.asset.name + ' v' + $scope.version.selected);
    if ($scope.options.commitAndPush && $scope.git.user && $scope.git.password) {
      $cookieStore.put('gitUser', $scope.git.user);
      $cookieStore.put('gitPassword', $scope.git.password);
      $scope.gitCredentials = { user: $scope.git.user, password: $scope.git.password };
      $scope.git = {};
    }
    Asset.put({
      packageName: $scope.asset.packageName,
      assetName: $scope.asset.name,
      version: $scope.version.selected,
      comment: $scope.version.comment,
      distributedSave: $scope.options.distributedSave
    }, 
    $scope.process ? JSON.stringify($scope.process, null, 2) : $scope.asset.content, 
    function success(response) {
      $scope.message = null;
      $scope.aceDirty = false;
      $scope.procDirty = false;
      var metaChange = $scope.asset.version !== $scope.version.selected;
      $scope.asset.version = $scope.version.selected;
      var commitMsg = $scope.version.comment;
      if (metaChange)
        $scope.initVersion();
      
      if ($scope.options.commitAndPush) {
        GitVcs.push({
          pkgPath: $scope.asset.packageName,
          asset: $scope.asset.name,
          gitAction: 'push',
          includeMeta: metaChange
        }, { 
          comment: commitMsg, 
          user: $scope.gitCredentials.user,
          password: $scope.gitCredentials.password
        },
        function success(response) {
          $scope.setPopElem(null);  // in anticipation of having been closed by POST
          if ($scope.options.cacheRefresh)
            $scope.refreshCaches(); // best effort
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
        if ($scope.options.cacheRefresh)
          $scope.refreshCaches(); // best effort
      }
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
  
  $scope.refreshCaches = function() {
    WorkflowCache.refresh({}, { distributed: $scope.options.distributedSave });
  };
  
  $scope.$on('$locationChangeStart', function(event) {
    if ($scope.isDirty()) {
      if (!confirm('Your changes will be lost.\nClick OK to confirm you want to leave this page.'))
        event.preventDefault();
    }
  });  
}]);
