// Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
'use strict';
var mdwBlvTypes = angular.module('blvtypes', [ 'blvutils' ]);

mdwBlvTypes
    .service(
        'blvtypes',
        [
            'blvutils',
            function(blvutils) {
              var blvTypes = {
                createActivities : function(activities, processName,
                    processVersion, processDefinition) {
                  var i;
                  var acts = [];
                  for (i = 0; i < activities.length; i++) {
                    acts.push(new blvTypes.Activity(activities[i], processName,
                        processVersion, processDefinition));
                  }
                  return acts;

                },
                createTransitions : function(transitions, processName,
                    processVersion) {
                  var i;
                  var trans = [];
                  for (i = 0; i < transitions.length; i++) {
                    trans.push(new blvTypes.Transition(transitions[i],
                        processName, processVersion));
                  }
                  return trans;

                },
                createVariables : function(variables) {
                  // alert("yo "+activities.length);
                  var i;
                  var vars = [];
                  for (i = 0; i < variables.Variable.length; i++) {
                    vars.push(new blvTypes.Variable(variables.Variable[i]));
                  }
                  return vars;

                },

                ProcessDefinition : function(jsonPD, parentProcessDefinition,
                    callingActivity) {

                  this.callingActivity = null;
                  this.callingActivity = callingActivity;
                  this.parentProcessDefinition = null;
                  this.parentProcessDefinition = parentProcessDefinition;

                  this.name = jsonPD['bpm:processDefinition']['bpm:Process'].name;
                  // if (parentProcessDefinition !== null) alert(this.name+" has
                  // a parent of
                  // "+parentProcessDefinition.name);
                  this.description = jsonPD['bpm:processDefinition']['bpm:Process'].description;
                  this.version = jsonPD['bpm:processDefinition']['bpm:Process'].version;
                  this.id = jsonPD['bpm:processDefinition']['bpm:Process'].Id;
                  // this.key = this.name + this.version;
                  this.key = this.name;// + "-" + this.id;
                  this.key = blvutils.getProcessDefinitionKey(this.name,
                      callingActivity, parentProcessDefinition);

                  this.activities = blvTypes
                      .createActivities(
                          jsonPD['bpm:processDefinition']['bpm:Process']['bpm:Activity'],
                          this.name, this.version, this);
                  // alert(this.activities);
                  this.transitions = blvTypes
                      .createTransitions(
                          jsonPD['bpm:processDefinition']['bpm:Process']['bpm:Transition'],
                          this.name, this.version);
                  // this.bamtransitions =
                  // createBAMTransitions(this.transitions,
                  // this.activities, this.name, this.version);
                  // alert(this.transitions);
                  this.bamData = new blvTypes.BAMMessage(jsonPD, false);
                  this.bamMonitoring = {};
                  this.bamEvents = [];
                  this.hasBamData = false;
                  // var attrLength =
                  // xml.getElementsByTagNameNS("*","Process")[0].getElementsByTagNameNS("*",
                  // "Attribute").length;
                  // $xml = $(xml);
                  // var pdattrs = $xml
                  // .find("bpm\\:processDefinition > bpm\\:Process >
                  // bpm\\:Attribute");
                  // alert("Found "+pdattrs.length+" attributes");
                  var pdattrs = jsonPD['bpm:processDefinition']['bpm:Process']['bpm:Attribute'];
                  for (var i = 0; i < pdattrs.length; i++) {
                    // alert(pdattrs[i].getAttribute("Name"));
                    var name = pdattrs[i].Name;
                    var value = pdattrs[i].Value;
                    var gotBAMAttribute = true;
                    if (name == 'ownerId') {
                      gotBAMAttribute = false;
                      this.owner = value;
                    } else if (name.indexOf("BAM@START_MSGDEF") >= 0) {
                      name = "BAM@START_MSGDEF";
                    } else if (name.indexOf("BAM@FINISH_MSGDEF") >= 0) {
                      name = "BAM@FINISH_MSGDEF";

                    } else {
                      gotBAMAttribute = false;
                    }
                    if (gotBAMAttribute) {
                      var bamEventName = blvutils.getBAMEventName(value);
                      if (!blvutils.isUndefinedOrNull(bamEventName, false)) {
                        this.hasBamData = true;
                        this.bamMonitoring[name] = bamEventName;
                        var bamEvent = new blvTypes.BamEvent(name, value, this,
                            null);
                        this.bamEvents.push(bamEvent);
                      }

                    }
                  }

                  // setBAMTransitions(this.activities, this.transitions);

                  this.processStatus = "Unknown";
                  this.processInstanceId = 0;
                  // alert(this.bamData);
                  //if (!hashProcessDefinitions) {
                 //   hashProcessDefinitions = {};
                 // }
                 // hashProcessDefinitions[this.key] = this;
                  blvutils.mdwLog("*****Saved hash pd", this.key);
                  this.updateInstanceData = function(processInstanceData) {
                    // Update process statuses
                    this.processInstanceData = processInstanceData;
                    // blvutils.mdwLog("Updated processInstanceData");
                    this.key = this.key + "-" + this.processInstanceData.processInstanceId;
                    // alert(this.bamData);
                   // hashProcessDefinitions[this.key] = this;
                    // blvutils.mdwLog("*****Saved hash pd",this.key);

                  };

                },
                ProcessInstanceData : function(jsonPD) {
                  this.processName = jsonPD.ProcessInstanceData.Process.ProcessName;

                  this.processStatus = jsonPD.ProcessInstanceData.Process.ProcessStatus;

                  this.processId = jsonPD.ProcessInstanceData.Process.ProcessId;
                  this.processInstanceId = jsonPD.ProcessInstanceData.Process.ProcessInstanceId;
                  this.variables = blvTypes
                      .createVariables(jsonPD.ProcessInstanceData.Process.Variables);
                  // HashMap of activityInstanceId against Subprocess
                  this.subProcessInstanceHash = blvTypes
                      .getSubProcessInstanceHash(jsonPD.ProcessInstanceData.Process.Activity);
                },

                Variable : function(variable) {

                  this.name = variable.Name;
                  this.id = variable.Id;
                  this.value = variable.Value;
                  this.type = variable.Type;
                  blvutils.mdwLog("Added variable data " + this.name + this.id + this.value + this.type);

                },

                Activity : function(act, processName, processVersion,
                    processDefinition) {

                  this.id = act.Id;
                  this.strippedOffid = blvutils.stripOffChars(act.Id);
                  this.implementation = act.Implementation;
                  this.name = act.Name;
                  // alert(this.id+this.implementation+this.name);
                  this.processDefinition = processDefinition;
                  this.bamMessages = [];
                  this.attributes = [];
                  this.pdKey = "";
                  // Default this to
                  this.firstBAMNode = false;
                  this.processName = processName;
                  this.processVersion = processVersion;
                  var attrLength = 1;
                  
                  if (_.isArray(act['bpm:Attribute'])) {
                    attrLength = act['bpm:Attribute'].length;
                  }
                  // alert(this.id+attrLength);
                  this.isSubProcess = false;
                  this.subProcessDefinition = null;
                  // alert(this.implementation);
                  // if (this.implementation ==
                  // "com.centurylink.mdw.workflow.activity.process.InvokeSubProcessActivity")
                  // {
                  // this.isSubProcess = true;
                  // alert("SubProcess");

                  // }
                  var i = 0;
                  this.hasBamData = false;
                  // this.key = processName + processVersion + this.id;
                  this.key = processName + this.strippedOffid;
                  this.bamMonitoring = {};
                  this.bamEvents = [];
                  for (i = 0; i < attrLength; i++) {
                    var name, value, owner;
                    if (attrLength == 1) {
                      name = act['bpm:Attribute'].Name;
                      value = act['bpm:Attribute'].Value;
                      owner = act['bpm:Attribute'].Owner;
                     
                    } else {
                      name = act['bpm:Attribute'][i].Name;
                      value = act['bpm:Attribute'][i].Value;
                      owner = act['bpm:Attribute'][i].Owner;
                    }
                    this.attributes.push(new blvTypes.Attribute(name, value,
                        owner));
                    this.owner = owner;
                    // Artis support
                    if (name.indexOf("ARTIS@Use") >= 0) {
                      this.useArtis = value;
                    } else if (name.indexOf("ARTIS@FunctionLabel") >= 0) {
                      this.artisFunctionLabel = value;

                    } else if (name.indexOf("ARTIS@ServiceName") >= 0) {
                      this.artisServiceName = value;

                    }

                    var gotBAMAttribute = true;
                    if (name.indexOf("BAM@START_MSGDEF") >= 0) {
                      name = "BAM@START_MSGDEF";
                    } else if (name.indexOf("BAM@FINISH_MSGDEF") >= 0) {
                      name = "BAM@FINISH_MSGDEF";

                    } else {
                      gotBAMAttribute = false;
                    }
                    if (gotBAMAttribute) {
                      var bamEventName = blvutils.getBAMEventName(value);
                      if (!blvutils.isUndefinedOrNull(bamEventName, false)) {
                        this.firstBAMNode = true;
                        this.hasBamData = true;
                        this.bamMonitoring[name] = bamEventName;
                        var bamEvent = new blvTypes.BamEvent(name, value,
                            processDefinition, this);
                        this.bamEvents.push(bamEvent);
                      }

                    }
                    // Sort and reverse so that start events come first
                    this.bamEvents.sort(blvutils.sortByTrigger);
                    this.bamEvents.reverse();
                    if ("processmap" == name || "processname" == name) {
                      this.isSubProcess = true;
                    }

                    if (this.isSubProcess) {

                      if ("processname" == name) {
                        this.subProcessName = value;

                      } else if ("processversion" == name) {
                        this.subProcessVersion = value;
                      } else if ("processmap" == name) {
                        this.subProcesses = [];
                        /**
                         * String map =
                         * getAttributeValue(WorkAttributeConstant.PROCESS_MAP);
                         * List<String[]> procmap; if (map==null) procmap = new
                         * ArrayList<String[]>(); else procmap =
                         * StringHelper.parseTable(map, ',', ';', 3); for (int
                         * i=0; i<procmap.size(); i++) { if
                         * (procmap.get(i)[0].equals(logicalProcName)) { String
                         * subproc_name = procmap.get(i)[1]; String v =
                         * procmap.get(i)[2]; return
                         * super.getSubProcessVO(subproc_name, v); } }
                         * 
                         */
                        var arrays = blvutils.parseTable(value, ',', ';', 3);
                        // alert(arrays);
                        //TBD
                        /**
                        for (var a = 0; a < arrays.length; a++) {
                          this.subProcesses.push(new blvtypes.SubProcess(arrays[a][0],
                              arrays[a][1], arrays[a][2]));
                        }
                           */
                        // Array of String arrays
                        // var tmps = value.split(",");
                        // this.subProcessName = tmps[1];
                        // this.subProcessVersion = tmps[2];
                      }
                      // this.subProcessDefinition = new ProcessDefinition(
                      // getProcessData("WorkflowAsset?name=" +
                      // this.subProcessName
                      // + "&version=0"), this.processDefinition);

                    }
                    if ("WORK_DISPLAY_INFO" == name) {
                      // Get the x,y,w,h
                      var tmps = value.split(",");

                      for (var j = 0; j < tmps.length; j++) {
                        var k = tmps[j].indexOf('=');
                        // alert(k);
                        if (k <= 0)
                          continue;
                        var an = tmps[j].substring(0, k);
                        // alert(an);
                        var av = tmps[j].substring(k + 1);
                        // alert(av);
                        if (an == "x")
                          this.x = parseInt(av);
                        else if (an == "y")
                          this.y = parseInt(av);
                        else if (an == "w")
                          this.w = parseInt(av);
                        else if (an == "h")
                          this.h = parseInt(av);
                      }

                    }
                    // alert(this.x+this.y+this.w+this.h);
                  }
                  // bamUpdates is an array of BamEvent objects with the latest
                  // data
                  this.updateBAMDefinitions = function(bamUpdates) {
                    // First clear any BAM definitions for this activity
                    this.bamEvents.length = 0;
                    this.bamMonitoring.length = 0;
                    this.hasBamData = false;

                    if (blvutils.isUndefinedOrNull(bamUpdates, false) || bamUpdates.length === 0) {
                      return;
                    }
                    for (var z = 0; z < bamUpdates.length; z++) {
                      this.hasBamData = true;
                      var bamEvent = bamUpdates[z];
                      this.bamMonitoring[bamEvent.bamDefinitionKey] = bamEvent.name;
                      this.bamEvents.push(bamEvent);
                    }
                  };

                  this.nextBAMActivities = [];
                  var _this = this;
                  this.addNextBAMActivity = function(act) {
                    _this.nextBAMActivities.push(act);
                    /**
                     * if (act.isSubProcess) { var subpd = new
                     * ProcessDefinition( getProcessData("WorkflowAsset?name=" +
                     * act.subProcessName + "&version=0"),
                     * this.processDefinition); // get first BAM activity in
                     * subprocess var bamActs =
                     * getFirstBAMEnabledActivities(subpd);
                     * _this.nextBAMActivities.push
                     * .apply(_this.nextBAMActivities, bamActs); }
                     */
                    blvutils.mdwLog("Activity" + _this.key + " nextBAMactivites now size=" + _this.nextBAMActivities.length);
                  };
                  // alert("Done");
                  // alert(this.hasBamData)

                },
                Subprocess : function(subproc) {

                  this.instanceId = subproc.InstanceId;
                  this.processName = subproc.ProcessName;
                  this.key = this.processName + "-" + this.instanceId;
                  this.startDate = subproc.StartDate;
                  this.endDate = subproc.EndDate;
                  this.statusCode = subproc.StatusCode;
                  blvutils.mdwLog("Added Subprocess data " + this.instanceId + this.processName + this.startDate + this.endDate + this.statusCode);

                },

                Attribute : function(name, value, owner) {
                  this.name = name;
                  this.value = value;
                  this.owner = owner;
                },

                Transition : function(trans, processName, processVersion) {
                  // alert(trans);
                  if (!blvutils.isUndefinedOrNull(trans, false)) {
                    this.id = trans.Id;
                    this.event = trans.Event;
                    // this.to = trans.getAttribute('To');
                    // this.from = trans.getAttribute('From');
                    this.completionCode = trans.CompletionCode;
                    // alert(this.id+this.event+this.to+this.from+this.completionCode);
                    // this.from = processName + processVersion +
                    // trans.getAttribute('From');
                    // this.to = processName + processVersion +
                    // trans.getAttribute('To');
                    this.from = processName + blvutils.stripOffChars(trans.From);
                    this.to = processName + blvutils.stripOffChars(trans.To);

                    this.attributes = [];
                    // var attrLength = trans['bpm:Attribute'].length;
                    // alert(attrLength);

                    // for ( var z = 0; z < attrLength; z++)
                    // {

                    // this.attributes.push(new
                    // Attribute(trans['bpm:Attribute'][z].Name,
                    // trans['bpm:Attribute'][z].Value));
                    this.attributes.push(new blvTypes.Attribute(
                        trans['bpm:Attribute'].Name,
                        trans['bpm:Attribute'].Value));
                    // Get values for lx, ly, xs, ys
                    // Get the x,y,w,h
                    var value = trans['bpm:Attribute'].Value;
                    var attrs = value.split(",");
                    for (var i = 0; i < attrs.length; i++) {
                      var k = attrs[i].indexOf('=');
                      // alert(k);
                      if (k <= 0)
                        continue;
                      var an = attrs[i].substring(0, k);
                      // alert(an);
                      var av = attrs[i].substring(k + 1);
                      // alert(av);
                      if (an == "lx")
                        this.lx = parseInt(av);
                      else if (an == "ly")
                        this.ly = parseInt(av);
                      else if (an == "xs") {
                        if (av !== null && av.length > 0) {
                          var sts1 = av.split("&");
                          this.xs = [];
                          for (var l1 = 0; l1 < sts1.length; l1++) {
                            this.xs.push(parseInt(sts1[l1]));
                          }
                        }
                      } else if (an == "ys") {
                        if (av !== null && av.length > 0) {
                          var sts2 = av.split("&");
                          this.ys = [];
                          for (var l2 = 0; l2 < sts2.length; l2++) {
                            this.ys.push(parseInt(sts2[l2]));
                          }
                        }
                      }

                      // alert("lx="+this.lx+" ly="+this.ly+" xs="+this.xs+"
                      // ys="+this.ys);

                    }
                  }

                },
                BamEvent : function(name, bamMessageDefinition,
                    processDefinition, activity) {
                  var parser = new DOMParser();
                  var xmlDoc = parser.parseFromString(bamMessageDefinition,
                      "text/xml");
                  // var xmlDoc = $.parseXML(bamMessageDefinition);

                  this.processDefinition = processDefinition;
                  this.activity = activity;
                  this.realm = "";
                  this.cat = "";
                  this.subcat = "";
                  this.name = '';
                  this.bamDefinitionKey = name;
                  this.trigger = '';
                  this.component = '';
                  this.attributes = [];

                  if (xmlDoc && bamMessageDefinition) {
                    this.name = xmlDoc.documentElement.getElementsByTagNameNS(
                        "*", "name")[0].firstChild.nodeValue;
                    this.trigger = xmlDoc.documentElement
                        .getElementsByTagNameNS("*", "trigger")[0].firstChild.nodeValue;
                    var realmelement = xmlDoc.documentElement
                        .getElementsByTagNameNS("*", "realm")[0];
                    this.realm = blvutils.isUndefinedOrNull(realmelement, true) ? ""
                        : realmelement.firstChild.nodeValue;

                    var catelement = xmlDoc.documentElement
                        .getElementsByTagNameNS("*", "cat")[0];
                    // alert("eventName="+eventName.firstChild.nodeValue);
                    this.cat = blvutils.isUndefinedOrNull(catelement, true) ? ""
                        : catelement.firstChild.nodeValue;
                    var subcatelement = xmlDoc.documentElement
                        .getElementsByTagNameNS("*", "subcat")[0];
                    // alert("eventName="+eventName.firstChild.nodeValue);
                    this.subcat = blvutils.isUndefinedOrNull(subcatelement,
                        true) ? "" : subcatelement.firstChild.nodeValue;
                    var componentelement = xmlDoc.documentElement
                        .getElementsByTagNameNS("*", "component")[0];
                    this.component = blvutils.isUndefinedOrNull(
                        componentelement, true) ? ""
                        : componentelement.firstChild.nodeValue;
                    var attrLength = xmlDoc.documentElement
                        .getElementsByTagNameNS("*", "attr").length;
                    this.attributes = [];
                    for (var i = 0; i < attrLength; i++) {
                      var nameElement = xmlDoc.documentElement
                          .getElementsByTagNameNS("*", "an")[i];
                      var nameattr = blvutils.isUndefinedOrNull(nameElement, true) ? ""
                          : nameElement.firstChild.nodeValue;
                      var valueElement = xmlDoc.documentElement
                          .getElementsByTagNameNS("*", "av")[i];
                      var value = blvutils
                          .isUndefinedOrNull(valueElement, true) ? ""
                          : valueElement.firstChild.nodeValue;
                      // alert("Adding name "+name+ " value "+value);
                      this.attributes.push(new blvTypes.Attribute(nameattr, value));

                    }

                    // this.cat =
                    // xmlDoc.documentElement.getElementsByTagNameNS("*",
                    // "cat")[0].firstChild.nodeValue;
                    // this.subcat =
                    // xmlDoc.documentElement.getElementsByTagNameNS("*",
                    // "subcat")[0].firstChild.nodeValue;
                  }
                  this.key = blvutils.replaceSpaces(this.processDefinition.name + (activity === null ? "" : activity.strippedOffid) + this.name.hashCode());

                  blvutils.mdwLog("Created BAMEvent name" + this.name.hashCode() + "key" + this.key);

                },
                BAMMessage : function(jsonPD, bamMessage) {
                  //TBD
                  //temp until jsonified
                  var xmldoc = null;
                  if (bamMessage) {
                    this.MasterRequestId = xmldoc.documentElement
                        .getElementsByTagNameNS("*", "MasterRequestId")[0].firstChild.nodeValue;
                    // alert(this.MasterRequestId);
                    this.Realm = xmldoc.documentElement.getElementsByTagNameNS(
                        "*", "Realm")[0].firstChild.nodeValue;
                    // alert(this.Realm);
                    this.EventName = xmldoc.documentElement
                        .getElementsByTagNameNS("*", "EventName")[0].firstChild.nodeValue;
                    this.EventTime = xmldoc.documentElement
                        .getElementsByTagNameNS("*", "EventTime")[0].firstChild.nodeValue;
                    // / alert(this.EventName);
                    this.ActivityId = "A";
                    if (xmldoc.documentElement.getElementsByTagNameNS("*",
                        "ActivityId").length > 0) {
                      this.ActivityId = xmldoc.documentElement
                          .getElementsByTagNameNS("*", "ActivityId")[0].firstChild.nodeValue;
                    }
                    this.EventUniqueId = "";

                    // alert(this.ActivityId);
                    this.EventCategory = xmldoc.documentElement
                        .getElementsByTagNameNS("*", "EventCategory")[0].firstChild.nodeValue;
                    this.ProcessInstanceId = xmldoc.documentElement
                        .getElementsByTagNameNS("*", "ProcessInstanceId")[0].firstChild.nodeValue;

                    this.ProcessName = xmldoc.documentElement
                        .getElementsByTagNameNS("*", "ProcessName")[0].firstChild.nodeValue;
                    this.ProcessId = xmldoc.documentElement
                        .getElementsByTagNameNS("*", "ProcessId")[0].firstChild.nodeValue;
                    // alert(this.EventCategory);
                    this.ProcessVersion = xmldoc.documentElement
                        .getElementsByTagNameNS("*", "ProcessVersion")[0].firstChild.nodeValue;
                    // this.key = this.ProcessName + this.ProcessVersion + "A"
                    // + this.ActivityId;
                    this.key = blvutils.replaceSpaces(this.ProcessName + this.ActivityId);
                    this.bamkey = blvutils.replaceSpaces(this.key + this.EventName.hashCode());
                    this.SubCategory = xmldoc.documentElement
                        .getElementsByTagNameNS("*", "SubCategory")[0].firstChild.nodeValue;
                    // alert(this.SubCategory);
                    this.AttributeNames = [];
                    this.AttributeValues = [];
                    var namesLength = xmldoc.documentElement
                        .getElementsByTagNameNS("*", "Name").length;
                    var i = 0;
                    this.callingProcessName = "";
                    this.callingActivity = "";
                    for (i = 0; i < namesLength; i++) {
                      var attributeName = xmldoc.documentElement
                          .getElementsByTagNameNS("*", "Name")[i].firstChild.nodeValue;
                      this.AttributeNames.push(attributeName);
                      var attributeValue = xmldoc.documentElement
                          .getElementsByTagNameNS("*", "Value")[i].firstChild.nodeValue;
                      this.AttributeValues.push(attributeValue);
                      if (attributeName == "CallingProcessName") {
                        this.callingProcessName = attributeValue;
                      }
                      if (attributeName == "CallingActivity") {
                        this.callingActivity = attributeValue;
                      }
                    }
                    this.bamkey = blvutils.replaceSpaces(this.key + this.EventName.hashCode() + this.callingProcessName + this.callingActivity);

                    // alert(this.Attributes);
                    this.SourceSystem = xmldoc.documentElement
                        .getElementsByTagNameNS("*", "SourceSystem")[0].firstChild.nodeValue;
                    // alert("Received BAM Message eventName="+this.EventName+"
                    // processInstanceId="+this.ProcessInstanceId+"
                    // processName="+this.ProcessName);

                  }
                  // alert(this.SourceSystem);
                },

                getSubProcessInstanceHash : function(activities) {
                  var hash = {};

                  for (var i = 0; i < activities.length; i++) {
                    var activityId = activities[i].ActivityId;
                    var subprocesses = blvTypes
                        .createSubProcesses(activities[i].SubProcess);
                    hash[activityId] = subprocesses;
                    blvutils.mdwLog("ActivityId " + activityId + "has" + subprocesses.length + "subprocesses");
                  }
                  return hash;

                },
                createSubProcesses : function(subprocesses) {
                  // alert("yo "+activities.length);
                  var i;
                  var procs = [];
                  if (subprocesses) {
                    for (i = 0; i < subprocesses.length; i++) {
                      procs.push(new blvTypes.Subprocess(subprocesses[i]));
                    }
                  }
                  return procs;

                }

              };
              return blvTypes;

            } ]);