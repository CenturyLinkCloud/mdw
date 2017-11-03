/*
 * Copyright (C) 2017 CenturyLink, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.centurylink.mdw.util.log;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.MdwWebSocketServer;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.model.JsonObject;
import com.centurylink.mdw.model.workflow.TransitionStatus;
import com.centurylink.mdw.model.workflow.WorkStatus;
import com.centurylink.mdw.soccom.SoccomClient;

public abstract class AbstractStandardLoggerBase implements StandardLogger {

    public static final String DEFAULT_HOST = "localhost";
    public static final String DEFAULT_PORT = "7181";

    private static final String SENTRY_MARK = "[SENTRY-MARK] ";

    private static final String MESSAGE_REG_EX = "\\[\\(.\\)([0-9.:]+) p([0-9]+)\\.([0-9]+) ([a-z])([0-9]+)?\\.([^\\]]+)\\] (.*)";

    private static Pattern pattern = Pattern.compile(MESSAGE_REG_EX, Pattern.DOTALL);

    private static DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd.HH:mm:ss.SSS");

    protected static String watcher = null;

    public String getDefaultHost() {
        return DEFAULT_HOST;
    }

    public String getDefaultPort() {
        return DEFAULT_PORT;
    }

    public String getSentryMark() {
        return SENTRY_MARK;
    }

    public void refreshCache() {
        watcher = PropertyManager.getProperty(PropertyNames.MDW_LOGGING_WATCHER);
    }

    protected String generate_log_line(char type, String tag, String message) {
        StringBuffer sb = new StringBuffer();
        sb.append("[(");
        sb.append(type);
        sb.append(")");
        sb.append(dateFormat.format(new Date()));
        if (tag!=null) {
            sb.append(" ");
            sb.append(tag);
        } else {
            sb.append(" ~");
            sb.append(Thread.currentThread().getId());
        }
        sb.append("] ");
        sb.append(message);
        return sb.toString();
    }

    public boolean watching() {
        return watcher != null;
    }

    protected JSONObject buildJSONLogMessage(String message) throws JSONException, ParseException {
        JSONObject obj = null;
        Matcher matcher = pattern.matcher(message);
        String subtype = null;
        String msg = null;
        if (matcher.matches()) {
            obj = new JsonObject();
            obj.put("name", "LogWatcher");
            String t = matcher.group(1);
            obj.put("time", dateFormat.parse(t).toInstant());
            obj.put("procId", new Long(matcher.group(2)));
            obj.put("procInstId", new Long(matcher.group(3)));
            subtype = matcher.group(4);
            obj.put("subtype", subtype);
            String id = matcher.group(5);
            if (id != null)
                obj.put("id", Long.parseLong(id));
            String instId = matcher.group(6);
            if (instId != null) {
                try {
                    obj.put("instId", Long.parseLong(instId));
                }
                catch (NumberFormatException ex) {
                    // master request id needn't be numeric
                    obj.put("instId", instId);
                }
            }
            msg = matcher.group(7);
            obj.put("msg", msg);
        }
        if ("a".equals(subtype) && msg != null) {
            if (msg.startsWith(WorkStatus.LOGMSG_COMPLETE)) {
                obj.put("status", WorkStatus.STATUS_COMPLETED);
            } else if (msg.startsWith(WorkStatus.LOGMSG_START)) {
                obj.put("status", WorkStatus.STATUS_IN_PROGRESS);
            } else if (msg.startsWith(WorkStatus.LOGMSG_FAILED)) {
                obj.put("status", WorkStatus.STATUS_FAILED);
            } else if (msg.startsWith(WorkStatus.LOGMSG_SUSPEND)) {
                obj.put("status", WorkStatus.STATUS_WAITING);
            } else if (msg.startsWith(WorkStatus.LOGMSG_HOLD)) {
                obj.put("status", WorkStatus.STATUS_HOLD);
            }
        }
        else if ("t".equals(subtype)) {
            obj.put("status", TransitionStatus.STATUS_COMPLETED);
        }
        else if ("m".equals(subtype)) {

        }
        return obj;
    }

    protected void sendToWatchers(String message) {
        if (watching())  // Designer
            sendToWatcher(message);

        if (MdwWebSocketServer.getInstance().isEnabled()) {
            try {
                JSONObject jsonObj = buildJSONLogMessage(message);

                if (jsonObj != null && jsonObj.has("procInstId") && jsonObj.has("procId")) {
                    if (MdwWebSocketServer.getInstance().hasInterestedConnections(String.valueOf(jsonObj.get("procInstId"))))
                        sendToWebWatcher(jsonObj, String.valueOf(jsonObj.get("procInstId")));
                    else if (MdwWebSocketServer.getInstance().hasInterestedConnections(String.valueOf(jsonObj.get("procId"))))
                        sendToWebWatcher(jsonObj, String.valueOf(jsonObj.get("procId")));
                }
                else if (jsonObj != null && jsonObj.has("masterRequestId")) {
                    if (MdwWebSocketServer.getInstance().hasInterestedConnections(jsonObj.getString("masterRequestId")))
                        sendToWebWatcher(jsonObj, jsonObj.getString("masterRequestId"));
                }
            }
            catch (Throwable e) {
                System.out.println("Exception when building JSON Object to send to web watcher: " + e);
                e.printStackTrace();
            };
        }
    }

    protected void sendToWatcher(String message) {
        SoccomClient client = null;
        try {
            String[] spec = watcher.split(":");
            String host = spec.length>0?spec[0]:getDefaultHost();
            String port = spec.length>1?spec[1]:getDefaultPort();
            client = new SoccomClient(host, port, null);
            client.putreq(message);
        } catch (Exception e) {
            watcher = null;
            System.out.println("Exception when sending log messages to watcher - turn it off");
            e.printStackTrace();
        } finally {
            if (client!=null) client.close();
        }
    }

    protected void sendToWebWatcher(JSONObject message, String key) throws JSONException {
        MdwWebSocketServer.getInstance().send(message.toString(), key);
    }

}
