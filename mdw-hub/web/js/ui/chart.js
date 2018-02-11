'use strict';

var chartMod = angular.module('mdwChart', ['mdw']);


chartMod.controller('MdwChartController', ['$scope','$cookieStore', '$http', '$location', 'mdw', 'util', 'EXCEL_DOWNLOAD' ,
                                             function($scope, $cookieStore, $http, $location, mdw, util, EXCEL_DOWNLOAD) {

  $scope.init = function() {
    $scope.spans = ['Week', 'Month']; 
    $scope.span = 'Week';
    $scope.timefilters = ['Milliseconds','Seconds','Mins','Hours', 'Days'];
    $scope.timefilter = '';
    $scope.days = 7;
        
      // TODO hardcoded
    $scope.initialSelect = 5;
      
    $scope.name='name';
    $scope.chartLabels=[];
      
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
    if ($scope.breakdownConfig.Status) {
      $scope.statuses = $scope.breakdownConfig.Status.throughput.slice();
    }
    $scope.setBreakdown($scope.breakdowns[0]);    
  };

  $scope.resetFilter = function() {
    $scope.$parent.closePopover();
    $scope.filter = {
      ending: new Date()
    };
  };
  $scope.resetFilter();
  
  $scope.setStatus = function(status) {
    $scope.filter.status = status;
    $scope.updateRange();
  };
  
  $scope.chartOptions = {
    legendCallback: function(chart) {
      var text = [];
      var top;
      var label;
      var title;
      var total=false;
      text.push('<ul class="mdw-chart-legend">');
      if(chart.config.type ==='pie'){
        for (var i = 0; i < chart.data.labels.length; i++) {
          var selLabelValue = $scope.selected[i];
          top = $scope.getTop(selLabelValue);
            if (top) {
                label = $scope.getLabel(top, true);
                title = $scope.getTitle(top);
              }
              if (label) {
                text.push('  <li>');
                text.push('    <span class="mdw-chart-legend-icon" style="background-color:' + chart.config.data.datasets[0].backgroundColor[i] + ';' + 
                          'border-color:' + chart.config.data.datasets[0].backgroundColor[i] + '"></span>');
                           //As pie chart has only one dataset, fetcch the background color
                if (title)
                  text.push('    <span class="mdw-chart-legend-text" title="' + title + '">' + label + '</span>');
                else
                  text.push('    <span class="mdw-chart-legend-text">' + label + '</span>');
                
                text.push('  </li>');
              }
              else{
                total=true;
                break;
              }      
        }
        if(total){
          text.push('  <li>');
          text.push('    <span class="mdw-chart-legend-text"> Total (' + $scope.total +') </span>');  
          text.push('  </li>');
        }
        text.push('</ul>');

          return text.join('\n  ');
       
      }
      for (var j = 0; j < chart.data.datasets.length; j++) {
        var selFieldValue = $scope.selected[j];
        top = $scope.getTop(selFieldValue);
        if (top) {
          label = $scope.getLabel(top, true);
          title = $scope.getTitle(top);
        }
        if (label) {
          text.push('  <li>');
          text.push('    <span class="mdw-chart-legend-icon" style="background-color:' + chart.data.datasets[j].backgroundColor + ';' + 
              'border-color:' + chart.data.datasets[j].borderColor + '"></span>');
          if (title)
            text.push('    <span class="mdw-chart-legend-text" title="' + title + '">' + label + '</span>');
          else
            text.push('    <span class="mdw-chart-legend-text">' + label + '</span>');
          text.push('  </li>');
        }
        else {
           total=true;
            break;
          }      
      }
      if(total){
        text.push('  <li>');
        text.push('    <span class="mdw-chart-legend-text"> Total (' + $scope.total +') </span>');  
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
    // ending today unless specified in filter
    var d = new Date($scope.filter.ending.getTime());
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
      if ($scope.filter.status)
        url += '&status=' + $scope.filter.status;    
      $http.get(url).error(function(data, status) {
        console.log('HTTP ' + status + ': ' + url);
      }).success(function(data, status, headers, config) {
        $scope.tops = data;
        if ($scope.selected.length === 0) {
          // initialize to top 5
          $scope.chartLabels = [];
          for (var i = 0; i < $scope.tops.length; i++) {
            var val = $scope.tops[i][$scope.selField]; 
            var label=$scope.tops[i][$scope.name];
            //display only first 5 labels for line chart with completionTime
            if (breakdown.throughput.indexOf("completionTime=true") != -1 && $scope.chartType ==='chart chart-line'){
               if(val && $scope.selected.length < $scope.initialSelect){
                 $scope.selected.push(val); 
                 $scope.chartLabels.push(label);
                }
             }else{
               $scope.chartLabels.push(label);
               $scope.selected.push(val); 
             }
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
        $scope.chartLabels=$scope.selected;
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
      var bigNumer=0; 
      // TODO: handle 'Other'
      var seriesData = [];
      var seriesTotal = 0;
      if (breakdown && $scope.chartType ==='chart chart-line') {      
        $scope.selected.forEach(function(sel) {
          //series should have names instead of ids. (For proper display of tooltips)
          var obj = $scope.getTop(sel);
          if (obj) {
            var name = $scope.getLabel(obj, false);
            $scope.series.push(name);
          }  
          seriesData = [];
          seriesTotal = 0;
           $scope.data.push(seriesData); 
           $scope.dates.forEach(function(date) {
            var ct = 0;
            var dateCounts = $scope.dateObjs[date];// horizondal  dates
            
         if (dateCounts) {

        for (var l = 0; l < dateCounts.length; l++) {
          if (dateCounts[l][$scope.selField] == sel){

        if((breakdown.throughput).indexOf("completionTime=true") > 0) {                     
          if(dateCounts[l].meanCompletionTime > bigNumer){
            bigNumer=dateCounts[l].meanCompletionTime; 
          }

        }
          }
        }
    }       
                           
     if(bigNumer/1000/60/60/24 > 1 && ($scope.timefilter=== "")){
       $scope.timefilter= 'Days';   
     } else if(bigNumer/1000/60/60> 1 && ($scope.timefilter=== "")){
       $scope.timefilter= 'Hours';   
     } else if(bigNumer/1000/60 > 1 && ($scope.timefilter=== "")){ //one min
      $scope.timefilter= 'Mins';              
     } else if(bigNumer/1000 > 1  && ($scope.timefilter=== "")){
       $scope.Seconds= 'Seconds';   
     } else{ 
      $scope.Milliseconds= 'Milliseconds';              
     }
             
            if (dateCounts) {
              for (var k = 0; k < dateCounts.length; k++) {
                if (dateCounts[k][$scope.selField] == sel) {
                    if((breakdown.throughput).indexOf("completionTime=true") == -1)  
                      ct = dateCounts[k].count;
                  else{ //Vertical seconds
                    if($scope.timefilter=== 'Milliseconds')
                      ct = dateCounts[k].meanCompletionTime;
                    else if ($scope.timefilter=== 'Seconds')
                      ct = dateCounts[k].meanCompletionTime/1000;
                    else if ($scope.timefilter=== 'Mins')
                      ct = dateCounts[k].meanCompletionTime/1000/60;
                    else if ($scope.timefilter=== 'Hours')  
                      ct = dateCounts[k].meanCompletionTime/1000/60/60;
                    else if ($scope.timefilter=== 'Days')
                      ct = dateCounts[k].meanCompletionTime/1000/60/60/24;                  
                     }                   
                  seriesTotal += ct;
                  break;
                }
              }
            }           
             seriesData.push(ct);
   
          });      
           
          var top = $scope.getTop(sel);
          if (top){
              top.seriesTotal = seriesTotal;
              $scope.total += seriesTotal; // TODO: overall total not used for breakdown
             }
         });
       }else if (breakdown && $scope.chartType ==='chart chart-pie'){
        // As pie chart has only one dataset. 
          seriesData = [];
          $scope.labels = $scope.chartLabels;
          $scope.selected.forEach(function(sel) {
          var seriesTotal = 0;
          $scope.dates.forEach(function(date) {
          var ct = 0;
          var dateCounts = $scope.dateObjs[date];
          
          if (dateCounts) {
                    
      for (var m = 0; m < dateCounts.length; m++) {
        if (dateCounts[m][$scope.selField] == sel){

      if((breakdown.throughput).indexOf("completionTime=true") > 0) {                     
        if(dateCounts[m].meanCompletionTime > bigNumer){
          bigNumer=dateCounts[m].meanCompletionTime; 
        }

      }
        }
      }
    }
                  
                           
   if(bigNumer/1000/60/60/24 > 1 && ($scope.timefilter=== "")){
     $scope.timefilter= 'Days';   
   } else if(bigNumer/1000/60/60> 1 && ($scope.timefilter=== "")){
     $scope.timefilter= 'Hours';   
   } else if(bigNumer/1000/60 > 1 && ($scope.timefilter=== "")){ //one min
    $scope.timefilter= 'Mins';              
   } else if(bigNumer/1000 > 1  && ($scope.timefilter==="")){
     $scope.Seconds= 'Seconds';   
   } else{ 
    $scope.Milliseconds= 'Milliseconds';              
         }
               
          if (dateCounts) {
           for (var k = 0; k < dateCounts.length; k++) {
              if (dateCounts[k][$scope.selField] == sel) {
    if((breakdown.throughput).indexOf("completionTime=true") == -1)
        ct = dateCounts[k].count;
    else{ //Vertical seconds
        if($scope.timefilter=== 'Milliseconds')
          ct = dateCounts[k].meanCompletionTime;
        else if ($scope.timefilter=== 'Seconds')
          ct = dateCounts[k].meanCompletionTime/1000;
        else if ($scope.timefilter=== 'Mins')
          ct = dateCounts[k].meanCompletionTime/1000/60;
        else if ($scope.timefilter=== 'Hours')  
          ct = dateCounts[k].meanCompletionTime/1000/60/60;
        else if ($scope.timefilter=== 'Days')
          ct = dateCounts[k].meanCompletionTime/1000/60/60/24;                  
         }
                seriesTotal += ct; 
                break;
              }
             }
           }
         });
            var top = $scope.getTop(sel);
            if (top){
                top.seriesTotal = seriesTotal;
                $scope.total += seriesTotal; // TODO: overall total not used for breakdown
                $scope.data.push(seriesTotal);
               }
           });
      }
      else {
      // just one total per date (without breakdown) 
        if($scope.chartType ==='chart chart-pie'){
            seriesData = [];
              $scope.dates.forEach(function(date) {
                var ct = 0;
                var dateCounts = $scope.dateObjs[date];
                if (dateCounts && dateCounts[0])
                  ct = dateCounts[0].count;
                $scope.data.push(ct);
                $scope.total += ct;
                
              });
          }else{
        // just one total per date
        seriesData = [];
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

  $scope.setTimefilter = function(timefilter) {
    $scope.timefilter = timefilter;      
    $scope.updateRange();
  };
     
  $scope.setBreakdown = function(breakdown) {   
    $scope.breakdown = breakdown;
    if ($scope.breakdownConfig[breakdown]) {
      $scope.selField = $scope.breakdownConfig[breakdown].selectField;
      $scope.selectLabel = $scope.breakdownConfig[breakdown].selectLabel;
    }
    $scope.selected = [];
    $scope.resetFilter();    
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
      if(seriesTotal === false)
        return label;
      if (top.definitionMissing)
        label = '[' + label + ']';      
      if (top.count){
        if((breakdown.throughput).indexOf("completionTime=true") == -1)  
          label += ' (' + top.count + ')';
        else{         
    if($scope.timefilter== 'Milliseconds')
      label += ' (' + top.meanCompletionTime + ')';
    else if ($scope.timefilter== 'Seconds')            
        label += ' (' + top.meanCompletionTime/1000 + ')';
    else if ($scope.timefilter== 'Mins')
      label += ' (' + top.meanCompletionTime/1000/60 + ')';              
    else if ($scope.timefilter== 'Hours')  
      label += ' (' + top.meanCompletionTime/1000/60/60 + ')';              
    else if ($scope.timefilter== 'Days')
      label += ' (' + top.meanCompletionTime/1000/60/60/24 + ')';             
        }
    } else if (seriesTotal && typeof top.seriesTotal != 'undefined')
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
      chartType: '@mdwChartType',
      breakdownConfig: '=mdwChartBreakdowns',
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

