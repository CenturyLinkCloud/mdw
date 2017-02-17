//Copyright (c) 2017 CenturyLink, Inc. All Rights Reserved.
'use strict';

var messageMod = angular.module('httpMessage', ['ngResource', 'ui.bootstrap', 'mdw']);

messageMod.controller('HttpMessageController', ['$scope', '$location', '$http', 'mdw', 'util', 'HttpMessage', 'HTTP_METHODS',
  											function($scope, $location, $http, mdw, util, HttpMessage, HTTP_METHODS) {

  $scope.waitingForResponse = false;
  $scope.httpHelper = {};
  $scope.httpHelper.httpMethods = [];
  $scope.httpHelper.httpMethod = 'POST';
  $scope.httpHelper.timeOut = 15000;
  $scope.httpHelper.url = "http://localhost:8080/mdw/services";
  $scope.httpHelper.responseCode = "";
  $scope.httpHelper.httpMethods = HTTP_METHODS.slice();

  var mdwProps = util.getMdwProperties();
  if (mdwProps) {
    $scope.httpHelper.url = mdwProps["mdw.services.url"] + "/services";
  }
  else {
    util.loadMdwProperties().then(function(response) {
      mdwProps = util.getMdwProperties();
      $scope.httpHelper.url = mdwProps["mdw.services.url"] + "/services";
    });
  }

  $scope.sendMessage = function() {
    $scope.waitingForResponse = true;

    if ($scope.httpHelper.headers) {
      var headersNameValue = $scope.httpHelper.headers.split(',');
      if (headersNameValue) {
        for (var i = 0; i < headersNameValue.length; i++) {
          var token = headersNameValue[i];
          if (token){
            var pair = token.split('=');
            $http.defaults.headers.common[pair[0]] = pair[1];
          }
        }
      }
    }
    console.log('sending message: ' + $scope.httpHelper.requestMessage + 'Method' + $scope.httpHelper.httpMethod);
    switch ($scope.httpHelper.httpMethod) {
    case 'POST':
      HttpMessage.create(HttpMessage.shallowCopy({}, $scope.httpHelper, $scope.authUser.cuid), handleSuccess, handleError);
      break;
    case 'GET':
      HttpMessage.query(HttpMessage.shallowCopy({}, $scope.httpHelper, $scope.authUser.cuid), handleSuccess, handleError);
      break;
    case 'PUT':
      HttpMessage.update(HttpMessage.shallowCopy({}, $scope.httpHelper, $scope.authUser.cuid), handleSuccess, handleError);
      break;
    case 'DELETE':
      HttpMessage.remove(HttpMessage.shallowCopy({}, $scope.httpHelper, $scope.authUser.cuid), handleSuccess, handleError);
      break;
    case 'PATCH':
      HttpMessage.patch(HttpMessage.shallowCopy({}, $scope.httpHelper, $scope.authUser.cuid), handleSuccess, handleError);
      break;
    }
    function handleSuccess(data) {
      $scope.httpHelper.response = data.response;
      $scope.httpHelper.responseCode = data.statusCode;
    }
    function handleError(error) {
      $scope.httpHelper.response = error.statusText;
      $scope.httpHelper.responseCode = error.status;
    }
    $scope.waitingForResponse = false;

  };
}]);

messageMod.factory('HttpMessage', ['$resource', 'mdw', function($resource, mdw) {
  return angular.extend({}, $resource(mdw.roots.services +'/Services/HttpMessages', mdw.serviceParams(), {
    query: { method: 'GET', isArray: false }, 
    create: { method: 'POST'},
    update: { method: 'PUT' },
    remove: { method: 'DELETE' },
    patch: { method: 'PATCH' }
  }), {
    shallowCopy: function(destMessage, srcMessage, user) {
      destMessage.timeOut = srcMessage.timeOut;
      destMessage.url = srcMessage.url;
      destMessage.requestMessage = srcMessage.requestMessage;
      destMessage.headers = srcMessage.headers;
      destMessage.user = user;
      return destMessage;
    }
  });
}]);
