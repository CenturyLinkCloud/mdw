// Copyright (c) 2017 CenturyLink, Inc. All Rights Reserved.
'use strict';

var valuesMod = angular.module('mdwValues', ['mdw']);

valuesMod.controller('MdwValuesController', ['$scope', 'mdw', 'util', 'DOCUMENT_TYPES',
                                             function($scope, mdw, util, DOCUMENT_TYPES) {
  $scope.init = function() {
    $scope.showLines = 2;
    $scope.maxLines = 5;
    
    if (!$scope.dateDisplayFormat)
      $scope.dateDisplayFormat = 'MMM-dd-yyyy';
    
    $scope.$watch('valuesObj', function(valuesObj) {

      // convert object into sorted array
      var valueObj = $scope.values;
      $scope.values = [];
      if (valuesObj && typeof valuesObj === 'object') {
        for (var key in valuesObj) {
          if (valuesObj.hasOwnProperty(key) && !key.startsWith('$')) {
            var val = valuesObj[key];
            val.name = key;
            if (!val.sequence)
              val.sequence = 0;
            val.isDocument = val.type && DOCUMENT_TYPES[val.type];
            if (val.isDocument) {
              val.showLines = $scope.showLines;
              if (val.value && val.value.lineCount) {
                var lineCount = val.value.lineCount();
                if (lineCount > $scope.maxLines)
                  val.showLines = $scope.maxLines;
                else if (lineCount > $scope.showLines)
                  val.showLines = lineCount;
              }
            }
            else if (val.type === 'java.util.Date' && val.value) {
              // TODO: option to specify date parse format
              val.value = new Date(val.value);
            }
            if (val.display && $scope.editable)
              val.editable = val.display !== 'ReadOnly';
            else
              val.editable = $scope.editable;
            $scope.values.push(val);
          }
        }
        $scope.values.sort(function(val1, val2) {
          var diff = val1.sequence - val2.sequence;
          if (diff === 0) {
            var label1 = val1.label ? val1.label : val1.name;
            var label2 = val2.label ? val2.label : val2.name;
            return label1.localeCompare(label2);
          }
          else {
            return diff;
          }
        });
      }
      
    }, true);
    
    $scope.$watch('editable', function(editable) {
      if ($scope.values) {
        $scope.values.forEach(function(val) {
          val.editable = editable;
        });
      };
    }, true);
  };
  
  $scope.dirty = function(value) {
    value.dirty = true;
  };
  
  $scope.openDatePopup = function(field) {
    $scope.datePopups = {};
    $scope.datePopups[field] = true;
  };
}]);

valuesMod.directive('mdwValues', function() {
  return {
    restrict: 'E',
    templateUrl: 'ui/values.html',
    scope: {
      editable: '=mdwValuesEditable',
      valuesObj: '=mdwValuesValues',
      dateDisplayFormat: '@mdwDateDisplay'
    },
    controller: 'MdwValuesController',
    controllerAs: 'mdwValues',
    link: function link(scope, elem, attrs, ctrls) {
      scope.init();
    }
  };
});