/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model.value.task;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.activity.types.TaskActivity;
import com.centurylink.mdw.common.constant.PropertyNames;
import com.centurylink.mdw.common.constant.TaskAttributeConstant;
import com.centurylink.mdw.common.constant.WorkAttributeConstant;
import com.centurylink.mdw.common.service.Jsonable;
import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.common.utilities.property.PropertyManager;
import com.centurylink.mdw.dataaccess.DataAccess;
import com.centurylink.mdw.model.data.task.TaskAction;
import com.centurylink.mdw.model.data.task.TaskState;
import com.centurylink.mdw.model.data.task.TaskStates;
import com.centurylink.mdw.model.data.task.TaskStatus;
import com.centurylink.mdw.model.data.task.TaskStatuses;
import com.centurylink.mdw.model.data.task.TaskType;
import com.centurylink.mdw.model.value.attribute.AttributeVO;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;
import com.centurylink.mdw.model.value.variable.VariableVO;
import com.centurylink.mdw.task.Attribute;
import com.centurylink.mdw.task.TaskTemplate;

import io.swagger.annotations.ApiModel;

/**
 * Value object for the Task persistable.
 * Shallow version does not include variables, attributes, userGroups or SLAs.
 */
@ApiModel(value="TaskTemplate", description="MDW task definition")
public class TaskVO extends RuleSetVO implements Jsonable {

    private static final char FIELD_DELIMITER = ',';
    private static final char ROW_DELIMITER = ';';
    public static final String MDW4_TASK_LOGICAL_ID_PREFIX = "MDW4_";
    public static final String AUTOFORM = "Autoform";

    private String taskName;
	private Long taskId;
    private Integer taskTypeId;
    private String taskCategory;
    private List<VariableVO> variables;
    private String logicalId;

    private boolean shallow;
    public boolean isShallow() { return shallow; }
    public void setShallow(boolean shallow) { this.shallow = shallow; }

    public TaskVO() {
        setLanguage(TASK);
    }

    public TaskVO(TaskTemplate template) {
        setLanguage(TASK);
        setLogicalId(template.getLogicalId());
        setTaskName(template.getName());
        setTaskTypeId(TaskType.TASK_TYPE_TEMPLATE);
        setTaskCategory(template.getCategory());
        setComment(template.getDescription());
        setShallow(false);
        if (template.getAttributeList() != null)
        {
          for (Attribute attr : template.getAttributeList())
              setAttribute(attr.getName(), attr.getStringValue());
        }
        String vars = getAttribute(TaskAttributeConstant.VARIABLES);
        if (vars != null)
            setVariablesFromString(vars, null);
        String groups = getAttribute(TaskAttributeConstant.GROUPS);
        if (groups != null)
            setUserGroupsFromString(groups);
    }

    public TaskVO(TaskVO cloneFrom) {
        super(cloneFrom);
        setLanguage(TASK);
        setLogicalId(cloneFrom.getLogicalId());
        setTaskName(cloneFrom.getTaskName());
        setTaskTypeId(cloneFrom.getTaskTypeId());
        setTaskCategory(cloneFrom.getTaskCategory());
        setComment(cloneFrom.getComment());
        setShallow(false);
        setAttributes(cloneFrom.getAttributes());
        String vars = getAttribute(TaskAttributeConstant.VARIABLES);
        if (vars != null)
            setVariablesFromString(vars, null);
        String groups = getAttribute(TaskAttributeConstant.GROUPS);
        if (groups != null)
            setUserGroupsFromString(groups);
    }

    public Integer getTaskTypeId(){
        return this.taskTypeId;
    }

    public void setTaskTypeId(Integer pType){
        this.taskTypeId = pType;
    }
	public Long getTaskId() {
		return taskId;
	}
	public void setTaskId(Long taskId) {
		this.taskId = taskId;
	}
	public String getLogicalId() {
		return logicalId;
	}
	public void setLogicalId(String logicalId) {
		this.logicalId = logicalId;
	}
	public String getTaskName() {
		return taskName;
	}
	public void setTaskName(String taskName) {
		this.taskName = taskName;
	}
    public String getTaskCategory() {
		return taskCategory;
	}
	public void setTaskCategory(String category) {
		this.taskCategory = category;
	}

	public List<String> getWorkgroups() {
	    return getUserGroups();
	}
	public List<String> getUserGroups() {
		return this.getGroups(TaskAttributeConstant.GROUPS);
	}

	public boolean isForWorkgroup(String group) {
	    for (String workgroup : getWorkgroups()) {
	        if (workgroup.equals(group))
	            return true;
	    }
	    return false;
	}

	public List<String> getNoticeGroups() {
      return this.getGroups(TaskAttributeConstant.NOTICE_GROUPS);
	}

	private List<String> getGroups(String groupAttributeName) {
	  List<String> groups = new ArrayList<String>();
      String groupsString = this.getAttribute(groupAttributeName);
      if (groupsString!=null && groupsString.length()>0) {
        for (String group : groupsString.split("#"))
          groups.add(group);
      }
      return groups;
	}

	public void setWorkgroups(List<String> workgroups) {
	    setUserGroups(workgroups);
	}
	public void setUserGroups(List<String> userGroups) {
		if (userGroups==null || userGroups.size()==0) {
	    	this.setAttribute(TaskAttributeConstant.GROUPS, null);
		} else {
			StringBuffer sb = new StringBuffer();
			boolean first = true;
			for (String grp : userGroups) {
				if (first) first = false;
				else sb.append('#');
				sb.append(grp);
			}
			this.setAttribute(TaskAttributeConstant.GROUPS, sb.toString());
		}
	}

    public List<VariableVO> getVariables() {
		return variables;
	}
	public void setVariables(List<VariableVO> pVars) {
		this.variables = pVars;
	}

	public String getComment() {
		return getAttribute(TaskAttributeConstant.DESCRIPTION);
	}
	public void setComment(String comments) {
		setAttribute(TaskAttributeConstant.DESCRIPTION, comments);
	}

	public int getSlaSeconds() {
		String sla = getAttribute(TaskAttributeConstant.TASK_SLA);
    	if (sla==null || sla.length()==0) return 0;
    	return Integer.parseInt(sla);
	}

	public void setSlaSeconds(int slaSeconds) {
	   	this.setAttribute(TaskAttributeConstant.TASK_SLA, Integer.toString(slaSeconds));
	}

	public int getAlertIntervalSeconds() {
        String alertIntervalString = getAttribute(TaskAttributeConstant.ALERT_INTERVAL);
        return StringHelper.isEmpty(alertIntervalString) ? 0 : Integer.parseInt(alertIntervalString);
	}

	public void setAlertIntervalSeconds(int alertIntervalSeconds) {
      this.setAttribute(TaskAttributeConstant.ALERT_INTERVAL, Integer.toString(alertIntervalSeconds));
    }

    public String getCustomPage() {
    	return getAttribute(TaskAttributeConstant.CUSTOM_PAGE);
    }

    public void setCustomPage(String page) {
    	setAttribute(TaskAttributeConstant.CUSTOM_PAGE, page);
    }

    public boolean isHasCustomPage() {
        return getCustomPage() != null;
    }

    public String getCustomPageAssetVersion() {
        return getAttribute(TaskAttributeConstant.CUSTOM_PAGE_ASSET_VERSION);
    }

    public void setCustomPageAssetVersion(String version) {
        setAttribute(TaskAttributeConstant.CUSTOM_PAGE_ASSET_VERSION, version);
    }

    public String getRenderingEngine() {
        String rendering = getAttribute(WorkAttributeConstant.RENDERING_ENGINE);
        if (rendering == null)
            rendering = PropertyManager.getProperty(PropertyNames.MDW_DEFAULT_RENDERING_ENGINE);
        if (rendering == null)
            rendering = WorkAttributeConstant.HTML5_RENDERING;
        return rendering;
    }

    public boolean isCompatibilityRendering() {
        return TaskAttributeConstant.COMPATIBILITY_RENDERING.equals(getRenderingEngine());
    }

    public String getFormName() {
    	return getAttribute(TaskAttributeConstant.FORM_NAME);
    }

    public void setFormName(String v) {
    	setAttribute(TaskAttributeConstant.FORM_NAME, v);
    }

    public boolean isTemplate() {
    	// this should be true for all task saved MDW 5.1 or newer
    	// including general and classic tasks
    	return getTaskTypeId().equals(TaskType.TASK_TYPE_TEMPLATE);
    }

    public boolean isUsingIndices() {
    	// uses TASK_INST_INDEX and TASK_INST_GRP_MAPP tables
    	return isTemplate() && (isGeneralTask() || isNeoClassicTask());
    }

    public boolean isGeneralTask() {
    	return getAttribute(TaskAttributeConstant.FORM_NAME) != null;
    }

    public boolean isAutoformTask() {
        if (isHasCustomPage())
          return false;
    	String formName = getAttribute(TaskAttributeConstant.FORM_NAME);
    	return AUTOFORM.equalsIgnoreCase(formName);
    }

    /**
     * Revived classic task for post-5.2 custom pages.
     */
    public boolean isNeoClassicTask() {
        return !isGeneralTask() && DataAccess.supportedSchemaVersion >= DataAccess.schemaVersion52;
    }

    /**
     * Checked if the passed in GroupIName is mapped to the task
     * @param pGroupName
     * @return boolean results
     */
    public boolean isGroupMapped(String pGroupName){
    	List<String> userGroups = this.getUserGroups();
        for(String g : userGroups){
            if(g.equals(pGroupName)) return true;
        }
        return false;
    }

     /**
     * Checked if the passed in Var Name is mapped to the task
     * @param pVarName
     * @return boolean results
     */
    public boolean isVariableMapped(String pVarName){
        if(variables == null){
            return false;
        }
        for (VariableVO vo : variables) {
            if(vo.getVariableName().equals(pVarName)){
                return true;
            }
        }
        return false;
    }

    public String getUserGroupsAsString() {
    	return getAttribute(TaskAttributeConstant.GROUPS);
    }

    private static VariableVO findVariable(List<VariableVO> list, VariableVO var) {
    	if (list==null) return null;
    	for (VariableVO one : list) {
    		if (var.getVariableName().equals(one.getVariableName())) return one;
    	}
    	return null;
    }

    public void setUserGroupsFromString(String str) {
    	setAttribute(TaskAttributeConstant.GROUPS, str);
    }

    public static String getVariablesAsString(List<VariableVO> processVariables, List<VariableVO> variables) {
    	if (processVariables==null) return null;
        StringBuffer sb = new StringBuffer();
        boolean firstRow = true;
        // first get variables already specified in the task
        if (variables!=null) {
	        for (VariableVO taskVar : variables) {
	            VariableVO var = findVariable(processVariables, taskVar);
	            if (var==null) continue;	// remove variables not in process definition
	            if (firstRow) firstRow = false;
	            else sb.append(ROW_DELIMITER);
	            sb.append(var.getVariableName()).append(FIELD_DELIMITER);
	            sb.append(taskVar.getVariableReferredAs()).append(FIELD_DELIMITER);
	            if (taskVar.getDisplayMode().equals(VariableVO.DATA_READONLY))
	                sb.append(TaskActivity.VARIABLE_DISPLAY_READONLY);
	            else if (VariableVO.DATA_OPTIONAL.equals(taskVar.getDisplayMode()))
	            	sb.append(TaskActivity.VARIABLE_DISPLAY_OPTIONAL);
	            else if (VariableVO.DATA_HIDDEN.equals(taskVar.getDisplayMode()))
	            	sb.append(TaskActivity.VARIABLE_DISPLAY_HIDDEN);
	            else sb.append(TaskActivity.VARIABLE_DISPLAY_REQUIRED);
	            sb.append(FIELD_DELIMITER);
	            if (taskVar.getDisplaySequence()==null) sb.append("0");
	            else sb.append(taskVar.getDisplaySequence().toString());
	            sb.append(FIELD_DELIMITER);
	            sb.append((taskVar.getDescription()==null)?"":taskVar.getDescription());
	            sb.append(FIELD_DELIMITER);
	            sb.append(var.getVariableType());
	        }
        }
        // now add process variables not specified in the task as not-displayed
        for (VariableVO var : processVariables) {
            VariableVO taskVar = findVariable(variables, var);
            if (taskVar!=null) continue;	// already handled above
            if (firstRow) firstRow = false;
            else sb.append(ROW_DELIMITER);
            sb.append(var.getVariableName()).append(FIELD_DELIMITER);
            String referredAs = var.getVariableName();
            sb.append(referredAs).append(FIELD_DELIMITER);
            sb.append(TaskActivity.VARIABLE_DISPLAY_NOTDISPLAYED);
            sb.append(FIELD_DELIMITER);
            sb.append("0");
            sb.append(FIELD_DELIMITER);
            sb.append(FIELD_DELIMITER);
            sb.append(var.getVariableType());
        }
        return sb.toString();
    }

    public String getVariablesAsString(List<VariableVO> processVariables) {
        return getVariablesAsString(processVariables, variables);
    }

    public void setVariablesFromString(String str, List<VariableVO> processVariables) {
    	variables = new ArrayList<VariableVO>();
    	if (str == null) return;
    	List<String[]> parsed = StringHelper.parseTable(str, FIELD_DELIMITER, ROW_DELIMITER, 6);
    	for (String[] one : parsed) {
    		if (one[2].equals(TaskActivity.VARIABLE_DISPLAY_NOTDISPLAYED)) continue;
    		VariableVO taskVar = new VariableVO();
    		taskVar.setVariableName(one[0]);
    		VariableVO var = findVariable(processVariables, taskVar);
    		if (var!=null) taskVar.setVariableType(var.getVariableType());
    		if (one[3].isEmpty())
    		    taskVar.setDisplaySequence(new Integer(0));
    		else
    		    taskVar.setDisplaySequence(new Integer(one[3]));
    		if (one[2].equals(TaskActivity.VARIABLE_DISPLAY_READONLY)) {
    			taskVar.setDisplayMode(VariableVO.DATA_READONLY);
    		} else if (one[2].equals(TaskActivity.VARIABLE_DISPLAY_OPTIONAL)) {
    			taskVar.setDisplayMode(VariableVO.DATA_OPTIONAL);
    		} else if (one[2].equals(TaskActivity.VARIABLE_DISPLAY_HIDDEN)) {
    			taskVar.setDisplayMode(VariableVO.DATA_HIDDEN);
    		} else {
    			taskVar.setDisplayMode(VariableVO.DATA_REQUIRED);
    		}
    		if (var!=null) taskVar.setVariableId(var.getVariableId());
    		taskVar.setVariableReferredAs(one[1]);
    		taskVar.setDescription(one[4]);		// reused as index key
    		if (StringHelper.isEmpty(taskVar.getVariableType())) {  // should have been set based on proc var type
                if (StringHelper.isEmpty(one[5])) taskVar.setVariableType(String.class.getName());
                else taskVar.setVariableType(one[5]);
    		}
    		int i, n = variables.size();
			for (i=0; i<n; i++) {
				VariableVO next = variables.get(i);
				if (taskVar.getDisplaySequence().intValue()<next.getDisplaySequence().intValue())
					break;
			}
			if (i<n) variables.add(i, taskVar);
			else variables.add(taskVar);
    	}
    }

    public void setVariablesFromAttribute() {
    	String str = this.getAttribute(TaskActivity.ATTRIBUTE_TASK_VARIABLES);
    	setVariablesFromString(str, null);
    }

    public boolean isMasterTask() {
        return !StringHelper.isEmpty(getAttribute(TaskActivity.ATTRIBUTE_SUBTASK_STRATEGY));
    }

    public static String updateVariableInString(String curString, List<VariableVO> processVariables) {
        List<String[]> parsed = StringHelper.parseTable(curString, FIELD_DELIMITER, ROW_DELIMITER, 6);
        int n = parsed.size();
        boolean[] keep = new boolean[n];
        for (int i=0; i<n; i++) keep[i] = parsed.get(i)[0].startsWith("#{") || parsed.get(i)[0].startsWith("${"); // expressions okay
        for (VariableVO var : processVariables) {
            boolean found = false;
            for (int i=0; !found && i<n; i++) {
                if (parsed.get(i)[0].equals(var.getVariableName())) {
                    found = true;
                    keep[i] = true;
                }
            }
            if (!found) {
                String[] newEntry = new String[6];
                newEntry[0] = var.getVariableName();
                if (var.getVariableReferredAs()==null)
                    newEntry[1] = newEntry[0];
                else newEntry[1] = var.getVariableReferredAs();
                newEntry[2] = TaskActivity.VARIABLE_DISPLAY_NOTDISPLAYED;
                newEntry[3] = "0";
                newEntry[4] = "";
                newEntry[5] = var.getVariableType();
                parsed.add(newEntry);
            }
        }
        for (int i=n-1; i>=0; i--) {
            if (!keep[i]) parsed.remove(i);
        }
        StringBuffer sb = new StringBuffer();
        boolean firstRow = true;
        for (String[] one : parsed) {
            if (firstRow) firstRow = false;
            else sb.append(ROW_DELIMITER);
            for (int i=0; i<one.length; i++) {
                if (i>0) sb.append(FIELD_DELIMITER);
                sb.append(one[i]);
            }
        }
        return sb.toString();
    }

    public static List<String> TASK_ACTION_OUTCOMES;
    static {
        TASK_ACTION_OUTCOMES = new ArrayList<String>();
        for (Integer statusCode : TaskStatus.allStatusCodes) {
            TASK_ACTION_OUTCOMES.add(TaskStatuses.getTaskStatuses().get(statusCode));
        }
        TASK_ACTION_OUTCOMES.add(TaskStates.getTaskStates().get(TaskState.STATE_ALERT));
        TASK_ACTION_OUTCOMES.add(TaskStates.getTaskStates().get(TaskState.STATE_JEOPARDY));
        TASK_ACTION_OUTCOMES.add(TaskAction.FORWARD);
    }

    public static String getDefaultNotices() {
        String noticeOutcomes = "";
        for (String outcome : TASK_ACTION_OUTCOMES) {
            noticeOutcomes += outcome + ",,,,;";
        }
        return noticeOutcomes;
    }
    public static String getDefaultCompatibilityNotices() {
        String noticeOutcomes = "";
        for (String outcome : TASK_ACTION_OUTCOMES) {
            noticeOutcomes += outcome + ",,,;";
        }
        return noticeOutcomes;
    }

    public int compareTo(TaskVO other) {
        return this.getTaskName().compareTo(other.getTaskName());
    }

    public boolean equals(Object other) {
        if (!(other instanceof TaskVO))
          return false;

        return getTaskId().equals(((TaskVO)other).getTaskId());
    }

    public TaskTemplate toTemplate() {
        TaskTemplate template = TaskTemplate.Factory.newInstance();
        template.setLogicalId(getLogicalId());
        template.setName(getTaskName());
        if (getTaskCategory() != null)
          template.setCategory(getTaskCategory());
        if (getComment() != null)
          template.setDescription(getComment());

        if (getAttributes() != null)
        {
          for (AttributeVO attrVO : getAttributes())
          {
            Attribute attr = template.addNewAttribute();
            attr.setName(attrVO.getAttributeName());
            attr.setStringValue(attrVO.getAttributeValue());
          }
        }
        return template;
    }

    public JSONObject getJson() throws JSONException {
        JSONObject json = new JSONObject();
        int days, hours;
        json.put("name", getTaskName());
        if (getTaskCategory() != null) {
            json.put("category", getTaskCategory());
        }
        if (getLogicalId() != null) {
            json.put("logicalId", getLogicalId());
        }
        days = getAlertIntervalSeconds() / 86400;
        json.put("alertIntervalDays", days);
        hours = (getAlertIntervalSeconds() - (days * 86400)) / 3600;
        json.put("alertIntervalHours", hours);
        days = getSlaSeconds() / 86400;
        json.put("slaDays", days);
        hours = (getSlaSeconds() - (days * 86400)) / 3600;
        json.put("slaHours", hours);
        String packageName = this.getPackageName();
        if (getTaskCategory() != null) {
            json.put("packageName", packageName);
        }
        json.put("taskId", this.getTaskId());
        List<AttributeVO> attributes = getAttributes();
        if (attributes != null) {
            for (AttributeVO attributeVO : attributes) {
                String attr = attributeVO.getAttributeName();
                String value = attributeVO.getAttributeValue();
                json.put(attr, value == null ? "" : value);
            }

        }
        return json;
    }

    public Long getId() {
        if (super.getId() == null)
            return getTaskId();
        else
            return super.getId();
    }

    public String getName() {
        if (super.getName() == null)
            return getTaskName();
        else
            return super.getName();
    }

    public String getLabel() {
        if (getVersion() == 0)
            return getTaskName(); // compatibility
        else
            return super.getLabel();
    }

    public String getJsonName() {
        return "TaskTemplate";
    }
}
