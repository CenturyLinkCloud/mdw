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
package com.centurylink.mdw.model.task;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.activity.types.TaskActivity;
import com.centurylink.mdw.common.service.Jsonable;
import com.centurylink.mdw.constant.TaskAttributeConstant;
import com.centurylink.mdw.model.asset.Asset;
import com.centurylink.mdw.model.variable.Variable;
import com.centurylink.mdw.util.JsonUtil;
import com.centurylink.mdw.util.StringHelper;

import io.swagger.annotations.ApiModel;

/**
 * Value object for the Task persistable.
 * Shallow version does not include variables, attributes, userGroups or SLAs.
 */
@ApiModel(value="TaskTemplate", description="MDW task definition")
public class TaskTemplate extends Asset implements Jsonable {

    private static final char FIELD_DELIMITER = ',';
    private static final char ROW_DELIMITER = ';';
    public static final String MDW4_TASK_LOGICAL_ID_PREFIX = "MDW4_";
    public static final String AUTOFORM = "Autoform";

    private String taskName;
    private Long taskId;
    private Integer taskTypeId;
    private String taskCategory;
    private List<Variable> variables;
    private String logicalId;

    public TaskTemplate() {
        setLanguage(TASK);
    }

    public TaskTemplate(com.centurylink.mdw.task.TaskTemplate template) {
        setLanguage(TASK);
        setLogicalId(template.getLogicalId());
        if (template.getVersion() != null && !"0".equals(template.getVersion()))
            setVersion(parseVersion(template.getVersion()));
        if (template.getAssetName() != null && !template.getAssetName().isEmpty())
            setName(template.getAssetName());
        setTaskName(template.getName());
        setTaskTypeId(TaskType.TASK_TYPE_TEMPLATE);
        setTaskCategory(template.getCategory());
        setComment(template.getDescription());
        if (template.getAttributeList() != null)
        {
          for (com.centurylink.mdw.task.Attribute attr : template.getAttributeList())
              setAttribute(attr.getName(), attr.getStringValue());
        }
        String vars = getAttribute(TaskAttributeConstant.VARIABLES);
        if (vars != null)
            setVariablesFromString(vars, null);
        String groups = getAttribute(TaskAttributeConstant.GROUPS);
        if (groups != null)
            setUserGroupsFromString(groups);
    }

    public TaskTemplate(TaskTemplate cloneFrom) {
        super(cloneFrom);
        setLanguage(TASK);
        setLogicalId(cloneFrom.getLogicalId());
        setTaskName(cloneFrom.getTaskName());
        setTaskTypeId(cloneFrom.getTaskTypeId());
        setTaskCategory(cloneFrom.getTaskCategory());
        setComment(cloneFrom.getComment());
        setAttributes(cloneFrom.getAttributes());
        String vars = getAttribute(TaskAttributeConstant.VARIABLES);
        if (vars != null)
            setVariablesFromString(vars, null);
        String groups = getAttribute(TaskAttributeConstant.GROUPS);
        if (groups != null)
            setUserGroupsFromString(groups);
        setVersion(cloneFrom.getVersion());
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

    public List<Variable> getVariables() {
        return variables;
    }
    public void setVariables(List<Variable> pVars) {
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

    public String getFormName() {
        return getAttribute(TaskAttributeConstant.FORM_NAME);
    }

    public void setFormName(String v) {
        setAttribute(TaskAttributeConstant.FORM_NAME, v);
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
        for (Variable vo : variables) {
            if(vo.getVariableName().equals(pVarName)){
                return true;
            }
        }
        return false;
    }

    public String getUserGroupsAsString() {
        return getAttribute(TaskAttributeConstant.GROUPS);
    }

    private static Variable findVariable(List<Variable> list, Variable var) {
        if (list==null) return null;
        for (Variable one : list) {
            if (var.getVariableName().equals(one.getVariableName())) return one;
        }
        return null;
    }

    public void setUserGroupsFromString(String str) {
        setAttribute(TaskAttributeConstant.GROUPS, str);
    }

    public static String getVariablesAsString(List<Variable> processVariables, List<Variable> variables) {
        if (processVariables==null) return null;
        StringBuffer sb = new StringBuffer();
        boolean firstRow = true;
        // first get variables already specified in the task
        if (variables!=null) {
            for (Variable taskVar : variables) {
                Variable var = findVariable(processVariables, taskVar);
                if (var==null) continue;    // remove variables not in process definition
                if (firstRow) firstRow = false;
                else sb.append(ROW_DELIMITER);
                sb.append(var.getVariableName()).append(FIELD_DELIMITER);
                sb.append(taskVar.getVariableReferredAs()).append(FIELD_DELIMITER);
                if (taskVar.getDisplayMode().equals(Variable.DATA_READONLY))
                    sb.append(TaskActivity.VARIABLE_DISPLAY_READONLY);
                else if (Variable.DATA_OPTIONAL.equals(taskVar.getDisplayMode()))
                    sb.append(TaskActivity.VARIABLE_DISPLAY_OPTIONAL);
                else if (Variable.DATA_HIDDEN.equals(taskVar.getDisplayMode()))
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
        for (Variable var : processVariables) {
            Variable taskVar = findVariable(variables, var);
            if (taskVar!=null) continue;    // already handled above
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

    public String getVariablesAsString(List<Variable> processVariables) {
        return getVariablesAsString(processVariables, variables);
    }

    public void setVariablesFromString(String str, List<Variable> processVariables) {
        variables = new ArrayList<Variable>();
        if (str == null) return;
        List<String[]> parsed = StringHelper.parseTable(str, FIELD_DELIMITER, ROW_DELIMITER, 6);
        for (String[] one : parsed) {
            if (one[2].equals(TaskActivity.VARIABLE_DISPLAY_NOTDISPLAYED)) continue;
            Variable taskVar = new Variable();
            taskVar.setVariableName(one[0]);
            Variable var = findVariable(processVariables, taskVar);
            if (var!=null) taskVar.setVariableType(var.getVariableType());
            if (one[3].isEmpty())
                taskVar.setDisplaySequence(new Integer(0));
            else
                taskVar.setDisplaySequence(new Integer(one[3]));
            if (one[2].equals(TaskActivity.VARIABLE_DISPLAY_READONLY)) {
                taskVar.setDisplayMode(Variable.DATA_READONLY);
            } else if (one[2].equals(TaskActivity.VARIABLE_DISPLAY_OPTIONAL)) {
                taskVar.setDisplayMode(Variable.DATA_OPTIONAL);
            } else if (one[2].equals(TaskActivity.VARIABLE_DISPLAY_HIDDEN)) {
                taskVar.setDisplayMode(Variable.DATA_HIDDEN);
            } else {
                taskVar.setDisplayMode(Variable.DATA_REQUIRED);
            }
            if (var!=null) taskVar.setVariableId(var.getVariableId());
            taskVar.setVariableReferredAs(one[1]);
            taskVar.setDescription(one[4]);        // reused as index key
            if (StringHelper.isEmpty(taskVar.getVariableType())) {  // should have been set based on proc var type
                if (StringHelper.isEmpty(one[5])) taskVar.setVariableType(String.class.getName());
                else taskVar.setVariableType(one[5]);
            }
            int i, n = variables.size();
            for (i=0; i<n; i++) {
                Variable next = variables.get(i);
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

    public static String updateVariableInString(String curString, List<Variable> processVariables) {
        List<String[]> parsed = StringHelper.parseTable(curString, FIELD_DELIMITER, ROW_DELIMITER, 6);
        int n = parsed.size();
        boolean[] keep = new boolean[n];
        for (int i=0; i<n; i++) keep[i] = parsed.get(i)[0].startsWith("#{") || parsed.get(i)[0].startsWith("${"); // expressions okay
        for (Variable var : processVariables) {
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

    public int compareTo(TaskTemplate other) {
        return this.getTaskName().compareTo(other.getTaskName());
    }

    public boolean equals(Object other) {
        if (!(other instanceof TaskTemplate))
          return false;

        return getTaskId().equals(((TaskTemplate)other).getTaskId());
    }

    public com.centurylink.mdw.task.TaskTemplate toTemplate() {
        com.centurylink.mdw.task.TaskTemplate template
            = com.centurylink.mdw.task.TaskTemplate.Factory.newInstance();
        template.setLogicalId(getLogicalId());
        template.setName(getTaskName());
        if (getTaskCategory() != null)
            template.setCategory(getTaskCategory());
        if (getComment() != null)
            template.setDescription(getComment());
        if (getVersion() > 0) {
            template.setVersion(formatVersion(getVersion()));
            template.setAssetName(getName());
        }

        if (getAttributes() != null) {
            for (com.centurylink.mdw.model.attribute.Attribute attrVO : getAttributes()) {
                if (!"TaskDescription".equals(attrVO.getAttributeName())) {
                    com.centurylink.mdw.task.Attribute attr = template.addNewAttribute();
                    attr.setName(attrVO.getAttributeName());
                    attr.setStringValue(attrVO.getAttributeValue());
                }
            }
        }
        return template;
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

    public TaskTemplate(JSONObject json) throws JSONException {
        this.logicalId = json.getString("logicalId");
        this.taskName = json.getString("name");
        this.setVersion(Asset.parseVersion(json.getString("version")));
        this.setLanguage(Asset.TASK);
        this.setTaskTypeId(TaskType.TASK_TYPE_TEMPLATE);
        if (json.has("category"))
            this.taskCategory = json.getString("category");
        if (json.has("description"))
            this.setComment(json.getString("description"));
        if (json.has("attributes"))
            this.setAttributes(JsonUtil.getAttributes(json.getJSONObject("attributes")));
        String vars = getAttribute(TaskAttributeConstant.VARIABLES);
        if (vars != null)
            setVariablesFromString(vars, null);
        String groups = getAttribute(TaskAttributeConstant.GROUPS);
        if (groups != null)
            setUserGroupsFromString(groups);
    }

    public JSONObject getJson() throws JSONException {
        JSONObject json = new JSONObject();

        json.put("logicalId", getLogicalId());
        json.put("name",  getTaskName());
        json.put("version", formatVersion(getVersion()));
        if (taskCategory != null)
            json.put("category", taskCategory);
        if (getComment() != null)
            json.put("description", getComment());
        if (getAttributes() != null)
            json.put("attributes", JsonUtil.getAttributesJson(getAttributes()));

        return json;
    }

    /**
     * Task template asset name (not task name).
     */
    public String getJsonName() {
        return getName();
    }
}
