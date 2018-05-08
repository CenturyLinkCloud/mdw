'use strict';

var listMod = angular.module('mdwList', ['mdw']);

listMod.controller('MdwListController', ['$scope', '$http', '$location', 'mdw', 'util', 'EXCEL_DOWNLOAD',
                                             function($scope, $http, $location, mdw, util, EXCEL_DOWNLOAD) {
  $scope.init = function() {
    
    $scope.$watch('listFilter', function(aft, bef) {
      $scope.items = [];
      $scope.listModel[$scope.listItems] = $scope.items;
      $scope.listModel.count = 0;
      $scope.listModel.total = 0;
      $scope.listModel.selected = null;
      $scope.getNextPage();
    }, true);
    
  };

  // TODO hardcoded
  $scope.max = 50;
  $scope.scrollBuffer = 5; // getting this close forces retrieval
  $scope.maxDownloadItems = 10000;
  
  $scope.items = [];
  $scope.busy = false;
  
  $scope.getNextPage = function(callback) {
    if (!$scope.busy) {
      $scope.busy = true;
      
      // retrieve the item list
      $scope.url = mdw.roots.services + $scope.serviceUrl + '?app=mdw-admin' + '&start=' + $scope.items.length + '&max=' + $scope.max;
      var urlParams = util.urlParams();
      if (util.isEmpty(urlParams)) {
        $scope.url += $scope.getFilterQuery();
      }
      else {
        util.getProperties(urlParams).forEach(function(key) {
          $scope.url += '&' + key + '=' + urlParams[key];
        });
      }
      
      $http.get($scope.url).error(function(data, status) {
        console.log('HTTP ' + status + ': ' + $scope.url);
      }).success(function(data, status, headers, config) {
        $scope.$emit('page-retrieved', data);
        $scope.listModel = data;
        var newItems = data[$scope.listItems];
        $scope.items = $scope.items.concat(newItems);
        $scope.listModel[$scope.listItems] = $scope.items;
        
        $scope.listModel.reload = function(callback) {
          $scope.items = [];
          $scope.getNextPage(callback);
        };
        
        if ($scope.styleClass == 'mdw-checklist') {
          $scope.listModel.selectedState = { all: false };
          $scope.listModel.toggleAll = function() {
            $scope.items.forEach(function(item) {
              item.selected = $scope.listModel.selectedState.all;
            });
          };
          $scope.listModel.notAllSelected = function() {
            $scope.listModel.selectedState.all = false;
          };
          $scope.listModel.getSelectedItems = function() {
            var selected = [];
            if ($scope.items) {
              $scope.items.forEach(function(item) {
                if (item.selected)
                  selected.push(item);
              });
            }
            return selected;
          };
        }
        
        $scope.listModel.downloadExcel = function() {
          if ($scope.listModel.total > $scope.maxDownloadItems) {
            window.alert('Cannot download more than ' + $scope.maxDownloadItems + ' items.  Please narrow results by filtering.');
          }
          else {
            var downloadUrl = mdw.roots.services + $scope.serviceUrl + '?app=mdw-admin&start=0&max=-1';
            downloadUrl += $scope.getFilterQuery() + '&' + EXCEL_DOWNLOAD;
            window.location = downloadUrl;
          }
        };    
        $scope.busy = false;

        if (callback) {
          callback($scope.listModel);
        }
      });
    }
  };
  
  $scope.getFilterQuery = function() {
    var query = '';
    for (var key in $scope.listFilter) {
      if ($scope.listFilter.hasOwnProperty(key)) {
        var val = $scope.listFilter[key];
        if (val !== null && typeof(val) != 'undefined') {
          if (val instanceof Date) 
            val = util.serviceDate(val);
          else if (typeof(val) === 'string')
            val = val.replace(/\[/g, "%5B").replace(/]/g, "%5D");
          query += '&' + key + '=' + val;
        }
      }
    }
    return query;
  };
  
  $scope.hasMore = function() {
    return $scope.items.length === 0 || $scope.items.length < $scope.listModel.total;
  };
  
}]);

listMod.directive('mdwList', function() {
  return {
    restrict: 'E',
    templateUrl: 'ui/list.html',
    scope: {
      styleClass: '@class',
      serviceUrl: '@mdwListService',
      listModel: '=mdwListModel',
      listFilter: '=mdwListFilter',
      listItems: '@mdwListItems',
      itemTemplate: '@mdwListItem'
    },
    controller: 'MdwListController',
    controllerAs: 'mdwList',
    link: function link(scope, elem, attrs, ctrls) {
      scope.init();
    }
  };
});