// Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
'use strict';

var taskMod = angular.module('task', ['ngResource', 'mdw', 'ui.grid', 'ui.grid.selection', 'ngFileUpload', 'taskTemplates']);

// task summary
taskMod.controller('TaskController', ['$scope', '$route', '$routeParams', '$http', 'mdw', 'util', 'TaskUtil', 'Tasks', 'TaskAction',
                                      function($scope, $route, $routeParams, $http, mdw, util, TaskUtil, Tasks, TaskAction) {

  $scope.taskInstanceId = $routeParams.taskInstanceId;
  
  $scope.save = function() {
    console.log('saving task: ' + $scope.task.id);
    Tasks.update({taskInstanceId: $scope.task.id}, $scope.task,
      function(data) {
        if (data.status.code !== 0) {
          mdw.messages = data.status.message;
        }
        else {
          mdw.messages = null;
          $scope.task.dirty = false;
        }
      }, 
      function(error) {
        mdw.messages = error.data.status.message;
      });
  };
  
  $scope.retrieve = function() {
    // retrieve task
    $scope.task = Tasks.get({taskInstanceId: $routeParams.taskInstanceId}, function() {
      mdw.messages = null;
      $scope.task.dirty = false;
      $scope.dbDate = new Date($scope.task.retrieveDate);
      TaskUtil.setTask($scope.task, $scope.dbDate);
      $scope.task.actionable = true; // TODO assigned to user and not in final state
      $scope.task.editable = true; // TODO assigned to user and not in final state 
    });
      
    // retrieve taskActions
    Tasks.actions({taskInstanceId: $routeParams.taskInstanceId, action: 'actions'}, 
      function(data) {
        $scope.taskActions = data;
      },
      function(error) {
        $scope.task.message = error.data.status.message;
      }
    );
  };
  $scope.retrieve();
  
  $scope.random = Math.random(); // param to force image reload
  
  $scope.popups = {};
  $scope.openDueDatePopup = function() {
    $scope.popups.dueDate = true;
  };
  
  $scope.dueDateChanged = function() {
    $scope.task.dirty = true;
    TaskUtil.setTask($scope.task, $scope.dbDate);
    if ($scope.task.dueDate)
      $scope.task.dueInSeconds = Math.round(($scope.task.dueDate.getTime() - Date.now())/1000);
    else
      $scope.task.dueInSeconds = -1;
  };

  $scope.findTypeaheadAssignees = function(typed) {
    return $http.get(mdw.roots.services + '/services/Tasks/assignees' + '?app=mdw-admin&find=' + typed).then(function(response) {
      return response.data.users;
    });
  };
  
  $scope.getAssigneePopPlace = function() {
    return 'left';
  };
    
  $scope.performAssign = function(assignee) {
    $scope.performAction('Assign', assignee.cuid);
  };
  
  $scope.performAction = function(action, assignee) {
    mdw.messages = null;
    $scope.closePopover(); // popover should be closed
    console.log('Performing action: ' + action + ' on task ' + $scope.task.id);
    var taskAction = {
        taskAction: action, 
        user: $scope.authUser.id, 
        taskInstanceId: $scope.task.id,
        assignee: assignee
    };
    
    TaskAction.action({action: action}, taskAction, function(data) {
      if (data.status.code !== 0) {
        mdw.messages = data.status.message;
      }
      else {
        $scope.retrieve();
      }
    }, function(error) {
      mdw.messages = error.data.status.message;
    });
  };
  
  $scope.refreshWorkflowImage = function() {
    $route.reload();
  };
}]);

// subtasks
taskMod.controller('SubtasksController', ['$scope', '$routeParams', '$location', 'Tasks', 'TaskTemplates',
                                           function($scope, $routeParams, $location, Tasks, TaskTemplates) {
  $scope.masterTaskId = $routeParams.taskInstanceId;
  $scope.task = {id: $scope.masterTaskId}; // for nav routing only
  $scope.subtaskList = Tasks.get({taskInstanceId: $routeParams.taskInstanceId, action: 'subtasks'});
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
    
    Tasks.create({}, {logicalId: $scope.subtask.template.logicalId, masterTaskInstanceId: $scope.masterTaskId},
      function(data) {
        if (data.status.code !== 0) {
          $scope.message = data.status.message;
        }
        else {
          $scope.setCreate(false);
          $scope.subtaskList = Tasks.get({taskInstanceId: $routeParams.taskInstanceId, action: 'subtasks'});  
          $location.path('/tasks/' + $scope.masterTaskId + '/subtasks');
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
               
               $scope.history = Tasks.get({taskInstanceId: $routeParams.taskInstanceId, action: 'history'},
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
  return $resource(mdw.roots.services +'/Services/Tasks/:taskInstanceId/:action', mdw.serviceParams(), {
    query: { method: 'GET'},
    actions: { method: 'GET', isArray: true},
    create: { method: 'POST'},
    action: { method: 'POST'},
    update: { method: 'PUT' }
  });
}]);
