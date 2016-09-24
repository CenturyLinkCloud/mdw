// Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
'use strict';

var tabsSvc = angular.module('tabs', ['mdw']);

tabsSvc.factory('tabs', ['mdw', function(mdw) {
  return {
    def: [
      {
        id: 'tasksTab',
        label: 'Tasks',     
        url: '#/tasks',
        routes: ['/tasks'],
        condition: 'mdw.tasksUi != "mdw-hub"'
      },
      {
        id: 'myTasksTab',
        label: 'My Tasks',
        url: $mdwHubRoot + '/taskList/myTasks',
        condition: 'user.workgroups && user.workgroups.length > 0 && mdw.tasksUi == "mdw-hub"'
      },
      {
        id: 'workgroupTasksTab',
        label: 'Workload',     
        url: $mdwHubRoot + '/taskList/workgroupTasks',
        condition: 'user.hasWorkgroupsOtherThanCommon() && mdw.tasksUi == "mdw-hub"'
      },
      {
        id: 'dashboardTab',
        label: 'Dashboard',   
        url: '#/dashboard/processes',
        routes: ['/dashboard/processes', 'dashboard/requests', 'dashboard/tasks', '/dashboard/activities']
      },
      {
        id: 'workflowTab',
        label: 'Workflow',
        url: '#/workflow/processes',
        routes: ['/workflow/processes', 'workflow/definition', '/workflow/requests', '/workflow/activities', 
                 '/solutions', '/blv','/blv/workflow','/blv/business']
      },
      {
        id: 'servicesTab',
        label: 'Services',
        url: '#/services',
        routes: ['/services', 'serviceRequests', '/httpHelper'],
        guestAccess: true
      },
      {
        id: 'adminTab',
        label: 'Admin',
        url: '#/users',
        routes: ['/users', '/groups', '/roles', '/assets', '/packages', '/asset', '/tests', '/history']
      },
      {
        id: 'systemTab',
        label: 'System',
        url: $mdwHubRoot + '/system/systemInformation.jsf',
        condition: 'user.hasRole("Site Admin")'
      }
    ]
  };
}]);
