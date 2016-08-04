// Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
'use strict';

var chartMod = angular.module('mdwChart', ['mdw']);

chartMod.controller('MdwChartController', ['$scope', '$http', '$location', 'mdw', 'util', 'EXCEL_DOWNLOAD', 
                                             function($scope, $http, $location, mdw, util, EXCEL_DOWNLOAD) {
  $scope.init = function() {

    $scope.spans = ['Week', 'Month']; // TODO hardcoded
    $scope.span = 'Week';
    $scope.days = 7;

    // TODO hardcoded
    $scope.startDate = new Date();
    // $scope.startDate.setTime($scope.startDate )
    
    // TODO hardcoded
    $scope.initialSelect = 5;
    
    // TODO: hardcoded
    $scope.max = 50;
    
    // TODO: hardcoded
    $scope.showTotal = true;

    $scope.tops = []; // (sorted by most instances)
    $scope.selected = [];
    $scope.breakdowns = [];
    var bd = 0;
    for (var prop in $scope.breakdownConfig) {
      if ($scope.breakdownConfig.hasOwnProperty(prop) && typeof $scope.breakdownConfig[prop] === 'object') {
        $scope.breakdowns[bd] = prop;
        bd++;
      }
    }
    if ($scope.showTotal)
      $scope.breakdowns.push('Total');
    $scope.setBreakdown($scope.breakdowns[0]);
  };
  
  $scope.chartOptions = {
    legendCallback: function(chart) {
      var text = [];
      text.push('<ul class="mdw-chart-legend">');
      for (var i = 0; i < chart.data.datasets.length; i++) {
        text.push('  <li>');
        text.push('    <span class="mdw-chart-legend-icon" style="background-color:' + chart.data.datasets[i].backgroundColor + ';' + 
            'border-color:' + chart.data.datasets[i].borderColor + '"></span>');
        var selFieldValue = chart.data.datasets[i].label;
        var top = $scope.getTop(selFieldValue);
        var label;
        var title;
        if (top) {
          label = $scope.getLabel(top, true);
          title = $scope.getTitle(top);
        }
        if (label) {
          if (title)
            text.push('    <span class="mdw-chart-legend-text" title="' + title + '">' + label + '</span>');
          else
            text.push('    <span class="mdw-chart-legend-text">' + label + '</span>');
        }
        else {
          text.push('    <span class="mdw-chart-legend-text">' + selFieldValue + '</span>');
        }
        text.push('  </li>');
      }
      text.push('</ul>');

      return text.join('\n  ');
    }
  };
  
  $scope.$on('chart-create', function (event, chart) {
    $scope.chartLegend = chart.generateLegend();
  });
  
  $scope.setSelected = function(sel) {
    $scope.selected = sel;
  };
  $scope.backupSelected = [];  // in case selection canceled
  $scope.setBackupSelected = function(backupSel) {
    $scope.backupSelected = backupSel;
  };
  
  // select popovers
  $scope.selectPop = null;
  $scope.setSelectPop = function(selPop) {
    $scope.selectPop = selPop;
  };
  $scope.closeSelectPop = function() {
    if ($scope.selectPop) {
      $scope.selectPop[0].click();
      $scope.selectPop = null;
    }
  };  

  $scope.select = function(sel) {
    if (!$scope.isSelected(sel))
      $scope.selected.push(sel);
  };
  $scope.deselect = function(sel) {
    var idx = $scope.selected.indexOf(sel);
    if (idx >= 0)
      $scope.selected.splice(idx, 1);
  };
  $scope.isSelected = function(sel) {
    return $scope.selected.includes(sel);
  };
  $scope.applySelect = function() {
    $scope.backupSelected = null;
    $scope.closeSelectPop();
    $scope.updateData();
  };
  $scope.cancelSelect = function() {
    $scope.selected = $scope.backupSelected;
    $scope.backupSelected = null;
    $scope.closeSelectPop();
  };
  
  $scope.clearRange = function() {
    $scope.dates = []; // displayed dates
    $scope.labels = [];
  };
  $scope.clearData = function() {
    $scope.series = [];
    $scope.data = [];
    $scope.total = 0;
  };
  $scope.updateDates = function() {
    // TODO: hardwired start based on span ending today
    var d = new Date();
    for (var h = 0; h < $scope.days; h++) {
      $scope.labels.unshift(util.monthAndDay(d));
      $scope.dates.unshift(util.serviceDate(d));
      if (h < $scope.days - 1)
        d.setTime(d.getTime() - util.dayMs);
    }  
    $scope.start = util.serviceDate(d);
  };
  
  $scope.updateRange = function() {
    $scope.clearData();  // prevent attempt to plot before updateData() call
    $scope.clearRange();
    $scope.updateDates();
    
    var breakdown = $scope.getBreakdown();
    if (breakdown && !Array.isArray(breakdown.throughput)) {
      var url = mdw.roots.services + breakdown.throughput;
      if (breakdown.throughput.indexOf('?') >= 0)
        url += '&';
      else
        url += '?';
      url += 'app=mdw-admin&max=' + $scope.max + '&startDate=' + $scope.start;
    
      $http.get(url).error(function(data, status) {
        console.log('HTTP ' + status + ': ' + url);
      }).success(function(data, status, headers, config) {
        $scope.tops = data;
        if ($scope.selected.length === 0) {
          // initialize to top 5
          for (var i = 0; i < $scope.tops.length; i++) {
            var val = $scope.tops[i][$scope.selField];
            if (val && $scope.selected.length < $scope.initialSelect)
              $scope.selected.push(val);
          }
        }
        $scope.updateData();
      });
    }
    else {
      // just retrieve totals
      $scope.tops = [];
      if (breakdown) {
        // hard-coded array (eg: statuses)
        $scope.selected = breakdown.throughput.slice();
        breakdown.throughput.forEach(function(tp) {
          var top = {};
          top[$scope.selField] = tp;
          $scope.tops.push(top);
        });
      }
      else {
        $scope.selected = [];
      }
      $scope.updateData();
    }
  };
  
  $scope.updateData = function() {
    
    // based on selected
    $scope.clearData();
    
    // retrieve breakdown
    var breakdown = $scope.getBreakdown();
    $scope.dataUrl = mdw.roots.services + $scope.breakdownConfig.instanceCounts;
    if ($scope.breakdownConfig.instanceCounts.indexOf('?') >= 0)
      $scope.dataUrl += '&';
    else
      $scope.dataUrl += '?';
    $scope.dataUrl += 'app=mdw-admin&startDate=' + $scope.start;
    if (breakdown && breakdown.instancesParam) 
        $scope.dataUrl += '&' + breakdown.instancesParam + '=[' + $scope.selected + ']';

    $http.get($scope.dataUrl).error(function(data, status) {
      console.log('HTTP ' + status + ': ' + $scope.dataUrl);
    }).success(function(data, status, headers, config) {
      $scope.dateObjs = data;

      // TODO: handle 'Other'
      if (breakdown) {
        $scope.selected.forEach(function(sel) {
          $scope.series.push(sel);
          var seriesData = [];
          var seriesTotal = 0;
          $scope.data.push(seriesData);
          $scope.dates.forEach(function(date) {
            var ct = 0;
            var dateCounts = $scope.dateObjs[date];
            if (dateCounts) {
              for (var k = 0; k < dateCounts.length; k++) {
                if (dateCounts[k][$scope.selField] == sel) {
                  ct = dateCounts[k].count;
                  seriesTotal += ct; 
                  break;
                }
              }
            }
            seriesData.push(ct);
          });
          var top = $scope.getTop(sel);
          if (top)
            top.seriesTotal = seriesTotal;
          $scope.total += seriesTotal; // TODO: overall total not used for breakdown
        });
      }
      else {
        // just one total per date
        var seriesData = [];
        $scope.data.push(seriesData);
        $scope.dates.forEach(function(date) {
          var ct = 0;
          var dateCounts = $scope.dateObjs[date];
          if (dateCounts && dateCounts[0])
            ct = dateCounts[0].count;
          seriesData.push(ct);
          $scope.total += ct;
        });
        $scope.series.push('Total (' + $scope.total + ')');
      }
      
      if ($scope.breakdownConfig.debug) {
        console.log('$scope.labels: ' + $scope.labels);
        console.log('$scope.series: ' + $scope.series);
        for (var l = 0; l < $scope.data.length; l++)
          console.log('$scope.data[' + l + ']: ' + $scope.data[l]);
      }
    });
  };
  
  // pass a function that returns true if the dateCount matches the added series
  $scope.addSeriesData = function(match) {
    var seriesData = [];
    var seriesTotal = 0;
    $scope.data.push(seriesData);
    $scope.dates.forEach(function(date) {
      var ct = 0;
      var dateCounts = $scope.dateObjs[date];
      if (dateCounts) {
        for (var k = 0; k < dateCounts.length; k++) {
          if (match(dateCounts[k])) {
            ct = dateCounts[k].count;
            seriesTotal += ct; 
            break;
          }
        }
      }
      seriesData.push(ct);
    });
    return seriesTotal;
  };
  
  $scope.onChartClick = function(points, evt) {
    console.log(points, evt);
  };
  
  $scope.setSpan = function(span) {
    $scope.span = span;
    $scope.days = $scope.span == 'Month' ? 30 : 7;    
    $scope.updateRange();
  };

  $scope.setBreakdown = function(breakdown) {
    $scope.breakdown = breakdown;
    if ($scope.breakdownConfig[breakdown]) {
      $scope.selField = $scope.breakdownConfig[breakdown].selectField;
      $scope.selectLabel = $scope.breakdownConfig[breakdown].selectLabel;
    }
    $scope.selected = [];
    $scope.updateRange();
  };
  // current breakdown obj (returns undefined, array or object)
  $scope.getBreakdown = function() {
    return $scope.breakdownConfig[$scope.breakdown];
  };
  
  $scope.getTop = function(selFieldValue) {
    if ($scope.tops) {
      for (var i = 0; i < $scope.tops.length; i++) {
        var top = $scope.tops[i];
        if (top[$scope.selField] === selFieldValue)
          return top;
      }
    }
  };
  $scope.getLabel = function(top, seriesTotal) {
    var breakdown = $scope.getBreakdown();
    if (breakdown && typeof breakdown.getLabel === "function") { // allow custom function 
      return breakdown.getLabel(top);
    }
    else {
      var label = top[$scope.selField];
      if ($scope.selField == 'id' && top.name)
        label = top.name;
      if (top.version)
        label += ' v' + top.version;
      if (top.definitionMissing)
        label = '[' + label + ']';
      if (top.count)
        label += ' (' + top.count + ')';
      else if (seriesTotal && typeof top.seriesTotal != 'undefined')
        label += ' (' + top.seriesTotal + ')';
      return label;
    }
  };
  $scope.getTitle = function(top) {
    var breakdown = $scope.getBreakdown();
    if (breakdown && typeof breakdown.getTitle === "function") { // allow custom function 
      return breakdown.getTitle(top);
    }
    else {
      if (top.packageName) {
        var title = top.packageName + '/' + top[$scope.selField];
        if ($scope.selField == 'id' && top.name)
          title = top.packageName + '/' + top.name;
        if (top.version)
          title += ' v' + top.version;
        if (top.definitionMissing)
          title = '[' + title + ']';
        return title;
      }
    }
  };
  
  $scope.goList = function() {
    if ($scope.listRoute) {
      $scope.$parent.getAuthUser().setActiveTab($scope.listRoute);
      $location.path($scope.listRoute.substring(1));
    }
  };
  
  $scope.downloadExcel = function() {
      window.location = $scope.dataUrl + '&' + EXCEL_DOWNLOAD;      
  };
  
}]);

chartMod.directive('mdwDashboardChart', function() {
  return {
    restrict: 'E',
    templateUrl: 'ui/chart.html',
    scope: {
      label: '@mdwChartLabel',
      breakdownConfig: '=mdwChartBreakdowns',
      filterTemplate: '@mdwFilters',
      listRoute: '@mdwList'
    },
    // require: ['label', 'breakdowns'],
    controller: 'MdwChartController',
    controllerAs: 'mdwChart',
    link: function link(scope, elem, attrs, ctrls) {
      scope.init();
    }
  };
});

chartMod.directive('selectPop', [function() {
  return {
    restrict: 'A',
    link: function link(scope, elem, attrs) {
      elem.bind('click', function() {
        if (scope.selectPop === elem) {
          // clicked to close
          scope.setSelectPop(null);
          if (scope.backupSelected !== null) {
            // not triggered programmatically
            scope.setSelected(scope.backupSelected);
            scope.setBackupSelected(null);
          }
        }
        else {
          scope.setSelectPop(elem);
          scope.setBackupSelected(scope.selected.slice());          
        }
      });
    }
  };
}]);