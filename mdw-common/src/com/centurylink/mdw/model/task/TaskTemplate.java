package com.centurylink.mdw.model.task;

import com.centurylink.mdw.activity.types.TaskActivity;
import com.centurylink.mdw.constant.TaskAttributeConstant;
import com.centurylink.mdw.model.Attributes;
import com.centurylink.mdw.model.Jsonable;
import com.centurylink.mdw.model.asset.Asset;
import com.centurylink.mdw.model.asset.AssetVersion;
import com.centurylink.mdw.model.variable.Variable;
import org.apache.commons.lang.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Value object for the Task persistable.
 * Shallow version does not include variables, attributes, userGroups or SLAs.
 */
public class TaskTemplate extends Asset implements Jsonable {

    private static final char FIELD_DELIMITER = ',';
    private static final char ROW_DELIMITER = ';';
    public static final String AUTOFORM = "Autoform";

    public Long getTaskId() {
        return getId();
    }

    private String taskName;
    public String getTaskName() {
        return taskName;
    }
    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    private String taskCategory;
    public String getTaskCategory() {
        return taskCategory;
    }
    public void setTaskCategory(String category) {
        this.taskCategory = category;
    }

    private List<Variable> variables;
    public List<Variable> getVariables() {
        return variables;
    }
    public void setVariables(List<Variable> variables) {
        this.variables = variables;
    }

    private String logicalId;
    public String getLogicalId() {
        return logicalId;
    }
    public void setLogicalId(String logicalId) {
        this.logicalId = logicalId;
    }

    private Attributes attributes;
    public Attributes getAttributes() { return attributes; }
    public String getAttribute(String name) {
        return attributes == null ? null : attributes.get(name);
    }
    public void setAttribute(String name, String value) {
        if (attributes == null)
            attributes = new Attributes();
        attributes.put(name, value);
    }

    public TaskTemplate() {
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
        return attributes == null ? new ArrayList<>() : attributes.getList(groupAttributeName);
    }

    public void setWorkgroups(List<String> workgroups) {
        setUserGroups(workgroups);
    }
    public void setUserGroups(List<String> userGroups) {
        if (userGroups==null || userGroups.size()==0) {
            this.setAttribute(TaskAttributeConstant.GROUPS, null);
        } else {
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (String grp : userGroups) {
                if (first) first = false;
                else sb.append('#');
                sb.append(grp);
            }
            this.setAttribute(TaskAttributeConstant.GROUPS, sb.toString());
        }
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
        return StringUtils.isBlank(alertIntervalString) ? 0 : Integer.parseInt(alertIntervalString);
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
     * @param pGroupName group name
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
     * @param pVarName variable name
     * @return boolean results
     */
    public boolean isVariableMapped(String pVarName){
        if(variables == null){
            return false;
        }
        for (Variable vo : variables) {
            if(vo.getName().equals(pVarName)){
                return true;
            }
        }
        return false;
    }

    public String getUserGroupsAsString() {
        return getAttribute(TaskAttributeConstant.GROUPS);
    }

    private static Variable findVariable(List<Variable> list, Variable var) {
        if (list == null)
            return null;
        for (Variable one : list) {
            if (var.getName().equals(one.getName())) return one;
        }
        return null;
    }

    public void setUserGroupsFromString(String str) {
        setAttribute(TaskAttributeConstant.GROUPS, str);
    }

    public void setVariablesFromAttribute(String name, List<Variable> processVariables) {
        variables = new ArrayList<>();
        if (attributes != null && attributes.containsKey(name)) {
            List<String[]> parsed = attributes.getTable(name, FIELD_DELIMITER, ROW_DELIMITER, 6);
            for (String[] one : parsed) {
                if (one[2].equals(TaskActivity.VARIABLE_DISPLAY_NOTDISPLAYED)) continue;
                Variable taskVar = new Variable();
                taskVar.setName(one[0]);
                Variable var = findVariable(processVariables, taskVar);
                if (var!=null) taskVar.setType(var.getType());
                if (one[3].isEmpty())
                    taskVar.setDisplaySequence(0);
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
                if (var!=null) taskVar.setId(var.getId());
                taskVar.setLabel(one[1]);
                taskVar.setDescription(one[4]);        // reused as index key
                if (StringUtils.isBlank(taskVar.getType())) {  // should have been set based on proc var type
                    if (StringUtils.isBlank(one[5])) taskVar.setType(String.class.getName());
                    else taskVar.setType(one[5]);
                }
                int i, n = variables.size();
                for (i=0; i<n; i++) {
                    Variable next = variables.get(i);
                    if (taskVar.getDisplaySequence() < next.getDisplaySequence())
                        break;
                }
                if (i<n) variables.add(i, taskVar);
                else variables.add(taskVar);
            }
        }
    }

    @Override
    public int compareTo(Asset other) {
        return this.getTaskName().compareTo(((TaskTemplate)other).getTaskName());
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof TaskTemplate))
          return false;

        return getId().equals(((TaskTemplate)other).getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
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
        this.setVersion(AssetVersion.parseVersion(json.getString("version")));
        if (json.has("category"))
            this.taskCategory = json.getString("category");
        if (json.has("description"))
            this.setComment(json.getString("description"));
        if (json.has("attributes")) {
            this.attributes = new Attributes(json.getJSONObject("attributes"));
            setVariablesFromAttribute(TaskAttributeConstant.VARIABLES, null);
        }
        String groups = getAttribute(TaskAttributeConstant.GROUPS);
        if (groups != null)
            setUserGroupsFromString(groups);
    }

    public JSONObject getJson() throws JSONException {
        JSONObject json = create();

        json.put("logicalId", getLogicalId());
        json.put("name",  getTaskName());
        json.put("version", AssetVersion.formatVersion(getVersion()));
        if (taskCategory != null)
            json.put("category", taskCategory);
        if (getComment() != null)
            json.put("description", getComment());
        if (attributes != null)
            json.put("attributes", attributes.getJson());

        return json;
    }

    /**
     * Task template asset name (not task name).
     */
    public String getJsonName() {
        return getName();
    }
}
