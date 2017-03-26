'use strict';

var servicesMod = angular.module('services', ['ngResource', 'mdw', 'assets']);

servicesMod.controller('ServicesController', ['$scope', 'mdw', 'ServiceApis', 
                                             function($scope, mdw, ServiceApis) {

  var swaggerDef = ServiceApis.get({}, function success() {
    $scope.serviceApis = {}; // path-to-api object

    var paths = swaggerDef.paths;
    Object.getOwnPropertyNames(paths).forEach(function(pathName) {
      var barePath = pathName;
      var slashCurly = pathName.indexOf('/{');
      if (slashCurly > 0)
        barePath = pathName.substring(0, slashCurly);
      barePath = '/' + barePath.substring(1).replace(/\//g, '.');
      
      var serviceApi = $scope.serviceApis[barePath];
      if (!serviceApi)
        serviceApi = {};
      serviceApi.label = barePath.replace(/\./g, '/');
      $scope.serviceApis[barePath] = serviceApi;
      
      var pathVal = paths[pathName];
      Object.getOwnPropertyNames(pathVal).forEach(function(methodName) {
        serviceApi[methodName] = pathVal[methodName];
        var tags = serviceApi[methodName].tags;
        if (tags && tags[0])
          serviceApi.description = tags[0];
      });
    });
  });
  
}]);

servicesMod.controller('ServiceController', ['$scope', '$routeParams', '$sce', '$window', 'mdw', 'ServiceApis', 'Assets', 'Asset', 
                                              function($scope, $routeParams, $sce, $window, mdw, ServiceApis, Assets, Asset) {

  $scope.serviceFullPath = $routeParams.servicePath;
  $scope.serviceApi = ServiceApis.get({servicePath: $routeParams.servicePath, ext: '.json'}, function success(serviceDef) {
    $scope.serviceApi.servicePath = $routeParams.servicePath; // service path is logical path (with dots separating subpaths)
    $scope.serviceApi.apiPath = $routeParams.servicePath.replace(/\./g, '/'); // api path is actual service path
    $scope.serviceApi.jsonContent = serviceDef.raw;
    ServiceApis.get({servicePath: $routeParams.servicePath, ext: '.yaml' }, function(serviceDef) {
      $scope.serviceApi.yamlContent = serviceDef.raw;
      // hack the swagger-editor local storage cache to avoid retrieving twice
      var swaggerEditorCache = $window.localStorage['ngStorage-SwaggerEditorCache'];
      if (!swaggerEditorCache)
        swaggerEditorCache = '{}';
      var cacheObj = JSON.parse(swaggerEditorCache);
      cacheObj.yaml = $scope.serviceApi.yamlContent;
      $window.localStorage['ngStorage-SwaggerEditorCache'] = JSON.stringify(cacheObj);
      var swaggerEditorUrl = 'swagger/swagger-editor/#?servicePath=' + $scope.serviceFullPath;
      document.getElementById('swaggerFrame').src = swaggerEditorUrl;
      
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
  return $resource(mdw.roots.hub + '/api/:servicePath:ext', {}, {
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
        serviceDef.apiBase = mdw.roots.hub + '/api';
        serviceDef.raw = data;
        return serviceDef;
      }
    }
  });
}]);

servicesMod.directive('inFrame', function() {
  return {
    restrict: 'EA',
    scope: {
      clazz: '@class',
      src: '@src',
      api: '@api'
    },
    template: '<iframe id="swaggerFrame" class="{{clazz}}" src="{{src}}" name="{{api}}"></iframe>',
    link: function(scope, elem, attrs) {
      elem.ready(function() {
        var frame = elem.find('iframe')[0];
        iFrameResize({heightCalculationMethod:'lowestElement'}, frame);
      });
    }
  };
});