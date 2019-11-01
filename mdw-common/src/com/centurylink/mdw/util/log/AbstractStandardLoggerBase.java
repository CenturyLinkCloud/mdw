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

import com.centurylink.mdw.common.service.MdwServiceRegistry;
import com.centurylink.mdw.common.service.WebSocketMessenger;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.model.JsonObject;
import com.centurylink.mdw.model.workflow.TransitionStatus;
import com.centurylink.mdw.model.workflow.WorkStatus.InternalLogMessage;
import com.centurylink.mdw.soccom.SoccomClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class AbstractStandardLoggerBase implements StandardLogger {

    public static final String DEFAULT_HOST = "localhost";
    public static final int DEFAULT_PORT = 7181;

    protected static String dateFormat = "yyyyMMdd.HH:mm:ss.SSS";

    private static final String MESSAGE_REG_EX = "\\[\\(.\\)([0-9.:]+) p([0-9]+)\\.([0-9]+) ([a-z])([0-9]+)?\\.([^]]+)] (.*)";
    private static Pattern pattern = Pattern.compile(MESSAGE_REG_EX, Pattern.DOTALL);

    protected static String watcher = null;

    public String getDefaultHost() {
        return DEFAULT_HOST;
    }

    public int getDefaultPort() {
        return DEFAULT_PORT;
    }

    public void refreshWatcher() {
        watcher = PropertyManager.getProperty(PropertyNames.MDW_LOGGING_WATCHER);
    }

    protected String generateLogLine(char type, String tag, String message) {
        StringBuilder sb = new StringBuilder();
        for (LogLineInjector injector : getInjectors()) {
            String prefix = injector.prefix();
            if (prefix != null)
                sb.append(prefix).append(" ");
        }
        sb.append("[(");
        sb.append(type);
        sb.append(")");
        sb.append(new SimpleDateFormat(dateFormat).format(new Date()));
        if (tag!=null) {
            sb.append(" ");
            sb.append(tag);
        } else {
            sb.append(" ~");
            sb.append(Thread.currentThread().getId());
        }
        sb.append("] ");
        sb.append(message);
        for (LogLineInjector injector : getInjectors()) {
            String suffix = injector.suffix();
            if (suffix != null)
                sb.append(suffix).append(" ");
        }
        return sb.toString();
    }

    public boolean watching() {
        return watcher != null;
    }

    /**
     * Returns null if not internal log message
     */
    protected JSONObject buildJSONLogMessage(String message) throws JSONException, ParseException {
        JSONObject obj = null;
        Matcher matcher = pattern.matcher(message);
        String subtype = null;
        String msg = null;
        if (matcher.matches() && InternalLogMessage.isInternalMessage(matcher.group(7))) {
            obj = new JsonObject();
            obj.put("name", "LogWatcher");
            String t = matcher.group(1);
            obj.put("time", new SimpleDateFormat(dateFormat).parse(t).toInstant());
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
            InternalLogMessage internalMessage = InternalLogMessage.match(msg);
            if (internalMessage != null) {
                obj.put("status", internalMessage.status);
            }
        }
        else if ("t".equals(subtype)) {
            obj.put("status", TransitionStatus.STATUS_COMPLETED);
        }
        else if ("m".equals(subtype)) {
            // TODO
        }
        return obj;
    }

    protected void sendToWatchers(String message) {
        // injector compatibility for watchers and websocket clients
        if (message.startsWith("[") && !message.startsWith("[("))
            message = message.substring(message.indexOf(']') + 1).trim();

        if (watching())
            sendToWatcher(message);

        try {
            JSONObject jsonObj = buildJSONLogMessage(message);

            if (jsonObj != null && jsonObj.has("procInstId") && jsonObj.has("procId")) {
                sendToWebWatcher(String.valueOf(jsonObj.get("procInstId")), jsonObj);
                sendToWebWatcher(String.valueOf(jsonObj.get("procId")), jsonObj);
            }
            else if (jsonObj != null && jsonObj.has("masterRequestId")) {
                sendToWebWatcher(jsonObj.getString("masterRequestId"), jsonObj);
            }
        }
        catch (Throwable e) {
            System.out.println("Error building log watcher json for: '" + message + "' -> " + e);
            e.printStackTrace();
        }
    }

    protected void sendToWatcher(String message) {
        SoccomClient client = null;
        try {
            String[] spec = watcher.split(":");
            String host = spec.length > 0 ? spec[0] : getDefaultHost();
            int port = spec.length > 1 ? Integer.parseInt(spec[1]) : getDefaultPort();
            client = new SoccomClient(host, port, null);
            client.putreq(message);
        } catch (Exception e) {
            watcher = null;
            System.out.println("Exception when sending log messages to watcher - turn it off");
            e.printStackTrace();
        } finally {
            if (client != null)
                client.close();
        }
    }

    protected void sendToWebWatcher(String topic, JSONObject message) throws JSONException, IOException {
        WebSocketMessenger.getInstance().send(topic, message.toString());
    }

    @Override
    public boolean isInfoEnabled() {
        return isEnabledFor(LogLevel.INFO);
    }

    @Override
    public boolean isDebugEnabled() {
        return isEnabledFor(LogLevel.DEBUG);
    }

    @Override
    public boolean isTraceEnabled() {
        return isEnabledFor(LogLevel.TRACE);
    }

    @Override
    public boolean isMdwDebugEnabled() {
        return isEnabledFor(LogLevel.TRACE);
    }

    private void logIt(LogLevel level, String message, Throwable t) {
        String origMsg = message;
        int index = message.indexOf(" ");
        if (index > 0 && message.startsWith("[("))
            message = "[" + message.substring(index+1);
        switch (level.toString()) {
            case "INFO":
                if (t == null)
                    info(message);
                else
                    info(message, t);
                break;
            case "ERROR":
                if (t == null)
                    error(message);
                else
                    error(message, t);
                break;
            case "DEBUG":
                if (t == null)
                    debug(message);
                else
                    debug(message, t);
                break;
            case "WARN":
                if (t == null)
                    warn(message);
                else
                    warn(message, t);
                break;
            case "TRACE":
                if (t == null)
                    trace(message);
                else
                    trace(message, t);
                break;
            default: break;
        }

        sendToWatchers(origMsg);
    }

    @Override
    public void exception(String tag, String message, Throwable e) {
        String line = generateLogLine('e', tag, message);
        logIt(LogLevel.ERROR, line, e);
    }

    public void info(String tag, String message) {
        if (isInfoEnabled()) {
            String line = generateLogLine('i', tag, message);
            logIt(LogLevel.INFO, line, null);
        }
    }

    public void debug(String tag, String message) {
        if (isDebugEnabled()) {
            String line = generateLogLine('d', tag, message);
            logIt(LogLevel.DEBUG, line, null);
        }
    }

    public void warn(String tag, String message) {
        String line = generateLogLine('w', tag, message);
        logIt(LogLevel.WARN, line, null);
    }

    public void severe(String tag, String message) {
        String line = generateLogLine('s', tag, message);
        logIt(LogLevel.ERROR, line, null);
    }

    public void trace(String tag, String message) {
        if (isTraceEnabled()) {
            String line = generateLogLine('t', tag, message);
            logIt(LogLevel.TRACE, line, null);
        }
    }

    private static List<LogLineInjector> injectors;
    protected List<LogLineInjector> getInjectors() {
        if (injectors == null) {
            injectors = new ArrayList<>(MdwServiceRegistry.getInstance().getDynamicServices(LogLineInjector.class));
        }
        return injectors;
    }
}
