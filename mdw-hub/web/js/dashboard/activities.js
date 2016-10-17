// Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
'use strict';

var dashboardActsMod = angular.module('dashboardActivities', ['mdw']);

dashboardActsMod.controller('DashboardActivitiesController', ['$scope', '$http', 'mdw', 'util', 'ACTIVITY_STATUSES', 
                                             function($scope, $http, mdw, util, ACTIVITY_STATUSES) {
  
  $scope.getAllStatuses = function() {
    return ACTIVITY_STATUSES;
  };
  
  $scope.activityBreakdowns = {
      instanceCounts: '/services/Activities/instanceCounts', // returns selected InstanceCounts
      'Stuck Activities': {
        selectField: 'id',
        selectLabel: 'Stuck Activities',
        throughput: '/services/Activities/topThroughput', // returns top asset InstanceCounts
        instancesParam: 'activityIds'  // service parameter for selected instances
      },
      'Status': {
        selectField: 'status',
        selectLabel: 'Statuses',
        throughput: ACTIVITY_STATUSES,
        instancesParam: 'statuses'
      }
  };
}]);