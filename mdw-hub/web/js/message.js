// Copyright (c) 2017 CenturyLink, Inc. All Rights Reserved.
'use strict';

var messageMod = angular.module('message', ['ngResource', 'ui.bootstrap', 'mdw']);

messageMod.controller('MessageController', ['$scope', '$location', '$http', 'mdw', 'Message', 'HTTP_METHODS',
                                           function($scope, $location, $http, mdw, Message, HTTP_METHODS) {
  
  $scope.waitingForResponse = false;
  $scope.httpHelper = {};
  $scope.httpHelper.httpMethods = [];
  $scope.httpHelper.httpMethod = 'POST';
  $scope.httpHelper.timeOut = 15000;
  $scope.httpHelper.url = mdw.roots.services + "/Services/REST";
  $scope.httpHelper.responseCode = "";
  
  
  $scope.httpHelper.httpMethods = HTTP_METHODS.slice();
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

      };
   function handleError(error) {
        $scope.httpHelper.response = error.statusText;
        $scope.httpHelper.responseCode = error.status;
        $scope.waitingForResponse = false;
      };
  };
}]);
 
messageMod.factory('Message', ['$resource', 'mdw', function($resource, mdw) {
  return angular.extend({}, $resource(mdw.roots.services +'/Services/HttpHelper', mdw.serviceParams(), {
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
