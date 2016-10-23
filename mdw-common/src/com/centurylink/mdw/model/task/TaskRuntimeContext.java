/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model.task;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.el.ValueExpression;

import com.centurylink.mdw.model.workflow.Package;
import com.centurylink.mdw.model.workflow.ProcessInstance;
import com.centurylink.mdw.model.workflow.ProcessRuntimeContext;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.util.StringHelper;
import com.sun.el.ValueExpressionLiteral;

/**
 * The runtime context for a task instance.
 */
public class TaskRuntimeContext extends ProcessRuntimeContext {

    private TaskTemplate taskTemplate;
    public TaskTemplate getTaskTemplate() { return taskTemplate; }

    public String getTaskAttribute(String name) {
        return taskTemplate.getAttribute(name);
    }

    private TaskInstance taskInstanceVO;
    public TaskInstance getTaskInstanceVO() { return taskInstanceVO; }

    public TaskRuntimeContext(Package packageVO, Process processVO,
            ProcessInstance processInstanceVO, TaskTemplate taskVO, TaskInstance taskInstanceVO) {
        super(packageVO, processVO, processInstanceVO);
        this.taskTemplate = taskVO;
        this.taskInstanceVO = taskInstanceVO;
    }

    public TaskRuntimeContext(Package packageVO, Process processVO,
            ProcessInstance processInstanceVO, TaskTemplate taskVO, TaskInstance taskInstanceVO, Map<String, Object> variables) {
        super(packageVO, processVO, processInstanceVO, variables);
        this.taskTemplate = taskVO;
        this.taskInstanceVO = taskInstanceVO;
    }


    private Map<String,ValueExpression> valueExpressionMap;
    @Override
    protected Map<String,ValueExpression> getValueExpressionMap() {
        if (valueExpressionMap == null) {
            valueExpressionMap = super.getValueExpressionMap();
            valueExpressionMap.put("context", new ValueExpressionLiteral(this, Object.class));
            valueExpressionMap.put("task", new ValueExpressionLiteral(this, Object.class));  //for backward compatibility
            valueExpressionMap.put("taskInstanceId", new ValueExpressionLiteral(this.getTaskInstanceVO().getTaskInstanceId(), String.class));
            valueExpressionMap.put("taskName", new ValueExpressionLiteral(this.getTaskTemplate().getTaskName(), String.class));
            valueExpressionMap.put("dueDate", new ValueExpressionLiteral(this.getTaskInstanceVO().getDueDate(), String.class));
            valueExpressionMap.put("orderId", new ValueExpressionLiteral(this.getTaskInstanceVO().getOrderId(), String.class));
            valueExpressionMap.put("taskInstanceUrl", new ValueExpressionLiteral(this.getTaskInstanceVO().getTaskInstanceUrl(), String.class));
            valueExpressionMap.put("taskClaimUserCuid", new ValueExpressionLiteral(this.getTaskInstanceVO().getTaskClaimUserCuid(), String.class));
        }
        return valueExpressionMap;
    }
    public Long getTaskInstanceId() {
        return this.taskInstanceVO.getTaskInstanceId();
    }
    public String getTaskName() {
        return this.taskTemplate.getTaskName();
    }
    public Date getDueDate() {
        return this.taskInstanceVO.getDueDate();
    }
    public String getOrderId() {
        return this.taskInstanceVO.getOrderId();
    }
    public String getTaskInstanceUrl() {
            return this.taskInstanceVO.getTaskInstanceUrl();
    }
    public String getTaskClaimUserCuid() {
        return this.taskInstanceVO.getTaskClaimUserCuid();
    }
    private String description;
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    private String taskActionUrl;
    public String getTaskActionUrl() { return taskActionUrl; }
    public void setTaskActionUrl(String taskActionUrl) { this.taskActionUrl = taskActionUrl; }

    public String getTaskLogicalId() {
        return taskTemplate.getLogicalId();
    }

    public String getName() { return getTaskName(); }
    public Long getInstanceId() { return getTaskInstanceId(); }
    public String getInstanceUrl() { return getTaskInstanceUrl(); }
    public String getMasterRequestId() { return taskInstanceVO.getMasterRequestId(); }
    public Date getStartDate() { return StringHelper.stringToDate(taskInstanceVO.getStartDate()); }
    public Date getEndDate() { return StringHelper.stringToDate(taskInstanceVO.getEndDate()); }
    public String getAssignee() { return taskInstanceVO.getTaskClaimUserCuid();  }
    public String getUserIdentifier() { return taskInstanceVO.getUserIdentifier(); }
    public Integer getStatusCode() { return taskInstanceVO.getStatusCode(); }
    public Integer getStateCode() { return taskInstanceVO.getStateCode(); }
    public String getStatus() { return TaskStatuses.getTaskStatuses().get(getStatusCode()); }
    public String getComments() { return taskInstanceVO.getComments(); }
    public String getMessage() { return taskInstanceVO.getActivityMessage(); }
    public String getActivityName() { return taskInstanceVO.getActivityName(); }
    public Long getTaskId() { return taskInstanceVO.getTaskId(); }
    public String getLogicalId() { return getTaskLogicalId(); }


    public String getFormattedDueDate() {
        Date dd = getDueDate();
        if (dd == null)
            return null;
        return new SimpleDateFormat("MM/dd/yyyy").format(dd);
    }

    public String getAdvisory()
    {
      String state = TaskStates.getTaskStates().get(getStateCode());
      if (!TaskState.STATE_JEOPARDY.equals(state) && !TaskState.STATE_ALERT.equals(state))
          return null;
      else
          return state;
    }

    public Map<String,String> getKeyParameters() {
        Map<String,String> keyParams = new HashMap<String,String>();
        keyParams.put("taskInstanceId", getTaskInstanceId().toString());
        if (getProcessInstanceId() != null)
          keyParams.put("processInstanceId", getProcessInstanceId().toString());
        if (getUserIdentifier() != null)
          keyParams.put("userId", getUserIdentifier());
        return keyParams;
    }
}
