'use strict';

var constantsMod = angular.module('constants', []);

constantsMod.constant('PROCESS_STATUSES', ['Pending', 'In Progress', 'Failed', 'Completed', 'Canceled', 'Waiting']);
constantsMod.constant('TASK_STATUSES', ['Open', 'Assigned', 'In Progress', 'Completed', 'Canceled']);
constantsMod.constant('ACTIVITY_STATUSES', ['In Progress', 'Failed', 'Waiting']);
constantsMod.constant('STUCK_ACTIVITY_STATUSES', ['In Progress', 'Failed', 'Canceled', 'Waiting']);
constantsMod.constant('REQUEST_STATUSES', ['In Progress', 'Failed', 'Waiting', 'Completed']);
// these are simplified
constantsMod.constant('TASK_ADVISORIES', ['Jeopardy', 'Alert', 'Invalid']);
constantsMod.constant('HTTP_METHODS', ['POST', 'PUT', 'GET', 'DELETE', 'PATCH']);

constantsMod.constant('QUEUE_NAMES', [
  'com.centurylink.mdw.external.event.queue',
  'com.centurylink.mdw.intra.event.queue',
  'com.centurylink.mdw.process.handler.queue',
  'com.centurylink.mdw.config.topic'
]);

constantsMod.constant('EXCEL_DOWNLOAD', 'DownloadFormat=xlsx');
constantsMod.constant('JSON_DOWNLOAD', 'DownloadFormat=json');
constantsMod.constant('ZIP_DOWNLOAD', 'DownloadFormat=zip');

// TODO: should be dynamic through a service
constantsMod.constant('DOCUMENT_TYPES', {
  'org.w3c.dom.Document': 'xml',
  'org.apache.xmlbeans.XmlObject': 'xml',
  'java.lang.Object': 'java',
  'org.json.JSONObject': 'json',
  'groovy.util.Node': 'xml',
  'com.centurylink.mdw.xml.XmlBeanWrapper': 'xml',
  'com.centurylink.mdw.model.StringDocument': 'text',
  'com.centurylink.mdw.model.HTMLDocument': 'html',
  'javax.xml.bind.JAXBElement': 'xml',
  'org.apache.camel.component.cxf.CxfPayload': 'xml',
  'com.centurylink.mdw.common.service.Jsonable': 'json',
  'org.yaml.snakeyaml.Yaml': 'yaml',
  'java.lang.Exception': 'json'
});

constantsMod.constant('WORKFLOW_STATUSES', [
   {status: 'Pending', color: 'blue'},
   {status: 'In Progress', color: 'green'},
   {status: 'Failed', color: 'red'},
   {status: 'Completed', color: 'black'},
   {status: 'Canceled', color: 'darkgray'},
   {status: 'Hold', color: 'cyan'},
   {status: 'Waiting', color: 'yellow'}
 ]);