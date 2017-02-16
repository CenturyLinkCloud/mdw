//Copyright (c) 2017 CenturyLink, Inc. All Rights Reserved.
'use strict';

var messageMod = angular.module('message', ['ngResource', 'ui.bootstrap', 'mdw']);

messageMod.controller('MessageController', ['$scope', '$location', '$http', 'mdw', 'util', 'Message', 'HTTP_METHODS',
  											function($scope, $location, $http, mdw, util, Message, HTTP_METHODS) {

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

 
  $scope.getMethod = function(selFieldValue) {
    if ($scope.httpHelper.httpMethods) {
      for (var i = 0; i < $scope.httpHelper.httpMethods.length; i++) {
        var method = $scope.httpHelper.httpMethods[i];
        if (method === selFieldValue)
          return method;
      }
    }
  };
  
  $scope.setMethod = function(method) {
    $scope.httpHelper.httpMethod = method;
  };
  
  $scope.sendMessage = function() {
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
      Message.create(Message.shallowCopy({}, $scope.httpHelper, $scope.authUser.cuid), handleSuccess, handleError);
      break;
    case 'GET':
      Message.query(Message.shallowCopy({}, $scope.httpHelper, $scope.authUser.cuid), handleSuccess, handleError);
      break;
    case 'PUT':
      Message.update(Message.shallowCopy({}, $scope.httpHelper, $scope.authUser.cuid), handleSuccess, handleError);
      break;
    case 'DELETE':
      Message.remove(Message.shallowCopy({}, $scope.httpHelper, $scope.authUser.cuid), handleSuccess, handleError);
      break;
    case 'PATCH':
      Message.patch(Message.shallowCopy({}, $scope.httpHelper, $scope.authUser.cuid), handleSuccess, handleError);
      break;
    }
    function handleSuccess(data) {
      if (data.statusCode !== 0) {
        $scope.httpHelper.response = data.response;
        $scope.httpHelper.responseCode = data.statusCode;
      }
      else {
        $scope.waitingForResponse = true;
        $location.path('/Services/Message');
      }
      $scope.waitingForResponse = false;
    }
    function handleError(error) {
      $scope.httpHelper.response = error.statusText;
      $scope.httpHelper.responseCode = error.status;
      $scope.waitingForResponse = false;
    }
  };
}]);

messageMod.factory('Message', ['$resource', 'mdw', function($resource, mdw) {
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
