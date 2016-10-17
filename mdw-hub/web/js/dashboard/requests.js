// Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
'use strict';

var dashboardReqsMod = angular.module('dashboardRequests', ['mdw']);

dashboardReqsMod.controller('DashboardRequestsController', ['$scope', '$http', 'mdw', 'util', 
                                             function($scope, $http, mdw, util) {
  
  $scope.requestBreakdowns = {
      instanceCounts: '/services/Requests/instanceCounts', // returns selected InstanceCounts
      'Direction': {
        selectField: 'type',
        selectLabel: 'Request Type',
        throughput: [ 'Inbound Requests', 'Outbound Requests'], // returns top asset InstanceCounts
        instancesParam: 'requests'  // service parameter for selected instances
      }
  };
}]);