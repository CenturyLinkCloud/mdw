'use strict';

var tabsSvc = angular.module('tabs', ['mdw']);

tabsSvc.factory('tabs', ['mdw', function(mdw) {
  return {
    def: [
      {
        id: 'workflowTab',
        label: 'Workflow',
        url: '#/workflow/processes',
        routes: ['/workflow', '/solutions', '/workflow/processes']
      },
      {
        id: 'dashboardTab',
        label: 'Dashboard',
        url: '#/dashboard/processes',
        routes: ['/dashboard/processes', 'dashboard/requests', 'dashboard/tasks', '/dashboard/activities']
      },
      {
        id: 'serviceTab',
        label: 'Services',
        url: '#/serviceApi',
        routes: ['/serviceApi', '/serviceRequests', '/httpHelper'],
        guestAccess: true
      },
      {
        id: 'tasksTab',
        label: 'Tasks',     
        url: '#/tasks',
        routes: ['/tasks']
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
        url: '#/system/sysInfo',
        routes: ['/system'],
        condition: 'user.hasRole("Site Admin")'
      }
    ]
  };
}]);
