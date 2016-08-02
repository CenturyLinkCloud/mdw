// Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
'use strict';

var dashboardTasksMod = angular.module('dashboardTasks', ['mdw']);

dashboardTasksMod.controller('DashboardTasksController', ['$scope', '$http', 'mdw', 'util', 'TASK_STATUSES', 
                                             function($scope, $http, mdw, util, TASK_STATUSES) {
  
  $scope.taskBreakdowns = {
      instanceCounts: '/services/Tasks/instanceCounts', // returns selected InstanceCounts
      'Task': {
        selectField: 'id',
        selectLabel: 'Tasks',
        throughput: '/services/Tasks/topThroughput/task', // returns top asset InstanceCounts
        instancesParam: 'taskIds'  // service parameter for selected instances
      },
      'Workgroup': {
        selectField: 'workgroup',
        selectLabel: 'Workgroups',
        throughput: '/services/Tasks/topThroughput/workgroup',
        instancesParam: 'workgroups'
      },
      'Assignee': {
        selectField: 'user',
        selectLabel: 'Users',
        throughput: '/services/Tasks/topThroughput/assignee',
        instancesParam: 'assignees'
      },
      'Status': {
        selectField: 'status',
        selectLabel: 'Statuses',
        throughput: TASK_STATUSES,
        instancesParam: 'statuses'
      }
  };
  
}]);