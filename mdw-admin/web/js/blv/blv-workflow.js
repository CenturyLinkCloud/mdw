// Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
'use strict';
var mdwBlvWorkflow = angular.module('blvworkflow', [ 'ui.bootstrap',
    'blvutils', 'blvtypes', 'blvProcessSvc', 'blvSharedData' ]);
var allConnectors, hashProcessDefinitions =[];
var cr = 8;
var alreadyDoneHash;
mdwBlvWorkflow
    .factory(
        'blvworkflow',
        [
            'blvutils',
            'blvtypes',
            'blvSharedData',
            'Processes',
            '$modal',
            function(blvutils, blvtypes, blvSharedData, processes, $modal) {
              var blvworkflow = {
                MainProcess : function(processDefinition, myStage) {
                  this.nodeDisplayContainer = new blvworkflow.NodeDisplayContainer(
                      processDefinition, 'container');
                  this.processDefinition = processDefinition;
                  var legend = new blvworkflow.Legend(processDefinition,
                      'Business Live View (Full)');
                  // TBD Don't add at the moment
                  //this.nodeDisplayContainer.addLegend(legend);
                  // Add the template container to the parent
                  // NodeDisplayCOntainer
                  // this.nodeDisplayContainer
                  // .addNodeTemplateContainer(this.nodeTemplateDisplayContainer);

                  var i = 0;
  
                  /**
                   * Add nodes
                   */
                  this.hashIds = {};
                  for (i = 0; i < processDefinition.activities.length; i++) {
                    // alert(i);
                    var activityNode = new blvworkflow.Node(
                        processDefinition.activities[i].x,
                        processDefinition.activities[i].y, 'green',
                        processDefinition.activities[i], processDefinition);
                    this.nodeDisplayContainer.addNode(activityNode, myStage);
                    processDefinition.activities[i].pdKey = processDefinition.key;
                    this.hashIds[processDefinition.activities[i].key] = activityNode;
                  }

                  /**
                   * Add connectors
                   */
                  for (i = 0; i < processDefinition.transitions.length; i++) {
                    var from = processDefinition.transitions[i].from;
                    var to = processDefinition.transitions[i].to;
                    // alert(from+to);
                    // var connector = new NodeLineConnector(
                    // this.hashIds[from].endPoints[1],
                    // this.hashIds[to].endPoints[0]);
                    var connector = new blvworkflow.NodeLineConnector(
                        processDefinition.transitions[i]);
                    // alert("Start "+connector);
                    this.nodeDisplayContainer.addConnector(connector);

                  }
                  // var self = this;
                  // this.nodeDisplayContainer.addLayerToStage();
                  myStage.add(this.nodeDisplayContainer.textlayer);
                  this.nodeDisplayContainer.layer
                      .add(this.nodeDisplayContainer.groupForLayer);
                  myStage.add(this.nodeDisplayContainer.layer);
                  blvutils.mdwLog("Added layer draggable = "+
                      this.nodeDisplayContainer.layer.getDraggable());
                  // var view = new blvworkflow.View(myStage,
                  // this.nodeDisplayContainer.layer);
                  this.nodeDisplayContainer.layer.hide();
                  // alert("Done with transitions");
                  this.activate = function(myStage, processInstanceId,
                      parentProcessDefinitionKey) {
                    // Fix this
                    // var processInstanceData = new
                    // blvtypes.ProcessInstanceData(
                    // getProcessData("ProcessInstanceData?ProcessInstanceId="
                    // + processInstanceId));
                    var processInstanceData = processes
                        .getProcessInstanceData();

                    this.processDefinition
                        .updateInstanceData(processInstanceData);
                    // Lazily add in the parent process definition as we won't
                    // know it for
                    // new BAM messages
                    //TBD
                    //commented out in the meantime
                    this.processDefinition.parentProcessDefinition = hashProcessDefinitions[parentProcessDefinitionKey];
                    blvutils.mdwLog('activating layer...'+
                        this.nodeDisplayContainer.layer.attrs.name);
                    this.nodeDisplayContainer.textlayer.show();
                    this.nodeDisplayContainer.layer.show();
                    blvutils.mdwLog('done activating layer...'+
                        this.nodeDisplayContainer.layer.attrs.name);
                    // myLayer.add();
                    // this.nodeDisplayContainer.stage.draw();
                    myStage.draw();
                    // myStage.draw();
                    blvutils.updateGUIProcessData(this.processDefinition);

                    // alert('done redrawing layer...'+
                    // this.nodeDisplayContainer.layer.attrs['name']);
                  };
                  this.deactivate = function(myStage) {
                    blvutils.mdwLog('deactivating layer...'+
                        this.nodeDisplayContainer.layer.attrs.name);
                    this.nodeDisplayContainer.textlayer.hide();
                    this.nodeDisplayContainer.layer.hide();
                    // myLayer.hide();
                    myStage.draw();

                  };

                  this.addBamMessage = function(bamMessage, myStage) {

                    blvutils.mdwLog("addBamMessage container="+
                        this.nodeDisplayContainer);
                    var tweens = [];
                    if (typeof bamMessage.ActivityId === "undefined")
                      return;

                    // Only add if it matches for this process instance
                    // Get the Node object
                    blvutils.mdwLog("Retrieving node key="+ bamMessage.key+
                        "hash="+ this.hashIds);
                    angular.forEach(this.hashIds, function(index, process) {
                      blvutils.mdwLog("key="+ index+ "obj="+ process);
                    });
                    // Find the relevant activity node object
                    var mdwNode = this.hashIds[bamMessage.key];
                    // This key is used to obtain the actvity nodes so we can
                    // update the
                    // colours
                    var key = "." + bamMessage.key;

                    blvutils.mdwLog("Adding BAM Message,mdwnode="+ mdwNode+
                        "bamkey="+ bamMessage.key+ "key="+ key);
                    // 
                    mdwNode.addBamMessage(bamMessage);

                    var mynodes = this.nodeDisplayContainer.layer.find(key);
                    var startBAM = typeof mdwNode.startText !== "undefined";
                    var endBAM = typeof mdwNode.finishText !== "undefined";
                    /**
                    blvutils.mdwLog("Adding BAM Event=", bamMessage.EventName,
                        "startBAM=", startBAM, "startText=", mdwNode.startText,
                        "endBAM=", endBAM, "finishText=", mdwNode.finishText,
                        " myNodes=" + mynodes);
                    */
                    // alert(bamMessage.EventName+":"+mdwNode.startText.getText());
                    if (startBAM  && bamMessage.EventName == mdwNode.startText.getText()) {
                      // Got a start BAM message
                      mdwNode.startText.setFill('white');
                      mdwNode.startText.setFontStyle('italic');
                      mdwNode.startRect.setFill('green');
                    }
                    blvutils.mdwLog(bamMessage.EventName + ":" + mdwNode.finishText);
                    if (endBAM && bamMessage.EventName == mdwNode.finishText.getText()) {
                      // Got a start BAM message
                      mdwNode.finishText.setFill('white');
                      mdwNode.finishText.setFontStyle('italic');
                      mdwNode.finishRect.setFill('green');
                    }
                    var finished = true;
                    if (startBAM && mdwNode.startRect.getFill() != 'green') {
                      finished = false;
                    }
                    if (endBAM && mdwNode.finishRect.getFill() != 'green') {
                      finished = false;
                    }
                    if (finished) {
                      // Update main text only if all bam events are complete
                      // for this
                      // activity
                      mdwNode.complexText.setFill('white');
                      mdwNode.complexText.setFontStyle('italic');
                      // apply transition to all nodes in the array
                      mynodes.each(function(shape) {
                        shape.setFill('green');
                        tweens.push(new Kinetic.Tween({
                          node : shape,
                          duration : 1,
                          scaleX : 1.1,
                          scaleY : 1.1,
                          easing : Kinetic.Easings.ElasticEaseOut

                        }).play());
                      });
                    }
                    this.nodeDisplayContainer.layer.draw();
                    // myStage.draw();

                  };
                },
                MainBLVProcessNew : function() {
                  this.initialize = function(processDefinition, myStage) {
                    this.nodeDisplayContainer = new blvworkflow.NodeDisplayContainer(
                        processDefinition, 'blvcontainer');
                    this.processDefinition = processDefinition;
                    var legend = new blvworkflow.Legend(processDefinition,
                        'Business Live View (BAM View)');

                    //this.nodeDisplayContainer.addLegend(legend);
                    this.hashIds = {};
                    this.activitiesDrawn = {};
                    this.previousNode = null;
                    this.x = 10;
                    this.y = 100;
                    this.myStage = myStage;
                    this.myStage.add(this.nodeDisplayContainer.textlayer);
                    this.nodeDisplayContainer.layer
                        .add(this.nodeDisplayContainer.groupForLayer);
                    this.myStage.add(this.nodeDisplayContainer.layer);
                    // this.nodeDisplayContainer.textlayer.draw();
                    // this.nodeDisplayContainer.layer.draw();
                   // this.myStage.draw();
                    // this.nodeDisplay.draw();
                  };
                  this.drawBAM = function() {
                 //   var bamEnabledActivities = blvworkflow
                 //       .getFirstBAMEnabledActivities(this.processDefinition);
                    // Populates the transitions
                 //   blvworkflow.displayBAMEnabledActivities(
                 //       this.processDefinition, bamEnabledActivities);
                //    blvutils.mdwLog("*******All BAM activities*****");
               //     for (var i = 0; i < bamEnabledActivities.length; i++) {
               //       blvworkflow.logBAMActivities(bamEnabledActivities[i]);
               //     }
                    // fixSubprocessLinks();
                    // updateProcessInstanceData(this.processDefinition.processInstanceData,
                    // this.processDefinition.parentProcessDefinition);
                    var bamEnabledActivities = blvworkflow.getCompleteBamWorkflow(this.processDefinition);

                    blvworkflow.drawBAMEvents(bamEnabledActivities, this.x,
                        this.y, this.nodeDisplayContainer, this.hashIds, null,
                        this.myStage, this.activitiesDrawn);

                    // var view = new View(myStage,
                    // this.nodeDisplayContainer.layer);
                    // this.nodeDisplayContainer.layer.hide();
                    // alert("Done with transitions");
                    //blvutils.updateGUIProcessData(this.processDefinition);
                    //this.myStage.draw();
                    this.nodeDisplayContainer.layer.draw();
                  };
                  this.activate = function(myStage, processInstanceId,
                      parentProcessDefinitionKey) {
                    //TBD fix this to use the correct processInstanceId
                    var processInstanceData = processes.getProcessInstanceData();
                    //var processInstanceData = new blvtypes.ProcessInstanceData(
                    //    getProcessData("ProcessInstanceData?ProcessInstanceId="+ processInstanceId));
                    this.processDefinition
                        .updateInstanceData(processInstanceData);
                    // Lazily add in the parent process definition as we won't
                    // know it for
                    // new BAM messages
                    this.processDefinition.parentProcessDefinition = hashProcessDefinitions[parentProcessDefinitionKey];
                    blvutils.mdwLog('activating layer...'+
                        this.nodeDisplayContainer.layer.attrs.name);
                    this.nodeDisplayContainer.textlayer.show();
                    this.nodeDisplayContainer.layer.show();
                    blvutils.mdwLog('done activating layer...'+
                        this.nodeDisplayContainer.layer.attrs.name);
                    // myLayer.add();
                    // this.nodeDisplayContainer.stage.draw();
                    myStage.draw();
                    // myStage.draw();
                    blvutils.updateGUIProcessData(this.processDefinition);

                    // alert('done redrawing layer...'+
                    // this.nodeDisplayContainer.layer.attrs['name']);
                  };
                  this.deactivate = function(myStage) {
                    blvutils.mdwLog('deactivating layer...'+
                        this.nodeDisplayContainer.layer.attrs.name);
                    this.nodeDisplayContainer.textlayer.hide();
                    this.nodeDisplayContainer.layer.hide();
                    // myLayer.hide();
                    myStage.draw();
                    // alert("deactivated
                    // "+this.nodeDisplayContainer.layer.name());
                    // myStage.draw();

                  };
                  this.clearBamMessages = function() {
                    blvutils.mdwLog("Clearing BAM messages...");
                    angular.forEach(this.hashIds, function(node, index) {
                      node.clearBamMessages();
                    });
                    this.bamMessages = [];
                  };

                  this.addBamMessage = function(bamMessage, myStage) {

                    blvutils.mdwLog("addBamMessage container="+
                        this.nodeDisplayContainer);
                    this.bamMessages.push(bamMessage);
                    var tweens = [];
                    // TODO Handle process BAM messages
                    if (typeof bamMessage.ActivityId === "undefined")
                      return;

                    // Get the Node object
                    blvutils.mdwLog("Retrieving node key="+ bamMessage.bamkey+
                        "hash="+ this.hashIds);
                    angular.forEach(this.hashIds, function(index, process) {
                      blvutils.mdwLog("hashIds key="+ index+ "obj="+ process);
                    });
                    // Find the relevant activity node object
                    var mdwNode = this.hashIds[bamMessage.bamkey];
                    if (mdwNode && mdwNode.activity && mdwNode.activity.processDefinition.parentProcessDefinition && (!bamMessage.ProcessInstanceId || bamMessage.ProcessInstanceId === "") && mdwNode.activity.processDefinition.parentProcessDefinition.processInstanceData && mdwNode.activity.processDefinition.parentProcessDefinition.processInstanceData.subProcessInstanceHash[mdwNode.activity.processDefinition.callingActivity.id]) {
                      // It's a subprocess
                      // Check for historical BAM message
                      /**
                       * If it's historical then get the subprocess instances
                       * for each subprocessinstance and draw them
                       * 
                       * var newy = mdwNode.rectY + mdwNode.rectHeight; var
                       * subprocesses =
                       * mdwNode.activity.processDefinition.parentProcessDefinition.processInstanceData.subProcessInstanceHash[mdwNode.activity.processDefinition.callingActivity.id];
                       * 
                       * blvutils.mdwLog("Got subprocesses length=",
                       * subprocesses.length); for ( var g = subprocesses.length -
                       * 1; g >= 0; g--) { var bamEventNode = new
                       * MDWUtils.BamEventNode(mdwNode.rectX, newy, 'green',
                       * mdwNode.activity, mdwNode.activity.processDefinition,
                       * mdwNode.bamEvent, subprocesses[g].instanceId);
                       * bamEventNode.addBamMessage(bamMessage);
                       * bamEventNode.shape.setFill('green');
                       * bamEventNode.complexText.setFill('white');
                       * bamEventNode.complexText.setFontStyle('italic');
                       * this.nodeDisplayContainer.addNode(bamEventNode,
                       * myStage); // this.hashIds[bamActivity.bamEvents[j].key] =
                       * bamEventNode; newy = newy + bamEventNode.rectHeight;
                       * blvutils.mdwLog("Added subprocess node for instance ",
                       * subprocesses[g].instanceId); // +
                       * bamEventNode.idText.getHeight(); }
                       */
                    }
                    // This key is used to obtain the actvity nodes so we can
                    // update the
                    // colours
                    // var key = ".A" + bamMessage.ActivityId;

                    var key = "." + bamMessage.bamkey;
                    blvutils.mdwLog("Adding BAM Message, ,mdwnode="+ mdwNode+
                        "bammessagekey="+ bamMessage.key+ "bamkey="+ key+
                        "event name="+ bamMessage.EventName);
                    // 
                    if (!blvutils.isUndefinedOrNull(mdwNode, false)) {
                      mdwNode.addBamMessage(bamMessage);
                      mdwNode.complexText.setFill('white');
                      mdwNode.complexText.setFontStyle('italic');
                    }

                    var mynodes = this.nodeDisplayContainer.layer.find(key);
                    // alert(bamMessage.EventName+":"+mynodes);

                    mynodes.each(function(shape) {
                      blvutils.mdwLog("filling to green");
                      shape.setFill('green');
                      tweens.push(new Kinetic.Tween({
                        node : shape,
                        duration : 1,
                        scaleX : 1,
                        scaleY : 1,
                        easing : Kinetic.Easings.ElasticEaseOut

                      }).play());

                    });
                    // this.nodeDisplayContainer.layer.draw();
                    // this.nodeDisplayContainer.layer.draw();
                    // myStage.draw();
                    blvutils.mdwLog("Done with drawing for node "+ mdwNode);

                  };
                },

                Node : function(x, y, fill, activity, processDefinition) {

                  // alert("Calling Main BAMNode");
                  // alert('MDWUtils.BAMNode...start' + x + ':' + y + ':' + fill
                  // + ':'
                  // + bamMessage.name);
                  this.endPoints = [];
                  // RectX and RectY denote the upper left corner of the node
                  this.processDefinition = processDefinition;
                  this.rectX = x;
                  this.rectY = y;
                  this.rectWidth = activity.w;
                  this.rectHeight = activity.h;
                  this.rectFill = fill;
                  this.activity = activity;
                  if (activity.hasBamData) {
                    this.rectStroke = 'blue';
                    this.strokeWidth = 3;
                    this.fontStyle = 'bold';
                  } else {
                    this.rectStroke = 'black';
                    this.strokeWidth = 2;
                    this.fontStyle = 'normal';

                  }
                  this.bamMessages = [];
                  this.name = activity.key;
                  this.rectStrokewidth = 4;
                  this.implementor = "";

                  this.shape = null;
                  var gotStartOrFinish = ('com.qwest.mdw.workflow.activity.impl.process.ProcessStartControlledActivity' == activity.implementation)|| ('com.qwest.mdw.workflow.activity.impl.process.ProcessFinishControlledActivity' == activity.implementation)|| ('com.centurylink.mdw.workflow.activity.process.ProcessStartActivity' == activity.implementation)|| ('com.centurylink.mdw.workflow.activity.process.ProcessFinishActivity' == activity.implementation);
                  this.group = new Kinetic.Group({
                  // draggable : true
                  });
                  this.group.mdwNode = this;
                  this.idText = new Kinetic.Text({
                    x : this.rectX - 10,
                    y : this.rectY - 25,
                    text : activity.id,
                    fontSize : 12,
                    fontFamily : 'Calibri',
                    fontStyle : this.fontStyle,
                    fill : '#555',
                    // width : this.rectWidth,
                    padding : 10,
                    align : 'left'
                  });
                  var textFill = '#555';
                  if (gotStartOrFinish) {
                    textFill = 'white';
                  }
                  this.complexText = new Kinetic.Text({
                    x : this.rectX + 1,
                    y : this.rectY,
                    text : activity.name,
                    fontSize : 12,
                    fontFamily : 'Calibri',
                    fontStyle : this.fontStyle,
                    fill : textFill,
                    // width : this.rectWidth,
                    padding : 6,
                    align : 'center'
                  });
                  var delta = 0;
                  var widestText = this.complexText.getWidth();
                  var totalHeight = this.complexText.getHeight();
                  var startBAM = typeof activity.bamMonitoring["BAM@START_MSGDEF"] !== "undefined";
                  var endBAM = typeof activity.bamMonitoring["BAM@FINISH_MSGDEF"] !== "undefined";
                  if (activity.hasBamData) {
                    var xcoord = this.rectX;
                    // var ycoord = this.rectY + this.complexText.getHeight();
                    var ycoord = this.rectY + this.rectHeight;
                    if (startBAM) {

                      this.startText = new Kinetic.Text({
                        x : xcoord + 1,
                        y : ycoord,
                        text : activity.bamMonitoring["BAM@START_MSGDEF"],
                        fontSize : 12,
                        fontFamily : 'Calibri',
                        fontStyle : this.fontStyle,
                        fill : '#555',
                        // width : this.rectWidth,
                        padding : 4,
                        align : 'left',
                        name : blvutils.getBAMTextKey(activity.name,
                            activity.bamMonitoring["BAM@START_MSGDEF"])
                      });
                      this.startRect = new Kinetic.Rect({
                        x : xcoord + 1,
                        y : ycoord,
                        width : this.startText.getWidth(),
                        // height : this.complexText.getHeight(),
                        height : this.startText.getHeight(),
                        stroke : this.rectStroke,
                        strokeWidth : 3,
                        fill : 'orange',
                        cornerRadius : 3,
                        name : blvutils.getBAMRectKey(activity.name,
                            activity.bamMonitoring["BAM@START_MSGDEF"])
                      });
                      blvutils.mdwLog("Added startText for event"+
                          activity.bamMonitoring["BAM@START_MSGDEF"]);
                      ycoord = ycoord + this.startText.getHeight();
                      if (this.startText.getWidth() > widestText) {
                        widestText = this.startText.getWidth();
                      }
                      totalHeight += this.startText.getHeight();
                    }
                    
                    if (endBAM) {
                      this.finishText = new Kinetic.Text({
                        x : xcoord + 1,
                        y : ycoord,
                        text : activity.bamMonitoring["BAM@FINISH_MSGDEF"],
                        fontSize : 12,
                        fontFamily : 'Calibri',
                        fontStyle : this.fontStyle,
                        fill : '#555',
                        // width : this.rectWidth,
                        padding : 4,
                        align : 'left',
                        name : blvutils.getBAMTextKey(activity.name,
                            activity.bamMonitoring["BAM@FINISH_MSGDEF"])

                      });
                      this.finishRect = new Kinetic.Rect({
                        x : xcoord + 1,
                        y : ycoord,
                        width : this.finishText.getWidth(),
                        // height : this.complexText.getHeight(),
                        height : this.finishText.getHeight(),
                        stroke : this.rectStroke,
                        strokeWidth : 3,
                        fill : 'orange',
                        cornerRadius : 2,
                        name : blvutils.getBAMRectKey(activity.name,
                            activity.bamMonitoring["BAM@FINISH_MSGDEF"])

                      });
                      if (this.finishText.getWidth() > widestText) {
                        widestText = this.finishText.getWidth();
                      }
                      totalHeight += this.finishText.getHeight();
                      blvutils.mdwLog("Added finishText for event"+
                          activity.bamMonitoring["BAM@FINISH_MSGDEF"]);

                    }
                    

                  }
                  if (widestText > this.rectWidth) {
                    delta = widestText - this.rectWidth;
                  }
                  //this.rectWidth = this.rectWidth + delta;
                  if ('com.qwest.mdw.workflow.activity.impl.process.ProcessStartControlledActivity' == activity.implementation|| 'com.centurylink.mdw.workflow.activity.process.ProcessStartActivity' == activity.implementation) {
                    this.shape = new Kinetic.Ellipse({
                      x : this.rectX + this.rectWidth / 2,
                      y : this.rectY + this.complexText.getHeight() / 2,
                      width : this.rectWidth,
                      height : this.rectHeight,
                      // height : totalHeight,
                      stroke : this.rectStroke,
                      strokeWidth : this.strokeWidth,
                      fill : 'green',
                      shadowColor : 'black',
                      shadowBlur : 10,
                      shadowOffset : [ 10, 10 ],
                      shadowOpacity : 0.2,
                      cornerRadius : 10,

                      name : this.name
                    });
                    this.shape.setFillLinearGradientColorStops(0, 'green', 0.5,
                        'white', 1, 'white');
                  } else if ('com.qwest.mdw.workflow.activity.impl.process.ProcessFinishControlledActivity' == activity.implementation|| 'com.centurylink.mdw.workflow.activity.process.ProcessFinishActivity' == activity.implementation) {
                    this.shape = new Kinetic.Ellipse({
                      x : this.rectX + this.rectWidth / 2,
                      y : this.rectY + this.complexText.getHeight() / 2,
                      width : this.rectWidth,
                      height : this.rectHeight,
                      // height : totalHeight,
                      stroke : this.rectStroke,
                      strokeWidth : this.strokeWidth,
                      fill : 'red',
                      shadowColor : 'black',
                      shadowBlur : 10,
                      shadowOffset : [ 10, 10 ],
                      shadowOpacity : 0.2,
                      cornerRadius : 10,

                      name : this.name
                    });
                    this.shape.setFillLinearGradientColorStops(0, 'red', 0.5,
                        'white', 1, 'white');

                  } else {
                    this.shape = new Kinetic.Rect({
                      x : this.rectX,
                      y : this.rectY,
                      width : this.rectWidth,
                      height : this.rectHeight,
                      // height : totalHeight,
                      stroke : this.rectStroke,
                      strokeWidth : this.strokeWidth,
                      fill : '#ddd',
                      shadowColor : 'black',
                      shadowBlur : 10,
                      shadowOffset : [ 10, 10 ],
                      shadowOpacity : 0.2,
                      cornerRadius : 10,

                      name : this.name
                    });
                  }
                  this.group.add(this.shape);
                  // alert(this.startText, this.finishText);
                  if (startBAM) {
                    this.group.add(this.startRect);
                    this.group.add(this.startText);
                  }
                  if (endBAM) {
                    this.group.add(this.finishRect);
                    this.group.add(this.finishText);
                  }
                  this.group.add(this.complexText);
                  this.group.add(this.idText);

                  var localBamMessages = this.bamMessages;
                  /**
                   * Original doubleClick functionality
                   * 
                   * this.group.on('dblclick', function(e) { var dlg =
                   * $("#popup").dialog( { autoOpen : false, draggable : true,
                   * title : 'BAM and Activity data', height : 240, width : 330,
                   * dialogClass : 'mdw-dialog'
                   * 
                   * }); var x = e.pageX - $(document).scrollLeft(); var y =
                   * e.pageY - $(document).scrollTop();
                   * 
                   * dlg.dialog("option", "position", [ x, y ]);
                   * dlg.html(formatBAMData(activity, localBamMessages,
                   * processDefinition)); dlg.dialog("open"); if
                   * (activity.isSubProcess) { $('#substatus').html(
                   * getSubprocessInstancesHtml(activity, processDefinition,
                   * false)); } });
                   */
                  /**
                  this.group.on('dblclickold', function(e) {
                 
                    // alert("In doublclick");
                   // var x = e.evt.pageX - $(document).scrollLeft();
                   // var y = e.evt.pageY - $(document).scrollTop();
                    var dlg = $("#popup").dialog(
                        {
                          autoOpen : false,
                          draggable : true,
                          title : activity.processDefinition.name + " -> " + activity.processDefinition.version + " -> "+ activity.name,
                          height : 440,
                          width : 550,
                          paging : false,
                          ordering : false,
                          info : false,
                          dialogClass : 'mdw-dialog'
                        });

                    dlg.html(businessDesignFunctions(activity,
                        localBamMessages, processDefinition));
                    $("#businesstabs").tabs();// .addClass("mdw_label");
                    $("#businesstabs-1").html(
                        createMonitoringTab(activity, localBamMessages,
                            processDefinition));
                    $('#bamclear').click(function() {
                      clearBamFields();
                    });
                    bamAttributes = $("#bamattributes").dataTable({
                      "bJQueryUI" : true,
                      "bFilter" : false,
                      "paging" : false,
                      "ordering" : false,
                      "info" : false

                    });
                    nEditing = null;

                    $('#bamadd').click(
                        function(e) {
                          e.preventDefault();

                          var aiNew = bamAttributes.fnAddData([ '', '',
                              '<a class="delete" href="">Delete</a>' ]);
                          var nRow = bamAttributes.fnGetNodes(aiNew[0]);
                          editRow(bamAttributes, nRow);
                          nEditing = nRow;
                        });

                    // Populate the 1st time it's displayed
                    populateBamMessageDefinitionData(activity, "Start");
                    $("select").change(
                        function() {
                          populateBamMessageDefinitionData(activity, $(
                              "#trigger option:selected").text());
                        }).trigger("change");

                    // alert('Opening');
                    dlg.dialog("option", "position", [ x, y ]);

                    dlg.dialog("open");
                    // alert('Opened');
                    updateLinks();
                    $('#savebam').click(function() {
                      saveBam(activity, $("#trigger option:selected").text());
                    });

                  });

                  this.group
                      .on(
                          'dblclickblah',
                          function(e) {
                            // alert("In doublclick");
                            var x = e.evt.pageX;// - $(document).scrollLeft();
                            var y = e.evt.pageY;// - $(document).scrollTop();

                            // $scope.valueToPass = "I must be passed";
                            var modalOptions = {
                              closeButtonText : 'Cancel',
                              actionButtonText : 'Delete Customer',
                              headerText : 'Delete ?',
                              bodyText : 'Are you sure you want to delete this customer?'
                            };

                            // blvmodalservice.showModal({},
                            // modalOptions);//.then(function (result) {
                            // dataService.deleteCustomer($scope.customer.id).then(function
                            // () {
                            // $location.path('/customers');
                            // }, processError);
                            // });

                            var modalInstance = $modal.open({
                              templateUrl : 'blv/mdwBamAttributes.html',
                              controller : 'mdwModalController',
                              resolve : {
                                aValue : function() {
                                  // return $scope.valueToPass;
                                  return "Hello";
                                }
                              }
                            });
                            // modalInstance.result.then(function
                            // (paramFromDialog) {
                            // $scope.paramFromDialog = paramFromDialog;
                            // });

                            /**
                             * var dlg = $modal.open( { autoOpen : false,
                             * draggable : true, title :
                             * activity.processDefinition.name + " -> " +
                             * activity.processDefinition.version + " -> " +
                             * activity.name, height : 440, width : 550, paging :
                             * false, ordering : false, info : false,
                             * dialogClass : 'mdw-dialog' });
                             * 
                             * dlg.html(businessDesignFunctions(activity,
                             * localBamMessages, processDefinition));
                             * $("#businesstabs").tabs();//
                             * .addClass("mdw_label");
                             * $("#businesstabs-1").html(
                             * createMonitoringTab(activity, localBamMessages,
                             * processDefinition));
                             * $('#bamclear').click(function() {
                             * clearBamFields(); }); bamAttributes =
                             * $("#bamattributes").dataTable( { "bJQueryUI" :
                             * true, "bFilter" : false, "paging" : false,
                             * "ordering" : false, "info" : false
                             * 
                             * }); nEditing = null;
                             * 
                             * $('#bamadd').click( function(e) {
                             * e.preventDefault();
                             * 
                             * var aiNew = bamAttributes.fnAddData([ '', '', '<a
                             * class="delete" href="">Delete</a>' ]); var nRow =
                             * bamAttributes.fnGetNodes(aiNew[0]);
                             * editRow(bamAttributes, nRow); nEditing = nRow;
                             * });
                             *  // Populate the 1st time it's displayed
                             * populateBamMessageDefinitionData(activity,
                             * "Start"); $("select").change( function() {
                             * populateBamMessageDefinitionData(activity, $(
                             * "#trigger option:selected").text());
                             * }).trigger("change");
                             *  // alert('Opening'); dlg.dialog("option",
                             * "position", [ x, y ]);
                             * 
                             * dlg.dialog("open"); // alert('Opened');
                             * updateLinks(); $('#savebam').click(function() {
                             * saveBam(activity, $("#trigger
                             * option:selected").text()); });
                             *
                          });
*/
                  this.shape.mdwNode = this;
                  this.addBamMessage = function(bamMessage) {
                    // alert("addBamMessage");
                    this.bamMessages.push(bamMessage);
                    // alert(this.name+this.bamMessages);
                  };

                },
                BamEventNode : function(x, y, fill, activity,
                    processDefinition, bamEvent, instanceId) {

                  // alert("Calling Main BAMNode");
                  // alert('MDWUtils.BAMNode...start' + x + ':' + y + ':' + fill
                  // + ':'
                  // + bamMessage.name);
                  this.bamEvent = bamEvent;
                  this.endPoints = [];
                  // RectX and RectY denote the upper left corner of the node
                  this.processDefinition = processDefinition;
                  this.rectX = x;
                  this.rectY = y;
                  this.bamMessages = [];
                  var key = bamEvent.key;
                  if (activity.processDefinition.parentProcessDefinition) {
                    key += activity.processDefinition.parentProcessDefinition.name;

                  }
                  if (activity.processDefinition.callingActivity) {
                    key += activity.processDefinition.callingActivity.id;

                  }
                  key = blvutils.replaceSpaces(key);

                  if (activity === null) {
                    this.id = "";
                    this.rectWidth = 80;
                    this.rectHeight = 50;
                    this.name = key;

                  } else {
                    this.id = activity.id;
                    // this.rectWidth = activity.w;
                    // this.rectHeight = activity.h;
                    this.rectWidth = 80;
                    this.rectHeight = 50;
                    this.name = key;

                  }
                  this.rectFill = fill;
                  this.activity = activity;
                  this.rectStroke = 'blue';
                  this.strokeWidth = 2;
                  this.fontStyle = 'bold';
                  this.rectStrokewidth = 2;
                  this.implementor = "";

                  this.shape = null;
                  this.group = new Kinetic.Group({
                  // draggable : true
                  });
                  // this.group.mdwNode = this;
                  var newtext = bamEvent.name;
                  var subprocessInstance = (!blvutils.isUndefinedOrNull(instanceId, false) && activity !== null && activity.processDefinition !== null && activity.processDefinition.parentProcessDefinition !== null);
                  if (subprocessInstance) {
                    // Add on the instanceId
                    newtext = instanceId;
                  }
                  // end of tooltip stuff
                  this.complexText = new Kinetic.Text({
                    x : this.rectX + 1,
                    y : this.rectY,
                    text : newtext,
                    fontSize : 12,
                    fontFamily : 'Calibri',
                    fontStyle : this.fontStyle,
                    fill : '#555',
                    width : this.rectWidth,
                    padding : 6,
                    align : 'center'
                  });
                  this.idText = new Kinetic.Text({
                    x : this.rectX - 10,
                    y : this.rectY - 25,
                    text : this.id,
                    fontSize : 12,
                    fontFamily : 'Calibri',
                    fontStyle : this.fontStyle,
                    fill : '#555',
                    // width : this.rectWidth,
                    padding : 10,
                    align : 'left'
                  });

                  var delta = 0;
                  if (this.complexText.getWidth() > this.rectWidth) {
                    delta = this.complexText.getWidth() - this.rectWidth;
                  }
                  this.rectWidth = this.rectWidth + delta;
                  // alert("Built complexText");
                  // alert("name of node="+this.name);
                  this.rectHeight = this.complexText.getHeight();// <
                                                                  // this.rectHeight
                                                                  // ?
                  // this.complexText.getHeight()
                  // : this.complexText.getHeight();
                  this.shape = new Kinetic.Rect({
                    x : this.rectX,
                    y : this.rectY,
                    width : this.rectWidth,
                    height : this.rectHeight,
                    // height : totalHeight,
                    stroke : this.rectStroke,
                    strokeWidth : this.strokeWidth,
                    fill : '#ddd',
                    shadowColor : 'black',
                    shadowBlur : 10,
                    shadowOffset : [ 10, 10 ],
                    shadowOpacity : 0.2,
                    cornerRadius : 10,

                    name : this.name
                  });
                  blvutils.mdwLog("-----Added node " + this.name);
                  this.group.add(this.shape);
                  this.group.add(this.complexText);

                  if (!subprocessInstance) {
                    this.group.add(this.idText);
                  }
                  var localBamMessages = this.bamMessages;
                  /**
                   * this.group.on('clickblah', function(e) { var dlg =
                   * $("#popup").dialog( { // / autoOpen : false, // draggable :
                   * true, title : 'BAM and Activity data',// , height : 240,
                   * width : 330 // dialogClass : 'mdw-dialog'
                   * 
                   * }); var x = e.pageX - $(document).scrollLeft(); var y =
                   * e.pageY - $(document).scrollTop();
                   * 
                   * dlg.dialog("option", "position", [ x, y ]);
                   * 
                   * dlg.html(setupBAMTabs(activity, localBamMessages,
                   * processDefinition, instanceId)); $("#bamtabs").tabs();//
                   * .addClass("mdw_label"); for (var x = 0; x <
                   * localBamMessages.length; x++) { $("#bamtabs-" + x).html(
                   * getTabContent(activity, localBamMessages[x],
                   * processDefinition, instanceId)); }
                   *  // alert('Opening'); dlg.dialog("open"); // /
                   * alert('Opened'); });
                   */

                  // this.shape.mdwNode = this;
                  this.addBamMessage = function(bamMessage) {
                    // alert("addBamMessage");
                    this.bamMessages.push(bamMessage);
                    // alert(this.name+this.bamMessages);
                  };
                  this.clearBamMessages = function() {
                    this.bamMessages = [];
                  };

                },
                NodeLineConnectorE : function(endPoint1, endPoint2) {

                  // Store the node endpoints in the connector
                  this.endPoint1 = endPoint1;
                  this.endPoint2 = endPoint2;

                  // Calculating midpoint of endpoint on node1
                  var fromx = (endPoint1.rectX + endPoint1.rectX + endPoint1.rectWidth) / 2;
                  var fromy = (endPoint1.rectY + endPoint1.rectY + endPoint1.rectHeight) / 2;

                  // Calculating midpoint of endpoint on node2
                  var tox = (endPoint2.rectX + endPoint2.rectX + endPoint2.rectWidth) / 2;
                  var toy = (endPoint2.rectY + endPoint2.rectY + endPoint2.rectHeight) / 2;
                  // //alert(fromx+":"+fromy+":"+tox+":"+toy);
                  // Draw a line connector using the endpoints
                  // this.line = new Kinetic.Line({
                  // points: [endPoint1MidX, endPoint1MidY, endPoint2MidX,
                  // endPoint2MidY],
                  // stroke: 'red',
                  // strokeWidth: 2,
                  // / lineCap: 'butt',
                  // lineJoin: 'miter'
                  // });

                  var headlen = 10; // how long you want the head of the arrow
                                    // to be, you
                  // could calculate this as a fraction of the distance
                  // between the points as well.
                  var angle = Math.atan2(toy - fromy, tox - fromx);
                  // //alert("angle:"+angle);
                  var first = tox - headlen * Math.cos(angle - Math.PI / 6);
                  // //alert("first:"+first);
                  var second = toy - headlen * Math.sin(angle - Math.PI / 6);
                  // //alert("second:"+second);
                  var third = tox - headlen * Math.cos(angle + Math.PI / 6);
                  // //alert("third:"+third);
                  var fourth = toy - headlen * Math.sin(angle + Math.PI / 6);
                  // //alert("fourth:"+fourth);
                  this.line = new Kinetic.Line({
                    points : [ fromx, fromy, tox, toy, first, second, tox, toy,
                        third, fourth ],
                    stroke : 'black',
                    strokeWidth : 2,
                    lineCap : 'butt',
                    lineJoin : 'miter'
                  });

                },
                NodeLineConnector : function(transition) {
                  var horizontal = transition.ys[0] == transition.ys[1] && (transition.xs[0] != transition.xs[1] || transition.xs[1] == transition.xs[2]);
                  var fromx = transition.xs[0];
                  var fromy = transition.ys[0];
                  var tox, toy;
                  if (horizontal) {
                    tox = transition.xs[1] > transition.xs[0] ? transition.xs[1] - cr : transition.xs[1] + cr;
                    toy = transition.ys[1];
                  } else {
                    tox = transition.xs[1];
                    toy = transition.ys[1] > transition.ys[0] ? transition.ys[1] - cr : transition.ys[1] + cr;

                  }
                  var headlen = 10; // how long you want the head of the arrow
                                    // to be, you
                  // could calculate this as a fraction of the distance
                  // between the points as well.
                  var angle = Math.atan2(toy - fromy, tox - fromx);
                  // //alert("angle:"+angle);
                  var first = tox - headlen * Math.cos(angle - Math.PI / 6);
                  // //alert("first:"+first);
                  var second = toy - headlen * Math.sin(angle - Math.PI / 6);
                  // //alert("second:"+second);
                  var third = tox - headlen * Math.cos(angle + Math.PI / 6);
                  // //alert("third:"+third);
                  var fourth = toy - headlen * Math.sin(angle + Math.PI / 6);
                  // //alert("fourth:"+fourth);
                  this.transition = transition;
                  this.line = new Kinetic.Line({
                    points : [ fromx, fromy, tox, toy, first, second, tox, toy,
                        third, fourth ],
                    stroke : 'gray',
                    strokeWidth : 1,
                    lineCap : 'butt',
                    lineJoin : 'miter'
                  });

                },
                Legend : function(processDefinition, heading) {
                  var maxx = 0;
                  var maxy = 0;
                  var i;
                  for (i = 0; i < processDefinition.activities.length; i++) {
                    if (processDefinition.activities[i].x > maxx) {
                      maxx = processDefinition.activities[i].x;
                    }
                    if (processDefinition.activities[i].y > maxy) {
                      maxy = processDefinition.activities[i].y;
                    }
                  }
                  this.containerRect = new Kinetic.Rect({
                    x : 0,
                    y : 0,
                    stroke : '#555',
                    strokeWidth : 4,
                    fill : '#8AC007',
                    opacity : 0.3,
                    width : maxx + 100,
                    height : maxy + 100,
                    shadowColor : 'black',
                    shadowBlur : 10,
                    shadowOffset : {
                      x : 10,
                      y : 10
                    },
                    shadowOpacity : 0.2,
                    cornerRadius : 8
                  });
                  this.legendTextHeading = new Kinetic.Text({
                    x : 3,
                    y : 3,
                    text : heading,
                    fontSize : 12,
                    fontFamily : 'Arial',
                    fontStyle : 'bold',
                    fill : '#555',
                    width : 200,
                    padding : 10,
                    align : 'left'
                  });
                  var pdData = "Name:        " + processDefinition.name + "\nVersion:     " + processDefinition.version + "\nDescription:     " + processDefinition.description;
                  this.legendTextDetails = new Kinetic.Text({
                    x : 3,
                    y : this.legendTextHeading.getHeight() - 10,
                    text : pdData,
                    fontSize : 12,
                    fontFamily : 'Arial',
                    fill : '#555',
                    width : 200,
                    padding : 10,
                    align : 'left'
                  });
                  /** alert("Created text"); */
                  this.legendRect = new Kinetic.Rect({
                    x : 0,
                    y : 0,
                    stroke : '#555',
                    strokeWidth : 5,
                    fill : '#ddd',
                    width : this.legendTextDetails.getWidth(),
                    height : this.legendTextDetails.getTextHeight() + this.legendTextHeading.getTextHeight() + 180,
                    shadowColor : 'black',
                    shadowBlur : 10,
                    shadowOffset : {
                      x : 10,
                      y : 10
                    },
                    shadowOpacity : 0.2,
                    cornerRadius : 10
                  });
                  this.legendGroup = new Kinetic.Group({});
                  this.legendGroup.add(this.legendRect);
                  this.legendGroup.add(this.legendTextHeading);
                  this.legendGroup.add(this.legendTextDetails);
                  // this.legendGroup.on('mousemove', function()
                  // {
                  // // alert('ji');
                  // document.body.style.cursor = 'pointer';
                  // $("#descriptionPopup").tooltip(
                  // {
                  // content : processDefinition.description,
                  // show :
                  // {

                  // effect : "slideDown",

                  // delay : 250

                  // }

                  // });
                  // $("#descriptionPopup").tooltip("open");
                  // });

                  // alert("Created rect ");

                },
                NodeDisplayContainer : function(processDefinition, name) {
                  // //alert("Creating Node Display Container");
                  this.layer = new Kinetic.Layer({
                    name : processDefinition.key,
                    // draggable : true,
                    width : 300,
                    height : 300

                  });
                  this.groupForLayer = new Kinetic.Group({
                    draggable : false
                  });
                  var self = this;
                  this.groupForLayer.on('mouseover', function(e) {
                    document.body.style.cursor = 'pointer';
                 });
                  this.groupForLayer.on('mouseout', function() {
                    document.body.style.cursor = 'default';
                  });

                  this.textlayer = new Kinetic.Layer();
                  this.addNode = function(MDWObject, stage) {
                    this.groupForLayer.add(MDWObject.group);
                    //this.groupForLayer.draw();

                  };
                  this.addConnector = function(x) {
                    if (!allConnectors) {
                      allConnectors = [];
                    }
                    allConnectors.push(x);
                    this.groupForLayer.add(x.line);
                  };
                  this.addLegend = function(x) {
                    this.textlayer.add(x.legendGroup);
                  };
                  this.addLayerToStage = function() {
                    this.layer.add(this.groupForLayer);
                    this.stage.add(this.layer);

                  };

                },

                drawBAMEvents : function(bamActivities, x, y,
                    nodeDisplayContainer, hashIds, previousBAMEvent, myStage, activitiesDrawn) {
                  var newx = x;
                  var newy = y;
                  blvutils.mdwLog("drawBAMEvents- ", bamActivities.length);
                  // Always draw each of these
                  var originalx = x;
                  //nodeDisplayContainer.textlayer.show();
                 // nodeDisplayContainer.layer.show();
                  //nodeDisplayContainer.layer.draw();
                  // blvStage.draw();

                  for (var i = 0; i < bamActivities.length; i++) {
                    newx = originalx;
                    var bamActivity = bamActivities[i];
                    var lastBAMEvent = null;
                    // var bamEventInActivity = null;
                    // previousBAMEvent=null;
                    var drewSomething = false;
                    activitiesDrawn[bamActivity.key] = bamActivity;
                    
                    for (var j = 0; j < bamActivity.bamEvents.length; j++) {
                      // Only draw if this hasn't been drawn yet
                      blvutils.mdwLog("Checking if node"+bamActivity.bamEvents[j].key+" has been drawn yet...");
                      if (!hashIds[bamActivity.bamEvents[j].key]) {
                        
                      
                      blvutils.mdwLog("Drawing "+ bamActivity.key+ "at"+ newx+ ":"+
                          newy);
                      var bamEventNode = new blvworkflow.BamEventNode(newx,
                          newy, 'green', bamActivity,
                          bamActivity.processDefinition,
                          bamActivity.bamEvents[j]);
                      drewSomething = true;
                      nodeDisplayContainer.addNode(bamEventNode, myStage);
                      var extensionKey = "";
                      if (bamActivity.processDefinition.parentProcessDefinition) {
                        extensionKey += bamActivity.processDefinition.parentProcessDefinition.name;

                      }
                      if (bamActivity.processDefinition.callingActivity) {
                        extensionKey += bamActivity.processDefinition.callingActivity.id;

                      }

                      bamActivity.bamEvents[j].key += blvutils
                          .replaceSpaces(extensionKey);
                      blvutils.mdwLog("saving bameventkey"+
                          bamActivity.bamEvents[j].key);

                      hashIds[bamActivity.bamEvents[j].key] = bamEventNode;
                      blvutils.mdwLog("Drew BAMNode with key"+
                          bamActivity.bamEvents[j].key);
                      if (j === 0) {
                        if (previousBAMEvent !== null) {
                          var connector = new blvworkflow.NodeLineConnector(
                              this.createTransition(previousBAMEvent,
                                  bamEventNode));
                          nodeDisplayContainer.addConnector(connector);

                        }
                      }
                       //The below line updates it one node at a time when running in debug
                      // but not when running non-debug
                      //nodeDisplayContainer.layer.draw();

                      /**
                       * if (previousBAMEvent != null ) { // var connector = new
                       * MDWUtils.NodeLineConnector( //
                       * this.previousNode.endPoints[1], bamNode.endPoints[0]);
                       * var connector = new MDWUtils.NodeLineConnector(
                       * createTransition(bamEventInActivity, bamEventNode)); //
                       * this.previousNode.endPoints[1], bamNode.endPoints[0]);
                       * nodeDisplayContainer.addConnector(connector); }
                       * previousBAMEvent = bamEventNode; bamEventInActivity =
                       * bamEventNode;
                       */
                      lastBAMEvent = bamEventNode;
                      newx = newx + bamEventNode.rectWidth;
                      }
                      // } else
                      // {
                      // Node already has been drawn
                      // Make sure connectors exist
                      // if (j == 0 && previousBAMEvent != null)
                      // {
                      // // var connector = new MDWUtils.NodeLineConnector(
                      // // this.previousNode.endPoints[1],
                      // bamNode.endPoints[0]);
                      // var connector = new
                      // MDWUtils.NodeLineConnector(createTransition(
                      // previousBAMEvent,
                      // hashIds[bamActivity.bamEvents[j].key]));
                      // // this.previousNode.endPoints[1],
                      // bamNode.endPoints[0]);
                      // nodeDisplayContainer.addConnector(connector);
                      // }
                      // lastBAMEvent = hashIds[bamActivity.bamEvents[j].key];

                      // }

                    }
                    //if (i%3 == 0) nodeDisplayContainer.layer.draw();
                    //if (i%3 == 0) nodeDisplayContainer.layer.draw();

                    // newx = newx + lastBAMEvent.rectWidth + 15;
                    newx = newx + 40;

                    /**
                     * 
                     * Look at all the next BAM activities
                     */
                    if (bamActivity.nextBAMActivities.length > 0) {
                      blvutils.mdwLog("drawBAMEvents"+ bamActivity.key+
                          "Got next BAM activities size="+
                          bamActivity.nextBAMActivities.length+ "next are...."+
                          bamActivity.nextBAMActivities);
                      var removeAlreadyDrawnBamActivities = [];
                      for (var cc = 0; cc < bamActivity.nextBAMActivities.length; cc++) {

                        blvutils.mdwLog(bamActivity.id + "->" + bamActivity.nextBAMActivities[cc].key);
                        if (!activitiesDrawn[bamActivity.nextBAMActivities[cc].key] && bamActivity.key != bamActivity.nextBAMActivities[cc].key ) {
                          removeAlreadyDrawnBamActivities.push(bamActivity.nextBAMActivities[cc]);
                        }
                      }
                      removeAlreadyDrawnBamActivities = _.uniqBy(removeAlreadyDrawnBamActivities, function (e) {
                        return e.key;
                      });
                      // "has not been drawn yet, drawing it...");
                      blvworkflow.drawBAMEvents(removeAlreadyDrawnBamActivities,
                          newx, newy, nodeDisplayContainer, hashIds,
                          lastBAMEvent, myStage, activitiesDrawn);

                      // newy = newy + 60;
                      // }

                    }
                    if (drewSomething) {
                      newy = newy + lastBAMEvent.shape.getHeight() + lastBAMEvent.idText.getHeight() + 20;
                    }
                  }
                },
                /**
                 * Create a default transition between 2 BAM nodes
                 * 
                 * @param fromBAMNode
                 * @param toBAMNode
                 */
                createTransition : function(fromBAMNode, toBAMNode) {
                  // Create a transition using the x and y coordinationates of
                  // the from/to BAM
                  // nodes
                  // This helps draw the correct connector.
                  var x1 = fromBAMNode.rectX;
                  var y1 = fromBAMNode.rectY;
                  var w1 = fromBAMNode.rectWidth;
                  var h1 = fromBAMNode.rectHeight;
                  var x2 = toBAMNode.rectX;
                  var y2 = toBAMNode.rectY;
                  var w2 = toBAMNode.rectWidth;
                  var h2 = toBAMNode.rectHeight;

                  var transition = new blvtypes.Transition();
                  transition.id = "Dummy";
                  transition.event = "FINISH";
                  transition.completionCode = "";
                  transition.from = fromBAMNode.name;
                  transition.to = toBAMNode.name;

                  transition.xs = [];
                  transition.ys = [];
                  if (Math.abs(x1 - x2) >= Math.abs(y1 - y2)) {
                    // more of a horizontal link
                    transition.xs.push((x1 <= x2) ? (x1 + w1) : x1);
                    transition.ys.push(y1 + h1 / 2);
                    transition.xs.push((x1 <= x2) ? x2 : (x2 + w2));
                    transition.ys.push(y2 + h2 / 2);
                    /**
                     * for (var i=1; i<1; i++) { if (i%2!=0) { ys[i] = ys[i-1];
                     * xs[i] = (xs[n-1]-xs[0])*((i+1)/2)/(n/2) + xs[0]; } else {
                     * xs[i] = xs[i-1]; ys[i] =
                     * (ys[n-1]-ys[0])*((i+1)/2)/((n-1)/2) + ys[0]; } }
                     */
                  } else { // more of a vertical link
                    transition.xs[0] = x1 + w1 / 2;
                    transition.ys[0] = (y1 <= y2) ? (y1 + h1) : y1;
                    transition.xs[1] = x2 + w2 / 2;
                    transition.ys[1] = (y1 <= y2) ? y2 : (y2 + h2);
                    /**
                     * for (int i=1; i<n-1; i++) { if (i%2!=0) { xs[i] =
                     * xs[i-1]; ys[i] = (ys[n-1]-ys[0])*((i+1)/2)/(n/2) + ys[0]; }
                     * else { ys[i] = ys[i-1]; xs[i] =
                     * (xs[n-1]-xs[0])*(i/2)/((n-1)/2) + xs[0]; } }
                     */
                  }

                  transition.lx = (transition.xs[0] + transition.xs[1]) / 2;
                  transition.ly = (transition.ys[0] + transition.ys[1]) / 2;
                  return transition;

                },
                /**
                 * Gets all the first BAM enabled activities in the workflow (there
                 * could be more than one) Logic is
                 * <li>Get the first activity</li>
                 * <li>If it has bam data then return it</li>
                 * <li>If not, then call getFirstBAMActivities to travers the
                 * transitions and get the next BAM events</li>
                 */
                getFirstBAMEnabledActivities : function(processDefinition) {
                  blvutils.mdwLog("****Entering getFirstBAMEnabledActivities-:pd:" + processDefinition.name);
                  // This is the start one
                  var firstAct = this.getFirstActivity(processDefinition);
                  if (firstAct.hasBamData) {
                    blvutils.mdwLog("  1st activity" + firstAct.key + "has BAM data");
                    var ar = [];
                    ar.push(firstAct);
                    return ar;
                  } else {
                    var firstBAMActivities = this.getFirstBAMActivities(firstAct,
                        processDefinition,
                        processDefinition.parentProcessDefinition);
                    return firstBAMActivities;
                  }
                },
                /**
                 * Starts from an activity (firstAct) Builds up an array of BAM
                 * enabled activities
                 */
                getFirstBAMActivities : function(firstAct, processDefinition,
                    parentProcessDefinition, ignoreFromSubProcess) {
                  if (!ignoreFromSubProcess) {
                    ignoreFromSubProcess = false;
                  }
                  blvutils.mdwLog("****Entering getFirstBAMActivities-firstAct " + firstAct.name + ":pd:" + processDefinition.name + "ignoreSubprocess" + ignoreFromSubProcess);
                  // if (firstAct.nextBAMActivities.length > 0) {
                  // //already processed
                  // return firstAct.nextBAMActivities;
                  // }

                  var activities = processDefinition.activities;
                  var allTransitions = processDefinition.transitions;
                  var processName = processDefinition.name;
                  var hash = {};
                  var fromkey, tokey;
                  var i;
                  // var used = {};
                  // var numberOfStartTransitions = {};
                  // Store in lookup.
                  for (var j = 0; j < activities.length; j++) {
                    hash[activities[j].key] = activities[j];
                    // used[activities[j].key] = 0;
                    // numberOfStartTransitions[activities[j].key]=0;
                    // alert("Added "+activities[j].key+"
                    // "+numberOfStartTransitions[activities[j].key]);
                  }
                  if (!alreadyDoneHash || !alreadyDoneHash[blvutils.replaceSpaces( allTransitions[0].from +  allTransitions[0].to)]) {
                    if (!alreadyDoneHash) alreadyDoneHash = {};
                    for (i = 0; i < allTransitions.length; i++) {
                      fromkey = allTransitions[i].from;
                      tokey = allTransitions[i].to;
                      alreadyDoneHash[blvutils.replaceSpaces(fromkey + tokey)] = 0;
                      // alert("fromkey "+fromkey+"
                      // "+numberOfStartTransitions[fromkey]);
                    }
                  }
                  blvutils.mdwLog("AlreadyDoneHash....");
                  angular.forEach(alreadyDoneHash, function(index, num) {
                      blvutils.mdwLog("AlreadyDoneHash key=" + index + " num=" + num);
                  });

               
                  // alert("getFirstBAMActivities pd="+ processName);
                  var nextActivities = [];
                  // Assume this activity is the last in the process definition
                  var lastActivityInProcessDefinition = true;
                  for (i = 0; i < allTransitions.length; i++) {
                    fromkey = allTransitions[i].from;
                    tokey = allTransitions[i].to;
                    var fromActivity = hash[fromkey];
                    var toActivity = hash[tokey];
                    var subProcessBAMActivities = {};
                    blvutils.mdwLog("      Before Checking transition from " + fromkey + "->" + tokey + " size=" + alreadyDoneHash[blvutils.replaceSpaces(fromkey + tokey)]);
                    if (fromkey == firstAct.key && alreadyDoneHash[blvutils.replaceSpaces(fromkey + tokey)] === 0) {
                      blvutils.mdwLog("    Checking transition from" + fromkey + "->" + tokey);
                      // used[fromkey] += 1;
                      alreadyDoneHash[blvutils.replaceSpaces(fromkey + tokey)] += 1;
                      blvutils.mdwLog("AlreadyDoneHash key=" + blvutils.replaceSpaces(fromkey + tokey) + " set to " + alreadyDoneHash[blvutils.replaceSpaces(fromkey + tokey)]);
                      // if (processDefinition.name == "HVS Eng Order")
                      // alert(processDefinition.name +"used "+fromkey+"
                      // "+used[fromkey]);
                      if (fromActivity.isSubProcess && !ignoreFromSubProcess) {
                        this
                            .mdwLog("----From is a subprocess, checking subprocesses... " + fromActivity.key);
                            subProcessBAMActivities = blvutils.getSubProcessBAMActivities(
                            fromActivity, processDefinition);
                        if (subProcessBAMActivities.length > 0) {
                          blvutils.mdwLog("----ActivitySub" + fromActivity.key + "has" + subProcessBAMActivities.length + "BAM activities");
                          nextActivities.push.apply(nextActivities,
                              subProcessBAMActivities);

                        } else if (toActivity.isSubProcess) {
                          this
                              .mdwLog("----No BAMData in from subprocess, checking to subprocess... " + toActivity.key);
                           subProcessBAMActivities = blvutils.getSubProcessBAMActivities(
                              toActivity, processDefinition);
                          if (subProcessBAMActivities.length > 0) {
                            this
                                .mdwLog("----ActivitySub" + toActivity.key + "has" + subProcessBAMActivities.length + "BAM activities");
                            nextActivities.push.apply(nextActivities,
                                subProcessBAMActivities);

                          } else if (toActivity.hasBamData) {
                            blvutils.mdwLog("----To Activity" + toActivity.key + "has BAM data...adding");
                            nextActivities.push(toActivity);
                          } else {
                            this
                                .mdwLog("----No subprocesses found, toActivity" + toActivity.key + "has NO BAM data, looking forward from to activity" + toActivity.key);
                            nextActivities.push.apply(nextActivities, this
                                .getFirstBAMActivities(toActivity,
                                    processDefinition,
                                    toActivity.parentProcessDefinition, true));
                          }
                        } else {
                          // ToActivity not a subprocess
                          if (toActivity.hasBamData) {
                            blvutils.mdwLog("----Activity" + toActivity.key + "has BAM data...adding");
                            nextActivities.push(toActivity);
                            // alert(nextActivities);
                          } else {
                            this
                                .mdwLog("----No subprocesses found, toActivity" + toActivity.key + "has NO BAM data, looking forward from to activity" + toActivity.key);
                            nextActivities.push
                                .apply(nextActivities, this.getFirstBAMActivities(
                                    toActivity, processDefinition, null, null));
                          }

                        }
                      } else {
                        
                        // From is not a subprocess
                        if (toActivity.hasBamData) {
                          blvutils.mdwLog("----Activity" + toActivity.key + "has BAM data...adding");
                          nextActivities.push(toActivity);
                          // alert(nextActivities);
                        } else if (toActivity.isSubProcess) {
                          blvutils.mdwLog("----Activity" + toActivity.key + "is subprocess...looking down...");
                           subProcessBAMActivities = blvworkflow.getSubProcessBAMActivities(
                              toActivity, processDefinition);
                          if (subProcessBAMActivities.length > 0) {
                            blvutils
                                .mdwLog("----ActivitySub" + toActivity.key + "has" + subProcessBAMActivities.length + "BAM activities");
                            nextActivities.push.apply(nextActivities,
                                subProcessBAMActivities);
                          } else {

                            blvutils
                                .mdwLog("----From isnt a subprocess, To no subprocesses found, looking forward from to activity "  + toActivity.key);
                            nextActivities.push.apply(nextActivities, this
                                .getFirstBAMActivities(toActivity,
                                    processDefinition,
                                    toActivity.parentProcessDefinition, true));
                          }
                        } else {
                          blvutils
                              .mdwLog("----From isnt a subprocess, To no subprocesses found,To not BAM, looking forward from to activity" + toActivity.key);
                          nextActivities.push.apply(nextActivities, this
                              .getFirstBAMActivities(toActivity, processDefinition,
                                  null, null));

                        }
                      }
                    }
                      
                     // lastActivityInProcessDefinition = false;
                       
                       // Got a transition, e.g. A1->A3 when my activity is A1 //
                        //blvutils.mdwLog("Matched", fromkey, "->", firstAct.key); 
                      else if (alreadyDoneHash[blvutils.replaceSpaces(fromkey + tokey)] === 0) {
                        if (toActivity.hasBamData) { 
                          if (!fromActivity.isSubProcess) {
                        
                      /** If the to transition hasBAMData and from isn't a
                       * subprocess then just return the to transition
                       */
                            blvutils.mdwLog("Got BamData in activity"+ toActivity.key);
                        
                            nextActivities.push(toActivity); 
                        } else { 
                          // to has BAMData , from is a subprocess, so look up // subprocess //
                          nextActivities.push(hash[allTransitions[i].to]); 
                          subProcessBAMActivities = this.getSubProcessBAMActivities(fromActivity,processDefinition );
                          if (subProcessBAMActivities.length === 0) { 
                            // Haven't found any so add to next in process // defintion
                            nextActivities.push(toActivity); 
                            } else { 
                              // otherwise add in first ones found in subprocess and 
                              // process this subprocess definition for transitions
                              nextActivities.push.apply(nextActivities,subProcessBAMActivities); 
                              // This should add in transitions for subprocess
                        //getBAMEnabledActivities(subProcessBAMActivities); 
                              } 
                          } 
                          }
                        else { 
                          /** If the to doesn't have BAM data */
                        
                        
                        blvutils.mdwLog("------------No BamData in activity"+toActivity.key);
                        }
                        /**
                        if (fromActivity.isSubProcess) { 
                          /** If from is subprocess then look down here for the first BAM event
                          
                        blvutils.mdwLog("------------Got Subprocess in activity",
                        fromActivity.key); // if
                        (hash[allTransitions[i].from].hasBamData) { var
                        subProcessBAMActivities =
                        getSubProcessBAMActivities(fromActivity,processDefinition );
                        if (subProcessBAMActivities.length > 0) { // process this
                        subprocess definition for transitions
                        nextActivities.push.apply(nextActivities,
                        subProcessBAMActivities); // This should add in transitions
                        for subprocess
                        getBAMEnabledActivities(subProcessBAMActivities); } else { //
                        look from the next activity
                        nextActivities.push.apply(nextActivities,
                        getFirstBAMActivities( toActivity, processDefinition,
                        null)); } } else if (toActivity.isSubProcess) { /** if to
                        is a subprocess
                        
                        blvutils.mdwLog("------------Got Subprocess in activity",
                        toActivity.key); var subProcessBAMEvents = new Array(); //
                        Hetero support var pd; if
                        (!isUndefinedOrNull(toActivity.subProcessName, false)) {
                        
                        pd = getProcessDefinition(null, toActivity.subProcessName,
                        0, processDefinition, toActivity);
                        
                        subProcessBAMEvents = getFirstBAMEnabledActivities(pd); }
                        else if (!isUndefinedOrNull(toActivity.subProcesses,
                        false)) { blvutils.mdwLog("------------Got subprocesses for
                        act", toActivity.key); for ( var x = 0; x <
                        toActivity.subProcesses.length; x++) { var pd =
                        getProcessDefinition(null,
                        toActivity.subProcesses[x].subProcessName, 0,
                        processDefinition, toActivity);
                        subProcessBAMEvents.push.apply(subProcessBAMEvents,
                        getFirstBAMEnabledActivities(pd)); } }
                        
                        if (subProcessBAMEvents.length == 0) { if
                        (toActivity.hasBamData) { nextActivities.push(toActivity); }
                        else { // Haven't found any so add to next in process //
                        defintion
                        
                        nextActivities.push.apply(getNextToBAMActivities(
                        processDefinition, fromActivity, false)); } } else { //
                        otherwise add in ones found in subprocess
                        nextActivities.push.apply(nextActivities,
                        subProcessBAMEvents); // This should add in transitions for
                        subprocess if
                        (!isUndefinedOrNull(toActivity.subProcessName, false)) {
                        getBAMEnabledActivities(getFirstBAMEnabledActivities(pd)); }
                        else if (!isUndefinedOrNull(toActivity.subProcesses,
                        false)) { for ( var x = 0; x <
                        toActivity.subProcesses.length; x++) { var pd =
                        getProcessDefinition(null,
                        toActivity.subProcesses[x].subProcessName, 0,
                        processDefinition, toActivity);
                        getBAMEnabledActivities(getFirstBAMEnabledActivities(pd)); } } } }
                        else { nextActivities.push.apply(nextActivities,
                        getFirstBAMActivities( toActivity, processDefinition,
                        null)); } }
                    */   
                    
                  
                
                    

                  }
                  }
                  // Hook into parent process
                  // if (lastActivityInProcessDefinition && parentProcessDefinition
                  // != null &&
                  // processDefinition.callingActivity != null) {
                  // nextActivities.push.apply(nextActivities,getNextToBAMActivities(processDefinition.parentProcessDefinition,
                  // processDefinition.callingActivity));

                  // }
                  blvutils.mdwLog("----Exiting getFirstBAMActivities-firstAct " + firstAct.name + ":pd:" + processDefinition.name);
                  blvutils.mdwLog("----Next BAM activities...");
                  for (var h = 0; h < nextActivities.length; h++) {
                    blvutils.mdwLog(processDefinition.name + "-" + firstAct.name + "-->" + nextActivities[h].processDefinition.name + "-" + nextActivities[h].name);
                  }
                  return nextActivities;

                },

                /**
                 * Returns the first activity in the whole workflow
                 */
                getFirstActivity : function(processDefinition) {
                  var activities = processDefinition.activities;
                  var allTransitions = processDefinition.transitions;
                  var processName = processDefinition.name;
                  var hash = {};
                  var j;
                  // Store in lookup.
                  for (j = 0; j < activities.length; j++) {
                    hash[activities[j].key] = activities[j];
                    // blvutils.mdwLog("Added", activities[j].key);
                  }
                  var firstActivity = null;
                  for (j = 0; j < activities.length; j++) {
                    var act = activities[j];
                    var first = true;
                    /**
                     * Iterate thru all transitions and figure out which is the
                     * first
                     */
                    for (var i = 0; i < allTransitions.length; i++) {
                      var tokey = allTransitions[i].to;
                      if (tokey == act.key) {
                        /**
                         * There exists a transition from something->tokey hence
                         * it's not the first
                         */

                        first = false;
                        continue;
                      }

                    }
                    if (first) {
                      firstActivity = hash[activities[j].key];
                      break;
                    }
                  }
                  if (firstActivity) {
                    blvutils.mdwLog("  got First Activity = " + firstActivity.key);
                  }
                  return firstActivity;

                },
            

                /**
                 * 
                 */
                /**
                 * firstBAMActivities holds an array of activities that are the
                 * first in the workflow
                 */
                displayBAMEnabledActivities : function(processDefinition,
                    firstBAMActivities) {
                  // var firstBAMActivities =
                  // getFirstBAMEnabledActivities(processDefinition);
                  blvworkflow.getBAMEnabledActivities(firstBAMActivities);
                },
                /**
                 * Takes an array for the firstBAMActivities Iterates thru them and
                 * gets the next BAM activities and adds transitions
                 * 
                 */
                getBAMEnabledActivities : function(firstBAMActivities) {
                  blvutils
                      .mdwLog("-----Entering getBAMEnabledActivities-firstBAMActivities " + firstBAMActivities.length);
                  for (var i = 0; i < firstBAMActivities.length; i++) {
                    var act = firstBAMActivities[i];
                    var processDefinition = act.processDefinition;
                    var parentProcessDefinition = processDefinition.parentProcessDefinition;
                    blvutils.mdwLog("----Transition---Looking at drawing activity " + act.key);
                    /*
                     * get the next BAM activities from activity act
                     */
                    var possiblyloopingbamActivities = this.getFirstBAMActivities(
                        act, processDefinition, parentProcessDefinition);
                    // var wf = act.key + "->";
                    var bamActivities = [];
                    var x;
                    // Avoid looping
                    for (x = 0; x < possiblyloopingbamActivities.length; x++) {
                      var activity = possiblyloopingbamActivities[x];
                      if (activity.key != act.key) {
                        // add it
                        bamActivities.push(activity);
                      }
                    }
                    /**
                     * If can't find any BAM activities then look in parent process
                     * definition
                     */
                    blvutils.mdwLog(bamActivities.length + act.hasBamData + this.isLastBAMActivityInProcessDefinition(act) + act.nextBAMActivities.length + act.processDefinition.parentProcessDefinition + act.processDefinition.callingActivity);
                    if (bamActivities.length === 0 && act.hasBamData && this.isLastBAMActivityInProcessDefinition(act) && act.nextBAMActivities.length === 0 && act.processDefinition.parentProcessDefinition && act.processDefinition.callingActivity) {
                      blvutils
                          .mdwLog("----Transition---Found no further BAM activities in this pd for " + act.key);
                      //blvutils.mdwLog("----Transition---Looking at parent " + act.processDefinition.callingActivity.key);
                      /**
                       * If the calling activity is heteregoneous then it's possible
                       * that we need to look at another one
                       */
                      var fullSubProcessName, subProcessName, slash;
                      if (
                          act.processDefinition.callingActivity.subProcesses) {
                        // Called from a hetero process
                        // Look through all subprocesses except this one
                       
                        for (x = 0; x < act.processDefinition.callingActivity.subProcesses.length; x++) {
                          fullSubProcessName = act.processDefinition.callingActivity.subProcesses[x].subProcessName;
                          subProcessName = fullSubProcessName;
                          slash = fullSubProcessName.indexOf("/");
                          if (slash >= 0) {
                            subProcessName = fullSubProcessName
                                .substring(slash + 1);
                          }
                          blvutils.mdwLog("---act.processDefinition.name" + act.processDefinition.name + "subprocessName:" + subProcessName);
                          if (act.processDefinition.name !== subProcessName) {
                            // var pd = getProcessDefinition(null, subProcessName,
                            // 0,
                            // act.processDefinition.callingActivity.processDefinition,
                            // act.processDefinition.callingActivity);
                            blvutils.mdwLog("----Transition---hetero subprocess " + subProcessName + "calling" + act.processDefinition.callingActivity.key);

                            // /getBAMEnabledActivities(getFirstBAMEnabledActivities(pd));
                            bamActivities.push
                                .apply(
                                    bamActivities,
                                    this
                                        .getFirstBAMActivities(
                                            act.processDefinition.callingActivity,
                                            act.processDefinition.callingActivity.processDefinition,
                                            act.processDefinition.callingActivity.parentProcessDefinition,
                                            true));
                          }
                          // if (act.name ==
                          // act.processDefinition.callingActivity.subProcesses[x].subProcessName)
                          // blvutils.mdwLog("----Transition---hetero subprocess ",
                          // act.processDefinition.callingActivity.subProcesses[x].subProcessName);
                          // var pd = getProcessDefinition(null,
                          // / act.subProcesses[x].subProcessName, 0,
                          // processDefinition, act);
                          // getBAMEnabledActivities(getFirstBAMEnabledActivities(pd));

                        }

                      } else {
                        fullSubProcessName = act.processDefinition.callingActivity.subProcessName;
                        subProcessName = fullSubProcessName;
                        slash = fullSubProcessName.indexOf("/");
                        if (slash >= 0) {
                          subProcessName = fullSubProcessName.substring(slash + 1);
                        }
                        // var pd = getProcessDefinition(null, subProcessName, 0,
                        // act.processDefinition.callingActivity.processDefinition,
                        // act.processDefinition.callingActivity);
                        blvutils.mdwLog("----Transition---hetero subprocess " + subProcessName + "calling" + act.processDefinition.callingActivity.key);

                        // /getBAMEnabledActivities(getFirstBAMEnabledActivities(pd));
                        bamActivities.push
                            .apply(
                                bamActivities,
                                this.getFirstBAMActivities(
                                    act.processDefinition.callingActivity,
                                    act.processDefinition.callingActivity.processDefinition,
                                    act.processDefinition.callingActivity.parentProcessDefinition,
                                    true));
                      }
                    }
                    for (var j = 0; j < bamActivities.length; j++) {
                      // wf = wf + bamActivities[j].key + "->";
                      this.addBAMTransition(act, bamActivities[j]);
                    }
                    // if (bamActivities.length > 0) {
                    // alert(wf);
                    // }

                    if (act.isSubProcess) {
                      blvutils.mdwLog("----Transition---Got subprocess activity " + act.key);
                      // Activity is a subprocess
                      if (bamActivities.length === 0) {
                        // Nothing from subprocesses, just add next in ths process
                        // definition
                        var nextToBAMActivities = this.getNextToBAMActivities(
                            processDefinition, act, false);
                        this.addBAMTransitions(act, nextToBAMActivities);
                      } else {
                        blvutils.mdwLog("----Transition---looking at subprocess " + act.subProcessName);
                        var pd;
                        if (act.subProcessName) {
                          //TBD fix
                          //pd = processes.getProcessDefinition(null, act.subProcessName,
                          //    0, processDefinition, act);

                          this
                              .getBAMEnabledActivities(this.getFirstBAMEnabledActivities(pd));
                        } 
                        /**
                         * TBD check this functionality
                         * else if (toActivity.subProcesses) {
                          blvutils.mdwLog("----Transition---looking at subprocesses " + act.subProcesses);
                          for (x = 0; x < act.subProcesses.length; x++) {
                            blvutils.mdwLog("----Transition---looking at subprocess " + act.subProcesses[x].subProcessName);
                            pd = getProcessDefinition(null,
                                act.subProcesses[x].subProcessName, 0,
                                processDefinition, act);
                            this
                                .getBAMEnabledActivities(this.getFirstBAMEnabledActivities(pd));

                          }
                        }
                        */
                      }
                    } else {
                      this.getBAMEnabledActivities(bamActivities);
                    }
                  }
                },

                isLastBAMActivityInProcessDefinition : function(activity) {
                  var nextBAMActivitiesInPD = this.getNextToBAMActivities(
                      activity.processDefinition, activity, false);
                  return nextBAMActivitiesInPD.length === 0;
                },
                /**
                 * Finds the next BAM activity in the processDefinition
                 */
                getNextToBAMActivities : function(processDefinition, fromActivity,
                    checkParentProcesses, checkedAlreadyPassed) {
                  blvutils.mdwLog("-----Entering getNextToBAMActivities-pd " + processDefinition.name + "activity:" + fromActivity.name);
                  var activities = processDefinition.activities;
                  var allTransitions = processDefinition.transitions;
                  var checkedAlready;
                  if (!checkedAlreadyPassed) {
                    checkedAlready = {};
                  } else {
                    checkedAlready = checkedAlreadyPassed;
                  }

                  var hash = {};
                  var nextBamActivities = [];
                  if (fromActivity === null) {
                    return nextBamActivities;
                  }
                  for (var j = 0; j < activities.length; j++) {
                    hash[activities[j].key] = activities[j];
                  }
                  var keyToSearchFor = fromActivity.key;
                  var acts;
                  for (var i = 0; i < allTransitions.length; i++) {
                    var fromkey = allTransitions[i].from;
                    var tokey = allTransitions[i].to;
                    
                    // Stop it looping indefinitely
                    if (fromkey == keyToSearchFor && (!checkedAlready[tokey])) {
                      checkedAlready[tokey] = 'true';
                      // found a transition
                      if (hash[tokey].hasBamData) {

                        nextBamActivities.push(hash[tokey]);
                      } else {
                        acts = this.getNextToBAMActivities(processDefinition,
                            hash[tokey], checkParentProcesses, checkedAlready);
                        if (acts.length > 0)
                          nextBamActivities.push.apply(nextBamActivities, acts);
                      }
                    }
                  }
                  if (checkParentProcesses && nextBamActivities.length === 0 && processDefinition.parentProcessDefinition !== null) {
                    // Couldn't find any so check any upward process definition
                    acts = this.getNextToBAMActivities(
                        processDefinition.parentProcessDefinition,
                        processDefinition.callingActivity, checkParentProcesses);
                    if (acts.length > 0)
                      nextBamActivities.push.apply(nextBamActivities, acts);
                  }
                  return nextBamActivities;
               //   mdwLog("-----Exiting getNextToBAMActivities-pd " + processDefinition.name + "activity:" + fromActivity.name);
               //   mdwLog("-----Got NextBAMactivities");
               //   for (var h = 0; h < nextActivities.length; h++) {
               //     mdwLog("-----Name:" + nextBamActivities[h].name + "pd:" + nextBamActivities[h].processDefinition.name);
               //   }
                },

                addBAMTransition : function(fromActivity, toActivity) {
                  // bamTransitions.push(allTransitions[i]);
                  // Register the activity as being not the first BAM activity
                  toActivity.firstBAMNode = false;
                  fromActivity.addNextBAMActivity(toActivity);
                  blvutils.mdwLog("-----Adding BAM transition from" + fromActivity.id + "to" + toActivity.id);

                },
                addBAMTransitions : function(fromActivity, toActivities) {
                  for (var i = 0; i < toActivities.length; i++) {
                    this.addBAMTransition(fromActivity, toActivities[i]);
                  }
                },
                logBAMActivities : function(bamActivity) {
                  if (!bamActivity.nextBAMActivities) {
                    blvutils.mdwLog("BAM activity " + bamActivity.name  + " has no next BAM activities");
                  } else {
                    blvutils.mdwLog("----------------------");
                    blvutils.mdwLog("BAM activity " + bamActivity.name + " has the following next BAM activities:");
                    var j;
                    for ( j = 0; j < bamActivity.nextBAMActivities.length; j++) {
                      var next = bamActivity.nextBAMActivities[j];
                      blvutils.mdwLog(bamActivity.processDefinition.name + "/" + bamActivity.name + "--->" + next.processDefinition.name + "/" + next.name);

                    }
                    for ( j = 0; j < bamActivity.nextBAMActivities.length; j++) {
                      this.logBAMActivities(bamActivity.nextBAMActivities[j]);

                    }
                    blvutils.mdwLog("----------------------");
                  }
                },
                storePossibleBAMEvents : function(processDefinition) {
                  var possibleBAMEvents = [];
                  possibleBAMEvents = this
                      .getAllPossibleBAMEvents(processDefinition);
                  // setPreviousAndNextEvents();
                  // alert("BAM activities size=" + possibleBAMEvents.length);
                  var hashBAMEvents = {};
                  var firstBAMActivity;
                  for (var i = 0; i < possibleBAMEvents.length; i++) {
                    blvutils.mdwLog("key=" + possibleBAMEvents[i].key + " name=" + possibleBAMEvents[i].name + " bamEvent=" + possibleBAMEvents[i]);
                    hashBAMEvents[possibleBAMEvents[i].key] = possibleBAMEvents[i];
                   // angular.forEach(hashBAMEvents, function(index, event) {
                   //   blvutils
                   //       .mdwLog("BAMEvent key=" + index + "obj=" + event.name);
                   // });
                    // while I'm here let's get the firstBAMEvent
                    if (possibleBAMEvents[i].activity !== null && possibleBAMEvents[i].activity.firstBAMNode) {
                      firstBAMActivity = possibleBAMEvents[i].activity;
                    }

                  }
                },
                /**
                 * Gets all the BAM enabled activities for the complete
                 * workflow,including subprocesses This allows us to draw it first.
                 */
                getAllPossibleBAMEvents : function(processDefinition) {
                  //Commented out for the moment
                  var bamEvents = [];
                 /**
                  blvutils.mdwLog("Looking at BAMEnabled activities for pd ",
                      processDefinition.key);
                  if (processDefinition.hasBamData) {

                    bamEvents.push.apply(bamEvents, processDefinition.bamEvents);
                  }
                  // addBAMOrderingForActivities(processDefinition);
                  for (var i = 0; i < processDefinition.activities.length; i++) {
                    var act = processDefinition.activities[i];

                    if (act.hasBamData) {
                      blvutils.mdwLog("Adding BAMEvent for act " + act.name + "len" + act.bamEvents.length);
                      bamEvents.push.apply(bamEvents, act.bamEvents);
                    }
                    if (act.isSubProcess) {
                      if (act.subProcessName) {
                        //TBD fix
                     //   var pd = null;
                        var pd1 = processes.getProcessDefinitionByName(null, act.subProcessName,
                            processDefinition, act, function(response) {
                            var pd = response.data;
                            var processDef = new blvtypes.ProcessDefinition(pd, processDefinition, act);
                            if (processDefinition) {
                              // Allow the caller to add the correct parent process Definition
                              // which will be used when a process is in a subprocess and we need to 
                              // find the process above (i.e.e for BAM events)
                              processDef.parentProcessDefinition = processDefinition;
                            }
                            bamEvents.push
                            .apply(bamEvents, blvworkflow.getAllPossibleBAMEvents(processDef));
                            return processDef;

                          });

                      } else if (act.subProcesses) {
                        for (var x = 0; x < act.subProcesses.length; x++) {
                          var pd2 = processes.getProcessDefinitionByName(null, act.subProcesses[x].subProcessName,
                            processDefinition, act, function(response) {
                            var pd = response.data;
                            var processDef = new blvtypes.ProcessDefinition(pd, processDefinition, act);
                            if (processDefinition) {
                              // Allow the caller to add the correct parent process Definition
                              // which will be used when a process is in a subprocess and we need to 
                              // find the process above (i.e.e for BAM events)
                              processDef.parentProcessDefinition = processDefinition;
                            }
                            bamEvents.push.apply(bamEvents,
                                blvutils.getAllPossibleBAMEvents(pd2));
                            return processDef;

                          });
                          //var pd2 = processes.getProcessDefinition(null,
                          //    act.subProcesses[x].subProcessName, 0,
                          //    processDefinition, act);
                     //     bamEvents.push.apply(bamEvents,
                     //         blvutils.getAllPossibleBAMEvents(pd2));

                        }
                      }
                    }

                  }
                  blvutils.mdwLog("Returning BAMEnabled events for pd " + processDefinition.key + bamEvents.length);
                  */
                  return bamEvents;
                },
                getCompleteBamWorkflow : function(processDefinition) {
                  var hashActivitiesTransitions = {};
                  var hashActivities = {};
                  var allWorkflow = this.getCompleteWorkflowTransitions(processDefinition, hashActivities, hashActivitiesTransitions);
                  
                  var bamEnabledActivities = this.getBamEnabledActivitiesFromCompleteWorkflow(processDefinition, hashActivities, allWorkflow);

                  return bamEnabledActivities;
                },
                getBamEnabledActivitiesFromCompleteWorkflow : function(processDefinition, hashActivities, completeWorkflow) {
                  
                  var currentBamActivities = [];
                  var doneTransitions = {};
                  // Get the first activity
              //    var activity = this.getFirstActivity(processDefinition);
                  // If it's a BAM event, then add it
              //    if (activity.hasBamData) {
               //     currentBamActivities.push(activity);
               //   }
                  // Look through the next activities for this activity and add to BAMEnabledActivities array
              //    this.getNextBamActivities({}, doneTransitions, currentBamActivities, completeWorkflow, activity.key );
                  var actsToBeIgnored = {};
              //    if (currentBamActivities) {
              //      angular.forEach(currentBamActivities, function(activity) {
              //        actsToBeIgnored[activity.key] = true;                   
              //      });
              //    }
              //    var firstActivityBamActivities = _.clone(currentBamActivities);
              //    angular.forEach(firstActivityBamActivities, function(activity) {
              ////       if (activity.hasBamData && !actsToBeIgnored[activity.key]) {
              //        currentBamActivities.push(activity);
              //      }
                  angular.forEach(completeWorkflow, function(nextActivities, activityKey) {
                    blvworkflow.getNextBamActivities(actsToBeIgnored, doneTransitions, completeWorkflow, hashActivities[activityKey], hashActivities );
                    
                  }); 
                  blvutils.mdwLog("CurrentBAMActs="+ currentBamActivities);
                 // var uniqueBamActivities = _.uniq(currentBamActivities);
                 // blvutils.mdwLog("UniqueBAMActs=", uniqueBamActivities);
                  /**
                  if (uniqueBamActivities.length > 1) {
                    for (var j = 0; j < uniqueBamActivities.length; j++) {
                      var nextBamActs = [];
                      actsToBeIgnored = {};
                      doneTransitions = {};
                      var activityKey = uniqueBamActivities[j].key;
                      var bamActs = blvworkflow.getNextBamActivities(actsToBeIgnored, doneTransitions, nextBamActs, completeWorkflow, hashActivities[activityKey], hashActivities );
                      this.addBAMTransitions(uniqueBamActivities[j], bamActs);
                    }
                  }*/
                  return _.filter(_.values(hashActivities), {'hasBamData' : true});
                 
                },
                getNextBamActivities : function(actsToBeIgnored, doneTransitions,  nextActivities, currentActivity, hashActivities) {
                  // Short circuit any recursion looping
                  if (hashActivities[currentActivity.key] && hashActivities[currentActivity.key].nextBAMActivities && hashActivities[currentActivity.key].nextBAMActivities.length > 0) {
                    return hashActivities[currentActivity.key].nextBAMActivities;
                  } else {
                    var currentBamActivities = [];
                    blvutils.mdwLog("Looking at all poss bam activities for "+ currentActivity.key);
                    //Get the next activities
                    var possibleNextBamActivities = nextActivities[currentActivity.key];
                    if (possibleNextBamActivities) {
                      //TBD Fix this
                      if (currentActivity.isSubProcess) {
                        // It's a subprocess, so only go this route
                        // TDB Fix this...it should only go here if the subprocess has BAM activities
                        possibleNextBamActivities =_.filter(possibleNextBamActivities, function(act) { 
                            return (act.processDefinition.key != currentActivity.processDefinition.key); 
                          });
                      }

                      // Iterate through and addBamActivity for each one found 
                      angular.forEach(possibleNextBamActivities, function(possibleBamActivity, index) {
                        blvutils.mdwLog("Looking at"+ currentActivity.key+ "->"+possibleBamActivity.key);
                        if (!doneTransitions[blvutils.replaceSpaces(currentActivity.key + possibleBamActivity.key)] && !actsToBeIgnored[possibleBamActivity.key]) {
                          doneTransitions[blvutils.replaceSpaces(currentActivity.key + possibleBamActivity.key)] = true;
                          if (possibleBamActivity.hasBamData) {
                            currentBamActivities.push(possibleBamActivity);
                          } else {
                            currentBamActivities.push.apply(currentBamActivities, blvworkflow.getNextBamActivities(actsToBeIgnored, doneTransitions, nextActivities, possibleBamActivity, hashActivities));
                        
                          }
                        }
                        //Got all
                        blvutils.mdwLog(currentActivity.key+ currentBamActivities);
                      });
                      blvutils.mdwLog("BAM -- activity "+currentActivity.key+ "-- next BAM activities ->"+currentBamActivities);
                      if (currentBamActivities.length > 0) {
                        for (var j = 0; j < currentBamActivities.length; j++) {
                          blvworkflow.addBAMTransition(hashActivities[currentActivity.key], hashActivities[currentBamActivities[j].key]);

                        }
                      }
                    }
                      return currentBamActivities;
                      }
                
                },
               /**
                 * Builds the complete workflow
                 */
                getCompleteWorkflowTransitions : function(processDefinition, hashActivities, hashActivitiesTransitions) {
                  
                    // Store in lookup.
                  var j;
                  for (j = 0; j < processDefinition.activities.length; j++) {
                    hashActivities[processDefinition.activities[j].key] = processDefinition.activities[j];
                   }
                
                 // var firstAct = this.getFirstActivity(processDefinition);
                  for (var i=0;i<processDefinition.activities.length;i++) {
                    hashActivitiesTransitions[processDefinition.activities[i].key] = this.getTransitions(processDefinition.activities[i], true, hashActivities);
                    if (processDefinition.activities[i].isSubProcess) {
                      // do the subprocess
                      var subprocessDef = processes.getProcessDefinitionByName(null, processDefinition.activities[i].subProcessName, processDefinition.activities[i].processDefinition, processDefinition.activities[i]);
                      this.getCompleteWorkflowTransitions(subprocessDef, hashActivities, hashActivitiesTransitions);
                    }
                  }
                  blvutils.mdwLog("Transitions are" + hashActivitiesTransitions);
                  return hashActivitiesTransitions;
                  
                  

                },
                /**
                 * Gets all the transitions from an activity
                 * @param activity
                 * @returns {Array}
                 */
                getTransitions : function(activity, lookInSubprocess, hashActivities) {
                  var activityTransitions = []; 
                 
                  /**
                   * Try in this process definition
                   */
                  for (var i = 0; i < activity.processDefinition.transitions.length; i++) {
                    var from = activity.processDefinition.transitions[i].from;
                    var to = activity.processDefinition.transitions[i].to;
                    if (from == activity.key) {
                      // Got a transition for this activity
                       activityTransitions.push( hashActivities[to]);

                     }
                  }
                  // Check if it's a subprocess
                  if (lookInSubprocess && activity.isSubProcess) {
                    var processDef = processes.getProcessDefinitionByName(null, activity.subProcessName, activity.processDefinition, activity);
                    activityTransitions.push(this.getFirstActivity(processDef));
                    
                  }
                  // last one in a subprocess
                  if (activityTransitions.length === 0 && activity.processDefinition.parentProcessDefinition && activity.processDefinition.callingActivity) {
                    activityTransitions.push.apply(activityTransitions, this.getTransitions(activity.processDefinition.callingActivity, false, hashActivities));
                  }
                  return activityTransitions;
                },
                /**
                 * Gets all the BAM enabled activities for the complete
                 * workflow,including subprocesses This allows us to draw it first.
                 */
                /**
                 * Assumption is that activity is a subprocess (either a hetero or a normal
                 * subprocess)
                 */
                getSubProcessBAMActivities : function(activity, processDefinition)
                {
                  blvutils.mdwLog("------------Got Subprocess in activity"+ activity.key);
                  var subProcessBAMEvents = [];
                  // Hetero support
                  var pd, promise;
                  if (activity.subProcessName)
                  {
                    //TBD fix
                   // pd = null;
                    pd = processes.getProcessDefinitionByName(null, activity.subProcessName,
                        processDefinition, activity);
 //                   promise.then(function(processdef){
 //                     pd = processdef;
                      subProcessBAMEvents = blvworkflow.getFirstBAMEnabledActivities(pd);
//                    }, function(reject){
                    // TBD
 //                   });
                          // alert(subProcessBAMEvents);
                          

                          
                  } else if (activity.subProcesses)
                  {
                    blvutils.mdwLog("------------Got hetero subprocesses for act"+ activity.key);
                    for ( var x = 0; x < activity.subProcesses.length; x++)
                    {
                      //TBD fix
                      //  pd = null;//processes.getProcessDefinition(null,
                          //activity.subProcesses[x].subProcessName, 0, processDefinition,
                         // activity);
                    //  subProcessBAMEvents.push.apply(subProcessBAMEvents,
                    //      this.getFirstBAMEnabledActivities(pd));
                //alert(activity.subProcesses[x].subProcessName+":"+subProcessBAMEvents);
                      
                      pd = processes.getProcessDefinitionByName(null, activity.subProcesses[x].subProcessName,
                          processDefinition, activity);
 //                     promise.then(function(processdef){
 //                       pd = processdef;
                        subProcessBAMEvents.push.apply(subProcessBAMEvents,
                            this.getFirstBAMEnabledActivities(pd));
 //                     }, function(reject){
                      // TBD
 //                     });

                    }
                  }
                  blvutils.mdwLog("----ActivitySub BAM activities for "+ activity.key);
                  for (var f=0;f<subProcessBAMEvents.length;f++) {
                  blvutils.mdwLog("----ActivitySub Got BAM activity "+ subProcessBAMEvents[f].key);
                  }

                  return subProcessBAMEvents;

                },
                /**
                 * In this scenario, we have a bunch of events and attributes. Unfortuantely we
                 * don't know which attributes were updated for which event
                 * 
                 * @param bamEventSummary
                 * @returns
                 */
                createHistoricalBAMMessages : function(bamEventSummary)
                {
                  if (!bamEventSummary)
                  {
                    return null;

                  }
 
     
                  var bamEvents = bamEventSummary['bam:LiveEvent']['bam:EventList']['bam:Event'];
                  if (bamEvents === null || bamEvents.length === 0)
                  {
                    return null;
                  }
                  var i = 0;
                  var bamMessages = [];
                  _.forEach(bamEvents, function(bamEvent) {
                    blvutils.mdwLog("Historical BAM Message"+i);
                    var bamMessage = new blvtypes.BAMMessage(null, false);
                    bamMessage.MasterRequestId = blvSharedData.data.masterRequestId;
                    blvutils.mdwLog("   MasterRequestId"+bamMessage.MasterRequestId);
                    // alert(this.MasterRequestId);
                    bamMessage.Realm = 'mdw';
                    blvutils.mdwLog("   Realm          "+bamMessage.Realm);

                    bamMessage.EventName = bamEvent['bam:EventName'];
                    blvutils.mdwLog("   Event Name     "+bamMessage.EventName);
                    bamMessage.ProcessName = bamEvent['bam:EventData'];
                    blvutils.mdwLog("   ProcessName    "+bamMessage.ProcessName);
                    bamMessage.EventUniqueId = bamEvent['bam:EventRowId'];
                    blvutils.mdwLog("   EventUniqueId     "+bamMessage.EventUniqueId);
                    bamMessage.ActivityId = bamEvent['bam:EventId'];
                    blvutils.mdwLog("   ActivityId     "+bamMessage.ActivityId);
                    bamMessage.EventTime = bamEvent['bam:EventTime'];
                    blvutils.mdwLog("   EventTime      ",bamMessage.EventTime);

                    bamMessage.EventCategory = bamEvent['bam:EventCategory'];
                    blvutils.mdwLog("   EventCat      "+bamMessage.EventCategory);
                    bamMessage.ProcessInstanceId = bamEvent['bam:ProcessInstanceId'];
                    blvutils.mdwLog("   ProcessInstanceId "+bamMessage.ProcessInstanceId);
                    bamMessage.key = blvutils.replaceSpaces(bamMessage.ProcessName + bamMessage.ActivityId);
                    blvutils.mdwLog("   key            "+bamMessage.key);
                    // Add attributes onto the last one
//                    if (i == bamEvents.length - 1)
//                    {
                    /**
                     *TBD 
                     * 
                     */
                      bamMessage.AttributeNames = [];
                      bamMessage.AttributeValues = [];
                      //$xml = $(bamEventSummary);
                      //alert($xml);
                      bamMessage.callingProcessName = "";
                      bamMessage.callingActivity="";
                      var attributeList = bamEventSummary['bam:LiveEvent']['bam:AttributeList']['bam:Attribute'];
                      _.forEach(attributeList, function(attribute)
                          {
                            if (attribute['bam:EventRowId'] == bamMessage.EventUniqueId)
                            {
                              // Found attributes for this message, so add
                              var name = attribute['bam:Name'];
                              bamMessage.AttributeNames.push(blvutils.isUndefinedOrNull(name, false) ? ""
                                 : name);
                              var value = attribute['bam:Value'];
                              bamMessage.AttributeValues.push(blvutils.isUndefinedOrNull(value, false) ? ""
                                 : value);
                              blvutils.mdwLog("name="+name+"value=",value);
                              if (name == "CallingProcessName") {
                                bamMessage.callingProcessName = value;
                              }
                              if (name == "CallingActivity") {
                                bamMessage.callingActivity = value;
                              }
                             }
                          });   
                           
                      bamMessage.bamkey = blvutils.replaceSpaces(bamMessage.key + bamMessage.EventName.hashCode()+bamMessage.callingProcessName+bamMessage.callingActivity);
                      blvutils.mdwLog("   bamkey         "+bamMessage.bamkey);

//                    }
                      blvutils.mdwLog("Historical attr names for event"+bamMessage.EventUniqueId+"="+ bamMessage.AttributeNames+
                        " values="+ bamMessage.AttributeValues);
                    bamMessages.push(bamMessage);
                    
                  });

                  
                  return bamMessages;

                },
                updateBAMWorkflow : function(bamMessage)
                {
                  blvworkflow.addBamMessage(bamMessage, null);
                }




              };
              return blvworkflow;

            } ]);