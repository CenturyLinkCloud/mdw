package com.centurylink.mdw.model.task;

import com.centurylink.mdw.model.Attributes;
import com.centurylink.mdw.model.user.User;
import com.centurylink.mdw.model.workflow.Package;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.model.workflow.ProcessInstance;
import com.centurylink.mdw.model.workflow.ProcessRuntimeContext;
import com.centurylink.mdw.util.log.StandardLogger;
import com.sun.el.ValueExpressionLiteral;

import javax.el.ValueExpression;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * The runtime context for a task instance.
 */
public class TaskRuntimeContext extends ProcessRuntimeContext {

    private TaskTemplate taskTemplate;
    public TaskTemplate getTaskTemplate() { return taskTemplate; }

    public Attributes getTaskAttributes() {
        return taskTemplate.getAttributes() == null ? new Attributes() : taskTemplate.getAttributes();
    }

    private TaskInstance taskInstance;
    public TaskInstance getTaskInstance() { return taskInstance; }

    private User assignee;
    public User getAssignee() { return assignee; }

    @Override
    public boolean isInService() {
        throw new UnsupportedOperationException();
    }

    public TaskRuntimeContext(StandardLogger logger, Package pkg, Process process,
            ProcessInstance processInstance, Map<String,Object> variables, TaskTemplate template, TaskInstance taskInstance, User assignee) {
        super(logger, pkg, process, processInstance, 0, false, variables);
        this.taskTemplate = template;
        this.taskInstance = taskInstance;
        this.assignee = assignee;
    }

    public TaskRuntimeContext(StandardLogger logger, ProcessRuntimeContext processContext, TaskTemplate template, TaskInstance taskInstance, User assignee) {
        this(logger, processContext.getPackage(), processContext.getProcess(),
                processContext.getProcessInstance(), processContext.getValues(), template, taskInstance, assignee);
    }


    private Map<String,ValueExpression> valueExpressionMap;
    @Override
    protected Map<String,ValueExpression> getValueExpressionMap() {
        if (valueExpressionMap == null) {
            valueExpressionMap = super.getValueExpressionMap();
            valueExpressionMap.put("context", new ValueExpressionLiteral(this, Object.class));
            valueExpressionMap.put("task", new ValueExpressionLiteral(this.taskInstance, Object.class));
            valueExpressionMap.put("taskInstanceId", new ValueExpressionLiteral(this.getTaskInstance().getTaskInstanceId(), String.class));
            valueExpressionMap.put("taskName", new ValueExpressionLiteral(this.getTaskTemplate().getTaskName(), String.class));
            valueExpressionMap.put("due", new ValueExpressionLiteral(this.getTaskInstance().getDue(), Instant.class));
            valueExpressionMap.put("taskInstanceUrl", new ValueExpressionLiteral(this.getTaskInstance().getTaskInstanceUrl(), String.class));
            valueExpressionMap.put("assignee", new ValueExpressionLiteral(this.getTaskInstance().getAssignee(), String.class));
        }
        return valueExpressionMap;
    }
    public Long getTaskInstanceId() {
        return this.taskInstance.getTaskInstanceId();
    }
    public String getTaskName() {
        return this.taskTemplate.getTaskName();
    }
    public Instant getDue() {
        return this.taskInstance.getDue();
    }
    public String getTaskInstanceUrl() {
            return this.taskInstance.getTaskInstanceUrl();
    }

    private String taskActionUrl;
    public String getTaskActionUrl() { return taskActionUrl; }
    public void setTaskActionUrl(String taskActionUrl) { this.taskActionUrl = taskActionUrl; }

    public String getTaskLogicalId() {
        return taskTemplate.getLogicalId();
    }

    public String getName() { return getTaskName(); }
    public String getTitle() { return taskInstance.getTitle(); }
    public Long getInstanceId() { return getTaskInstanceId(); }
    public String getInstanceUrl() { return getTaskInstanceUrl(); }
    public String getMasterRequestId() { return taskInstance.getMasterRequestId(); }
    public Instant getStart() { return taskInstance.getStart(); }
    public Instant getEnd() { return taskInstance.getEnd(); }
    public String getUserIdentifier() { return taskInstance.getUserIdentifier(); }
    public Integer getStatusCode() { return taskInstance.getStatusCode(); }
    public Integer getStateCode() { return taskInstance.getStateCode(); }
    public String getStatus() { return TaskStatuses.getTaskStatuses().get(getStatusCode()); }
    public String getComments() { return taskInstance.getComments(); }
    public String getDescription() { return this.taskTemplate.getComment(); }
    public Long getTaskId() { return taskInstance.getTaskId(); }
    public String getLogicalId() { return getTaskLogicalId(); }

    public String getAdvisory()
    {
      if (!TaskState.STATE_JEOPARDY.equals(getStateCode()) && !TaskState.STATE_ALERT.equals(getStateCode()))
          return null;
      else
          return TaskStates.getTaskStates().get(getStateCode());
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

    public String getAttribute(String name) {
        return getTaskAttributes().get(name);
    }
}
