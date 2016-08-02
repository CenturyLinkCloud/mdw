// Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
'use strict';
var taskTemplateMod = angular.module('taskTemplates', ['ngResource', 'mdw']);

taskTemplateMod.controller('TasksTemplatesController', ['$scope', '$http', '$location', 'TaskTemplates', 'Workgroups', '$routeParams',
    function($scope, $http, $location, Tasks, Workgroups, $routeParams) {
        Tasks.taskTemplatelist = Tasks.get();
        Workgroups.groupList = Workgroups.get();
        var url = '/' + 'mdw' + '/Services/TaskTemplates?app=mdw-admin&category=all';
        $http.get(url).error(function(data, status) {}).success(function(data, status, headers, config) {
            Tasks.categoryList = data;
        });

        $scope.selected = null;

        $scope.getTaskTemplateList = function() {
            var selectedTask = {
                taskTemplates: ''
            };
            if ($scope.selected !== null) {
                selectedTask.taskTemplates = [$scope.selected];
                return selectedTask;
            } else
                return Tasks.taskTemplatelist;
        };

        $scope.find = function(typed) {
            var filteredTaskList = [];
            var taskName;
            for (var k = 0; k < Tasks.taskTemplatelist.taskTemplates.length; k++) {
                taskName = Tasks.taskTemplatelist.taskTemplates[k].name;
                if (taskName.indexOf(typed) >= 0) {
                    filteredTaskList.push(Tasks.taskTemplatelist.taskTemplates[k]);
                }
            }
            return filteredTaskList;
        };

        $scope.select = function() {
            console.log("select called");
        };

        $scope.change = function() {
            console.log("change called");
        };
    }
]);

taskTemplateMod.controller('TaskTemplateController', ['$scope', '$location', 'TaskTemplates', 'Workgroups', '$routeParams',
    function($scope, $location, Tasks, Workgroups, $routeParams) {
        for (var i = 0; i < Tasks.taskTemplatelist.taskTemplates.length; i++) {
            if (Tasks.taskTemplatelist.taskTemplates[i].name == $routeParams.taskName) {
                $scope.taskTemplate = Tasks.taskTemplatelist.taskTemplates[i];
                $scope.unEditedTaskTemplate = Tasks.shallowCopy({}, $scope.taskTemplate); // backup original for cancel
                break;
            }
        }

        $scope.status = {
            message: ''
        };
        if ($scope.taskTemplate["Prioritization Rules"] !== null && typeof $scope.taskTemplate["Prioritization Rules"] != 'undefined')
            Tasks.prStrategyList = $scope.taskTemplate["Prioritization Rules"].split(',');
        if ($scope.taskTemplate["SubTask Rules"] !== null && typeof $scope.taskTemplate["SubTask Rules"] != 'undefined')
            Tasks.subStrategyList = $scope.taskTemplate["SubTask Rules"].split(',');
        if ($scope.taskTemplate.Groups !== null && typeof $scope.taskTemplate.Groups != 'undefined')
            Tasks.groups = $scope.taskTemplate.Groups.split(',');
        if ($scope.taskTemplate["Routing Rules"] !== null && typeof $scope.taskTemplate["Routing Rules"] != 'undefined')
            Tasks.routingRules = $scope.taskTemplate["Routing Rules"].split(',');
        if ($scope.taskTemplate["Auto Assign Rules"] !== null && typeof $scope.taskTemplate["Auto Assign Rules"] != 'undefined')
            Tasks.autoAssignRules = $scope.taskTemplate["Auto Assign Rules"].split(',');
        if ($scope.taskTemplate.NoticeGroups !== null && typeof $scope.taskTemplate.NoticeGroups != 'undefined')
            Tasks.noticeGroups = $scope.taskTemplate.NoticeGroups.split(',');
        $scope.taskName = $routeParams.taskName;
        if ($scope.taskTemplate.Notices !== null && typeof $scope.taskTemplate.Notices != 'undefined' && $scope.taskTemplate.Notices.indexOf(',') >= 0) {
            Tasks.Notices = $scope.taskTemplate.Notices.split(';');
            for (var j = 0; j < Tasks.Notices.length; j++) {
                Tasks.Action = Tasks.Notices[j].split(',');
                if (Tasks.Action[0] == 'Open') {
                    Tasks.open = Tasks.Action;
                } else if (Tasks.Action[0] == 'Assigned') {
                    Tasks.assigned = Tasks.Action;
                } else if (Tasks.Action[0] == 'Failed') {
                    Tasks.failed = Tasks.Action;
                } else if (Tasks.Action[0] == 'Completed') {
                    Tasks.completed = Tasks.Action;
                } else if (Tasks.Action[0] == 'Cancelled') {
                    Tasks.cancelled = Tasks.Action;
                } else if (Tasks.Action[0] == 'In Progress') {
                    Tasks.progress = Tasks.Action;
                } else if (Tasks.Action[0] == 'Alert') {
                    Tasks.alert = Tasks.Action;
                } else if (Tasks.Action[0] == 'Jeopardy') {
                    Tasks.jeopardy = Tasks.Action;
                } else if (Tasks.Action[0] == 'Hold') {
                    Tasks.hold = Tasks.Action;
                } else if (Tasks.Action[0] == 'Forward') {
                    Tasks.forward = Tasks.Action;
                }
            }
            $scope.emailTemplates = {
                "open": Tasks.open[1],
                "assigned": Tasks.assigned[1],
                "completed": Tasks.completed[1],
                "cancelled": Tasks.cancelled[1],
                "progress": Tasks.progress[1],
                "alert": Tasks.alert[1],
                "jeopardy": Tasks.jeopardy[1],
                "forward": Tasks.forward[1]
            };
        }
        
        $scope.unEditedEmailTemplates = {
            "open": Tasks.open[1],
            "assigned": Tasks.assigned[1],
            "completed": Tasks.completed[1],
            "cancelled": Tasks.cancelled[1],
            "progress": Tasks.progress[1],
            "alert": Tasks.alert[1],
            "jeopardy": Tasks.jeopardy[1],
            "forward": Tasks.forward[1]
        };

        $scope.assembleNotice = function() {
            Tasks.open[1] = $scope.emailTemplates.open;
            Tasks.assigned[1] = $scope.emailTemplates.assigned;
            Tasks.completed[1] = $scope.emailTemplates.completed;
            Tasks.cancelled[1] = $scope.emailTemplates.cancelled;
            Tasks.progress[1] = $scope.emailTemplates.progress;
            Tasks.alert[1] = $scope.emailTemplates.alert;
            Tasks.jeopardy[1] = $scope.emailTemplates.jeopardy;
            Tasks.forward[1] = $scope.emailTemplates.forward;
            $scope.taskTemplate.Notices = Tasks.open.join() + ";" + Tasks.assigned.join() + ";" +
                Tasks.failed.join() + ";" + Tasks.completed.join() + ";" +
                Tasks.cancelled.join() + ";" + Tasks.progress.join() + ";" +
                Tasks.alert.join() + ";" + Tasks.jeopardy.join() + ";" +
                Tasks.hold.join() + ";" + Tasks.forward.join() + ";";
        };

        $scope.getTaskTemplateList = function() {

            if ($scope.selected != 'undefined')
                return $scope.selected;
            else
                return Tasks.taskTemplatelist;
        };
        $scope.getCategoryList = function() {

            return Tasks.categoryList;
        };
        $scope.getPrStrategyList = function() {
            return Tasks.prStrategyList;
        };
        $scope.getSubStrategyList = function() {

            return Tasks.subStrategyList;
        };

        $scope.getRoutingRules = function() {

            return Tasks.routingRules;
        };

        $scope.geAutoAssignRules = function() {

            return Tasks.autoAssignRules;
        };

        $scope.getgroups = function() {
            return Tasks.groups;
        };

        $scope.getNoticeGroups = function() {
            return Tasks.noticeGroups;
        };

        $scope.isSaveEnabled = function() {
            return true;
        };
        $scope.save = function() {
            $scope.assembleNotice();
            Tasks.update({
                    taskId: $scope.taskTemplate.taskId
                }, Tasks.shallowCopy({}, $scope.taskTemplate), function(data) {
                    if (data.status.code !== 0) {
                        $scope.status.message = data.status.message;
                    }
                },
                function(error) {
                    $scope.status.message = error.data.status.message;
                });
            $scope.closePopover();
        };

        $scope.addWorkgroup = function(workgroup) {
            $scope.taskTemplate.Groups = $scope.taskTemplate.Groups + "," + workgroup;
            $scope.assembleNotice();

            Tasks.update({
                    taskId: $scope.taskTemplate.taskId
                }, $scope.taskTemplate, function(data) {

                    if (data.status.code !== 0) {
                        $scope.status.message = data.status.message;
                    } else {

                        Tasks.groups.push(workgroup);
                        Tasks.groups.sort();
                    }
                },
                function(error) {
                    $scope.status.message = error.data.status.message;
                });
            $scope.closePopover();
        };

        $scope.removeWorkgroup = function(workgroup) {
            Tasks.groups.splice(Tasks.groups.indexOf(workgroup), 1);
            $scope.taskTemplate.Groups = Tasks.groups.join();
            Tasks.update({
                    taskId: $scope.taskTemplate.taskId
                }, $scope.taskTemplate, function(data) {
                    if (data.status.code !== 0) {
                        $scope.status.message = data.status.message;
                    }
                },
                function(error) {
                    $scope.status.message = error.data.status.message;
                });
            $scope.closePopover();
        };

        $scope.removeNotifyWorkgroup = function(workgroup) {
            Tasks.noticeGroups.splice(Tasks.noticeGroups.indexOf(workgroup), 1);
            $scope.taskTemplate.NoticeGroups = Tasks.noticeGroups.join();

            Tasks.update({
                    taskId: $scope.taskTemplate.taskId
                }, $scope.taskTemplate, function(data) {
                    if (data.status.code !== 0) {
                        $scope.status.message = data.status.message;
                    }
                },
                function(error) {
                    $scope.status.message = error.data.status.message;
                });
            $scope.closePopover();
        };

        $scope.addNotifyWorkgroup = function(noticegroup) {
            $scope.taskTemplate.NoticeGroups = $scope.taskTemplate.NoticeGroups + "," + noticegroup;
            $scope.assembleNotice();
            Tasks.update({
                    taskId: $scope.taskTemplate.taskId
                }, $scope.taskTemplate, function(data) {
                    if (data.status.code !== 0) {
                        $scope.status.message = data.status.message;
                    } else {
                        Tasks.noticeGroups.push(noticegroup);
                        Tasks.noticeGroups.sort();
                    }
                },
                function(error) {
                    $scope.status.message = error.data.status.message;
                });
            $scope.closePopover();
        };

        $scope.getOtherWorkgroups = function() {
            var otherWorkgroups = [];

            for (var i = 0; i < Workgroups.groupList.workgroups.length; i++) {
                var workgroup = Workgroups.groupList.workgroups[i];
                if (Tasks.groups.indexOf(workgroup.name) < 0)
                    otherWorkgroups.push(workgroup.name);
            }
            return otherWorkgroups;
        };

        $scope.getOtherNotifyWorkgroups = function() {
            var otherNotifyWorkgroups = [];

            for (var i = 0; i < Workgroups.groupList.workgroups.length; i++) {
                var workgroup = Workgroups.groupList.workgroups[i];
                if (Tasks.noticeGroups.indexOf(workgroup.name) < 0)
                    otherNotifyWorkgroups.push(workgroup.name);
            }
            return otherNotifyWorkgroups;
        };


        $scope.getTaskTemplate = function() {
            return $scope.taskTemplate;
        };


        $scope.cancel = function() {
            $scope.taskTemplate = Tasks.shallowCopy({}, $scope.unEditedTaskTemplate); // restore original for backup
            $scope.emailTemplates = JSON.parse(JSON.stringify($scope.unEditedEmailTemplates)); // restore Notice Templates
            return;
        };
    }
]);

taskTemplateMod.factory('TaskTemplates', ['$resource', 'mdw', function($resource, mdw) {
    return angular.extend({}, $resource(mdw.roots.services + '/Services/TaskTemplates/:taskId', mdw.serviceParams(), {
        create: {
            method: 'POST'
        },
        update: {
            method: 'PUT'
        },
        remove: {
            method: 'DELETE'
        }
    }), {
        shallowCopy: function(destTask, srcTask) {
            destTask.taskId = srcTask.taskId;
            destTask.name = srcTask.name;
            destTask.TaskDescription = srcTask.TaskDescription;
            destTask.logicalId = srcTask.logicalId;
            destTask.category = srcTask.category;
            destTask.alertIntervalDays = srcTask.alertIntervalDays;
            destTask.alertIntervalHours = srcTask.alertIntervalHours;
            destTask.slaDays = srcTask.slaDays;
            destTask.slaHours = srcTask.slaHours;
            destTask.PriorityStrategy = srcTask.PriorityStrategy;
            destTask.SubTaskStrategy = srcTask.SubTaskStrategy;
            destTask.RoutingStrategy = srcTask.RoutingStrategy;
            destTask.AutoAssign = srcTask.AutoAssign;
            destTask.Groups = srcTask.AutoAssign;
            destTask.NoticeGroups = srcTask.NoticeGroups;
            destTask.Notices = srcTask.Notices;
            return destTask;
        }
    });
}]);