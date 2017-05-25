'use strict';

var taskMod = angular.module('mdwTask', ['mdw']);

taskMod.controller('MdwTaskTemplateController', 
    ['$scope', '$http', '$q', 'mdw', 'Workgroups', 'Configurator',
    function($scope, $http, $q, mdw, Workgroups, Configurator) {
  
  $scope.init = function() {
    if (typeof $scope.task === 'string' || $scope.task instanceof String)
      $scope.taskTemplate = JSON.parse($scope.task);
    else
      $scope.taskTemplate = $scope.task;

    var pageletPath;
    if ($scope.isAutoform())
      pageletPath = 'com.centurylink.mdw.base/AutoFormManualTask.pagelet';
    else
      pageletPath = 'com.centurylink.mdw.base/CustomManualTask.pagelet';
    
    // we need workgroups populated
    var groups = Workgroups.get();
    var pagelet = $http.get(mdw.roots.services + '/services/Pagelets/' + pageletPath);
    var taskCats = $http.get(mdw.roots.services + '/services/Tasks/categories');
    
    $q.all([groups, pagelet, taskCats])
    .then(function(results) {
      Workgroups.groupList = results[0];
      $scope.pagelet = results[1].data;
      var obj = {
        diagram: {editable: $scope.editable}  
      };
      var template = {
        category: pageletPath,
        icon: 'task.png',
        label: 'Task Template',
        pagelet: $scope.pagelet
      };
      
      // General tab has two separate templates;
      var taskCategories = results[2].data;
      var objectTemplate = angular.copy(template);
      var attrsTemplate = angular.copy(template);
      objectTemplate.category = 'object';
      objectTemplate.pagelet.widgets = [];
      attrsTemplate.pagelet.widgets = [];
      template.pagelet.widgets.forEach(function(widget) {
        if (widget.name == 'name' || widget.name == 'logicalId' || widget.name == 'description')
          objectTemplate.pagelet.widgets.push(widget);
        else if (widget.name == 'category') {
          widget.options = [];
          taskCategories.forEach(function(taskCat) {
            widget.options.push(taskCat.name);
            if (widget.value == taskCat.code)
              widget.value = taskCat.name;
          });
          objectTemplate.pagelet.widgets.push(widget);
        }
        else {
          attrsTemplate.pagelet.widgets.push(widget);
        }
      });
      
      
      $scope.configurators = [];
      $scope.configurators.push(new Configurator("General", 'task', $scope.taskTemplate, obj, objectTemplate));
      $scope.configurators.push(new Configurator("Design", 'task', $scope.taskTemplate, obj, attrsTemplate));
      $scope.configurators.push(new Configurator("Workgroups", 'task', $scope.taskTemplate, obj, angular.copy(template)));
      $scope.configurators.push(new Configurator("Notices", 'task', $scope.taskTemplate, obj, angular.copy(template)));
      $scope.configurators.push(new Configurator("Recipients", 'task', $scope.taskTemplate, obj, angular.copy(template)));
      $scope.configurators.push(new Configurator("CC Recipients", 'task', $scope.taskTemplate, obj, angular.copy(template)));
      if ($scope.isAutoform())
        $scope.configurators.push(new Configurator("Variables", 'task', $scope.taskTemplate, obj, angular.copy(template)));
      else
        $scope.configurators.push(new Configurator("Indexes", 'task', $scope.taskTemplate, obj, angular.copy(template)));
      $scope.configurators.push(new Configurator("Source", 'task', $scope.taskTemplate, obj, angular.copy(template)));
      
      $scope.configurators.forEach(function(configurator) {
        configurator.initValues($scope.edit);
      });
    });
  };
  
  $scope.isAutoform = function() {
    return $scope.taskTemplate.attributes.FormName === 'Autoform';
  };
  
  $scope.edit = function(widget) {
    
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
    }
  };
}]);
