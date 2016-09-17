// Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
'use strict';

var constantsMod = angular.module('constants', []);

constantsMod.constant('PROCESS_STATUSES', ['Pending', 'In Progress', 'Failed', 'Completed', 'Canceled', 'Waiting']);
constantsMod.constant('TASK_STATUSES', ['Open', 'Assigned', 'In Progress', 'Completed', 'Canceled']);
constantsMod.constant('ACTIVITY_STATUSES', ['In Progress', 'Failed', 'Waiting']);
constantsMod.constant('STUCK_ACTIVITY_STATUSES', ['In Progress', 'Failed', 'Canceled', 'Waiting']);
constantsMod.constant('REQUEST_STATUSES', ['In Progress', 'Failed', 'Waiting', 'Completed']);
// these are simplified
constantsMod.constant('TASK_ADVISORIES', ['Jeopardy', 'Alert', 'Invalid']);

constantsMod.constant('EXCEL_DOWNLOAD', 'DownloadFormat=xlsx');
constantsMod.constant('JSON_DOWNLOAD', 'DownloadFormat=json');
constantsMod.constant('ZIP_DOWNLOAD', 'DownloadFormat=zip');

constantsMod.constant('DOCUMENT_TYPES', ['org.w3c.dom.Document', 'org.apache.xmlbeans.XmlObject', 'java.lang.Object', 'org.json.JSONObject',
                                         'groovy.util.Node', 'com.centurylink.mdw.xml.XmlBeanWrapper', 'com.centurylink.mdw.model.StringDocument',
                                         'com.centurylink.mdw.model.HTMLDocument', 'javax.xml.bind.JAXBElement', 'org.apache.camel.component.cxf.CxfPayload']);
