/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.util.log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.MdwWebSocketServer;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.soccom.SoccomClient;

public abstract class AbstractStandardLoggerBase implements StandardLogger {

    public static final String DEFAULT_HOST = "localhost";
    public static final String DEFAULT_PORT = "7181";

    private static final String SENTRY_MARK = "[SENTRY-MARK] ";

    private static final String MESSAGE_REG_EX = "\\[\\(.\\)([0-9.:]+) p([0-9]+)\\.([0-9]+) ([a-z])([^\\]]+)\\] (.*)";

    private static Pattern pattern = Pattern.compile(MESSAGE_REG_EX, Pattern.DOTALL);

    protected String watcher = null;

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
        SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd.HH:mm:ss.SSS");
        sb.append(df.format(new Date()));
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

    protected JSONObject buildJSONLogMessage(String message) throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("name", "LogWatcher");

        Matcher matcher = pattern.matcher(message);

        if (matcher.matches())
        {
            obj.put("time", matcher.group(1));
            obj.put("procId", new Long(matcher.group(2)));
            obj.put("procInstId", new Long(matcher.group(3)));
            obj.put("subtype", matcher.group(4));
            obj.put("id", matcher.group(5));
            obj.put("msg", matcher.group(6));
        }

        return obj;
    }

    protected void sendToWatchers(String message) {
        if (watching())  // Designer
            sendToWatcher(message);

        try {
            JSONObject jsonObj = buildJSONLogMessage(message);

            if (jsonObj != null && jsonObj.has("ProcInstId")) {
                if (MdwWebSocketServer.getInstance().hasInterestedConnections(jsonObj.getString("ProcInstId")))
                    sendToWebWatcher(jsonObj);
            }
        }
        catch (Throwable e) {};
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

    protected void sendToWebWatcher(JSONObject message) throws JSONException {
        MdwWebSocketServer.getInstance().send(message.toString(2), message.getString("ProcInstId"));
    }

}
