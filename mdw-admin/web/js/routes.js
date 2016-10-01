// Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
'use strict';

var routesSvc = angular.module('routes', []);

routesSvc.factory('routes', function() {
  return {
    def: [
      {
        path: '/admin-nav', 
        templateUrl: 'layout/admin-nav.html'
      },
      {
        path: '/users', 
        templateUrl: 'users/users.html',
        controller: 'UsersController'
      },
      {
        path: '/users/:userId',
        templateUrl: 'users/user.html',
        controller: 'UserController'
      },
      {
        path: '/groups',
        templateUrl: 'groups/groups.html',
        controller: 'GroupsController'
      },
      {
        path: '/groups/:groupName',
        templateUrl: 'groups/group.html',
        controller: 'GroupController'
      },
      {
        path: '/roles',
        templateUrl: 'roles/roles.html',
        controller: 'RolesController'
      },
      {
        path: '/roles/:roleName',
        templateUrl: 'roles/role.html',
        controller: 'RoleController'
      },
      {
        path: '/packages',
        templateUrl: 'assets/packages.html',
        controller: 'PackagesController'
      },
      {
        path: '/assets/gitImport',
        templateUrl: 'assets/gitImport.html',
        controller: 'PackagesController'
      },
      {
        path: '/assets/fileImport',
        templateUrl: 'assets/fileImport.html',
        controller: 'PackagesController'
      },
      {
        path: '/packages/:packageName',
        templateUrl: 'assets/package.html',
        controller: 'PackageController'
      },    
      {
        path: '/asset/:packageName/:assetName',
        templateUrl: 'assets/asset.html',
        controller: 'AssetController'
      },
      {
        path: '/tests',
        templateUrl: 'testing/tests.html',
        controller: 'TestsController'
      },
      {
        path: '/tests/:packageName/:testCaseName',
        templateUrl: 'testing/test.html',
        controller: 'TestController'
      },      
      {
        path: '/history',
        templateUrl: 'history/history.html',
        controller: 'HistoryController'
      },
      {
        path: '/task-nav', 
        templateUrl: 'layout/task-nav.html'
      },
      {
        path: '/tasks',
        templateUrl: 'tasks/tasks.html',
        controller: 'TasksController'
      },
      {
        path: '/tasks/:taskInstanceId',
        templateUrl: 'tasks/task.html',
        controller: 'TaskController'
      },
      {
        path: '/tasks/:taskInstanceId/values',
        templateUrl: 'tasks/taskValues.html',
        controller: 'TaskValuesController'
      },
      {
        path: '/tasks/:taskInstanceId/subtasks',
        templateUrl: 'tasks/subtasks.html',
        controller: 'SubtasksController'
      },
      {
        path: '/tasks/:taskInstanceId/notes',
        templateUrl: 'tasks/notes.html',
        controller: 'TaskNotesController'
      },
      {
        path: '/tasks/:taskInstanceId/attachments',
        templateUrl: 'tasks/attachments.html',
        controller: 'TaskAttachmentsController'
      },
      {
        path: '/tasks/:taskInstanceId/history',
        templateUrl: 'tasks/history.html',
        controller: 'TaskHistoryController'
      },
      {
        path: '/taskTemplates',
        templateUrl: 'taskTemplates/templates.html',
        controller: 'TasksTemplatesController'
      },
      {
        path: '/taskTemplates/:taskName',
        templateUrl: 'taskTemplates/tasktabs.html',
        controller: 'TaskTemplateController'
      },
      {
        path: '/workflow-nav', 
        templateUrl: 'layout/workflow-nav.html'
      },
      {
        path: '/workflow/processes',
        templateUrl: 'workflow/processes.html',
        controller: 'ProcessesController'
      },
      {
        path: '/workflow/processes/:instanceId',
        templateUrl: 'workflow/process.html',
        controller: 'ProcessController'
      },
      {
        path: '/workflow/processes/:instanceId/values',
        templateUrl: 'workflow/values.html',
        controller: 'ProcessController'
      },
      {
        path: '/workflow/processes/:instanceId/values/:name',
        templateUrl: 'workflow/value.html',
        controller: 'ProcessController'
      },
      {
        path: '/workflow/definition/:packageName/:processName',
        templateUrl: 'workflow/definition.html',
        controller: 'ProcessDefController'
      },
      {
        path: '/workflow/definition/:packageName/:processName/:version',
        templateUrl: 'workflow/definition.html',
        controller: 'ProcessDefController'
      },
      {
        path: '/workflow/requests',
        templateUrl: 'requests/requests.html',
        controller: 'RequestsController'
      },
      {
        path: '/workflow/requests/:requestId',
        templateUrl: 'requests/request.html',
        controller: 'RequestController'
      },
      {
        path: '/workflow/masterRequests/:masterRequestId',
        templateUrl: 'requests/request.html',
        controller: 'RequestController'
      },
      {
        path: '/workflow/responses/:requestId',
        templateUrl: 'requests/response.html',
        controller: 'RequestController'
      },
      {
        path: '/workflow/masterResponses/:masterRequestId',
        templateUrl: 'requests/response.html',
        controller: 'RequestController'
      },
      {
        path: '/workflow/activities',
        templateUrl: 'workflow/activities.html',
        controller: 'ActivitiesController'
      },
      {
        path: '/workflow/activities/:instanceId',
        templateUrl: 'workflow/activity.html',
        controller: 'ActivityController'
      },     
      {
        path: '/dashboard/processes',
        templateUrl: 'dashboard/processes.html',
        controller: 'DashboardProcessesController'
      },
      {
        path: '/dashboard/requests',
        templateUrl: 'dashboard/requests.html',
        controller: 'DashboardRequestsController'
      },
      {
        path: '/dashboard/tasks',
        templateUrl: 'dashboard/tasks.html',
        controller: 'DashboardTasksController'
      },
      {
        path: '/dashboard/activities',
        templateUrl: 'dashboard/activities.html',
        controller: 'DashboardActivitiesController'
      },
      {
        path: '/exceptions',
        templateUrl: 'dashboard/exceptions.html'
      },
      {
        path: '/solutions',
        templateUrl: 'solutions/solutions.html',
        controller: 'SolutionsController'        
      },
      {
        path: '/solutions/:solutionId',
        templateUrl: 'solutions/solution.html',
        controller: 'SolutionController'
      },
      {
        path: '/blv',
        templateUrl: 'blv/requests.html',
        controller: 'BlvRequestsController'
      },
      {
        path: '/blv-workflow/:blvViewType/:masterRequestId',
        templateUrl: 'blv/blv-workflow/blv.html',
        controller: 'BlvController'
      },
      {
        path: '/blv-business/:blvViewType/:masterRequestId',
        templateUrl: 'blv/blv-business/blv.html',
        controller: 'BlvController'
      },
      {
        path: '/services-nav', 
        templateUrl: 'layout/services-nav.html'
      },
      {
        path: '/services',
        templateUrl: 'services/services.html',
        controller: 'ServicesController'
      },
      {
        path: '/services/:servicePath',
        templateUrl: 'services/swagger.html',
        controller: 'ServiceController'
      },      
      {
        path: '/services/:servicePath/:serviceSubPath',
        templateUrl: 'services/swagger.html',
        controller: 'ServiceController'
      },
      {
        path: '/services/:servicePath/:serviceSubPath/:serviceSub2',
        templateUrl: 'services/swagger.html',
        controller: 'ServiceController'
      },
      {
        path: '/servicesCombined',
        templateUrl: 'services/swagger.html?combined',
        controller: 'CombinedServiceController'
      },      
      {
        path: '/httpHelper',
        templateUrl: 'services/httpHelper.html',
        controller: 'ServicesController'
      }
   ]
  };
});
