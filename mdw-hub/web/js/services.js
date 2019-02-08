'use strict';

var servicesMod = angular.module('services', ['ngResource', 'mdw', 'assets']);

servicesMod.controller('ServicesController', ['$scope', 'mdw', 'ServiceApis', 'Assets',
                                             function($scope, mdw, ServiceApis, Assets) {

  var pkgList = Assets.get({}, function success() {
    var pkgPaths = pkgList.packages.map(function(pkg) {
      return '/' + pkg.name.replace(/\./g, '/');
    });

    var swaggerDef = ServiceApis.get({}, function success() {
      $scope.serviceApis = {}; // path-to-api object
      var paths = swaggerDef.paths;
      Object.keys(paths).forEach(function(path) {
        var rootPath = path;
        var slash = path.indexOf('/', 1);
        if (slash > 0) {
          rootPath = path.substring(0, slash);
        }
        // if path corresponds to pkg(s), root path is longest matching pkg path
        for (let i = 0; i < pkgPaths.length; i++) {
          let pkgPath = pkgPaths[i];
          if (path.startsWith(pkgPath)) {
            slash = path.indexOf('/', pkgPath.length + 1);
            if (slash > 0)
              rootPath = pkgPath + path.substring(pkgPath.length, slash);
            else
              rootPath = pkgPath + path.substring(pkgPath.length);
          }
        }

        // serviceApis = one per root path
        var servicePath = '/' + rootPath.substring(1).replace(/\//g, '.');
        var serviceApi = $scope.serviceApis[servicePath];
        if (!serviceApi) {
          serviceApi = {};
          serviceApi.label = rootPath;
          $scope.serviceApis[servicePath] = serviceApi;
        }

        if (!serviceApi.description) {
          var pathVal = paths[path];
          Object.keys(pathVal).forEach(function(methodName) {
            serviceApi[methodName] = pathVal[methodName];
            var tags = serviceApi[methodName].tags;
            if (tags && tags[0])
              serviceApi.description = tags[0];
          });
        }
      });
    });
  });
}]);

servicesMod.controller('ServiceController', ['$scope', '$routeParams', '$sce', '$window', 'mdw', 'ServiceApis', 'Assets', 'Asset', 
                                              function($scope, $routeParams, $sce, $window, mdw, ServiceApis, Assets, Asset) {

  // api path is actual service path
  $scope.apiUrl = 'api-docs/' + $routeParams.servicePath + '.json';
  $scope.serviceApi = ServiceApis.get({servicePath: $routeParams.servicePath, ext: '.json'}, function success(serviceDef) {
    $scope.serviceApi.servicePath = $routeParams.servicePath; // service path is logical path (with dots separating subpaths)
    $scope.serviceApi.apiPath = $routeParams.servicePath.replace(/\./g, '/'); // api path is actual service path
    $scope.serviceApi.jsonContent = serviceDef.raw;
    ServiceApis.get({servicePath: $routeParams.servicePath, ext: '.yaml' }, function(serviceDef) {
      $scope.serviceApi.yamlContent = serviceDef.raw;
      
      // populate the serviceApi sample assets
      $scope.serviceApi.samplePackageName = null;
      $scope.serviceApi.sampleAssets = [];
      Assets.get({}, function(pkgList) {
        var pkgs = pkgList.packages;
        pkgs.forEach(function(pkg) {
          if (pkg.name.endsWith('api.samples')) {
            if (pkg.name == 'com.centurylink.mdw.services.api.samples' || 
                  $scope.serviceApi.servicePath.startsWith(pkg.name.substring(0, pkg.name.length - 12))) {
              console.log('finding api samples in: ' + pkg.name);
              Assets.get({packageName: pkg.name},
                function(pkgData) {
                  for (var i = 0; i < pkgData.assets.length; i++) {
                    var asset = pkgData.assets[i];
                    var svcPathRoot = $scope.serviceApi.servicePath + '_';
                    if (asset.name.startsWith(svcPathRoot)) {
                      console.log('  ' + asset.name);
                      $scope.serviceApi.samplePackageName = pkg.name;
                      $scope.serviceApi.sampleAssets.push(asset);
                    }
                  }
                });
            }
          }
        });
      });
    });
  });
  
  $scope.selectSample = function(sampleAssetName) {
    if (sampleAssetName) {
      $scope.serviceApi.selectedSampleName = sampleAssetName;
      var lastDot = sampleAssetName.lastIndexOf('.');
      if (lastDot > 0 && lastDot < sampleAssetName.length - 2) {
        $scope.serviceApi.selectedSampleLang = sampleAssetName.substring(lastDot + 1);
        if ($scope.serviceApi.selectedSampleLang === 'json')
          $scope.serviceApi.selectedSampleLang = 'js'; // allow comments
      }
      
      $scope.serviceApi.selectedSample = Asset.get({
        packageName: $scope.serviceApi.samplePackageName,
        assetName: sampleAssetName
      });
    }
    else {
      $scope.serviceApi.selectedSample = null;
    }    
  };  
}]);

servicesMod.controller('CombinedServiceController', ['$scope', '$routeParams', '$sce', 'mdw', 'ServiceApis', 
                                                      function($scope, $routeParams, $sce, mdw, ServiceApis) {
 
 $scope.serviceApi = ServiceApis.get({servicePath: 'swagger', ext: '.json'}, function success(serviceDef) {
   
   var path = 'swagger';
   $scope.serviceApi.servicePath = 'swagger';
   $scope.serviceApi.jsonContent = serviceDef.raw;
   ServiceApis.get({servicePath: path, ext: '.yaml' }, function(serviceDef) {
       $scope.serviceApi.yamlContent = serviceDef.raw;
     });
 });
}]);

servicesMod.factory('ServiceApis', ['$resource', 'mdw', function($resource, mdw) {
  return $resource(mdw.roots.hub + '/api-docs/:servicePath:ext', {}, {
    get: {
      method: 'GET',
      transformResponse: function(data, headers) {
        var contentType = headers()['content-type'];
        var serviceDef = {};
        if (contentType && contentType.startsWith("application/json")) {
          serviceDef = angular.fromJson(data);
          serviceDef.format = 'json';
        }
        else if (contentType && contentType.startsWith("text/yaml")) {
          serviceDef.format = 'yaml';
        }
        serviceDef.apiBase = mdw.roots.hub + '/api-docs';
        serviceDef.raw = data;
        return serviceDef;
      }
    }
  });
}]);