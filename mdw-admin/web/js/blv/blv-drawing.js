// Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
'use strict';

var blvDrawingSvc = angular.module('blvDrawingSvc', [ 'mdw', 'blvProcessSvc',
    'blvutils', 'blvworkflow', 'blvSharedData' ]);
// alert("created blvDrawingSvc");

blvDrawingSvc.factory('DrawingService',
    [
        'mdw',
        'Processes',
        'blvutils',
        'blvworkflow',
        'blvSharedData',
        function(mdw, processes, blvutils, blvworkflow, blvSharedData) {
          var mainProcess = {};
          return {
            drawWorkflow : function(processDefinition) {
              // alert("DrawingService.drawWorkflow()");
              // alert("stegaed");
              // initParams();
              // storePossibleBAMEvents(processDefinition);
             // blvworkflow.storePossibleBAMEvents(processDefinition);
              var processInstanceData = processes.getProcessInstanceData();
              processDefinition.updateInstanceData(processInstanceData);
              var myStage = this.getStage();
              var processWorkflow = this.getMainProcess(processDefinition,
                  myStage);
              processWorkflow.activate(myStage,
                  processDefinition.processInstanceData.processInstanceId);
             // hashProcesses[processDefinition.key] = processWorkflow;
             // angular.forEach(this.hashProcesses, function(index, process) {
             //   mdwLog("key=" + index + "obj=" + process);
             // });

              return processWorkflow;
            },

            getStage : function() {
              // if (this.stage) {
              // return this.stage;
              // }
              return new Kinetic.Stage({
                container : "container",
                width : 3000,
                height : 3000
              // ,
              // draggable : false
              });

              // return this.stage;
            },

            // The Main function which generates the various Nodes
            getMainProcess : function(processDefinition, myStage) {
              mainProcess = new blvworkflow.MainProcess(processDefinition, myStage);
              return mainProcess;
            },
            // The Main function which generates the various Nodes
            getMainBLVProcess : function() {
              mainProcess =  new blvworkflow.MainBLVProcessNew();
              return mainProcess;
            },

            drawBamWorkflow : function(processDefinition) {
              var myStage = this.getStage();
              var bamWorkflow = new blvworkflow.MainBLVProcessNew();
             // blvworkflow.storePossibleBAMEvents(processDefinition);
              bamWorkflow.initialize(processDefinition, myStage);
              bamWorkflow.drawBAM();
              return bamWorkflow;

            },
            updateHistoricalBAMMessages : function() {
              processes.getHistoricalBAMMessages().then(
                function(historicalBAMData) {
                  blvSharedData.data.mainProcess.clearBamMessages();
                  var historicalBAMMessages = blvworkflow.createHistoricalBAMMessages(historicalBAMData);
                  if (historicalBAMMessages)
                  {
                    blvutils.mdwLog("Got"+ historicalBAMMessages.length+"HistoricalBAMMessages");
                    var bamMessages = [];
                    for ( var j = 0; j < historicalBAMMessages.length; j++)
                    {
                      bamMessages.push(historicalBAMMessages[j]);
                      // We don't know which
                      // process instance ids
                      // these
                      // historical messages
                      // relate to, so we can't
                      // update
                      // any of the detailed
                      // workflow
                      // updateDetailWorkflows(historicalBAMMessages[j]);
                      // if (blvType != "Full")
                      // {
                      blvSharedData.data.mainProcess.addBamMessage(historicalBAMMessages[j]);
                      // }
                      // $('#attributes').html("");
                      // addAttributes(historicalBAMMessages[j], true);
                    }
                  }
                });


          }

          };

        } ]);
