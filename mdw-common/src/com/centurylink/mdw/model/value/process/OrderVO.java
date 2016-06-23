/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model.value.process;

import java.io.Serializable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.Instance;
import com.centurylink.mdw.common.service.Jsonable;

/**
 * This class represents process instances.
 * It is used for two purposes: a) represent memory image
 * of process instances in execution engine; b) represent
 * process instance runtime information for designer/task manager
 *
 * The following fields are used by designer only:
 *   - activity instances
 *   - transition instances
 *   - remote server
 *   - start and end dates
 *
 * Objects of this class can be created in two cases for designer:
 *   1) when displaying a list of process instances in the designer
 *   2) when returning a list of parent/ancestor process instances.
 * In the second case, the version is not returned (has value 0)
 *
 * For the engine, the objects can also be created in two cases:
 *   3) when starting a process, create the object from JMS message
 *      content
 *   4) when loading an existing process instance, where contents
 *      are loaded from database.
 *
 */
public class OrderVO implements Serializable, Jsonable, Instance {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a skeleton process instance VO (without an ID).
     * @param processVO
     */
    public OrderVO(Long processId, String processName) {
        setMainProcessName(processName);
        setMainProcessId(processId);
    }

    public OrderVO(ProcessInstanceVO pVO) {
        setMainProcessName(pVO.getProcessName());
        setMainProcessId(pVO.getId()); // instance id
        setMasterRequestId(pVO.getMasterRequestId());
        setStartDate(pVO.getStartDate());
        setEndDate(pVO.getEndDate());
        setStatusCode(pVO.getStatusCode());
    }

    private Long id;
    public String getId() {
/*        if (processInstanceVOs != null) {
            for (ProcessInstanceVO instance : processInstanceVOs) {
                if (instance.getProcessId() == mainProcessId){
                    id = instance.getId();
                    break;
                }
            }
        }*/
        return getMainProcessId().toString();
    }
    public void setId(Long l) { id = l; }

    private Long mainProcessId;
    public Long getMainProcessId() { return mainProcessId; }
    public void setMainProcessId(Long l) { mainProcessId = l; }

    private String mainProcessName;
    public String getMainProcessName() { return mainProcessName; }
    public void setMainProcessName(String s) { mainProcessName = s; }

    private String masterRequestId;
    public String getMasterRequestId() { return masterRequestId; }
    public void setMasterRequestId(String s) { masterRequestId = s; }


    private Integer statusCode;
    public Integer getStatusCode() { return statusCode; }
    public void setStatusCode(Integer i) { statusCode = i; }

    private String startDate;
    public String getStartDate() { return startDate; }
    public void setStartDate(String d) { startDate = d; }

    private String completionCode;
    public String getCompletionCode() { return completionCode; }
    public void setCompletionCode(String s) { this.completionCode = s; }


    // for designer run time information display only
    private String endDate;
    public String getEndDate() { return endDate; }
    public void setEndDate(String d) { endDate = d; }

    private String comment;
    public String getComment() { return comment; }
    public void setComment(String s) { comment = s; }


    private ProcessInstanceVO[] processInstanceVOs;

    /**
     * @return the processInstanceVOs
     */
    public ProcessInstanceVO[] getProcessInstanceVOs() {
        return processInstanceVOs;
    }
    /**
     * @param processInstanceVOs the processInstanceVOs to set
     */
    public void setProcessInstanceVOs(ProcessInstanceVO[] processInstanceVOs) {
        this.processInstanceVOs = processInstanceVOs;
    }
    public OrderVO() {
    }

    public OrderVO(String json) throws JSONException {
        this(new JSONObject(json));
    }

    public OrderVO(JSONObject jsonObj) throws JSONException {
       if (jsonObj.has("id")){
            this.id = jsonObj.getLong("id");
            this.mainProcessId = jsonObj.getLong("id");
        }
        if (jsonObj.has("mainProcessName"))
            mainProcessName = jsonObj.getString("mainProcessName");
        if (jsonObj.has("masterRequestId"))
            masterRequestId = jsonObj.getString("masterRequestId");
        if (jsonObj.has("startDate"))
            startDate = jsonObj.getString("startDate");
        if (jsonObj.has("endDate"))
            endDate = jsonObj.getString("endDate");
        if (jsonObj.has("statusCode"))
            statusCode = jsonObj.getInt("statusCode");
        if (jsonObj.has("instanceUrl"))
            orderUrl = jsonObj.getString("instanceUrl");
        if (jsonObj.has("processInstanceVOs")) {
            JSONArray piVOs = jsonObj.getJSONArray("processInstanceVOs");
            processInstanceVOs = new ProcessInstanceVO[piVOs.length()];
            for (int i = 0; i < piVOs.length(); i++) {
                processInstanceVOs[i] = new ProcessInstanceVO();
                String name = piVOs.getString(i);
                processInstanceVOs[i].setProcessName(name);
                processInstanceVOs[i].setProcessId(this.id);
                this.mainProcessName = name;
            }
        }
        else {
            processInstanceVOs = new ProcessInstanceVO[0];
        }

     }

    public JSONObject getJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", mainProcessId);
        json.put("masterRequestId", masterRequestId);
        json.put("startDate", startDate);
        json.put("endDate", endDate);
        json.put("statusCode", statusCode);
        json.put("instanceUrl", orderUrl);
        json.put("mainProcessName", mainProcessName);
        json.put("mainProcessId", mainProcessId);

        if (processInstanceVOs != null) {
            JSONArray grpsJson = new JSONArray();
            for (ProcessInstanceVO instance : processInstanceVOs) {
                grpsJson.put(instance.getProcessName());
            }
            json.put("processInstanceVOs", grpsJson);
        }
        return json;
    }

    public String getJsonName() {
        return ORDER_JSONNAME;
    }
    public static final String ORDER_JSONNAME = "Order";
    public String getInstanceUrl() {
        return orderUrl;
    }
    private String orderUrl;
}
