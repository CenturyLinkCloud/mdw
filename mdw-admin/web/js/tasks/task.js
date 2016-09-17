// Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
'use strict';

var taskMod = angular.module('task', ['ngResource', 'mdw', 'ui.grid', 'ui.grid.selection', 'ngFileUpload', 'taskTemplates']);

// task summary
taskMod.controller('TaskController', ['$scope', '$route', '$routeParams', '$http', 'mdw', 'util', 'TaskUtil', 'Tasks', 'Task', 'TaskAction',
                                      function($scope, $route, $routeParams, $http, mdw, util, TaskUtil, Tasks, Task, TaskAction) {

  $scope.taskInstanceId = $routeParams.taskInstanceId;
  
  $scope.save = function() {
    console.log('saving task: ' + $scope.task.id);
    Tasks.update({taskInstanceId: $scope.task.id}, $scope.task,
      function(data) {
        if (data.status.code !== 0) {
          mdw.messages = data.status.message;
        }
        else {
          $scope.task.dirty = false;
        }
      }, 
      function(error) {
        mdw.messages = error.data.status.message;
      });
  };
  
  $scope.task = Task.retrieveTask($scope, $routeParams.taskInstanceId);
  
  $scope.random = Math.random(); // param to force image reload
  
  $scope.datePopups = {};
  $scope.openDueDatePopup = function() {
    $scope.datePopups.dueDate = true;
  };
  
  $scope.dueDateChanged = function() {
    $scope.task.dirty = true;
    TaskUtil.setTask($scope.task, $scope.task.dbDate);
    if ($scope.task.dueDate)
      $scope.task.dueInSeconds = Math.round(($scope.task.dueDate.getTime() - Date.now())/1000);
    else
      $scope.task.dueInSeconds = -1;
  };
  
  $scope.refreshWorkflowImage = function() {
    $route.reload();
  };
}]);

// task values
taskMod.controller('TaskValuesController', ['$scope', '$route', '$routeParams', 'mdw', 'Tasks', 'Task', 'DOCUMENT_TYPES',
                                          function($scope, $route, $routeParams, mdw, Tasks, Task, DOCUMENT_TYPES) {
  mdw.message = null;
  $scope.task = Task.getTask($scope);
  if (!$scope.task)
    $scope.task = Task.retrieveTask($scope, $routeParams.taskInstanceId);
  
  // retrieve task values
  var values = Tasks.get({taskInstanceId: $routeParams.taskInstanceId, extra: 'values'}, function() {
    // TODO: what happens on nav when dirty?
    $scope.task.dirty = false;
    
    $scope.task.values = [];
    if (values && typeof values === 'object') {
      // convert object into sorted array
      for (var key in values) {
        if (values.hasOwnProperty(key) && !key.startsWith('$')) {
          var val = values[key];
          val.name = key;
          if (!val.sequence)
            val.sequence = 0;
          val.isDocument = val.type && DOCUMENT_TYPES.indexOf(val.type) >= 0;
          if (val.isDocument) {
            val.showLines = 8;
            if (val.value && val.value.lineCount) {
              var lineCount = val.value.lineCount();
              if (lineCount > 25)
                val.showLines = 25;
              else if (lineCount > 8)
                val.showLines = lineCount();
            }
          }
          val.editable = $scope.task.editable && val.display !== 'ReadOnly';  
          $scope.task.values.push(val);
        }
      }
      $scope.task.values.sort(function(val1, val2) {
        var diff = val1.sequence - val2.sequence;
        if (diff === 0) {
          var label1 = val1.label ? val1.label : val1.name;
          var label2 = val2.label ? val2.label : val2.name;
          return label1.localeCompare(label2);
        }
        else {
          return diff;
        }
      });
    }
  });
  
  $scope.openDatePopup = function(field) {
    $scope.datePopups = {};
    $scope.datePopups[field] = true;
  };
  
  $scope.dirty = function(value) {
    value.dirty = true;
    $scope.task.dirty = true;
  };
  
  // save values
  $scope.save = function() {
    console.log('saving task values for instance: ' + $scope.task.id);
    var newValues = {};
    $scope.task.values.forEach(function(value) {
      if (value.display && value.display !== 'ReadOnly' && value.value)
        newValues[value.name] = value.value;
    });
    
    Tasks.update({taskInstanceId: $scope.task.id, extra: 'values'}, newValues,
      function(data) {
        if (data.status.code !== 0) {
          mdw.messages = data.status.message;
        }
        else {
          $route.reload();
        }
      }, 
      function(error) {
        mdw.messages = error.data.status.message;
      }
    );
  };
  
}]);

// subtasks
taskMod.controller('SubtasksController', ['$scope', '$routeParams', '$location', 'mdw', 'Tasks', 'Task', 'TaskUtil', 'TaskTemplates',
                                           function($scope, $routeParams, $location, mdw, Tasks, Task, TaskUtil, TaskTemplates) {
  mdw.message = null;
  $scope.task = Task.getTask($scope);
  if (!$scope.task)
    $scope.task = Task.retrieveTask($scope, $routeParams.taskInstanceId);
  
  $scope.subtaskList = Tasks.get({taskInstanceId: $routeParams.taskInstanceId, extra: 'subtasks'},
    function(data) {
      if (data.status && data.status.code !== 0) {
        mdw.messages = data.status.message;
      }
      else {
        if ($scope.subtaskList && $scope.subtaskList.subtasks) {
          var dbDate = new Date($scope.subtaskList.retrieveDate);
          $scope.subtaskList.subtasks.forEach(function(subtask) {
            TaskUtil.setTask(subtask, dbDate);
          });
        }
      }
    }, 
    function(error) {
      mdw.messages = error.data.status.message;
    }    
  );
  $scope.create = false;
  $scope.setCreate = function(create) {
    $scope.create = create;
    $scope.subtask = { template: '' }; // blank name helps w/applying error styles
  };
  
  $scope.cancel = function() {
    $scope.setCreate(false);
  };
  
  TaskTemplates.taskTemplatelist = TaskTemplates.get();
  $scope.findTemplate = function(typed) {
    var filteredTemplateList = [];
    var templateName;
    for (var i = 0; i < TaskTemplates.taskTemplatelist.taskTemplates.length; i++) {
        templateName = TaskTemplates.taskTemplatelist.taskTemplates[i].name;
        if (templateName.indexOf(typed) >= 0) {
            filteredTemplateList.push(TaskTemplates.taskTemplatelist.taskTemplates[i]);
        }
    }
    return filteredTemplateList;
  };

  $scope.save = function() {
    console.log('creating subtask: ' + $scope.subtask.template.logicalId);
    
    Tasks.create({}, {logicalId: $scope.subtask.template.logicalId, masterTaskInstanceId: $scope.task.id},
      function(data) {
        if (data.status && data.status.code !== 0) {
          $scope.message = data.status.message;
        }
        else {
          $scope.setCreate(false);
          $scope.subtaskList = Tasks.get({taskInstanceId: $routeParams.taskInstanceId, extra: 'subtasks'});  
          $location.path('/tasks/' + $scope.task.id + '/subtasks');
        }
      }, 
      function(error) {
        $scope.message = error.data.status.message;
      });
  };
  
  $scope.isSaveEnabled = function() {
    return $scope.subtask && $scope.subtask.template && $scope.subtask.template.name && $scope.subtask.template.name.length > 0;
  };
  
}]);

taskMod.controller('TaskNotesController', ['$scope', '$routeParams', '$location', '$timeout', '$interval', 'Notes', 'Tasks',
                                      function($scope, $routeParams, $location, $timeout, $interval, Notes, Tasks) {
   
  $scope.hovering = false;
  $scope.setHovering = function(note, hover) {
    $scope.hovering = hover;
    $scope.note = note;
    $scope.add = false;
  };

  $scope.gridOptions = { enableRowSelection: true, enableRowHeaderSelection: false };
  $scope.gridOptions.columnDefs = [
                                   { name: 'name', displayName: 'Summary' },
                                   { name: 'user', displayName: 'Modified By'},
                                   { name: 'date', displayName: 'Modified Date', type:'date', cellFilter: 'date:"MMM dd yyyy HH:MM a"'}
                                 ];
                                
  $scope.gridOptions.multiSelect = false;
  $scope.gridOptions.modifierKeysToMultiSelect = false;
  $scope.gridOptions.noUnselect = true;
  $scope.gridOptions.onRegisterApi = function( gridApi ) {
    $scope.gridApi = gridApi;
    gridApi.selection.on.rowSelectionChanged($scope,function(row){
      var msg = 'row selected ' + row.isSelected;
      $scope.note = row.entity;
      $scope.add = false;
    });

  };
  
  $scope.taskInstanceId = $routeParams.taskInstanceId;
  $scope.add = false;
  $scope.setAdd = function(add) {
    $scope.add = add;
    $scope.note = {};
  };
  $scope.edit = false;
  $scope.setEdit = function(note, edit) {
    $scope.edit = edit;
    $scope.note = note;
    if (edit) // backup original for cancel
      $scope.uneditedNote = Notes.shallowCopy({}, $scope.note, $scope.authUser.cuid, $scope.taskInstanceId);
  };
  
  $scope.confirm = false;
  $scope.isConfirm = function(note) {
    return note.confirm;
  };
  $scope.setConfirm = function(note, confirm) {
    $scope.note = note;
    $scope.note.confirm = confirm;
    $scope.note.message = confirm ? 'Delete note "' + $scope.note.name + '"?' : null;
  };
  $scope.deleteNote = function(note) {
    console.log('deleting note: ' + note.name);
    Notes.remove({noteId: note.id , noteUser: note.user },
      function(data) {
        if (data.status.code !== 0) {
          $scope.note.message = data.status.message;
        }
        else {
          $scope.setConfirm(note, false);
          refreshNotesTable();
          $location.path('/tasks/'+$scope.taskInstanceId+'/notes');
        }
      }, 
      function(error) {
        $scope.note.message = error.data.status.message;
      });
      
  };  
  $scope.cancel = function(note) {
    if ($scope.edit)
      $scope.note = Notes.shallowCopy($scope.note, $scope.uneditedNote, $scope.authUser.cuid, $scope.taskInstanceId);
    $scope.setEdit($scope.note, false);
    $scope.setAdd(false);
    $scope.setConfirm(note, false);
  };
  
  $scope.isSaveEnabled = function() {
    return $scope.note && $scope.note.name && $scope.note.name.length > 0 &&
      $scope.note.name && $scope.note.name.length > 0;
  };
  
  $scope.save = function() {
    console.log('saving note: ' + $scope.note.name);
    Notes.create({noteId: $scope.note.id}, Notes.shallowCopy({}, $scope.note, $scope.authUser.cuid, $scope.taskInstanceId),
      function(data) {
        if (data.status.code !== 0) {
          $scope.note.message = data.status.message;
        }
        else {
          $scope.noteName = $scope.note.name;
          $scope.setEdit(false);
          $scope.setAdd(false);
          refreshNotesTable();
          $location.path('/tasks/'+$scope.taskInstanceId+'/notes');
        }
      }, 
      function(error) {
        $scope.note.message = error.data.status.message;
      });
  };
  $scope.update = function() {
    console.log('updating note: ' + $scope.note.name);
    Notes.update({noteId: $scope.note.id}, Notes.shallowCopy({}, $scope.note, $scope.authUser.cuid, $scope.taskInstanceId),
      function(data) {
        if (data.status.code !== 0) {
          $scope.note.message = data.status.message;
        }
        else {
          $scope.noteName = $scope.note.name;
          $scope.setEdit(false);
          refreshNotesTable();
        }
      }, 
      function(error) {
        $scope.note.message = error.data.status.message;
      });
  };

  $scope.task = Tasks.get({taskInstanceId: $routeParams.taskInstanceId}, refreshNotesTable());
  
  function refreshNotesTable() {
        $scope.notes = Notes.get({ownerId: $routeParams.taskInstanceId, user : 'aa70413', ownerType : 'TASK_INSTANCE'},
          function() {
          if ($scope.notes.task_instanceNotes) {
            $scope.notes.task_instanceNotes.forEach(function(part, index, notesArray) {
              notesArray[index].date = Date.parse(notesArray[index].date);
            });
          }
          // Need this for date formatting to work in angular
          if ($scope.task.dueDate) $scope.task.dueDate = Date.parse($scope.task.dueDate);
          if ($scope.task.startDate) $scope.task.startDate = Date.parse($scope.task.startDate);
          $scope.gridOptions.data = $scope.notes.task_instanceNotes;
          $interval(function() {
            if ($scope.gridOptions.data[0]) {
              $scope.gridApi.selection.selectRow($scope.gridOptions.data[0]);
            }
          }, 0, 1);
          
        });
  }
        
     
    
}]);

/**
 * {
  "count": 1,
  "ownerId": 22962,
  "ownerType": "TASK_INSTANCE",
  "retrieveDate": "11-23-2015 14:33:34",
  "task_instanceNotes": [{
    "date": "11-23-2015 15:28:14",
    "id": 11050,
    "name": "IBNote",
    "user": "aa70413"
  }]
}
 */
taskMod.controller('TaskAttachmentsController', ['$scope', '$routeParams', '$location', '$timeout', '$interval', 'Tasks', 'Upload','Attachments','mdw',
                                                 function($scope, $routeParams, $location, $timeout, $interval, Tasks, Upload, Attachments, mdw) {
               
       // File Upload stuff
        $scope.uploadFiles = function(file, errFiles) {
          $scope.f = file;
          var h = mdw.roots.services;
          $scope.errFile = errFiles && errFiles[0];
          if (file) {
              file.upload = Upload.upload({
                  url: '/' + mdw.roots.services + '/Services/Attachments',
                  data: {file: file}
              });

              file.upload.then(function (response) {
                  $timeout(function () {
                      file.result = response.data;
                  });
              }, function (response) {
                  if (response.status > 0)
                      $scope.errorMsg = response.status + ': ' + response.data;
              });
          }   
      };
             // Table stuff
             $scope.gridOptions = { enableRowSelection: true, enableRowHeaderSelection: false };
             $scope.gridOptions.columnDefs = [
                                              { name: 'Hyperlink', displayName: 'File Name' ,
                                                cellTemplate:'<div>' +
                                                '  <a href="/servlet/AttachmentDownloadServlet?attachmentId={{row.entity.id}}&amp;masterRequestId={{row.entity.masterRequestId}}&amp;ownerId={{row.entity.ownerId}}&amp;owner={{row.entity.owner}}&amp;fileName={{row.entity.fileName}}&amp;contentType={{row.entity.contentType}}">{{row.entity.name}}</a>' +
                                                '</div>'},
                                              { name: 'user', displayName: 'Modified By'},
                                              { name: 'date', displayName: 'Modified Date'}
                                            ];
                                           
             $scope.gridOptions.multiSelect = false;
             $scope.gridOptions.modifierKeysToMultiSelect = false;
             $scope.gridOptions.noUnselect = true;
             $scope.gridOptions.onRegisterApi = function( gridApi ) {
               $scope.gridApi = gridApi;
             };
             
             $scope.taskInstanceId = $routeParams.taskInstanceId;
             $scope.add = false;
             $scope.setAdd = function(add) {
               $scope.add = add;
               $scope.note = {};
             };
             
             $scope.confirm = false;
             $scope.setConfirm = function(confirm) {
               $scope.confirm = confirm;
               $scope.note.message = confirm ? 'Unattach file "' + $scope.file.name + '"?' : null;
             };
             $scope.deleteNote = function() {
               console.log('unattaching file: ' + $scope.file.name);
               Attachments.remove({noteId: $scope.note.id , noteUser: $scope.note.user },
                 function(data) {
                   if (data.status.code !== 0) {
                     $scope.note.message = data.status.message;
                   }
                   else {
                     $scope.setConfirm(false);
                     refreshAttachmentsTable();
                     $location.path('/tasks/'+$scope.taskInstanceId+'/notes');
                   }
                 }, 
                 function(error) {
                   $scope.note.message = error.data.status.message;
                 });
                 
             };  
             
             $scope.isSaveEnabled = function() {
               return $scope.note && $scope.note.name && $scope.note.name.length > 0 &&
                 $scope.note.name && $scope.note.name.length > 0;
             };
             
             $scope.save = function() {
               console.log('saving note: ' + $scope.note.name);
               Attachments.create({noteId: $scope.note.id}, Attachments.shallowCopy({}, $scope.note, $scope.authUser.cuid, $scope.taskInstanceId),
                 function(data) {
                   if (data.status.code !== 0) {
                     $scope.note.message = data.status.message;
                   }
                   else {
                     $scope.noteName = $scope.note.name;
                     $scope.setEdit(false);
                     refreshAttachmentsTable();
                   }
                 }, 
                 function(error) {
                   $scope.note.message = error.data.status.message;
                 });
             };


             $scope.task = Tasks.get({taskInstanceId: $routeParams.taskInstanceId}, refreshAttachmentsTable());
             
             function refreshAttachmentsTable() {
               /**
                   $scope.attachments = Attachments.get({ownerId: $routeParams.taskInstanceId, user : 'aa70413', ownerType : 'TASK_INSTANCE'},
                     function() {
                     $scope.gridOptions.data = $scope.notes.task_instanceNotes;
                     $interval(function() {
                       if ($scope.gridOptions.data[0]) {
                         $scope.gridApi.selection.selectRow($scope.gridOptions.data[0]);
                       }
                     }, 0, 1);
                     
                   });
                   */
             }
                   
                
               
           }]);
taskMod.controller('TaskHistoryController', ['$scope', '$routeParams', '$location', '$timeout', '$interval', 'Tasks', 'Upload','mdw',
                                                 function($scope, $routeParams, $location, $timeout, $interval, Tasks, Upload, mdw) {
               
             // Table stuff
             $scope.gridOptions = { enableRowSelection: false, enableRowHeaderSelection: false };
             $scope.gridOptions.columnDefs = [
                                              { name: 'createDate', displayName: 'Date/Time',  type:'date', cellFilter: 'date:"MMM dd yyyy HH:MM a"'},
                                              { name: 'eventName', displayName: 'Action Performed'},
                                              { name: 'createUser', displayName: 'User'},
                                              { name: 'comment', displayName: 'Comments'}
                                            ];
                                           
             $scope.gridOptions.multiSelect = false;
             $scope.gridOptions.modifierKeysToMultiSelect = false;
             $scope.gridOptions.noUnselect = true;
             $scope.gridOptions.onRegisterApi = function( gridApi ) {
               $scope.gridApi = gridApi;
             };
             
             $scope.taskInstanceId = $routeParams.taskInstanceId;

             $scope.task = Tasks.get({taskInstanceId: $routeParams.taskInstanceId}, refreshHistoryTable());
             
             function refreshHistoryTable() {
               
               $scope.history = Tasks.get({taskInstanceId: $routeParams.taskInstanceId, extra: 'history'},
                 function() {
                 if ($scope.history.taskHistory) {
                   $scope.history.taskHistory.forEach(function(part, index, historyArray) {
                     historyArray[index].createDate = Date.parse(historyArray[index].createDate);
                   });
                 }
                 $scope.gridOptions.data = $scope.history.taskHistory;
                 $interval(function() {
                   if ($scope.gridOptions.data[0]) {
                     $scope.gridApi.selection.selectRow($scope.gridOptions.data[0]);
                   }
                 }, 0, 1);
                
               });
               
         }
                  
                
               
           }]);

taskMod.factory('Attachments', ['$resource', 'mdw', function($resource, mdw) {
  return angular.extend({}, $resource(mdw.roots.services +'/Services/Attachments/:taskInstanceId', mdw.serviceParams(), {
    find: { method: 'GET', isArray: false },
    attach: { method: 'POST'},
    remove: { method: 'DELETE' }
  }), {
    shallowCopy: function(destNote, srcNote, ownerId, ownerType) {
/**      destNote.id = srcNote.id;
      destNote.name = srcNote.name;
      destNote.user = srcNote.user;
      destNote.date = srcNote.date;
      destNote.details = srcNote.details;
      // Inject OwnerId and OwnerType
      destNote.ownerId = ownerId;
      destNote.ownerType = ownerType;
      destNote.user = $scope.authUser.id;
      */
      return null;
    }
  });
}]);
taskMod.factory('Notes', ['$resource', 'mdw', function($resource, mdw) {
  return angular.extend({}, $resource(mdw.roots.services +'/Services/Notes/:noteId/:taskInstanceId/:noteUser', mdw.serviceParams(), {
    find: { method: 'GET', isArray: false },
    create: { method: 'POST'},
    update: { method: 'PUT' },
    remove: { method: 'DELETE' }
  }), {
    shallowCopy: function(destNote, srcNote, user, taskInstanceId) {
      destNote.id = srcNote.id;
      destNote.name = srcNote.name;
      destNote.user = srcNote.user;
      destNote.date = srcNote.date;
      destNote.details = srcNote.details;
      destNote.ownerId = taskInstanceId;
      destNote.ownerType = "TASK_INSTANCE";
      destNote.user = user;
      return destNote;
    }
  });
}]);

taskMod.factory('Tasks', ['$resource', 'mdw', function($resource, mdw) {
  return $resource(mdw.roots.services +'/Services/Tasks/:taskInstanceId/:extra', mdw.serviceParams(), {
    query: { method: 'GET'},
    actions: { method: 'GET', isArray: true},
    create: { method: 'POST'},
    action: { method: 'POST'},
    update: { method: 'PUT' }
  });
}]);

// service for caching task instance
taskMod.factory('Task', ['$http', '$route', 'mdw', 'TaskUtil', 'Tasks', 'TaskAction', function($http, $route, mdw, TaskUtil, Tasks, TaskAction) {
  return {
    getTask: function(scope) {
      scope.item = this.task; // for taskItem template
      if (this.task)
        scope.taskActions = this.task.actions; // for taskActions template
      this.addActionFunctions(scope);
      return this.task;
    },
    retrieveTask: function(scope, instanceId) {
      // retrieve task
      this.task = Tasks.get({taskInstanceId: instanceId});
      
      this.task.$promise.then(
        function(task) {
          task.dirty = false;
          task.dbDate = new Date(task.retrieveDate);
          TaskUtil.setTask(task, task.dbDate);
          task.actionable = true; // TODO assigned to user and not in final state
          task.editable = true; // TODO assigned to user and not in final state
          
          scope.item = task; // for taskItem template
          
          // retrieve taskActions
          Tasks.actions({taskInstanceId: task.id, extra: 'actions'}, 
            function(data) {
              task.actions = data;
              scope.taskActions = task.actions; // for taskActions template
            },
            function(error) {
              task.message = error.data.status.message;
            }
          );
        },
        function(error) {
          mdw.messages = error.statusText;
        }
      );
      
      this.addActionFunctions(scope);
      return this.task;
    },
    addActionFunctions: function(scope) {
      scope.findTypeaheadAssignees = function(typed) {
        return $http.get(mdw.roots.services + '/services/Tasks/assignees' + '?app=mdw-admin&find=' + typed).then(function(response) {
          return response.data.users;
        });
      };
      
      scope.getAssigneePopPlace = function() {
        return 'left';
      };
        
      scope.performAssign = function(assignee) {
        scope.performAction('Assign', assignee.cuid);
      };
      
      var taskSvc = this;
      scope.performAction = function(action, assignee) {
        scope.closePopover(); // popover should be closed
        console.log('Performing action: ' + action + ' on task ' + scope.task.id);
        var taskAction = {
            taskAction: action, 
            user: scope.authUser.id, 
            taskInstanceId: scope.task.id,
            assignee: assignee
        };
        
        TaskAction.action({action: action}, taskAction, function(data) {
          if (data.status.code !== 0) {
            mdw.messages = data.status.message;
          }
          else {
            taskSvc.task = null; // force refresh
            $route.reload();
          }
        }, function(error) {
          mdw.messages = error.data.status.message;
        });
      };
    }
  };
}]);
