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
package com.centurylink.mdw.model.workflow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.constant.ActivityResultCodeConstant;
import com.centurylink.mdw.constant.OwnerType;
import com.centurylink.mdw.constant.ProcessVisibilityConstant;
import com.centurylink.mdw.constant.WorkAttributeConstant;
import com.centurylink.mdw.model.Changes;
import com.centurylink.mdw.model.Jsonable;
import com.centurylink.mdw.model.Value;
import com.centurylink.mdw.model.asset.Asset;
import com.centurylink.mdw.model.asset.AssetRequest;
import com.centurylink.mdw.model.asset.AssetRequest.HttpMethod;
import com.centurylink.mdw.model.attribute.Attribute;
import com.centurylink.mdw.model.event.EventType;
import com.centurylink.mdw.model.event.ExternalEvent;
import com.centurylink.mdw.model.variable.Variable;
import com.centurylink.mdw.model.variable.VariableType;
import com.centurylink.mdw.util.JsonUtil;
import com.centurylink.mdw.util.StringHelper;

/**
 * Value object representing a process definition.
 */
public class Process extends Asset implements Jsonable {

    public static final String TRANSITION_ON_NULL = "Matches Null Return Code";
    public static final String TRANSITION_ON_DEFAULT = "Acts as Default";

    public static final Integer PROCESS_TYPE_CONCRETE = new Integer(1);
    public static final Integer PROCESS_TYPE_ALIAS = new Integer(2);        // for MDW 5, this means process def is stored in RULE_SET

    public static final String OLD_START_ACTIVITY_BASE_CLASS = "com.qwest.mdw.workflow.activity.types.StartActivity";

    private List<ExternalEvent> externalEvents;
    private List<Variable> variables;
    private List<Transition> transitions;
    private List<Process> subprocesses;
    private List<Activity> activities;
    private List<TextNote> textNotes;
    private List<Attribute> attributes;
    private List<ActivityImplementor> implementors;

    public Process() {
        setLanguage(Asset.PROCESS);
    }

    public Process(Long id) {
        this();
        setId(id);
    }

    public Process(Long pProcessId, String pPrName, String pDesc, List<ExternalEvent> externalEvents) {
        setLanguage(Asset.PROCESS);
        this.setId(pProcessId);
        this.setName(pPrName);
        this.setComment(pDesc);
        this.externalEvents = externalEvents;
    }

    public Process(Process cloneFrom) {
        super(cloneFrom);
        setExternalEvents(cloneFrom.getExternalEvents());
        setVariables(cloneFrom.getVariables());
        setTransitions(cloneFrom.getTransitions());
        setSubprocesses(cloneFrom.getSubprocesses());
        setActivities(cloneFrom.getActivities());
        setAttributes(cloneFrom.getAttributes());
        setImplementors(cloneFrom.getImplementors());
        setTextNotes(cloneFrom.getTextNotes());
        overrideAttributesApplied = cloneFrom.overrideAttributesApplied();
    }

    public void set(List<Attribute> attributes,  List<Variable> variables, List<Transition> transitions,
                List<Process> subprocesses, List<Activity> activities){
        this.activities = activities;
        this.attributes = attributes;
        this.transitions = transitions;
        this.variables = variables;
        this.subprocesses = subprocesses;
    }

    public boolean isService() {
        return ProcessVisibilityConstant.SERVICE.equals(getProcessType());
    }

    /**
     * checks if the passed in workId is
     * Activity
     * @return boolean status
     */
    public boolean isWorkActivity(Long pWorkId) {

        if(this.activities == null){
            return false;
        }
        for(int i=0; i<activities.size(); i++){
            if(pWorkId.longValue() == activities.get(i).getId().longValue()){
                return true;
            }
        }
        return false;
    }

    /**
     * returns the process VO identified by the passed in work id
     * It is also possible the sub process is referencing self.
     * @param id
     */
    public Process getSubProcessVO(Long id) {
        if (this.getId().equals(id))
            return this;
        if (this.subprocesses == null)
            return null;
        for (Process ret : subprocesses) {
            if (ret.getId().equals(id))
                return ret;
        }
        return null;
    }

    /**
     * Returns the Activity VO
     * @param pWorkId
     * @returb ActivityVO
     */
     public Activity getActivityVO(Long pWorkId) {
         if(this.activities == null){
            return null;
        }
        for(int i=0; i<activities.size(); i++){
            if(pWorkId.longValue() == activities.get(i).getId().longValue()){
                return activities.get(i);
            }
        }
        return null;

    }

     /**
     * Returns the WorkTransitionVO
     * @param pWorkTransId
     * @returb WorkTransitionVO
     */
     public Transition getWorkTransitionVO(Long pWorkTransId) {
         if(this.transitions == null){
            return null;
        }
        for(int i=0; i<transitions.size(); i++){
            if(pWorkTransId.longValue() == transitions.get(i).getId().longValue()){
                return transitions.get(i);
            }
        }
        return null;

    }

    /**
     * @return the activities
     */
    public List<Activity> getActivities() {
        return activities;
    }

    /**
     * @param activities the activities to set
     */
    public void setActivities(List<Activity> activities) {
        this.activities = activities;
    }

    public boolean hasDynamicJavaActivity() {
        if (activities != null) {
            for (Activity activity : activities) {
                if (activity.getImplementor() != null && activity.getImplementor().endsWith("DynamicJavaActivity"))
                    return true;
            }
        }
        return false;
    }

    public List<TextNote> getTextNotes() {
        return textNotes;
    }

    public void setTextNotes(List<TextNote> v) {
        this.textNotes = v;
    }

    /**
     * @return the attributes
     */
    public List<Attribute> getAttributes() {
        return attributes;
    }

    /**
     * @param attributes the attributes to set
     */
    public void setAttributes(List<Attribute> attributes) {
        this.attributes = attributes;
    }

    public String getDescription() {
        return this.getComment();
    }
    public void setDescription(String description) {
        this.setComment(description);
    }

    public List<Process> getSubprocesses() {
        return this.subprocesses;
    }
    public void setSubprocesses(List<Process> subprocesses) {
        this.subprocesses = subprocesses;
    }

    public List<Transition> getTransitions() {
        return transitions;
    }
    public void setTransitions(List<Transition> transitions) {
        this.transitions = transitions;
    }

    public List<Variable> getVariables() {
        return variables;
    }
    public Variable getVariable(String varName) {
        for (Variable var : variables) {
            if (var.getName().equals(varName))
                return var;
        }
        return null; // not found
    }
    public Variable getVariable(Long id) {
        for (Variable var : variables) {
            if (var.getId().equals(id))
                return var;
        }
        return null; // not found
    }
    public void setVariables(List<Variable> variables) {
        this.variables = variables;
    }
    public void addLocalVariable(VariableType variableType, String variableName) {
        if (getVariable(variableName) == null) {
            Variable varVO = new Variable();
            varVO.setName(variableName);
            varVO.setType(variableType.getVariableType());
            varVO.setVariableCategory(Variable.CAT_LOCAL);
            variables.add(varVO);
        }
    }

    public List<ExternalEvent> getExternalEvents() {
        return this.externalEvents;
    }
    public void setExternalEvents(List<ExternalEvent> externalEvents) {
        this.externalEvents = externalEvents;
    }

    public List<ActivityImplementor> getImplementors(){
        return this.implementors;
    }
    public void setImplementors(List<ActivityImplementor> imps){
        this.implementors = imps;
    }
    private ActivityImplementor getImplementor(Activity activity) {
        if (implementors != null) {
            for (ActivityImplementor impl : implementors) {
                if (impl.getImplementorClassName().equals(activity.getImplementor()))
                    return impl;
            }
        }
        return null;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Process))
            return false;
        Process p = (Process)obj;
        return getId().longValue() == p.getId().longValue();
    }

    /**
     * Finds one work transition for this process matching the specified parameters
     * @param fromId
     * @param eventType
     * @param completionCode
     * @return the work transition value object (or null if not found)
     */
    public Transition getTransition(Long fromId, Integer eventType, String completionCode) {
        Transition ret = null;
        for (Transition transition : getTransitions()) {
            if (transition.getFromId().equals(fromId)
                    && transition.match(eventType, completionCode)) {
                if (ret == null) ret = transition;
                else {
                    throw new IllegalStateException("Multiple matching work transitions when one expected:\n"
                            + " processId: " + getId() + " fromId: " + fromId + " eventType: "
                            + eventType + "compCode: " + completionCode);
                }
            }
        }
        return ret;
    }

    public Integer getEventType() {
        String subtype = getAttribute(WorkAttributeConstant.EMBEDDED_PROCESS_TYPE);
        if (subtype==null) return EventType.ERROR;
        else if (subtype.equals(ProcessVisibilityConstant.EMBEDDED_ERROR_PROCESS))
            return EventType.ERROR;
        else if (subtype.equals(ProcessVisibilityConstant.EMBEDDED_ABORT_PROCESS))
            return EventType.ABORT;
        else if (subtype.equals(ProcessVisibilityConstant.EMBEDDED_CORRECT_PROCESS))
            return EventType.CORRECT;
        else if (subtype.equals(ProcessVisibilityConstant.EMBEDDED_DELAY_PROCESS))
            return EventType.DELAY;
        else return EventType.ERROR;
    }

    public Process findSubprocess(Integer eventType, String completionCode) {
        if (this.subprocesses == null)
            return null;
        for (Process subproc : subprocesses) {
            if (eventType.equals(subproc.getEventType())) {
                String entrycode = subproc.getAttribute(WorkAttributeConstant.ENTRY_CODE);
                if (StringHelper.isEmpty(entrycode)) {
                    if (StringHelper.isEmpty(completionCode))
                        return subproc;
                } else {
                    if (entrycode.equals(completionCode))
                        return subproc;
                }
            }
        }
        for (Process subproc : subprocesses) {
            if (eventType.equals(subproc.getEventType())) {
                String entrycode = subproc.getAttribute(WorkAttributeConstant.ENTRY_CODE);
                if (StringHelper.isEmpty(entrycode))
                    return subproc;
            }
        }
        return null;
    }

    /**
     * Finds the work transitions from the given activity
     * that match the event type and completion code.
     * A DEFAULT completion code matches any completion
     * code if and only if there is no other matches
     *
     * @param fromWorkId
     * @param eventType
     * @parame completionCode
     * @return the matching work transition value objects
     */
    public List<Transition> getTransitions(Long fromWorkId, Integer eventType, String completionCode) {
        List<Transition> allTransitions = getAllTransitions(fromWorkId);
        List<Transition> returnSet = findTransitions(allTransitions, eventType, completionCode);
        if (returnSet.size() > 0)
            return returnSet;
        // look for default transition
        boolean noLabelIsDefault = getTransitionWithNoLabel().equals(TRANSITION_ON_DEFAULT);
        if (noLabelIsDefault) returnSet = findTransitions(allTransitions, eventType, null);
        else returnSet = findTransitions(allTransitions, eventType, ActivityResultCodeConstant.RESULT_DEFAULT);
        if (returnSet.size() > 0)
            return returnSet;
        // look for resume transition
        if (eventType.equals(EventType.FINISH)) {
            returnSet = new ArrayList<Transition>();
            for (Transition trans : allTransitions) {
                if (trans.getEventType().equals(EventType.RESUME))
                    returnSet.add(trans);
            }
        }
        return returnSet;
    }

    public List<Transition> getAllTransitions(Long fromWorkId) {
        List<Transition> allTransitions = new ArrayList<Transition>();
        for (Transition workTransitionVO : getTransitions()) {
            if (workTransitionVO.getFromId().equals(fromWorkId)) {
                allTransitions.add(workTransitionVO);
            }
        }
        return allTransitions;
    }

    private List<Transition> findTransitions(List<Transition> all,
            Integer eventType, String compcode) {
        List<Transition> set = new ArrayList<Transition>();
        for (Transition trans : all) {
            if (trans.match(eventType, compcode)) set.add(trans);
        }
        return set;
    }

    /**
     * Find a work transition based on its id value
     * @param id
     * @return the matching transition VO, or null if not found
     */
    public Transition getTransition(Long id) {
        for (Transition transition : getTransitions()) {
            if (transition.getId().equals(id))
                return transition;
        }
        return null; // not found
    }

    /**
     * Returns the value of a process attribute.
     * @param attrname
     * @return the value of the attribute, or null if the attribute does not exist
     */
    public String getAttribute(String attrname) {
        return Attribute.findAttribute(attributes, attrname);
    }

    /**
     * Set the value of a process attribute.
     * If the value is null, the attribute is removed.
     * If the attribute does not exist and the value is not null, the attribute
     * is created.
     * @param attrname
     * @param value
     */
    public void setAttribute(String attrname, String value) {
        if (attributes == null)
            attributes = new ArrayList<Attribute>();
        Attribute.setAttribute(attributes, attrname, value);
    }

    public boolean isEmbeddedProcess() {
        String procVisibility = getAttribute(WorkAttributeConstant.PROCESS_VISIBILITY);
        return (procVisibility != null && procVisibility.equals(ProcessVisibilityConstant.EMBEDDED));
    }

    public boolean isEmbeddedExceptionHandler() {
        if (!isEmbeddedProcess()) return false;
        String subtype = getAttribute(WorkAttributeConstant.EMBEDDED_PROCESS_TYPE);
        return (subtype==null || subtype.equals(ProcessVisibilityConstant.EMBEDDED_ERROR_PROCESS));
    }

    public Process findEmbeddedProcess(String type) {
        if (subprocesses == null)
            return null;
        for (Process subproc : subprocesses) {
            if (ProcessVisibilityConstant.EMBEDDED.equals(
                    subproc.getAttribute(WorkAttributeConstant.PROCESS_VISIBILITY))) {
                String subtype = subproc.getAttribute(WorkAttributeConstant.EMBEDDED_PROCESS_TYPE);
                if (subtype == null)
                    subtype = ProcessVisibilityConstant.EMBEDDED_ERROR_PROCESS;
                if (type.equals(subtype))
                    return subproc;
            }
        }
        return null;
    }

    public List<Activity> getUpstreamActivities(Long activityId) {
        List<Activity> upstreamActivities = new ArrayList<Activity>();
        for (Transition workTransitionVO : getTransitions()) {
            if (workTransitionVO.getToId().equals(activityId))
                upstreamActivities.add(getActivityVO(workTransitionVO.getFromId()));
        }
        if (getSubprocesses() != null) {
            for (Process embeddedSubProc : getSubprocesses()) {
                for (Transition workTransitionVO : embeddedSubProc.getTransitions()) {
                    if (workTransitionVO.getToId().equals(activityId))
                        upstreamActivities.add(embeddedSubProc.getActivityVO(workTransitionVO.getFromId()));
                }
            }
        }
        return upstreamActivities;
    }

    public Activity getActivity(String activityName) {
        for (Activity activityVO : getActivities()) {
            if (activityVO.getName().equals(activityName))
                return activityVO;
        }
        return null;
    }

    public Activity getActivityByLogicalId(String logicalId) {
        for (Activity activityVO : getActivities()) {
            if (activityVO.getLogicalId().equals(logicalId))
                return activityVO;
        }
        return null;
    }

    /**
     * Also searches subprocs.
     */
    public Activity getActivityById(String logicalId) {
        for (Activity activityVO : getActivities()) {
            if (activityVO.getLogicalId().equals(logicalId)) {
                activityVO.setProcessName(getName());
                return activityVO;
            }
        }
        for (Process subProc : this.subprocesses) {
            for (Activity activityVO : subProc.getActivities()) {
                if (activityVO.getLogicalId().equals(logicalId)) {
                    activityVO.setProcessName(getName() + ":" + subProc.getName());
                    return activityVO;
                }
            }
        }

        return null;
    }

    public String getTransitionWithNoLabel() {
        String v = this.getAttribute(WorkAttributeConstant.TRANSITION_WITH_NO_LABEL);
        return (v == null) ? TRANSITION_ON_NULL : v;
    }

    public String getProcessType() {
        String v = this.getAttribute(WorkAttributeConstant.PROCESS_VISIBILITY);
        return (v == null) ? ProcessVisibilityConstant.REGULAR : v;
    }

    public String getLabel() {
        return getName() + (getVersion() == 0 ? "" : " v" + getVersionString());
    }

    public String getFullLabel() {
        return getQualifiedName() + (getVersion() == 0 ? "" : " v" + getVersionString());
    }

    public static int versionFromString(String v) {
        int k = v.indexOf('.');
        int version;
        if (k>0) {
            version = Integer.parseInt(v.substring(0,k))*1000
                + Integer.parseInt(v.substring(k+1));
        } else version = Integer.parseInt(v);
        return version;
    }

    public static String versionToString(int version) {
        return version/1000 + "." + version%1000;
    }

    public void removeDeletedTransitions() {
        List<Transition> toDelete = new ArrayList<Transition>();
        for (Transition trans : this.transitions) {
            Changes changes = new Changes(trans.getAttributes());
            if (changes.getChangeType()==Changes.DELETE) {
                toDelete.add(trans);
            }
        }
        for (Transition trans : toDelete) {
            this.transitions.remove(trans);
        }
        if (this.getSubprocesses() != null) {
            for (Process subproc : this.getSubprocesses()) {
                toDelete.clear();
                for (Transition trans : subproc.transitions) {
                    Changes changes = new Changes(trans.getAttributes());
                    if (changes.getChangeType()==Changes.DELETE) {
                        toDelete.add(trans);
                    }
                }
                for (Transition trans : toDelete) {
                    subproc.transitions.remove(trans);
                }
            }
        }
    }

    public boolean isLoaded() {
        return activities!=null;
    }

    public int getPerformanceLevel() {
        String v = this.getAttribute(WorkAttributeConstant.PERFORMANCE_LEVEL);
        try {
            return (v==null)?0:Integer.parseInt(v);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public Activity getStartActivity() {
        if (implementors != null) {
            for (Activity activity : getActivities()) {
                ActivityImplementor impl = getImplementor(activity);
                if (impl != null && impl.isStart())
                    return activity;
            }
        }
        // revert to logic of assuming first activity in asset is start
        return getActivities().get(0);
    }

    public List<Activity> getDownstreamActivities(Activity activity) {
        List<Activity> downstreamActivities = new ArrayList<Activity>();
        for (Transition transition : getAllTransitions(activity.getId()))
            downstreamActivities.add(getActivityVO(transition.getToId()));
        return downstreamActivities;
    }

    private int sequenceId;
    public int getSequenceId() { return sequenceId; }
    public void setSequenceId(int sequenceId) { this.sequenceId = sequenceId; }

    public void applyOverrideAttributes(Map<String,String> overrideAttrs) {
        if (overrideAttrs != null) {
            for (String name : overrideAttrs.keySet()) {
                if (name.startsWith(WorkAttributeConstant.OVERRIDE_QUALIFIER)) {
                    int colon = name.indexOf(':');
                    String attrname = name.substring(colon + 1);
                    Long id = new Long(name.substring(WorkAttributeConstant.OVERRIDE_QUALIFIER.length(), colon));
                    if (name.startsWith(WorkAttributeConstant.OVERRIDE_TRANSITION)) {
                        Transition wt = getTransition(id);
                        if (wt == null && getSubprocesses() != null) {
                            for (Process subproc : getSubprocesses()) {
                                wt = subproc.getTransition(id);
                                if (wt != null)
                                    break;
                            }
                        }
                        if (wt != null)
                            wt.setAttribute(attrname, overrideAttrs.get(name));
                    }
                    else if (name.startsWith(WorkAttributeConstant.OVERRIDE_SUBPROC)) {
                        Process subproc = getSubProcessVO(id);
                        if (subproc != null)
                            subproc.setAttribute(attrname, overrideAttrs.get(name));
                    }
                    else {
                        Activity act = getActivityVO(id);
                        if (act == null && getSubprocesses() != null) {
                            for (Process subproc : getSubprocesses()) {
                                act = subproc.getActivityVO(id);
                                if (act != null)
                                    break;
                            }
                        }
                        if (act != null)
                            act.setAttribute(attrname, overrideAttrs.get(name));
                    }
                }
                else {
                    setAttribute(name, overrideAttrs.get(name));
                }
            }
        }
        overrideAttributesApplied = true;
    }

    public Map<String,String> getOverrideAttributes() {
        Map<String,String> overrideAttrs = new HashMap<String,String>();
        if (getAttributes() != null) {
            for (Attribute attr : getAttributes()) {
                if (WorkAttributeConstant.isOverrideAttribute(attr.getAttributeName()))
                    overrideAttrs.put(attr.getAttributeName(), attr.getAttributeValue());
            }
        }
        if (getActivities() != null) {
            for (Activity activity : getActivities())
                overrideAttrs.putAll(getSubOverrideAttributes(activity.getAttributes(), OwnerType.ACTIVITY, activity.getId()));
        }
        if (getTransitions() != null) {
            for (Transition transition : getTransitions())
                overrideAttrs.putAll(getSubOverrideAttributes(transition.getAttributes(), OwnerType.WORK_TRANSITION, transition.getId()));
        }
        if (getSubprocesses() != null) {
            for (Process subproc : getSubprocesses()) {
                overrideAttrs.putAll(getSubOverrideAttributes(subproc.getAttributes(), OwnerType.PROCESS, subproc.getId()));
                if (subproc.getActivities() != null) {
                    for (Activity activity : subproc.getActivities())
                        overrideAttrs.putAll(getSubOverrideAttributes(activity.getAttributes(), OwnerType.ACTIVITY, activity.getId()));
                }
                if (subproc.getTransitions() != null) {
                    for (Transition trans : subproc.getTransitions())
                        overrideAttrs.putAll(getSubOverrideAttributes(trans.getAttributes(), OwnerType.WORK_TRANSITION, trans.getId()));
                }
            }
        }

        return overrideAttrs;
    }

    private Map<String,String> getSubOverrideAttributes(List<Attribute> attrs, String subType, Long subId) {
        Map<String,String> overrideAttrs = new HashMap<String,String>();
        if (attrs != null) {
            for (Attribute attr : attrs) {
                if (WorkAttributeConstant.isOverrideAttribute(attr.getAttributeName())) {
                    String name = WorkAttributeConstant.getOverrideAttributeName(attr.getAttributeName(), subType, String.valueOf(subId));
                    overrideAttrs.put(name, attr.getAttributeValue());
                }
            }
        }
        return overrideAttrs;
    }

    public void removeEmptyAndOverrideAttributes() {
        setAttributes(removeEmptyAndOverrideAttrs(getAttributes()));
        if (getActivities() != null) {
            for (Activity activity : getActivities())
                activity.setAttributes(removeEmptyAndOverrideAttrs(activity.getAttributes()));
        }
        if (getTransitions() != null) {
            for (Transition trans : getTransitions())
                trans.setAttributes(removeEmptyAndOverrideAttrs(trans.getAttributes()));
        }
        if (getSubprocesses() != null) {
            for (Process embedded : getSubprocesses())
                embedded.removeEmptyAndOverrideAttributes();
        }
    }

    /**
     * Also removes these junk attributes: REFERENCED_ACTIVITIES, REFERENCED_PROCESSES.
     */
    private List<Attribute> removeEmptyAndOverrideAttrs(List<Attribute> attrs) {
        if (attrs == null)
            return null;
        List<Attribute> toKeep = new ArrayList<Attribute>();
        for (Attribute attr : attrs) {
            if (attr.getAttributeName() != null && attr.getAttributeName().trim().length() > 0 &&
                    attr.getAttributeValue() != null && attr.getAttributeValue().trim().length() > 0 &&
                    !WorkAttributeConstant.isOverrideAttribute(attr.getAttributeName()) &&
                    !attr.getAttributeName().equals("REFERENCED_ACTIVITIES") &&
                    !attr.getAttributeName().equals("REFERENCED_PROCESSES")) {
                toKeep.add(attr);
            }
        }
        return toKeep;
    }

    public void removeEmptyAttributes() {
        setAttributes(removeEmptyAttrs(getAttributes()));
        if (getActivities() != null) {
            for (Activity activity : getActivities())
                activity.setAttributes(removeEmptyAttrs(activity.getAttributes()));
        }
        if (getTransitions() != null) {
            for (Transition trans : getTransitions())
                trans.setAttributes(removeEmptyAttrs(trans.getAttributes()));
        }
        if (getSubprocesses() != null) {
            for (Process embedded : getSubprocesses())
                embedded.removeEmptyAttributes();
        }
    }

    private List<Attribute> removeEmptyAttrs(List<Attribute> attrs) {
        if (attrs == null)
            return null;
        List<Attribute> toKeep = new ArrayList<Attribute>();
        for (Attribute attr : attrs) {
            if (attr.getAttributeValue() != null && attr.getAttributeValue().trim().length() > 0)
                toKeep.add(attr);
        }
        return toKeep;
    }

    private boolean overrideAttributesApplied;
    public boolean overrideAttributesApplied() { return overrideAttributesApplied; }

    public Map<String,Value> getInputVariables() {
        Map<String,Value> inputVars = new HashMap<>();
        for (Variable var : getVariables()) {
            if (var.isInput()) {
                inputVars.put(var.getName(), var.toValue());
            }
        }
        return inputVars;
    }

    public Process(String packageName, String processName, JSONObject json) {
        this(json);
        setPackageName(packageName);
        setName(processName);
    }

    public Process(JSONObject json) throws JSONException {
        if (json.has("name"))
            setName(json.getString("name"));
        if (json.has("description"))
            setDescription(json.getString("description"));
        if (json.has("attributes")) {
            this.attributes = JsonUtil.getAttributes(json.getJSONObject("attributes"));
        }
        // many places don't check for null arrays, so we must instantiate
        this.activities = new ArrayList<Activity>();
        this.transitions = new ArrayList<Transition>();
        if (json.has("activities")) {
            JSONArray activitiesJson = json.getJSONArray("activities");
            for (int i = 0; i < activitiesJson.length(); i++) {
                JSONObject activityJson = activitiesJson.getJSONObject(i);
                Activity activity = new Activity(activityJson);
                this.activities.add(activity);
                if (activityJson.has("transitions")) {
                    JSONArray transitionsJson = activityJson.getJSONArray("transitions");
                    for (int j = 0; j < transitionsJson.length(); j++) {
                        Transition transition = new Transition(transitionsJson.getJSONObject(j));
                        transition.setFromId(activity.getId());
                        this.transitions.add(transition);
                    }
                }
            }
        }
        this.subprocesses = new ArrayList<Process>();
        if (json.has("subprocesses")) {
            JSONArray subprocsJson = json.getJSONArray("subprocesses");
            for (int i = 0; i < subprocsJson.length(); i++) {
                JSONObject subprocJson = subprocsJson.getJSONObject(i);
                Process subproc = new Process(subprocJson);
                String logicalId = subprocJson.getString("id");
                if (logicalId.startsWith("SubProcess"))
                    logicalId = "P" + logicalId.substring(10);
                subproc.setId(Long.valueOf(logicalId.substring(1)));
                subproc.setAttribute(WorkAttributeConstant.LOGICAL_ID, subprocJson.getString("id"));
                this.subprocesses.add(subproc);
            }
        }
        this.textNotes = new ArrayList<TextNote>();
        if (json.has("textNotes")) {
            JSONArray textNotesJson = json.getJSONArray("textNotes");
            for (int i = 0; i < textNotesJson.length(); i++)
                this.textNotes.add(new TextNote(textNotesJson.getJSONObject(i)));
        }
        this.variables = new ArrayList<Variable>();
        if (json.has("variables")) {
            JSONObject variablesJson = json.getJSONObject("variables");
            Map<String,JSONObject> objects = JsonUtil.getJsonObjects(variablesJson);
            for (String name : objects.keySet()) {
                Variable variable = new Variable(objects.get(name));
                variable.setName(name);
                this.variables.add(variable);
            }
        }
    }

    /**
     * JSON name = getName(), so not included.
     */
    public JSONObject getJson() throws JSONException {
        JSONObject json = create();
        if (getDescription() != null && !getDescription().isEmpty())
          json.put("description", getDescription());
        if (attributes != null && !attributes.isEmpty()) {
            json.put("attributes", JsonUtil.getAttributesJson(attributes));
        }
        if (activities != null && !activities.isEmpty()) {
            JSONArray activitiesJson = new JSONArray();
            for (Activity activity : activities) {
                JSONObject activityJson = activity.getJson();
                List<Transition> transitions = getAllTransitions(activity.getId());
                if (transitions != null && !transitions.isEmpty()) {
                    JSONArray transitionsJson = new JSONArray();
                    for (Transition transition : transitions) {
                        JSONObject transitionJson = transition.getJson();
                        if (transition.getToId() < 0) // newly created
                            transitionJson.put("to", getActivityVO(transition.getToId()).getLogicalId());
                        transitionsJson.put(transitionJson);
                    }
                    activityJson.put("transitions", transitionsJson);
                }
                activitiesJson.put(activityJson);
            }
            json.put("activities", activitiesJson);
        }
        if (subprocesses != null && !subprocesses.isEmpty()) {
            JSONArray subprocsJson = new JSONArray();
            for (Process subproc : subprocesses) {
                JSONObject subprocJson = subproc.getJson();
                String logicalId = subproc.getAttribute(WorkAttributeConstant.LOGICAL_ID);
                subprocJson.put("id", logicalId);
                subprocJson.put("name", subproc.getName());
                if (subprocJson.has("version"))
                  subprocJson.remove("version");
                subprocsJson.put(subprocJson);
            }
            json.put("subprocesses", subprocsJson);
        }
        if (textNotes != null && !textNotes.isEmpty()) {
            JSONArray textNotesJson = new JSONArray();
            for (TextNote textNote : textNotes)
                textNotesJson.put(textNote.getJson());
            json.put("textNotes", textNotesJson);
        }
        if (variables != null && !variables.isEmpty()) {
            JSONObject variablesJson = create();
            for (Variable variable : variables)
                variablesJson.put(variable.getJsonName(), variable.getJson());
            json.put("variables", variablesJson);
        }

        return json;
    }

    public String getJsonName() {
        return getName();
    }

    public AssetRequest getRequest() {
        String path = getAttribute("requestPath");
        String method = getAttribute("requestMethod");
        if (path != null && method != null) {
            if (!path.startsWith("/"))
                path = "/" + path;
            HttpMethod httpMethod = HttpMethod.valueOf(method);
            String assetPath = getPackageName() + "/" + getName();
            String parameters = getAttribute("requestParameters");
            JSONArray params = parameters == null ? null : new JSONArray(parameters);
            return new AssetRequest(assetPath, httpMethod, path, params);
        }
        else {
            return null;
        }
    }
}
