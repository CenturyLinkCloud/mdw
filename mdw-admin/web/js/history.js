// Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
'use strict';

var historyMod = angular.module('history', ['ngResource','ngTouch', 'mdw', 'ui.grid', 'ui.grid.pagination', 'ui.grid.resizeColumns', 'ui.grid.moveColumns', 'ui.grid.selection', 'ui.grid.exporter']);

historyMod.controller('HistoryController', ['$scope', 'History', 'mdw', '$http', function($scope, History, mdw, $http) {
  $scope.gridOptions = {
      paginationPageSizes: [25, 50, 75, 100],
      paginationPageSize: 25, 
      enableGridMenu: true,
      enableSelectAll: true,
      exporterCsvFilename: 'mdw-history-' + $scope.$parent.authUser.id + '.csv',
      enableFiltering: true,
      exporterCsvLinkElement: angular.element(document.querySelectorAll(".custom-csv-link-location"))
  };
  $scope.gridOptions.columnDefs = [
                                   {name:'Id', field: 'id', width: '10%', enableColumnResizing: false, visible: false},
                                   {name: 'Action', field:'name', width: '10%'},
                                   {name: 'Source', field:'source', width: '20%'},
                                   {name: 'Entity Name', field:'EntityName', width: '10%'},
                                   {name:'user', width: '10%'},
                                   {name: 'description', width: '30%', minWidth: '30'},
                                   {name: 'date', field:'date', width: '15%'}
                                 ];
  $scope.historyLength = 300;
  $scope.historyList = History.get(function(data) {
    $scope.gridOptions.data =  data.allHistory;
  });
  
  $scope.reloadHistory = function(){
      var url = mdw.roots.services + '/Services/History?app=mdw-admin&historyLength=' + $scope.historyLength;
      $http.get(url).success(function(data) {
        $scope.gridOptions.data =  data.allHistory;
      });
  };
}]);

historyMod.factory('History', ['$resource', 'mdw', function( $resource, mdw) {
  return $resource(mdw.roots.services + '/Services/History?historyLength=300', mdw.serviceParams(), {
    get: {method: 'GET', isArray: false }
  });
}]);
