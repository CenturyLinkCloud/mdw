//Copyright (c) 2017 CenturyLink, Inc. All Rights Reserved.
'use strict';

var messageMod = angular.module('jmsMessage', ['ngResource', 'ui.bootstrap', 'mdw']);

messageMod.controller('JmsMessageController', ['$scope', '$location', '$http', 'mdw', 'JmsMessage', 'QUEUE_NAMES',
  											function($scope, $location, $http, mdw, JmsMessage, QUEUE_NAMES) {

  $scope.waitingForResponse = false;
  $scope.jmsHelper = {};
  $scope.jmsHelper.queueNames = [];
  $scope.jmsHelper.queueName = 'com.centurylink.mdw.external.event.queue';
  $scope.jmsHelper.timeOut = 10;
  $scope.jmsHelper.endPoint = "<Internal>";
  $scope.jmsHelper.responseCode = "";
  $scope.jmsHelper.queueNames = QUEUE_NAMES.slice();

  $scope.sendMessage = function() {
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
