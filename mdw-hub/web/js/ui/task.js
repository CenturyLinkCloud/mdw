'use strict';

var taskMod = angular.module('mdwTask', ['mdw']);

taskMod.controller('MdwTaskTemplateController', 
    ['$scope', '$http', 'mdw',
    function($scope, $http, mdw) {
  
  $scope.init = function() {
    if (typeof $scope.task === 'string' || $scope.task instanceof String)
      $scope.task = JSON.parse($scope.task);
    
    var pageletPath;
    if ($scope.isAutoform)
      pageletPath = 'com.centurylink.mdw.base/AutoFormManualTask.pagelet';
    else
      pageletPath = 'com.centurylink.mdw.base/CustomManualTask.pagelet';
    
    $http({ method: 'GET', url: mdw.roots.services + '/services/Pagelets/' + pageletPath })
    .then(function success(response) {
      $scope.pagelet = response.data;
    }, function error(response) {
      mdw.messages = response.statusText;
    });
  };
  
  $scope.isAutoform = function() {
    return $scope.task.attributes.FormName === 'Autoform';
  };
}]);

// attributes
//   - task (object): task template asset path
//   - editable: if true, template can be modified
taskMod.directive('mdwTaskTemplate', [function() {
  return {
    restrict: 'E',
    templateUrl: 'ui/task-template.html',
    scope: {
      task: '=task',
      editable: '@editable'
    },
    controller: 'MdwTaskTemplateController',
    controllerAs: 'mdwTaskTemplate',
    link: function link(scope, elem, attrs, ctrls) {
      scope.init();
      scope.$on('$destroy', function() {
        scope.dest();
      });
    }
  };
}]);
