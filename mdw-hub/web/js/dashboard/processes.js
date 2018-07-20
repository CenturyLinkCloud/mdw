'use strict';

var dashboardProcsMod = angular.module('dashboardProcesses', ['mdw']);

dashboardProcsMod.controller('DashboardProcessesController', ['$scope', '$http', '$routeParams', 'mdw', 'util', 'PROCESS_STATUSES', 
                                             function($scope, $http, $routeParams, mdw, util, PROCESS_STATUSES) {
  
  $scope.chartType= "chart chart-"+$routeParams.chart;  
  $scope.processBreakdowns = {
      instanceCounts: '/services/Processes/instanceCounts', // returns selected InstanceCounts
      'Master': {
        selectField: 'id',
        selectLabel: 'Master Processes',
        throughput: '/services/Processes/topThroughput?master=true', // returns top asset InstanceCounts
        instancesParam: 'processIds'  // service parameter for selected instances
      },
      'Process': {
        selectField: 'id',
        selectLabel: 'Processes',
        throughput: '/services/Processes/topThroughput',
        instancesParam: 'processIds'
      },'Completion Time': {
          selectField: 'id',
          selectLabel: 'Processes',
          throughput: '/services/Processes/topThroughput?completionTime=true',
          instancesParam: 'completionTime=true&processIds'
        },
      'Status': {
        selectField: 'status',
        selectLabel: 'Statuses',
        throughput: PROCESS_STATUSES,
        instancesParam: 'statuses'
      }
  };
  
}]);