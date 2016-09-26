// Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
'use strict';

var inspectorTabSvc = angular.module('mdwInspectorTabs', ['mdw']);

inspectorTabSvc.factory('InspectorTabs', ['mdw', function(mdw) {
  return {
    definition: {
      process: {
        Definition: {
          Name: 'name',
          Description: 'description',
          Created: 'created' 
        },
        Variables: { 'variables': {
          Name: '=',
          Type: 'type',
          Mode: 'category'
        }},
        Attributes: 'attributes',
        Versions: {},
        Documentation: {},
        Monitoring: {}
      },
      activity: {
        Definition: {
          ID: 'id',
          Name: 'name',
          Implementor: 'implementor',
          Description: 'description'
        },
        Attributes: 'attributes',
        Documentation: {},
        Monitoring: {},
        Stubbing: {}
      },
      subprocess: {
        Definition: {
          ID: 'id',
          Name: 'name',
          Description: 'description'
        },
        Attributes: 'attributes',
        Documentation: {}
      }
    },
    instance: {
      process: {
        Instance: {
          'Master Request': 'masterRequestId',
          ID: 'id',
          Status: 'status',
          Start: 'startDate',
          End: 'endDate',
          Label: 'comments'
        },
        Values: { 'variables': [{
          Name: 'name',
          Value: 'value',
          Type: 'type'
        }]},
        Source: {
          'Initiated By': 'owner',
          ID: 'ownerId'
        }
      },
      activity: {
        Instances: [{
          ID: 'id',
          Status: 'status',
          Start: 'startDate',
          End: 'endDate',
          Result: 'statusMessage'
        }],
        Subprocesses: { // TODO: better condition
          condition: 'runtimeInfo.length > 0 && (workflowObject.implementor == "com.centurylink.mdw.workflow.activity.process.InvokeSubProcessActivity" || workflowObject.implementor == "com.centurylink.mdw.workflow.activity.process.InvokeHeterogeneousProcessActivity")'
        },
        Tasks: { // TODO: better condition
          condition: 'runtimeInfo.length > 0 && (workflowObject.implementor == "com.centurylink.mdw.workflow.activity.task.AutoFormManualTaskActivity" || workflowObject.implementor == "com.centurylink.mdw.workflow.activity.task.CustomManualTaskActivity")'
        },
        Subtasks: { // TODO: better condition
          condition: 'runtimeInfo.length > 0 && (workflowObject.implementor == "com.centurylink.mdw.workflow.activity.task.AutoFormManualTaskActivity" || workflowObject.implementor == "com.centurylink.mdw.workflow.activity.task.CustomManualTaskActivity")'
        }
      },
      subprocess: {
        Instances: [{
          ID: 'id',
          Status: 'status',
          Start: 'startDate',
          End: 'endDate'
        }]
      }
    },
    attributes: {
      Documentation: { exclude: true },
      Rule: { alias: 'Script', langAttr: 'SCRIPT'},
      SCRIPT: { alias: 'Language' }
    },
    assetAttrs: [
      // beyond those identified by _assetVersion
      'TASK_PAGELET' 
    ]
  }
}]);
