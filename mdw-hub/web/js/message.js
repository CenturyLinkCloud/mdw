// Copyright (c) 2017 CenturyLink, Inc. All Rights Reserved.
'use strict';

var messageMod = angular.module('message', ['ngResource', 'mdw']);

messageMod.controller('MessageController', ['$scope', '$location', '$http', 'mdw', 'Message',
                                           function($scope, $location, $http, mdw, Message) {
  
  $scope.waitingForResponse = false;
  $scope.httpHelper = {};
  $scope.httpHelper.timeOut = 15000;
  $scope.httpHelper.url = mdw.roots.services + "/Services/REST";
  
  $scope.sendMessage = function() {
    console.log('sending message: ' + $scope.httpHelper.requestMessage);
    Message.send(Message.shallowCopy({}, $scope.httpHelper, $scope.authUser.cuid),
      function(data) {
        if (data.statusCode !== 0) {
          $scope.httpHelper.response = data.response;
        }
        else {
          $scope.waitingForResponse = true;
          $location.path('/Services/Message');
        }
      }, 
      function(error) {
        $scope.httpHelper.response = error.response;
      });
    $scope.waitingForResponse = false;
  };
  $scope.waitingForResponse = false;
}]);
 

messageMod.factory('Message', ['$resource', 'mdw', function($resource, mdw) {
  return angular.extend({}, $resource(mdw.roots.services +'/Services/HttpHelper', mdw.serviceParams(), {
    send: { method: 'POST'}
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

