/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.dataaccess.file;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.centurylink.mdw.dataaccess.BaselineData;
import com.centurylink.mdw.model.data.task.TaskCategory;
import com.centurylink.mdw.model.data.task.TaskState;
import com.centurylink.mdw.model.data.task.TaskStatus;
import com.centurylink.mdw.model.value.user.UserGroupVO;
import com.centurylink.mdw.model.value.user.UserRoleVO;
import com.centurylink.mdw.model.value.variable.VariableTypeVO;

public class MdwBaselineData implements BaselineData {

    private List<VariableTypeVO> variableTypes;
    public List<VariableTypeVO> getVariableTypes() {
        if (variableTypes == null) {
            variableTypes = new ArrayList<VariableTypeVO>(16);
            variableTypes.add(new VariableTypeVO(101L, "java.lang.String","com.centurylink.mdw.common.translator.impl.StringTranslator"));
            variableTypes.add(new VariableTypeVO(102L, "java.lang.Long","com.centurylink.mdw.common.translator.impl.LongTranslator"));
            variableTypes.add(new VariableTypeVO(103L, "java.lang.Integer","com.centurylink.mdw.common.translator.impl.IntegerTranslator"));
            variableTypes.add(new VariableTypeVO(104L, "java.lang.Boolean","com.centurylink.mdw.common.translator.impl.BooleanTranslator"));
            variableTypes.add(new VariableTypeVO(105L, "java.util.Date","com.centurylink.mdw.common.translator.impl.DateTranslator"));
            variableTypes.add(new VariableTypeVO(106L, "java.lang.String[]","com.centurylink.mdw.common.translator.impl.StringArrayTranslator"));
            variableTypes.add(new VariableTypeVO(107L, "java.lang.Integer[]","com.centurylink.mdw.common.translator.impl.IntegerArrayTranslator"));
            variableTypes.add(new VariableTypeVO(108L, "java.lang.Long[]","com.centurylink.mdw.common.translator.impl.LongArrayTranslator"));
            variableTypes.add(new VariableTypeVO(109L, "java.util.Map","com.centurylink.mdw.common.translator.impl.StringMapTranslator"));
            variableTypes.add(new VariableTypeVO(110L, "java.net.URI", "com.centurylink.mdw.common.translator.impl.URITranslator"));
            variableTypes.add(new VariableTypeVO(111L, "java.util.List<String>", "com.centurylink.mdw.common.translator.impl.StringListTranslator"));
            variableTypes.add(new VariableTypeVO(112L, "java.util.List<Integer>", "com.centurylink.mdw.common.translator.impl.IntegerListTranslator"));
            variableTypes.add(new VariableTypeVO(113L, "java.util.List<Long>", "com.centurylink.mdw.common.translator.impl.LongListTranslator"));
            variableTypes.add(new VariableTypeVO(114L, "java.util.Map<String,String>", "com.centurylink.mdw.common.translator.impl.StringStringMapTranslator"));

            // document variables
            variableTypes.add(new VariableTypeVO(201L, "org.w3c.dom.Document", "com.centurylink.mdw.common.translator.impl.DomDocumentTranslator"));
            variableTypes.add(new VariableTypeVO(202L, "org.apache.xmlbeans.XmlObject", "com.centurylink.mdw.common.translator.impl.XmlBeanTranslator"));
            variableTypes.add(new VariableTypeVO(203L, "java.lang.Object", "com.centurylink.mdw.common.translator.impl.JavaObjectTranslator"));
            variableTypes.add(new VariableTypeVO(204L, "org.json.JSONObject", "com.centurylink.mdw.common.translator.impl.JsonObjectTranslator"));
            variableTypes.add(new VariableTypeVO(205L, "groovy.util.Node", "com.centurylink.mdw.common.translator.impl.GroovyNodeTranslator"));
            variableTypes.add(new VariableTypeVO(206L, "com.centurylink.mdw.xml.XmlBeanWrapper", "com.centurylink.mdw.common.translator.impl.XmlBeanWrapperTranslator"));
            variableTypes.add(new VariableTypeVO(207L, "com.centurylink.mdw.model.StringDocument", "com.centurylink.mdw.common.translator.impl.StringDocumentTranslator"));
            variableTypes.add(new VariableTypeVO(208L, "com.centurylink.mdw.model.FormDataDocument", "com.centurylink.mdw.common.translator.impl.FormDataDocumentTranslator"));
            variableTypes.add(new VariableTypeVO(209L, "com.centurylink.mdw.model.HTMLDocument", "com.centurylink.mdw.common.translator.impl.HtmlDocumentTranslator"));
            variableTypes.add(new VariableTypeVO(210L, "javax.xml.bind.JAXBElement", "com.centurylink.mdw.jaxb.JaxbElementTranslator"));
            // requires the mdw-camel bundle installed
            variableTypes.add(new VariableTypeVO(310L, "org.apache.camel.component.cxf.CxfPayload", "com.centurylink.mdw.camel.cxf.CxfPayloadTranslator"));
        }

        return variableTypes;
    }

    public String getVariableType(Object value) {
        for (VariableTypeVO varType : getVariableTypes()) {
            try {
                if (!varType.isJavaObjectType() && (Class.forName(varType.getVariableType()).isInstance(value)))
                    return varType.getVariableType();
            }
            catch (Exception ex) {
                return Object.class.getName();
            }
        }
        return null;
    }

    private List<String> userRoles;
    public List<String> getUserRoles() {
        if (userRoles == null) {
            userRoles = new ArrayList<String>();
            userRoles.add(UserRoleVO.PROCESS_DESIGN);
            userRoles.add(UserRoleVO.PROCESS_EXECUTION);
            userRoles.add(UserRoleVO.USER_ADMIN);
            userRoles.add(UserRoleVO.SUPERVISOR);
            userRoles.add(UserRoleVO.TASK_EXECUTION);
        }
        return userRoles;
    }

    private List<String> workgroups;
    public List<String> getWorkgroups() {
        if (workgroups == null) {
            workgroups = new ArrayList<String>();
            workgroups.add(UserGroupVO.COMMON_GROUP);
            workgroups.add(UserGroupVO.SITE_ADMIN_GROUP);
            workgroups.add("MDW Support");
        }
        return workgroups;
    }

    // TODO get rid of codes
    private Map<Integer,String> taskCategoryCodes;
    public Map<Integer,String> getTaskCategoryCodes() {
        if (taskCategoryCodes == null) {
            taskCategoryCodes = new HashMap<Integer,String>();
            taskCategoryCodes.put(1, "ORD");
            taskCategoryCodes.put(2, "GEN");
            taskCategoryCodes.put(3, "BIL");
            taskCategoryCodes.put(4, "COM");
            taskCategoryCodes.put(5, "POR");
            taskCategoryCodes.put(6, "TRN");
            taskCategoryCodes.put(7, "RPR");
            taskCategoryCodes.put(8, "INV");
            taskCategoryCodes.put(9, "TST");
            taskCategoryCodes.put(10, "VAC");
            taskCategoryCodes.put(11, "CNT");
        }
        return taskCategoryCodes;
    }

    private Map<Integer,TaskCategory> taskCategories;
    public Map<Integer,TaskCategory> getTaskCategories() {
        if (taskCategories == null) {
            taskCategories = new LinkedHashMap<Integer,TaskCategory>();
            taskCategories.put(1, new TaskCategory(1L, "ORD", "Ordering"));
            taskCategories.put(2, new TaskCategory(2L, "GEN", "General Inquiry"));
            taskCategories.put(3, new TaskCategory(3L, "BIL", "Billing"));
            taskCategories.put(4, new TaskCategory(4L, "COM", "Complaint"));
            taskCategories.put(5, new TaskCategory(5L, "POR", "Portal Support"));
            taskCategories.put(6, new TaskCategory(6L, "TRN", "Training"));
            taskCategories.put(7, new TaskCategory(7L, "RPR", "Repair"));
            taskCategories.put(8, new TaskCategory(8L, "INV", "Inventory"));
            taskCategories.put(9, new TaskCategory(9L, "TST", "Test"));
            taskCategories.put(10, new TaskCategory(10L, "VAC", "Vacation Planning"));
            taskCategories.put(11, new TaskCategory(11L, "CNT", "Customer Contact"));
        }
        return taskCategories;
    }

    private Map<Integer,TaskState> taskStates;
    public Map<Integer,TaskState> getTaskStates() {
        if (taskStates == null) {
            taskStates = new LinkedHashMap<Integer,TaskState>();
            taskStates.put(1, new TaskState(1L, "Open"));
            taskStates.put(2, new TaskState(2L, "Alert"));
            taskStates.put(3, new TaskState(3L, "Jeopardy"));
            taskStates.put(4, new TaskState(4L, "Closed"));
            taskStates.put(5, new TaskState(5L, "Invalid"));
        }
        return taskStates;
    }

    private List<TaskState> allTaskStates;
    public List<TaskState> getAllTaskStates() {
        if (allTaskStates == null) {
            allTaskStates = new ArrayList<TaskState>();
            allTaskStates.addAll(getTaskStates().values());
        }
        return allTaskStates;
    }

    private Map<Integer,TaskStatus> taskStatuses;
    public Map<Integer,TaskStatus> getTaskStatuses() {
        if (taskStatuses == null) {
            taskStatuses = new LinkedHashMap<Integer,TaskStatus>();
            taskStatuses.put(1, new TaskStatus(1L, "Open"));
            taskStatuses.put(2, new TaskStatus(2L, "Assigned"));
            taskStatuses.put(4, new TaskStatus(4L, "Completed"));
            taskStatuses.put(5, new TaskStatus(5L, "Cancelled"));
            taskStatuses.put(6, new TaskStatus(6L, "In Progress"));
        }
        return taskStatuses;
    }

    private List<TaskStatus> allTaskStatuses;
    public List<TaskStatus> getAllTaskStatuses() {
        if (allTaskStatuses == null) {
            allTaskStatuses = new ArrayList<TaskStatus>();
            allTaskStatuses.addAll(getTaskStatuses().values());
        }
        return allTaskStatuses;
    }

}
