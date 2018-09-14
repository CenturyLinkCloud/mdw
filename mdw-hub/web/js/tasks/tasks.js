'use strict';

var tasksMod = angular.module('tasks', ['ngResource', 'mdw']);

// task list controller
tasksMod.controller('TasksController', ['$scope', '$window', '$http', '$location','$cookieStore', 'mdw', 'util', 'TaskAction', 'TaskUtil', 'TASK_ADVISORIES',
                                       function($scope, $window, $http, $location, $cookieStore, mdw, util, TaskAction, TaskUtil, TASK_ADVISORIES) {
  
  $scope.getFilter = function() {
    var taskFilter = $cookieStore.get('taskFilter');
    if (!taskFilter)
      taskFilter = {};
    return taskFilter;
  };
  
  $scope.setFilter = function(taskFilter) {
    if (taskFilter) {
      $cookieStore.put('taskFilter', taskFilter);
    }
  };
  
//If taskList is a top-level object in $scope, this means child scopes (such as ng-if) can 
  // break two-way binding, so taskList is a field in the top-level model object
  // https://github.com/angular/angular.js/issues/4046 -->
  
  $scope.resetFilter = function() {
    $scope.model.taskFilter = {
        workgroups: '[My Workgroups]',
        status: '[Active]',
        advisory: '[Not Invalid]',
        sort: 'startDate',
        descending: true,
        indexes: null
     };
  };
  
  // templateId and taskSpec passed in query params
  var templateIdParam = util.urlParams().templateId;
  var taskSpecParam = util.urlParams().taskSpec;
  var indexesParam = util.urlParams().indexes;
  var taskFilter = $scope.getFilter();
  if (templateIdParam && taskSpecParam) {
    taskFilter.taskId = templateIdParam;
    taskFilter.workgroups = '[My Workgroups]';
    taskFilter.status = null;
    taskFilter.advisory = '[Not Invalid]';
    taskFilter.sort = 'startDate';
    taskFilter.descending = true;
    taskFilter.indexes = null;
    $scope.setFilter(taskFilter);
    if (taskSpecParam.endsWith('.task'))
      taskSpecParam = taskSpecParam.substring(0, taskSpecParam.length - 5);
    $cookieStore.put('taskSpec', taskSpecParam);
    if (!indexesParam) {  // otherwise wait redirect after setting values
      window.location = mdw.roots.hub + '#/tasks';
      return;
    }
  }
  if (indexesParam) {
    taskFilter.workgroups = '[My Workgroups]';
    taskFilter.status = null;
    taskFilter.advisory = '[Not Invalid]';
    taskFilter.sort = 'startDate';
    taskFilter.descending = true;
    taskFilter.indexes = indexesParam;
    $scope.setFilter(taskFilter);
    window.location = mdw.roots.hub + '#/tasks';
    return;
  }
  
  $scope.model = {};
  $scope.model.taskList = {};
  $scope.model.taskFilter = $cookieStore.get('taskFilter');
  if (!$scope.model.taskFilter) {
    $scope.resetFilter();
  }
  else {
    // don't remember these
    if ($scope.model.taskFilter.instanceId)
      $scope.model.taskFilter.instanceId = null;
    if ($scope.model.taskFilter.masterRequestId)
      $scope.model.taskFilter.masterRequestId = null;
  }
  
  $scope.taskAdvisories = ['[Not Invalid]'].concat(TASK_ADVISORIES);

  // preselected taskSpec
  if ($scope.model.taskFilter.taskId) {
    $scope.model.typeaheadMatchSelection = $cookieStore.get('taskSpec');
  }
  else {
    $cookieStore.remove('taskSpec');
  }
  
  // retrieve TaskCategories
  $http.get(mdw.roots.services + '/services/BaseData/TaskCategories' + '?app=mdw-admin')
    .then(function(response) {
      $scope.taskCategories = response.data;
    });
  
  $scope.$on('page-retrieved', function(event, taskList) {
    // create date and due date
    taskList.tasks.forEach(function(task) {
      TaskUtil.setTask(task);
    });
    if ($scope.model.taskFilter.taskId && taskList.tasks.length > 0) {
      $cookieStore.put('taskSpec', taskList.tasks[0].name);
    }
    else {
      $cookieStore.remove('taskSpec');
    }
    $cookieStore.put('taskFilter', $scope.model.taskFilter);
    
    // retrieve TaskActions
    $http.get(mdw.roots.hub + '/services/Tasks/bulkActions?app=mdw-admin&myTasks=' + ($scope.model.taskFilter.assignee == '[My Tasks]'))
      .then(function(response) {
        $scope.taskActions = response.data;
      });
  });
  
  // must match actions wrap width in tasks.html
  // TODO: cleaner approach with calculation
  $scope.wrapWidth = '1425px'; 
  $scope.getAssigneePopPlace = function() {
    var minWidth = $scope.wrapWidth;
    if (minWidth && !$window.matchMedia('(min-width: ' + minWidth + ')').matches)
      return 'right';
    else
      return 'left';
  };
  
  $scope.performAssign = function(assignee) {
    $scope.performAction('Assign', assignee.cuid);
  };
  
  $scope.performAction = function(action, assignee) {
    $scope.closePopover(); // popover should be closed
    var selectedTasks = $scope.model.taskList.getSelectedItems();
    if (selectedTasks && selectedTasks.length > 0) {
      console.log('Performing action: ' + action + ' on selected task(s)');
      var instanceIds = [];
      selectedTasks.forEach(function(task) {
        instanceIds.push(task.id);
      });
      var taskAction = {
          taskAction: action, 
          user: $scope.authUser.id, 
          taskInstanceIds: instanceIds,
          assignee: assignee
      };
      
      TaskAction.action({action: action}, taskAction, function(data) {
        if (data.status.code >= 300) {
          $scope.model.taskList.reload(function(taskList) {
            $scope.updateOnActionError(data.status.message, instanceIds, taskList);
          });
        }
        else {
          $scope.model.taskList.reload();
        }
      }, function(error) {
        $scope.model.taskList.reload(function(taskList) {
          $scope.updateOnActionError(error.data.status.message, instanceIds, taskList);
        });
      });
    }
    else {
      mdw.messages = 'Please select task(s) to perform action on.';
    }
  };
  
  $scope.updateOnActionError = function(message, prevSelInstIds, taskList) {
    mdw.messages = message;
    // find problem instance id if available
    var instIdIdx = message.indexOf('instance: ');
    if (instIdIdx !== -1) {
      var erroredInstId = parseInt(message.substring(instIdIdx + 10));
      if (erroredInstId) {
        var after = false;
        var newSelInstIds = [];
        prevSelInstIds.forEach(function(prevSelInstId) {
          if (prevSelInstId == erroredInstId)
            after = true;
          if (after) {
            newSelInstIds.push(prevSelInstId);
          }
        });
        taskList.tasks.forEach(function(task) {
          if (newSelInstIds.indexOf(task.id) >= 0) {
            task.selected = true;
          }
        });
      }
    }
    $scope.digest(); // to show mdw.messages
  };
  
  $scope.model.typeaheadUser = null;
  $scope.findTypeaheadAssignees = function(typed) {
      return $http.get(mdw.roots.services + '/services/Tasks/assignees' + '?app=mdw-admin&find=' + typed).then(function(response) {
      var users = response.data.users;
      if ('[My Tasks]'.startsWith(typed))
        users.unshift({name: '[My Tasks'});
      if ('[Everyone\'s Tasks]'.startsWith(typed))
        users.unshift({name: '[Everyone\'s Tasks'});
      if ('[Unassigned]'.startsWith(typed))
        users.unshift({name: '[Unassigned'});
      return users;
    });
  };
  $scope.typeaheadUserChange = function() {
    if ($scope.model.typeaheadUser === null)
      $scope.model.taskFilter.assignee = null;
  };
  $scope.typeaheadUserSelect = function() {
    $scope.model.taskFilter.assignee = $scope.model.typeaheadUser.cuid;
  }; 
  $scope.setAssigneeFilter = function(assignee) {
    $scope.model.typeaheadUser = {name: assignee};
    $scope.model.taskFilter.assignee = assignee;
  };  
  
  // instanceId, masterRequestId, taskName, packageName
  $scope.findTypeaheadMatches = function(typed) {
    return $http.get(mdw.roots.services + '/services/Tasks' + '?app=mdw-admin&find=' + typed).then(function(response) {
      // service matches on instanceId or masterRequestId
      var taskInsts = response.data.tasks;
      if (taskInsts.length > 0) {
        var matches = [];
        taskInsts.forEach(function(taskInst) {
          if (taskInst.id.toString().startsWith(typed))
            matches.push({type: 'instanceId', value: taskInst.id.toString()});
          else
            matches.push({type: 'masterRequestId', value: taskInst.masterRequestId});
        });
        return matches;
      }
      else {
        return $scope.findTaskTemplate(typed);
      }
    });
  };
  $scope.clearTypeaheadFilters = function() {
    // check if defined to avoid triggering evaluation
    if ($scope.model.taskFilter.instanceId)
      $scope.model.taskFilter.instanceId = null;
    if ($scope.model.taskFilter.masterRequestId)
      $scope.model.taskFilter.masterRequestId = null;
    if ($scope.model.taskFilter.taskId)
      $scope.model.taskFilter.taskId = null;
  };
  $scope.typeaheadChange = function() {
      if ($scope.model.typeaheadMatchSelection === null)
      $scope.clearTypeaheadFilters();
  };
  $scope.clearTypeahead = function() {
    $scope.model.typeaheadMatchSelection = null;
    $scope.clearTypeaheadFilters();
    $cookieStore.remove('taskSpec');
  };
  
  $scope.typeaheadSelect = function() {
    $scope.clearTypeaheadFilters();
    if ($scope.model.typeaheadMatchSelection.id)
      $scope.model.taskFilter[$scope.model.typeaheadMatchSelection.type] = $scope.model.typeaheadMatchSelection.id;
    else
      $scope.model.taskFilter[$scope.model.typeaheadMatchSelection.type] = $scope.model.typeaheadMatchSelection.value;
  };
  
  // creating a new task
  $scope.create = false;
  $scope.setCreate = function(create) {
    $scope.create = create;
    if (!create)
      $scope.model.newTaskTemplate = null;
  };
  
  $scope.model.newTaskTemplate = null;

  $scope.findTaskTemplate = function(typed) {
     return $http.get(mdw.roots.services + '/services/Tasks/templates' + '?app=mdw-admin&find=' + typed).then(function(response) {
      // services matches on task name or package name
      if (response.data.length > 0) {
        var matches = [];
        response.data.forEach(function(taskDef) {
          if (typed.indexOf('.') > 0)
            matches.push({type: 'taskId', value: taskDef.packageName + '/' + taskDef.name + ' v' + taskDef.version, id: taskDef.taskId, logicalId: taskDef.logicalId});
          else
            matches.push({type: 'taskId', value: taskDef.name + ' v' + taskDef.version, id: taskDef.taskId, logicalId: taskDef.logicalId});
        });
        return matches;
      }
    });
  };
  
  $scope.save = function() {
    console.log('creating task: ' + $scope.model.newTaskTemplate.logicalId);
    var createAction = {
        taskAction: 'create',
        logicalId: $scope.model.newTaskTemplate.logicalId,
        user: $scope.authUser.id
    };
    TaskAction.create({action: 'create'}, createAction,
      function(data) {
        if (data.status && data.status.code >= 300) {
          $scope.task.message = data.status.message;
        }
        else {
          var instanceId = data.taskInstanceId;
          console.log('  new task instanceId: ' + instanceId);
          $location.path('/tasks/' + instanceId);
        }
      }, 
      function(error) {
        $scope.task.message = error.data.status.message;
      });
  };
  
}]);

tasksMod.controller('TemplatesController', ['$scope', '$cookieStore', 'mdw', 'util', 'Templates',
                       function($scope, $cookieStore, mdw, util, Templates) {
  $scope.templateList = Templates.retrieve({grouped:true}, function success() {
  var pkgs = $scope.templateList.packages;
  pkgs.forEach(function(pkg) {
    pkg.templates.forEach(function(template) {
    template.baseName = template.name.substring(0, template.name.lastIndexOf('.'));
    });
    $scope.applyPkgCollapsedState();
  });
  });

  $scope.collapse = function(pkg) {
    pkg.collapsed = true;
    $scope.savePkgCollapsedState();
  };
  $scope.collapseAll = function() {
  $scope.templateList.packages.forEach(function(pkg) {
    pkg.collapsed = true;
  });
  $scope.savePkgCollapsedState();
  };
  $scope.expand = function(pkg) {
  pkg.collapsed = false;
  $scope.savePkgCollapsedState();
  };
  $scope.expandAll = function() {
  $scope.templateList.packages.forEach(function(pkg) {
    pkg.collapsed = false;
  });
  $scope.savePkgCollapsedState();
  };
  $scope.savePkgCollapsedState = function() {
    var st = {};
    $scope.templateList.packages.forEach(function(pkg) {
      if (pkg.collapsed)
        st[pkg.name] = true;
    });
    $cookieStore.put('procsPkgCollapsedState', st);
  };
  $scope.applyPkgCollapsedState = function() {
  var st = $cookieStore.get('procsPkgCollapsedState');
  if (st) {
    util.getProperties(st).forEach(function(pkgName) {
    var col = st[pkgName];
    if (col === true) {
      for (var i = 0; i < $scope.templateList.packages.length; i++) {
      if (pkgName == $scope.templateList.packages[i].name) {
        $scope.templateList.packages[i].collapsed = true;
          break;
      }
      }
    }
    });
  }
  };
}]);

tasksMod.factory('TaskAction', ['$resource', 'mdw', function($resource, mdw) {
  return $resource(mdw.roots.services +'/Services/Tasks/:action', mdw.serviceParams(), {
    create: { method: 'POST'},
    action: { method: 'POST'}
  });
}]);

tasksMod.factory('TaskUtil', ['$location', '$route', 'mdw', 'util', function($location, $route, mdw, util) {
  return {
    setTask: function(task) {
      if (task.instanceUrl) {
        task.link = task.instanceUrl;
      }
      else {
        task.link = '#/tasks/' + task.id;
      }
      var startDate = new Date(task.start);
      task.start = util.past(startDate);
      if (task.due) {
        if (task.end) {
          task.due = null;
        }
        else {
          var dueDate = new Date(task.due);
          if (dueDate.getTime() >= Date.now()) {
            task.alert = false;
            task.due = util.future(dueDate);
          }
          else {
            task.alert = true;
            task.due = util.past(dueDate);
          }
        }
      }
      else {
        task.alert = false;
        task.due = null;
      }
      if (task.end) {
        task.end = util.past(new Date(task.end));
      }
    }
  };
}]);

tasksMod.factory('Templates', ['$resource', 'mdw', function($resource, mdw) {
  return $resource(mdw.roots.services + '/Services/Tasks/templates', mdw.serviceParams(), {
    retrieve: { method: 'GET', isArray: false }
  });
}]);
