// Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
'use strict';

var inspectorTabSvc = angular.module('mdwInspectorTabs', ['mdw']);

inspectorTabSvc.factory('InspectorTabs', ['$http', '$q', 'mdw', function($http, $q, mdw) {
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
        Subprocesses:  {
          'processInstances': [{
            ID: 'id',
            '_url': '${"#/workflow/processes/" + it.id}',
            Name: '${it.processName + " v" + it.processVersion}',
            Status: 'status',
            Start: 'startDate',
            End: 'endDate'
          }],
          'getSubprocesses': function(workflowType, workflowObject, runtimeInfo) {
            if (typeof runtimeInfo == 'undefined') {
              // no runtimeInfo means just checking for tab applicability
              return workflowType == 'activity' &&
                (workflowObject.implementor == 'com.centurylink.mdw.workflow.activity.process.InvokeSubProcessActivity' ||
                   workflowObject.implementor == 'com.centurylink.mdw.workflow.activity.process.InvokeHeterogeneousProcessActivity');        
            }
            else {
              if (runtimeInfo === null || runtimeInfo.length === 0)
                return [];
              var subprocs = [];
              if (workflowObject.implementor == 'com.centurylink.mdw.workflow.activity.process.InvokeSubProcessActivity') {
                subprocs.push(workflowObject.attributes.processname + ' v' + workflowObject.attributes.processversion);
              }
              else if (workflowObject.implementor == 'com.centurylink.mdw.workflow.activity.process.InvokeHeterogeneousProcessActivity') {
                var attr = workflowObject.attributes.processmap;
                if (attr) {
                  var specs = attr.split(';');
                  specs.forEach(function(spec) {
                    spec = spec.replace('\\,', '~');
                    var segments = spec.split(',');
                    subprocs.push(segments[1] + ' v' + segments[2].replace('~', ','));
                  });
                }
              }
              var gets = [];
              subprocs.forEach(function(subproc) {
                var url = mdw.roots.services + '/services/Processes?definition=' + encodeURIComponent(subproc) + '&owner=PROCESS_INSTANCE&ownerId=';
                runtimeInfo.forEach(function(instance) {
                  gets.push($http.get(url + instance.processInstanceId));
                });
              });
              return $q.all(gets);
            }
          }
        }
//        Tasks: { // TODO: better condition
//          condition: 'runtimeInfo.length > 0 && (workflowObject.implementor == "com.centurylink.mdw.workflow.activity.task.AutoFormManualTaskActivity" || workflowObject.implementor == "com.centurylink.mdw.workflow.activity.task.CustomManualTaskActivity")'
//        },
//        Subtasks: { // TODO: better condition
//          condition: 'runtimeInfo.length > 0 && (workflowObject.implementor == "com.centurylink.mdw.workflow.activity.task.AutoFormManualTaskActivity" || workflowObject.implementor == "com.centurylink.mdw.workflow.activity.task.CustomManualTaskActivity")'
//        }
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
  };
}]);
