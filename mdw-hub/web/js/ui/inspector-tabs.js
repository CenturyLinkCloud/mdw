'use strict';

/* 
 * Default evaluation context for 'definitions' tabs is workflowObject,
 * and for 'instance' tabs its runtimeInfo.
 *  
 * Formats for tab declarations are described below under the first
 * example of each type.
 */

var inspectorTabSvc = angular.module('mdwInspectorTabs', ['mdw']);

inspectorTabSvc.factory('InspectorTabs', ['$http', '$q', 'mdw', function($http, $q, mdw) {
  return {
    definition: {
      process: {
        /* plain object:
         * List where prop names are labels and prop values are evaluated.
         */
        Definition: {
          Name: 'name',
          Description: 'description',
          Created: 'created' 
        },
        /* named object:
         * Tabular content evaluated against named obj collection (eg: 'variables'),
         * with prop names being labels and prop values evaluated.
         * Note: '=' refers to the identity (name) of each obj in collection
         */
        Variables: { 'variables': {
          Name: '=',
          Type: 'type',
          Mode: 'category'
        }},
        /* string:
         * Evaluated string refers to obj collection for list-type display.
         * Note: 'attributes' and 'assetAttrs' objects at the bottom of file
         * designate special behavior for assets (TODO: refactor).
         */
        Attributes: 'attributes',
        Versions: {},
        Documentation: { 
          '_attribute': { name: 'Documentation', markdown: true }
        },
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
      },
      transition: {
        Definition: {
          ID: 'id',
          Result: 'resultCode',
          Event: 'event'
        },
        Attributes: 'attributes'
      },
      textNote: {
        Definition: {
          ID: 'id',
          Text: 'content'
        }
      }
    },
    instance: {
      process: {
        Instance: {
          'Master Request': 'masterRequestId',
          '_url': '${"#/workflow/masterRequests/" + it.masterRequestId}',
          ID: 'id',
          Status: 'status',
          Start: 'startDate',
          End: 'endDate',
          Label: 'comments'
        },
        /* named one-item array of object:
         * Name evaluates to an array which is displayed as a table with
         * each item's name/value designated by the array's single object.
         */
        Values: { 'variables': [{
          Name: 'name',
          Value: 'value',
          Type: 'type'
        }]},
        Source: {
          'Initiated By': 'owner',
          ID: 'ownerId',
          '_url': '${it.owner == "PROCESS_INSTANCE" ? "#/workflow/processes/" + it.ownerId : (it.owner == "ERROR" ? "#/workflow/triggers/" + it.ownerId : "#/workflow/requests/" + it.ownerId)}'
        }
      },
      activity: {
        /* unnamed one-item array of object:
         * Displayed as list where each instance in runtimeInfo is
         * evaluation context (prop name = label, prop value = eval). 
         */
        Instances: [{
          ID: 'id',
          '_url': '${"#/workflow/activities/" + it.id}',
          Status: 'status',
          Start: 'startDate',
          End: 'endDate',
          Result: 'statusMessage'
        }],
        /* named one-item array, followed by function:
         * Function returns an array of promises from which are extracted
         * the JSON array with the corresponding (eg: 'processInstances').
         * From these arrays each item is evaluated using the standard
         * rules of prop name = label and prop value = eval.
         * 
         * Note: Illustrates special '_url' key and also $-expressions
         * (where 'it' designates the instance item being iterated over).
         */
        Subprocesses: {
          'processInstances': [{
            ID: 'id',
            '_url': '${"#/workflow/processes/" + it.id}',
            Name: '${it.processName + " v" + it.processVersion}',
            Status: 'status',
            Start: 'startDate',
            End: 'endDate'
          }],
          'getSubprocesses': function(diagramObject, workflowObject, runtimeInfo) {
            if (typeof runtimeInfo == 'undefined') {
              // no runtimeInfo means just checking for tab applicability
              return diagramObject.workflowType == 'activity' && 
                diagramObject.implementor && diagramObject.implementor.category == 'com.centurylink.mdw.activity.types.InvokeProcessActivity';        
            }
            else {
              if (runtimeInfo === null || runtimeInfo.length === 0)
                return null;
              var subprocs = [];
              if (workflowObject.attributes.processname) {
                subprocs.push(workflowObject.attributes.processname + ' v' + workflowObject.attributes.processversion);
              }
              else if (workflowObject.attributes.processmap) {
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
        },
        Tasks: {
          'tasks': [{
            ID: 'id',
            '_url': '${"#/tasks/" + it.id}',
            Name: 'name',
            Status: 'status',
            Assignee: 'assignee',
            Created: 'startDate',
            Due: 'dueDate',
            Completed: 'endDate'
          }],
          'getTasks': function(diagramObject, workflowObject, runtimeInfo) {
            if (typeof runtimeInfo == 'undefined') {
              // no runtimeInfo means just checking for tab applicability
              return diagramObject.workflowType == 'activity' && 
                diagramObject.implementor && diagramObject.implementor.category == 'com.centurylink.mdw.activity.types.TaskActivity';        
            }
            else {
              if (runtimeInfo === null || runtimeInfo.length === 0)
                return null;
              var url = mdw.roots.services + '/services/Tasks?processInstanceId=' + runtimeInfo[0].processInstanceId + '&activityInstanceIds=[';
              for (var i = 0; i < runtimeInfo.length; i++) {
                url += runtimeInfo[i].id;
                if (i < runtimeInfo.length - 1)
                  url += ",";
              }
              url += ']&sort=startDate&descending=true';
              return $http.get(url);
            }
          }
        },
        Requests: {
          'requests': [{
            ID: 'id',
            '_url': '${"#/workflow/requests/" + it.id}',
            Sent: 'created',
            Responded: 'responded'
          }],
          'getRequests': function(diagramObject, workflowObject, runtimeInfo) {
            if (typeof runtimeInfo == 'undefined') {
              // no runtimeInfo means just checking for tab applicability
              return diagramObject.workflowType == 'activity' && 
                diagramObject.implementor && diagramObject.implementor.category == 'com.centurylink.mdw.activity.types.AdapterActivity';        
            }
            else {
              if (runtimeInfo === null || runtimeInfo.length === 0)
                return null;
              var url = mdw.roots.services + '/services/Requests?type=outboundRequests&ownerIds=[';              
              for (var i = 0; i < runtimeInfo.length; i++) {
                url += runtimeInfo[i].id;
                if (i < runtimeInfo.length - 1)
                  url += ",";
              }
              url += ']&descending=true';
              return $http.get(url);
            }
          }
        },
        Responses: {
          'requests': [{
            ID: 'responseId',
            '_url': '${"#/workflow/responses/" + it.id}',
            Sent: 'created',
            Responded: 'responded'
          }],
          'getResponses': function(diagramObject, workflowObject, runtimeInfo) {
            if (typeof runtimeInfo == 'undefined') {
              // no runtimeInfo means just checking for tab applicability
              return diagramObject.workflowType == 'activity' && 
                diagramObject.implementor && diagramObject.implementor.category == 'com.centurylink.mdw.activity.types.AdapterActivity';        
            }
            else {
              if (runtimeInfo === null || runtimeInfo.length === 0)
                return null;
              var url = mdw.roots.services + '/services/Requests?type=outboundRequests&ownerIds=[';              
              for (var i = 0; i < runtimeInfo.length; i++) {
                url += runtimeInfo[i].id;
                if (i < runtimeInfo.length - 1)
                  url += ",";
              }
              url += ']&descending=true';
              return $http.get(url);
            }
          }
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
    // TODO: refactor this into Attributes tab itself
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
