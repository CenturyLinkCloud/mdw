//Copyright (c) 2017 CenturyLink, Inc. All Rights Reserved.
'use strict';

var messageMod = angular.module('message', ['ngResource', 'ui.bootstrap', 'mdw']);

messageMod.controller('MessageController', ['$scope', '$location', '$http', 'mdw', 'util', 'HttpMessage', 'JmsMessage', 'HTTP_METHODS', 'QUEUE_NAMES',
  											function($scope, $location, $http, mdw, util, HttpMessage, JmsMessage, HTTP_METHODS, QUEUE_NAMES) {
  

  $scope.waitingForResponse = false;
  $scope.httpHelper = {};
  $scope.httpHelper.httpMethods = [];
  $scope.httpHelper.httpMethod = 'POST';
  $scope.httpHelper.timeOut = 15000;
  $scope.httpHelper.url = "http://localhost:8080/mdw/services";
  $scope.httpHelper.responseCode = "";
  $scope.httpHelper.httpMethods = HTTP_METHODS.slice();
  
  $scope.jmsHelper = {};
  $scope.jmsHelper.queueNames = [];
  $scope.jmsHelper.queueName = 'com.centurylink.mdw.external.event.queue';
  $scope.jmsHelper.timeOut = 10;
  $scope.jmsHelper.endPoint = "<Internal>";
  $scope.jmsHelper.responseCode = "";
  $scope.jmsHelper.queueNames = QUEUE_NAMES.slice();

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

  $scope.sendHttpMessage = function() {
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


$scope.sendJmsMessage = function() {
  console.log('sending message: ' + $scope.jmsHelper.requestMessage + 'Queue Name' + $scope.jmsHelper.queueName);
  $scope.waitingForResponse = true;
  JmsMessage.create(JmsMessage.shallowCopy({}, $scope.jmsHelper, $scope.authUser.cuid), handleSuccess, handleError);

function handleSuccess(data) {
  $scope.jmsHelper.response = data.response;
  $scope.jmsHelper.responseCode = data.statusCode;
}
function handleError(error) {
  $scope.jmsHelper.response = error.statusText;
  $scope.jmsHelper.responseCode = error.status;
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

messageMod.factory('JmsMessage', ['$resource', 'mdw', function($resource, mdw) {
  return angular.extend({}, $resource(mdw.roots.services +'/Services/JmsMessages', mdw.serviceParams(), {
    create: { method: 'POST'}
  }), {
    shallowCopy: function(destMessage, srcMessage, user) {
      destMessage.timeOut = srcMessage.timeOut;
      destMessage.endPoint = srcMessage.endPoint;
      destMessage.requestMessage = srcMessage.requestMessage;
      destMessage.queueName = srcMessage.queueName;
      destMessage.user = user;
      return destMessage;
    }
  });
}]);
