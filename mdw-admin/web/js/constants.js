// Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
'use strict';

var constantsMod = angular.module('constants', []);

constantsMod.constant('PROCESS_STATUSES', ['Pending', 'In Progress', 'Failed', 'Completed', 'Canceled', 'Waiting']);
constantsMod.constant('TASK_STATUSES', ['Open', 'Assigned', 'In Progress', 'Completed', 'Canceled']);
constantsMod.constant('ACTIVITY_STATUSES', ['In Progress', 'Failed', 'Waiting']);
constantsMod.constant('STUCK_ACTIVITY_STATUSES', ['In Progress', 'Failed', 'Canceled', 'Waiting']);
// these are simplified
constantsMod.constant('TASK_ADVISORIES', ['Jeopardy', 'Alert', 'Invalid']);

constantsMod.constant('EXCEL_DOWNLOAD', 'DownloadFormat=application%2Fvnd.openxmlformats-officedocument.spreadsheetml.sheet');
