package com.centurylink.mdw.common.service;

import com.centurylink.mdw.dataaccess.file.GitProgressMonitor;
import com.centurylink.mdw.model.Jsonable;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class WebSocketProgressMonitor implements GitProgressMonitor, Jsonable {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    private String topic;
    private String title;
    private int totalTasks;
    private String currentTask;
    private int currentTaskTotalWork;
    private int currentTaskCompleted;
    private boolean done;
    private Throwable error;

    public WebSocketProgressMonitor(String topic, String title) {
        this.topic = topic;
        this.title = title;
    }

    @Override
    public void start(int totalTasks) {
        this.totalTasks = totalTasks;
        this.currentTask = null;
        this.currentTaskTotalWork = 0;
        this.currentTaskCompleted = 0;
        this.done = false;
        if (logger.isDebugEnabled())
            logger.debug(title + " started with " + totalTasks + " tasks");
        send();
    }

    @Override
    public void beginTask(String title, int totalWork) {
        this.currentTask = title;
        this.currentTaskTotalWork = totalWork;
        this.currentTaskCompleted = 0;
        if (logger.isDebugEnabled())
            logger.debug(this.title + ": '" + title + "' started");
        send();
    }

    @Override
    public void update(int completed) {
        int before = getCurrentTaskPercentComplete();
        this.currentTaskCompleted += completed;
        int after = getCurrentTaskPercentComplete();
        if (after != before) {
            if (logger.isDebugEnabled())
                logger.debug(title + ": '" + currentTask + "' progress -> " + getCurrentTaskPercentComplete());
            send();
        }
    }

    @Override
    public void endTask() {
        this.currentTaskCompleted = this.currentTaskTotalWork;
        if (logger.isDebugEnabled())
            logger.debug(title + ": '" + currentTask + "' end");
        send();
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    public int getCurrentTaskPercentComplete() {
        int percent = (int)Math.round((double)this.currentTaskCompleted * 100 / this.currentTaskTotalWork);
        return percent >= 0 ? percent : 0;  // sometimes jgit reports negative
    }

    public void done() {
        this.done = true;
        if (logger.isDebugEnabled())
            logger.debug(title + " done");
        send();
    }

    public void error(Throwable t) {
        this.error = t;
        if (logger.isDebugEnabled())
            logger.debug(t.getMessage(), t);
        send();
    }

    private void send() {
        try {
            WebSocketMessenger.getInstance().send(topic, getJson().toString());
        }
        catch (IOException ex) {
            logger.error(ex.getMessage(), ex);
        }
    }

    @Override
    public JSONObject getJson() throws JSONException {
        JSONObject json = create();
        json.put("title", this.title);
        if (this.error != null) {
            json.put("error", this.error.getMessage());
        }
        else if (this.done) {
            json.put("done", true);
        }
        else {
            json.put("task", this.currentTask);
            json.put("progress", getCurrentTaskPercentComplete());
        }
        return json;
    }
}
