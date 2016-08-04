// Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
'use strict';
var mdwBlvUtils = angular.module('blvutils', []);

mdwBlvUtils
    .service(
        'blvutils',[
          function()
        {
          var blvutils = {
            getProcessDefinitionKey : function(processName, callingActivity,
                parentPD) {
              var pn = "";
              var ca = "";
              var ppd = "";
              if (processName) {
                pn = processName;
              }
              if (callingActivity) {
                ca = callingActivity.id;
              }
              if (parentPD) {
                ppd = parentPD.name;
              }
              return this.replaceSpaces(pn + ca + ppd);
            },

            isUndefinedOrNull : function(param, checkChild) {
              if (typeof param === "undefined" || param === null) {
                return true;
              }
              if (checkChild) {
                return this.isUndefinedOrNull(param.firstChild, false);

              }
              return false;
            },
            replaceSpaces : function(str) {
              var rep = str.replace(/ /g, "_");
              return rep;
            },
            stripOffChars : function(str) {
              var newstr = str.replace(/\D/g, '');
              return newstr;
            },
            sortByTrigger : function(a, b) {
              var aName = a.trigger;
              var bName = b.trigger;
              return ((aName < bName) ? -1 : ((aName > bName) ? 1 : 0));
            },
            getBAMEventName : function(bamMessageDefinition) {
              var x2js = new X2JS();
              var bamMsgDefJson = x2js.xml_str2json(bamMessageDefinition);

              if (bamMsgDefJson !== null) {
                return bamMsgDefJson.BamMsgDef.name;
                // alert(nameElement);
              }
              return null;
            },
            mdwLog : function(msg) {
              // attempt to send a message to the console
              try {
                console.log(msg);
              }
              // fail gracefully if it does not exist
              catch (e) {
              }
            },
            getBAMRectKey : function(actName, eventName) {
              // this.mdwLog("getBAMRectKey - ", actName, eventName, "Rect");
              return actName + eventName + "Rect";
            },
            getBAMTextKey : function(actName, eventName) {
              // this.mdwLog("getBAMTextKey - ", actName, eventName, "Text");
              return actName + eventName + "Text";
            },
            getBamEvent : function(activityNode, trigger) {
              for (var j = 0; j < activityNode.activity.bamEvents.length; j++) {
                var bamEvent = activityNode.activity.bamEvents[j];
                if (bamEvent.trigger.toUpperCase() == trigger.toUpperCase()) {
                  return angular.copy(bamEvent);
                }
              }
              return null;

            },
            /**
             * 
             */
            updateGUIProcessData : function(processDefinition) {
              blvutils.mdwLog("Update GUI data ***FIX THIS***");
              /**
               * $("#masterRequest").html( "<b>Master Request Id - " +
               * masterRequestId + " - " + processDefinition.name + " v" +
               * processDefinition.version + " instance " +
               * processDefinition.processInstanceData.processInstanceId + "</b>");
               * var parentProcessDefinition =
               * processDefinition.parentProcessDefinition; if
               * (parentProcessDefinition == null) { $("#upLayer").hide(); }
               * else { // Add a button to get back to this parent layer
               * $('#upLayer').unbind('click'); var txt = "Back to " +
               * parentProcessDefinition.name + " " +
               * parentProcessDefinition.version + " Instance " +
               * parentProcessDefinition.processInstanceData.processInstanceId;
               * $("#upLayer").button( { label : txt, text : txt, icons : {
               * 
               * primary : "ui-icon-arrowthick-1-n"
               *  } }); $("#upLayer").click( function() {
               * 
               * activatePage(parentProcessDefinition.key,
               * parentProcessDefinition.processInstanceData.processInstanceId);
               * 
               * });
               * 
               * $("#upLayer").show();
               *  }
               */
            },

         
          /**
           * Ported from StringHelper.parseTable
           */
          parseTable : function(string, field_delimiter, row_delimiter, columnCount)
          {
            // alert(string);
            // List<String[]> table = new ArrayList<String[]>();
            var table = [];
            if (!string)
              return table;
            var row_start = 0;
            var field_start;
            var n = string.length;
            // String[] row;
            var row = [];
            var m, j;
            // StringBuffer sb;
            var sb;
            while (row_start < n)
            {
              row = new Array(columnCount);
              j = 0;
              field_start = row_start;
              var ch = field_delimiter;
              while (ch == field_delimiter)
              {
                sb = "";
                var escaped = false;
                for (m = field_start; m < n; m++)
                {
                  ch = string.charAt(m);
                  if (ch == '\\' && !escaped)
                  {
                    escaped = true;
                  } else
                  {
                    if (!escaped && (ch == field_delimiter || ch == row_delimiter))
                    {
                      break;
                    } else
                    {
                      sb += ch;
                      escaped = false;
                    }
                  }
                }
                if (j < columnCount)
                  row[j] = sb;
                if (m >= n || ch == row_delimiter)
                {
                  row_start = m + 1;
                  break;
                } else
                { // ch==field_delimiter
                  field_start = m + 1;
                  j++;
                }
              }
              table.push(row);

            }
            return table;
          }
          };


          return blvutils;

        }]);

var hashProcessDefinitions = {};
var possibleBAMEvents;
var hashBAMEvents = {};
var firstBAMActivity;
var hashProcesses = {};
/**
 * @see http://stackoverflow.com/q/7616461/940217
 * @return {number}
 */
String.prototype.hashCode = function() {
  if (Array.prototype.reduce) {
    return this.split("").reduce(function(a, b) {
      a = ((a << 5) - a) + b.charCodeAt(0);
      return a & a;
    }, 0);
  }
  var hash = 0;
  if (this.length === 0)
    return hash;
  for (var i = 0; i < this.length; i++) {
    var character = this.charCodeAt(i);
    hash = ((hash << 5) - hash) + character;
    hash = hash & hash; // Convert to 32bit integer
  }
  return hash;
};

String.prototype.htmlEscape = function() {
 // return $('<div/>').text(this.toString()).html();
};
