// Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
'use strict';

var blvProcessSvc = angular.module('blvProcessSvc', [ 'mdw', 'blvtypes' ]);

blvProcessSvc.factory('Processes', [
    '$http',
    'mdw',
    'blvtypes',
    'blvSharedData',
    function($http, mdw, blvtypes, blvSharedData) {

      
      
      /**
       *       pd = new ProcessDefinition(getProcessData("WorkflowAsset?name="
          + processName + "&version=" + processVersion), parentPD, callingActivity);

       */
      return {
        getProcessDefinition : function() {
          var masterRequestId = blvSharedData.data.masterRequestId;
          var url = mdw.roots.services + '/Services/WorkflowAsset?format=json&MasterRequestId=' + masterRequestId;
          console.log('retrieving process definition for master request id: ',masterRequestId, ' from url '+ url);
          var promise = $http.get(url).then(function(response) {

            var pd = response.data;
            return new blvtypes.ProcessDefinition(pd);
          });
          return promise;
        },        
        getProcessDefinitionByName : function(masterRequestId, processName, parentPD, callingActivity) {
          var url = mdw.roots.services + '/Services/WorkflowAsset?format=json&name=' + processName+'.proc&version=0';
          console.log('retrieving process definition for process name: ',processName, ' from url ', url);
          var request = new window.XMLHttpRequest();
          request.open('GET', url, false);  // `false` makes the request synchronous
          request.send(null);

             if (request.status === 200) {
               var jsonResponse = JSON.parse(request.responseText);
              var processDef = new blvtypes.ProcessDefinition(jsonResponse, parentPD, callingActivity);
              if (parentPD) {
                // Allow the caller to add the correct parent process Definition
                // which will be used when a process is in a subprocess and we need to 
                // find the process above (i.e.e for BAM events)
                processDef.parentProcessDefinition = parentPD;
              
              }
              return processDef;
            }
          
/**          var promise = $http.get(url).then(function(response) {
            var pd = response.data;
            var processDef = new blvtypes.ProcessDefinition(pd, parentPD, callingActivity);
            if (parentPD) {
              // Allow the caller to add the correct parent process Definition
              // which will be used when a process is in a subprocess and we need to 
              // find the process above (i.e.e for BAM events)
              processDef.parentProcessDefinition = parentPD;
            
            }
            return processDef;
          });

          return promise;
          */
        },
        getHistoricalBAMMessages : function() {
          var masterRequestId = blvSharedData.data.masterRequestId;
          var url = mdw.roots.services + '/Services/BamEventSummary?MasterRequestId='+ masterRequestId + '&Realm=mdw&format=json';
          var promise = $http.get(url).then(function(response) {
            var pid = response.data;
            return pid;
          });
          return promise;
        },
        getProcessInstanceData : function() {
          var masterRequestId = blvSharedData.data.masterRequestId;
          var url = mdw.roots.services + '/Services/ProcessInstanceData?format=json&MasterRequestId=' + masterRequestId;
          var promise = $http.get(url).then(function(response) {
            var pid = response.data;
            return new blvtypes.ProcessInstanceData(pid);
          });
          return promise;
        }
      };
    } ]);