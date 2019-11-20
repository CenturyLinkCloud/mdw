'use strict';

/* 
 * Default evaluation context for 'definitions' tabs is workflowObject,
 * and for 'instance' tabs its runtimeInfo.
 *  
 * Formats for tab declarations are described below under the first
 * example of each type.
 */

var inspectorTabSvc = angular.module('mdwInspectorTabs', ['mdw']);

inspectorTabSvc.factory('InspectorTabs', ['$http', '$q', 'mdw', 'Compatibility', 
                                          function($http, $q, mdw, Compatibility) {
  return {
    definition: {
      process: {
        /* plain object:
         * List where prop names are labels and prop values are evaluated.
         */
        Definition: {
          '_template': mdw.roots.hub + '/js/ui/templates/processDefinition.json'
        },
        Design: {
          '_template': mdw.roots.hub + '/js/ui/templates/process.json'
        },
        /* named object:
         * Tabular content evaluated against named obj collection (eg: 'variables'),
         * with prop names being labels and prop values evaluated.
         * Note: '=' refers to the identity (name) of each obj in collection
         */
        Variables: {
          '_template': mdw.roots.hub + '/js/ui/templates/variables.json'
        },
        Request: {
          '_attribute': { name: 'Request' },
          '_template': mdw.roots.hub + '/js/ui/templates/request.json'
        },
        Documentation: { 
          '_attribute': { name: 'Documentation', markdown: true },
          '_template': mdw.roots.hub + '/js/ui/templates/documentation.json'
        },
        Monitoring: {
          '_template': mdw.roots.services + '/template/configurator/processMonitoring.json'
        },
        Hierarchy: {
          'hierarchy': [{
            Process: '${it.treeLabel}',
            Definition: '${it.id}',
            '_url': '${"#/workflow/definitions/" + it.packageName + "/" + it.name + "/" + it.version}',
            '': '${it.thisFlag}'
          }],
          'getHierarchyList': function(diagramObject, workflowObject) {
            var url = mdw.roots.services + '/services/Workflow?callHierarchyFor=' + workflowObject.definitionId + '&app=mdw-admin';
            return $http.get(url).then(function(response) {
              if (response.status !== 200)
                return null;
              var result = {
                  status: response.status,
                  maxWidth: 150,
                  data: {
                    hierarchy: []
                  }
              };
              var top;
              var pad;
              var addChildren = function(caller) {
                if (caller.process) {
                  var treeLabel = pad;
                  if (!top) {
                    treeLabel += '- ';
                  }
                  treeLabel += caller.process.packageName + "/" + caller.process.name + " v" + caller.process.version;
                  if (caller.circular)
                    treeLabel += ' (+)';
                  caller.process.treeLabel = treeLabel;
                  caller.process.thisFlag = caller.process.id === workflowObject.definitionId ? '*' : '';
                  result.data.hierarchy.push(caller.process);
                }
                if (caller.children) {
                  pad += '  ';
                  if (top) {
                    top = false;
                  }
                  else {
                    pad += '  ';
                  }
                  caller.children.forEach(function(child) {
                    addChildren(child);
                  });
                  pad = pad.substring(4);
                }
              };

              response.data.hierarchy.forEach(function(caller) {
                top = true;
                pad = '';
                addChildren(caller);
              });
              return result;
            });
          }
        }
      },
      activity: {
        Definition: {
          ID: 'id',
          Name: 'name',
          Implementor: 'implementor',
          Description: 'description',
          '_template': mdw.roots.hub + '/js/ui/templates/activityDefinition.json'
        },
        Design: {
          '_template': mdw.roots.services + '/services/Implementors/${it.implementor}?app=mdw-admin'
        },
        Events: {
          '_template': mdw.roots.services + '/services/Implementors/${it.implementor}?app=mdw-admin',
          '_categories': ['com.centurylink.mdw.activity.types.TaskActivity', 'com.centurylink.mdw.activity.types.InvokeProcessActivity', 'com.centurylink.mdw.activity.types.SynchronizationActivity', 'com.centurylink.mdw.activity.types.PauseActivity', 'com.centurylink.mdw.activity.types.DependenciesWaitActivity']
        },
        Recipients: {
          '_template': mdw.roots.services + '/services/Implementors/${it.implementor}?app=mdw-admin',
          '_categories': ['com.centurylink.mdw.activity.types.NotificationActivity']
        },
        Authentication: {
          '_template': mdw.roots.services + '/services/Implementors/${it.implementor}?app=mdw-admin',
          '_categories': ['com.centurylink.mdw.activity.types.AdapterActivity']
        },
        Script: {
          '_template': mdw.roots.services + '/services/Implementors/${it.implementor}?app=mdw-admin',
          '_categories': ['com.centurylink.mdw.activity.types.AdapterActivity']
        },
        Documentation: { 
          '_attribute': { name: 'Documentation', markdown: true },
          '_template': mdw.roots.hub + '/js/ui/templates/documentation.json'
        },
        Monitoring: {
          '_template': mdw.roots.services + '/services/Implementors/${it.implementor}?app=mdw-admin'
        },
        /* string:
         * Evaluated string refers to obj collection for list-type display.
         * Note: 'attributes' and 'assetAttrs' objects at the bottom of file
         * designate special behavior for assets (TODO: refactor).
         */
        Attributes: 'attributes'
      },
      subprocess: {
        Definition: {
          ID: 'id',
          Name: 'name',
          Description: 'description',
          '_template': mdw.roots.hub + '/js/ui/templates/processDefinition.json'          
        },
        Attributes: 'attributes',
        Documentation: { 
          '_attribute': { name: 'Documentation', markdown: true },
          '_template': mdw.roots.hub + '/js/ui/templates/documentation.json'          
        }
      },
      transition: {
        Definition: {
          ID: 'id',
          Result: 'resultCode',
          Event: 'event',
          '_template': mdw.roots.hub + '/js/ui/templates/transitionDefinition.json'
        },
        Design: {
          '_template': mdw.roots.hub + '/js/ui/templates/transition.json'
        },
        Attributes: 'attributes'
      },
      textNote: {
        Definition: {
          ID: 'id',
          Text: 'content',
          '_template': mdw.roots.hub + '/js/ui/templates/textNote.json'          
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
        Source: {
          'Initiated By': 'owner',
          ID: 'ownerId',
          '_url': '${it.owner == "PROCESS_INSTANCE" ? "#/workflow/processes/" + it.ownerId : (it.owner == "ERROR" ? "#/workflow/triggers/" + it.ownerId : "#/workflow/requests/" + it.ownerId)}'
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
        /*
         * see Subprocesses below
         */
        'Service Summary': {
          'serviceList': [{
            'Service': '${it.name}',
            'ID': '${it.id}',
            '_url': '${it.url}',
            'When': '${it.started}',
            'Status': '${it.status}',
            '': '${it.thisFlag}'
          }],
          'getServiceList': function(diagramObject, workflowObject, runtimeInfo) {
            // TODO: non-standard service summary variable name
            if (typeof runtimeInfo == 'undefined') {
              // no runtimeInfo means just checking for tab applicability
              return workflowObject.variables && workflowObject.variables.serviceSummary;
            }
            else {
              if (runtimeInfo === null || !runtimeInfo.variables)
                return null;
              var serviceSummary = runtimeInfo.variables.find(function(variable) {
                return variable.name === 'serviceSummary'; 
              });
              if (!serviceSummary)
                return null;
              var url = mdw.roots.services + '/services/com/centurylink/mdw/microservice/summary/' + serviceSummary.value.substring(9) + '-' + runtimeInfo.id + '?app=mdw-admin';
              return $http.get(url).then(function(response) {
                if (response.status !== 200)
                  return null;
                var result = { 
                    status: response.status,
                    maxWidth: 150,
                    data: {
                      serviceList: []
                    }
                };
                var formatDate = function(date) {
                  let now = new Date();
                  let str = (date.getMonth() + 1) + '/' + date.getDate();
                  if (date.getFullYear() != now.getFullYear()) {
                    str += '/' + date.getFullYear();
                  }
                  str += ' ' + date.getHours() + ':' + date.getMinutes() + ':' + date.getSeconds() + '.' + date.getMilliseconds(); 
                  return str;
                };
                var parseMicroservices = function(serviceSummary) {
                  var microservices = serviceSummary.microservices;
                  var subServiceSummaries = serviceSummary.subServiceSummaries;
                  var list = [];
                  if (microservices) {
                    microservices.forEach(function(microservice) {
                      if (microservice.instances) {
                        microservice.instances.forEach(function(instance) {
                          list.push({
                            name: microservice.name,
                            id: instance.id,
                            url: '#/workflow/processes/' + instance.id,
                            started: instance.triggered ? formatDate(new Date(instance.triggered)) : null,
                            status: instance.status,
                            thisFlag: instance.id === runtimeInfo.id ? '*' : ''
                          });
                          instance.invocations.forEach(function(invocation) {
                            list.push({
                              name: '  invocation',
                              id: invocation.requestId,
                              url: '#/workflow/requests/' + invocation.requestId,
                              started: invocation.sent ? formatDate(new Date(invocation.sent)) : null,
                              status: invocation.status ? (invocation.status.code + ": " + invocation.status.message) : null
                            });
                          });
                          if (instance.updates) {
                            instance.updates.forEach(function(update) {
                              list.push({
                                name: '  update',
                                id: update.requestId,
                                url: '#/workflow/requests/' + update.requestId,
                                started: update.received ? formatDate(new Date(update.received)) : null,
                                status: update.status ? (update.status.code + ": " + update.status.message) : null
                              });
                            });
                          }
                        });
                      }
                    });
                  }
                  if (subServiceSummaries) {
                    subServiceSummaries.forEach(function(subSummary) {
                      list = list.concat(parseMicroservices(subSummary));
                    });
                  }
                  return list;
                };
                // populate the serviceList pseudo array
                var serviceSummary = response.data;
                result.data.serviceList = parseMicroservices(serviceSummary);
                return result;
              });
            }
          }
        },
        Hierarchy: {
          'hierarchyInstances': [{
            Process: '${it.treeLabel}',
            ID: '${it.id}',
            '_url': '${"#/workflow/processes/" + it.id}',
            '': '${it.thisFlag}',
            Status: '${it.status}'
          }],
          'getHierarchyList': function(diagramObject, workflowObject, runtimeInfo) {
            if (typeof runtimeInfo == 'undefined') {
              // no runtimeInfo means just checking for tab applicability
              return true;
            }
            else {
              if (runtimeInfo === null)
                return null;
              var url = mdw.roots.services + '/services/Processes?callHierarchyFor=' + runtimeInfo.id + '&app=mdw-admin';
              return $http.get(url).then(function(response) {
                if (response.status !== 200)
                  return null;
                var result = { 
                    status: response.status,
                    maxWidth: 150,
                    data: {
                      hierarchyInstances: []
                    }
                };
                var top = true;
                var pad = '';
                var addChildren = function(caller) {
                  if (caller.processInstance) {
                    var treeLabel = pad;
                    if (!top) {
                      treeLabel += '- ';
                    }
                    treeLabel += caller.processInstance.packageName + "/" + caller.processInstance.processName + " v" + caller.processInstance.processVersion;
                    caller.processInstance.treeLabel = treeLabel;
                    caller.processInstance.thisFlag = caller.processInstance.id === runtimeInfo.id ? '*' : '';
                    result.data.hierarchyInstances.push(caller.processInstance);
                  }
                  if (caller.children) {
                    pad += '  ';
                    if (top) {
                      top = false;
                    }
                    else {
                      pad += '  ';
                    }
                    caller.children.forEach(function(child) {
                      addChildren(child);
                    });
                    pad = pad.substring(4);
                  }
                };

                addChildren(response.data);
                return result;
              });
            }
          }
        },
        Log: {
          'logLines': [{
            Activity: 'activityInstanceId',
            '_url': '${"#/workflow/activities/" + it.activityInstanceId}',
            Level: '${it.level}',
            When: '${it.when}',
            Thread: '${it.thread}',
            Log: '${it.message}'
          }],
          'getLogLines': function(diagramObject, workflowObject, runtimeInfo) {
            if (typeof runtimeInfo == 'undefined') {
              // no runtimeInfo means just checking for tab applicability
              return true;
            }
            else {
              if (runtimeInfo === null)
                return null;
              var url = mdw.roots.services + '/api/Processes/' + runtimeInfo.id + '/log?withActivities=true&app=mdw-admin';
              return $http.get(url).then(function(response) {
                 if (response.status !== 200)
                   return null;
                 var logLines = response.data.logLines;
                 var logLineDt;
                 var padTwo = function(n) {
                   return '' + (n < 10 ? '0' + n : n);
                 };
                 logLines.forEach(function(logLine) {
                   // format date/time
                   logLineDt = new Date(logLine.when);
                   logLine.when = logLineDt.getFullYear() + '-' + padTwo(logLineDt.getMonth() + 1) + '-' + padTwo(logLineDt.getDate()) +
                       ' ' + padTwo(logLineDt.getHours()) + ':' + padTwo(logLineDt.getMinutes()) + ':' + padTwo(logLineDt.getSeconds()) +
                       '.' + (logLineDt.getMilliseconds() < 100 ? '0' + padTwo(logLineDt.getMilliseconds()) : '' + logLineDt.getMilliseconds());
                   logLine.message = logLine.message.replace(/\n/g, '\\n');
                 });
                 return {
                     status: response.status,
                     maxWidth: 150,
                     data: {
                       logLines: logLines
                     }
                 };
               });
            }
          }
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
          Result: 'message'
        }],
        /* named one-item array, followed by function:
         * Function returns an array of promises from which are extracted
         * the JSON array with the corresponding name (eg: 'processInstances').
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
                diagramObject.implementor && (diagramObject.implementor.category == 'com.centurylink.mdw.activity.types.InvokeProcessActivity' || diagramObject.implementor.category == 'com.centurylink.mdw.activity.types.OrchestratorActivity');
            }
            else {
              if (runtimeInfo === null || runtimeInfo.length === 0)
                return null;
              var gets = [];
              if (workflowObject.attributes.processname || workflowObject.attributes.processmap) {
                // traditional subproc activity
                var subprocs = [];
                if (workflowObject.attributes.processname) {
                  subprocs.push(workflowObject.attributes.processname + ' v' + workflowObject.attributes.processversion);
                }
                else if (workflowObject.attributes.processmap) {
                  var attr = workflowObject.attributes.processmap;
                  if (attr) {
                    var subprocTbl = Compatibility.getTable(attr);
                    for (let i = 0; i < subprocTbl.length; i++) {
                      var subprocRow = subprocTbl[i];
                      var subprocSpec = subprocRow[1] + ' v' + subprocRow[2];
                      if (!subprocs.includes(subprocSpec)) {
                        subprocs.push(subprocSpec);
                      }
                    }
                  }
                }
                subprocs.forEach(function(subproc) {
                  var url = mdw.roots.services + '/services/Processes?definition=' + encodeURIComponent(subproc) + '&app=mdw-admin&owner=PROCESS_INSTANCE&secondaryOwner=ACTIVITY_INSTANCE&ownerId=';
                  runtimeInfo.forEach(function(instance) {
                    var processInstanceId = instance.embeddedProcessInstanceId ? instance.embeddedProcessInstanceId : instance.processInstanceId;
                    gets.push($http.get(url + processInstanceId + '&secondaryOwnerId=' + instance.id));
                  });
                });
              }
              else if ((diagramObject.diagram.process.variables && diagramObject.diagram.process.variables.serviceSummary) || workflowObject.attributes.serviceSummaryVariable) {
                // orchestrator with serviceSummary
                var serviceSummaryVarName = 'serviceSummary';
                if (workflowObject.attributes.serviceSummaryVarName) {
                  serviceSummaryVarName = workflowObject.attributes.serviceSummaryVarName;
                }
                var serviceSummary = diagramObject.diagram.instance.variables.find(function(variable) {
                  return variable.name === serviceSummaryVarName;
                });
                if (serviceSummary) {
                  gets.push($http.get(mdw.roots.services + '/services/com/centurylink/mdw/microservice/summary/' + serviceSummary.value.substring(9) + '-' + runtimeInfo[0].id + '/subflows?app=mdw-admin'));
                }
              }
              return $q.all(gets);
            }
          }
        },
        Tasks: {
          'tasks': [{
            ID: 'id',
            '_url': '${"#/tasks/" + it.id}',
            Name: 'name',
            Title: 'title',
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
              var url = mdw.roots.services + '/services/Tasks?processInstanceId=' + runtimeInfo[0].processInstanceId + '&app=mdw-admin&activityInstanceIds=%5B';
              for (var i = 0; i < runtimeInfo.length; i++) {
                url += runtimeInfo[i].id;
                if (i < runtimeInfo.length - 1)
                  url += ",";
              }
              url += '%5D&sort=startDate&descending=true';
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
                diagramObject.implementor &&
                    (diagramObject.implementor.category == 'com.centurylink.mdw.activity.types.AdapterActivity' ||
                      (diagramObject.implementor.category == 'com.centurylink.mdw.activity.types.TaskActivity' &&
                        diagramObject.activity && diagramObject.activity.attributes && diagramObject.activity.attributes.Monitors));
            }
            else {
              if (runtimeInfo === null || runtimeInfo.length === 0)
                return null;
              var url = mdw.roots.services + '/services/Requests?type=outboundRequests&app=mdw-admin&ownerIds=%5B';
              for (var i = 0; i < runtimeInfo.length; i++) {
                url += runtimeInfo[i].id;
                if (i < runtimeInfo.length - 1)
                  url += ",";
              }
              url += '%5D&descending=true';
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
                return diagramObject.implementor &&
                    (diagramObject.implementor.category == 'com.centurylink.mdw.activity.types.AdapterActivity' ||
                      (diagramObject.implementor.category == 'com.centurylink.mdw.activity.types.TaskActivity' &&
                        diagramObject.activity && diagramObject.activity.attributes && diagramObject.activity.attributes.Monitors));
            }
            else {
              if (runtimeInfo === null || runtimeInfo.length === 0)
                return null;
              var url = mdw.roots.services + '/services/Requests?type=outboundRequests&app=mdw-admin&ownerIds=%5B';
              for (var i = 0; i < runtimeInfo.length; i++) {
                url += runtimeInfo[i].id;
                if (i < runtimeInfo.length - 1)
                  url += ",";
              }
              url += '%5D&descending=true';
              return $http.get(url);
            }
          }
        },
        ServiceSummary: {
          '_template': mdw.roots.hub + '/js/ui/templates/stubbing.json'
        },
        Log: {
          'logLines': [{
            ID: 'activityInstanceId',
            '_url': '${"#/workflow/activities/" + it.activityInstanceId}',
            Level: '${it.level}',
            When: '${it.when}',
            Thread: '${it.thread}',
            Log: '${it.message}'
          }],
          'getLogLines': function(diagramObject, workflowObject, runtimeInfo) {
            if (typeof runtimeInfo == 'undefined') {
              // no runtimeInfo means just checking for tab applicability
              return true;
            }
            else {
              if (runtimeInfo === null || runtimeInfo.length === 0 || !diagramObject.diagram || !diagramObject.diagram.instance)
                return null;
              var procInstId = diagramObject.diagram.instance.id;
              var actInstIds = '%5B';
              for (let i = 0; i < runtimeInfo.length; i++) {
                actInstIds += runtimeInfo[i].id;
                if (i < runtimeInfo.length - 1)
                  actInstIds += ',';
              }
              actInstIds += '%5D';
              var url = mdw.roots.services + '/api/Processes/' + procInstId + '/log?activityInstanceIds=' + actInstIds + '&app=mdw-admin';
              return $http.get(url).then(function(response) {
                 if (response.status !== 200)
                   return null;
                 var logLines = response.data.logLines;
                 var logLineDt;
                 var padTwo = function(n) {
                   return '' + (n < 10 ? '0' + n : n);
                 };
                 logLines.forEach(function(logLine) {
                   // format date/time
                   logLineDt = new Date(logLine.when);
                   logLine.when = logLineDt.getFullYear() + '-' + padTwo(logLineDt.getMonth() + 1) + '-' + padTwo(logLineDt.getDate()) +
                       ' ' + padTwo(logLineDt.getHours()) + ':' + padTwo(logLineDt.getMinutes()) + ':' + padTwo(logLineDt.getSeconds()) +
                       '.' + (logLineDt.getMilliseconds() < 100 ? '0' + padTwo(logLineDt.getMilliseconds()) : '' + logLineDt.getMilliseconds());
                   logLine.message = logLine.message.replace(/\n/g, '\\n');
                 });
                 return {
                     status: response.status,
                     maxWidth: 150,
                     data: {
                       logLines: logLines
                     }
                 };
               });
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
      },
      transition: {
        Instances: [{
          ID: 'id',
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
