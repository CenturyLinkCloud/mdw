// Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
'use strict';

var processMod = angular.module('requests', ['mdw']);

processMod.controller('RequestsController', ['$scope', '$location', '$http', 'mdw', 'util', 'EXCEL_DOWNLOAD',
                                             function($scope, $location, $http, mdw, util, EXCEL_DOWNLOAD) {
  
  // TODO: hardcoded
  var d = new Date();
  d.setTime(d.getTime() - 7 * util.dayMs);
  $scope.startDate = util.serviceDate(d);
  $scope.start = 0;
  $scope.max = 50;
  
  $scope.requestTypes = {
      masterRequests: 'Master Requests', 
      inboundRequests: 'Inbound Requests', 
      outboundRequests: 'Outbound Requests'
  };
  
  $scope.requestType = 'masterRequests';
  $scope.setRequestType = function(requestType) {
    $scope.requestType = requestType;
    $scope.retrieve();
  };
  
  $scope.retrieve = function() {
    $scope.total = 0;
    $scope.requests = [];
    $scope.url = mdw.roots.services + '/services/Requests?app=mdw-admin' + '&type=' + $scope.requestType + 
        '&start=' + $scope.start + '&max=' + $scope.max + '&startDate=' + $scope.startDate;
    $http.get($scope.url).error(function(data, status) {
      console.log('HTTP ' + status + ': ' + $scope.url);
    }).success(function(data, status, headers, config) {
      $scope.total = data.total;
      $scope.requests = data.requests;
    });
  };
  
  $scope.downloadExcel = function() {
    window.location = $scope.url + '&' + EXCEL_DOWNLOAD;
  };
  
  $scope.retrieve();

  
}]);