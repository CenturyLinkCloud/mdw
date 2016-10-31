/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model.request;

import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.Jsonable;
import com.centurylink.mdw.util.StringHelper;

public class Request implements Jsonable {

    // this is the document id
    private Long id = 0L;
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    private Date created;
    public Date getCreated() { return created; }
    public void setCreated(Date received) { this.created = received; }

    private Date responded;
    public Date getResponded() { return responded; }
    public void setResponded(Date responded) { this.responded = responded; }

    private Long responseId;
    public Long getResponseId() { return responseId; }
    public void setResponseId(Long responseId) { this.responseId = responseId; }

    // these fields are for master requests
    private String masterRequestId;
    public String getMasterRequestId() { return masterRequestId; }
    public void setMasterRequestId(String masterRequestId) { this.masterRequestId = masterRequestId; }

    private Long processInstanceId;
    public Long getProcessInstanceId() { return processInstanceId; }
    public void setProcessInstanceId(Long instanceId) { this.processInstanceId = instanceId; }

    private Long processId;
    public Long getProcessId() { return processId; }
    public void setProcessId(Long id) { this.processId = id; }

    private String processName;
    public String getProcessName() { return processName; }
    public void setProcessName(String processName) { this.processName = processName; }

    private String processVersion;
    public String getProcessVersion() { return processVersion; }
    public void setProcessVersion(String version) { this.processVersion = version; }

    private String packageName;
    public String getPackageName() { return packageName; }
    public void setPackageName(String pkg) { this.packageName = pkg; }

    private String processStatus;
    public String getProcessStatus() { return processStatus; }
    public void setProcessStatus(String status) { this.processStatus = status; }

    private Date processStart;
    public Date getProcessStart() { return processStart; }
    public void setProcessStart(Date start) { this.processStart = start; }

    private Date processEnd;
    public Date getProcessEnd() { return processEnd; }
    public void setProcessEnd(Date end) { this.processEnd = end; }

    private String content;
    public String getContent() { return content; }
    public void setContent(String c) { this.content = c; }

    private String responseContent;
    public String getResponseContent() { return responseContent; }
    public void setResponseContent(String rc) { this.responseContent = rc; }

    private boolean outbound;
    public boolean isOutbound() { return outbound; }
    public void setOutbound(boolean ob) { this.outbound = ob; }

    public Request(Long id) {
        this.id = id;
    }

    public Request(JSONObject json) throws JSONException {
        id = json.getLong("id");
        if (json.has("created"))
            created = StringHelper.stringToDate(json.getString("created"));
        if (json.has("responded"))
            responded = StringHelper.stringToDate(json.getString("responded"));
        if (json.has("responseId"))
            responseId = json.getLong("responseId");
        if (json.has("masterRequestId"))
            masterRequestId = json.getString("masterRequestId");
        if (json.has("processInstanceId"))
            processInstanceId = json.getLong("processInstanceId");
        if (json.has("processId"))
            processId = json.getLong("processId");
        if (json.has("processName"))
            processName = json.getString("processName");
        if (json.has("processVersion"))
            processVersion = json.getString("processVersion");
        if (json.has("packageName"))
            packageName = json.getString("packageName");
        if (json.has("processStatus"))
            processStatus = json.getString("processStatus");
        if (json.has("processStart"))
            processStart = StringHelper.stringToDate(json.getString("processStart"));
        if (json.has("processEnd"))
            processEnd = StringHelper.stringToDate(json.getString("processEnd"));
        if (json.has("outbound"))
            outbound = json.getBoolean("outbound");
        if (json.has("content"))
            content = json.getString("content");
        if (json.has("responseContent"))
            responseContent = json.getString("responseContent");
    }

    public JSONObject getJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", id);
        if (created != null)
            json.put("created", StringHelper.dateToString(created));
        if (responded != null)
            json.put("responded", StringHelper.dateToString(responded));
        if (responseId != null)
            json.put("responseId", responseId);
        if (masterRequestId != null)
            json.put("masterRequestId", masterRequestId);
        if (processInstanceId != null)
            json.put("processInstanceId", processInstanceId);
        if (processId != null)
            json.put("processId", processId);
        if (processName != null)
            json.put("processName", processName);
        if (processVersion != null)
            json.put("processVersion", processVersion);
        if (packageName != null)
            json.put("packageName", packageName);
        if (processStatus != null)
            json.put("processStatus", processStatus);
        if (processStart != null)
            json.put("processStart", StringHelper.dateToString(processStart));
        if (processEnd != null)
            json.put("processEnd", StringHelper.dateToString(processEnd));
        if (outbound)
            json.put("outbound", outbound);
        if (content != null)
            json.put("content", content);
        if (responseContent != null)
            json.put("responseContent", responseContent);
        return json;
    }

    public String getJsonName() {
        return "request";
    }

}
