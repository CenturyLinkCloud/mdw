'use strict';

var taskMod = angular.module('mdwTask', ['mdw']);

taskMod.controller('MdwTaskTemplateController', 
    ['$scope', '$http', '$q', 'mdw', 'Workgroups', 'Configurator',
    function($scope, $http, $q, mdw, Workgroups, Configurator) {
  
  $scope.init = function() {
    if ($scope.task) {
      // already retrieved
      if (typeof $scope.task === 'string' || $scope.task instanceof String)
        $scope.taskTemplate = JSON.parse($scope.task);
      else
        $scope.taskTemplate = $scope.task;
      $scope.buildConfigurators();
    }
    else {
      $http.get(mdw.roots.services + '/asset/' + $scope.taskAsset)
      .then(function success(response) {
        $scope.taskTemplate = response.data;
        $scope.buildConfigurators();
      }, function error(response) {
        mdw.messages = response.statusText;
      });
    }
  };
  
  $scope.buildConfigurators = function() {
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
        if (widget.name == 'name' || widget.name == 'logicalId' || widget.name == 'description' || widget.name == 'Task Template Help')
          objectTemplate.pagelet.widgets.push(widget);
        else if (widget.name == 'category') {
          widget.options = [];
          taskCategories.forEach(function(taskCat) {
            widget.options.push(taskCat.name);
          });
          widget.converter = {
            toWidgetValue: function(widget, codeValue) {
              for (let i = 0; i < taskCategories.length; i++) {
                if (codeValue == taskCategories[i].code)
                  return taskCategories[i].name;
              }
            },
            fromWidgetValue: function(widget, widgetValue) {
              for (let i = 0; i < taskCategories.length; i++) {
                if (widgetValue == taskCategories[i].name)
                  return taskCategories[i].code;
              }
            }
          };
          objectTemplate.pagelet.widgets.push(widget);
        }
        else {
          attrsTemplate.pagelet.widgets.push(widget);
        }
      });
      
      $scope.configurators = [];
      $scope.configurators.push(new Configurator('General', 'task', $scope.taskTemplate, obj, objectTemplate));
      $scope.configurators.push(new Configurator('Design', 'task', $scope.taskTemplate, obj, attrsTemplate));
      $scope.configurators.push(new Configurator('Workgroups', 'task', $scope.taskTemplate, obj, angular.copy(template)));
      $scope.configurators.push(new Configurator('Notices', 'task', $scope.taskTemplate, obj, angular.copy(template)));
      $scope.configurators.push(new Configurator('Recipients', 'task', $scope.taskTemplate, obj, angular.copy(template)));
      $scope.configurators.push(new Configurator('CC Recipients', 'task', $scope.taskTemplate, obj, angular.copy(template)));
      if ($scope.isAutoform())
        $scope.configurators.push(new Configurator('Variables', 'task', $scope.taskTemplate, obj, angular.copy(template)));
      else
        $scope.configurators.push(new Configurator('Indexes', 'task', $scope.taskTemplate, obj, angular.copy(template)));
      
      $scope.configurators.forEach(function(configurator) {
        configurator.initValues();
      });
      // add pseudo-configurator for source tab
      $scope.configurators.push({tab: 'Source'});
    });
  };
  
  $scope.isAutoform = function() {
    return $scope.taskTemplate.attributes.FormName === 'Autoform';
  };
  
  $scope.getConfigurator = function(tab) {
    for (let i = 0; i < $scope.configurators.length; i++) {
      if ($scope.configurators[i].tab == tab)
        return $scope.configurators[i];
    }
  };

  $scope.valueChanged = function(widget, evt) {
    if (widget.configurator.valueChanged(widget, evt)) {
      $scope.$parent.setDirty(true);
      $scope.source.content = JSON.stringify($scope.taskTemplate, null, 2);
      $scope.$parent.asset.content = $scope.source.content;
    }
  };
  
  $scope.curTab = null;
  $scope.tabSelect = function(tab) {
    if (tab === 'Source' || $scope.curTab === null) {
      $scope.source = { content: JSON.stringify($scope.taskTemplate, null, 2) };
      if ($scope.aceSession)
        $scope.aceSession.setValue($scope.source.content);
    }
    else if ($scope.curTab === 'Source'){
      $scope.taskTemplate = JSON.parse($scope.source.content);
      $scope.configurators.forEach(function(configurator) {
        if (configurator.tab !== 'Source') {
          configurator.workflowObj = $scope.taskTemplate;
          configurator.initValues();
        }
      });
    }
    $scope.curTab = tab;
  };
  
  $scope.edits = 0;
  if ($scope.editable) {
    $scope.editOptions = {
      theme: 'eclipse', 
      mode: 'json',
      onChange: function(args) {
        // first two times don't count
        if ($scope.edits < 2) {
          $scope.editDirty = false;
          $scope.aceSession = args[1].session;
          $scope.edits++;
        }
        else {
          $scope.$parent.setDirty(true);
          $scope.$parent.asset.content = $scope.source.content;
        }
      },
      basePath: '/mdw/lib/ace-builds/src-min-noconflict',
    };
  }
}]);

// attributes (either task or task-asset is required)
//   - task (object or raw JSON content): task template
//   - task-asset (string): task template asset path
//   - editable: if true, template can be modified
taskMod.directive('mdwTaskTemplate', [function() {
  return {
    restrict: 'E',
    templateUrl: 'ui/task-template.html',
    scope: {
      task: '=task',
      taskAsset: '@taskAsset',
      editable: '@editable'
    },
    controller: 'MdwTaskTemplateController',
    controllerAs: 'mdwTaskTemplate',
    link: function link(scope, elem, attrs, ctrls) {
      scope.init();
    }
  };
}]);
