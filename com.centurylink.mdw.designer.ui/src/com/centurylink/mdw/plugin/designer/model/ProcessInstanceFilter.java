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
package com.centurylink.mdw.plugin.designer.model;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.centurylink.mdw.model.value.variable.VariableVO;

public class ProcessInstanceFilter {
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MMM-yyyy");

    private String process;

    public String getProcess() {
        return process;
    }

    public void setProcess(String proc) {
        this.process = proc;
    }

    private Long processInstanceId;

    public Long getProcessInstanceId() {
        return processInstanceId;
    }

    public void setProcessInstanceId(Long piid) {
        this.processInstanceId = piid;
    }

    private String masterRequestId;

    public String getMasterRequestId() {
        return masterRequestId;
    }

    public void setMasterRequestId(String mrid) {
        this.masterRequestId = mrid;
    }

    private String owner;

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    private Long ownerId;

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    private Integer statusCode;

    public Integer getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(Integer sc) {
        this.statusCode = sc;
    }

    private Date startDateFrom;

    public Date getStartDateFrom() {
        return startDateFrom;
    }

    public void setStartDateFrom(Date sdf) {
        this.startDateFrom = sdf;
    }

    private Date startDateTo;

    public Date getStartDateTo() {
        return startDateTo;
    }

    public void setStartDateTo(Date sdt) {
        this.startDateTo = sdt;
    }

    private Date endDateFrom;

    public Date getEndDateFrom() {
        return endDateFrom;
    }

    public void setEndDateFrom(Date edf) {
        this.endDateFrom = edf;
    }

    private Date endDateTo;

    public Date getEndDateTo() {
        return endDateTo;
    }

    public void setEndDateTo(Date edt) {
        this.endDateTo = edt;
    }

    private Integer pageSize = 20;

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer size) {
        this.pageSize = size;
    }

    public Map<String, String> getProcessCriteria() {
        Map<String, String> criteria = new HashMap<String, String>();

        if (processInstanceId != null)
            criteria.put("id", processInstanceId.toString());
        if (masterRequestId != null)
            criteria.put("masterRequestId", masterRequestId);
        if (owner != null)
            criteria.put("owner", owner);
        if (ownerId != null)
            criteria.put("ownerId", ownerId.toString());
        if (statusCode != null)
            criteria.put("statusCode", statusCode.toString());
        if (startDateFrom != null)
            criteria.put("startDatefrom", dateFormat.format(startDateFrom));
        if (startDateTo != null)
            criteria.put("startDateto", dateFormat.format(startDateTo));
        if (endDateFrom != null)
            criteria.put("endDatefrom", dateFormat.format(endDateFrom));
        if (endDateTo != null)
            criteria.put("endDateto", dateFormat.format(endDateTo));

        return criteria;
    }

    private Map<String, String> variableValues = new HashMap<String, String>();

    public Map<String, String> getVariableValues() {
        return variableValues;
    }

    public Map<String, String> getVariableCriteria(WorkflowProcess processVersion) {
        Map<String, String> criteria = new HashMap<String, String>();
        for (String varName : variableValues.keySet()) {
            String varValue = variableValues.get(varName);
            if (varValue != null && varValue.trim().length() > 0) {
                VariableVO varVO = processVersion.getVariable(varName);
                if (varVO != null && varVO.getVariableType().equals("java.util.Date")) {
                    String mon = varValue.substring(4, 7).toUpperCase();
                    String dd = varValue.substring(8, 10);
                    String yyyy = varValue.substring(24);
                    String dateVarVal = "to_date('" + mon + " " + dd + " " + yyyy
                            + "', 'MON DD YYYY')";
                    criteria.put("DATE:" + varName, " = " + dateVarVal);
                }
                else {
                    criteria.put(varName, " = '" + varValue + "'");
                }
            }
        }
        return criteria;
    }
}
