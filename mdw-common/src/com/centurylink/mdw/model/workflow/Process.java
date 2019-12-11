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

import com.centurylink.mdw.constant.ActivityResultCodeConstant;
import com.centurylink.mdw.constant.ProcessVisibilityConstant;
import com.centurylink.mdw.constant.WorkAttributeConstant;
import com.centurylink.mdw.constant.WorkTransitionAttributeConstant;
import com.centurylink.mdw.model.JsonObject;
import com.centurylink.mdw.model.Jsonable;
import com.centurylink.mdw.model.Yamlable;
import com.centurylink.mdw.model.asset.Asset;
import com.centurylink.mdw.model.asset.AssetRequest;
import com.centurylink.mdw.model.asset.AssetRequest.HttpMethod;
import com.centurylink.mdw.model.attribute.Attribute;
import com.centurylink.mdw.model.event.EventType;
import com.centurylink.mdw.model.project.Data;
import com.centurylink.mdw.model.variable.Variable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;

/**
 * Value object representing a process definition.
 */
public class Process extends Asset implements Jsonable, Yamlable, Linkable {

    public static final String TRANSITION_ON_NULL = "Matches Null Return Code";
    public static final String TRANSITION_ON_DEFAULT = "Acts as Default";

    private List<Variable> variables;
    private List<Transition> transitions;
    private List<Process> subprocesses;
    private List<Activity> activities;
    private List<TextNote> textNotes;
    private List<Attribute> attributes;

    public static Process fromString(String contents) {
        if (contents.startsWith("{")) {
            return new Process(new JsonObject(contents));
        }
        else {
            return new Process(Yamlable.fromString(contents));
        }
    }

    public Process() {
        setLanguage(Asset.PROCESS);
    }

    public Process(Long id) {
        this();
        setId(id);
    }

    public Process(Long id, String name, String description) {
        setLanguage(Asset.PROCESS);
        this.setId(id);
        this.setName(name);
        this.setComment(description);
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

    public Process getSubProcess(Long id) {
        if (this.getId() != null && this.getId().equals(id)) // Id field is null for instance definitions
            return this;
        if (this.subprocesses == null)
            return null;
        for (Process ret : subprocesses) {
            if (ret.getId().equals(id))
                return ret;
        }
        return null;
    }

     public Activity getActivity(Long id) {
         if (this.activities == null) {
             return null;
         }
         for (Activity activity : activities) {
             if (id.longValue() == activity.getId().longValue()) {
                 return activity;
             }
         }
         return null;
    }

    /**
     * Also searches subprocs.
     */
    public Activity getActivity(String logicalId) {
        return getActivity(logicalId, true);

    }

    public Activity getActivity(String logicalId, boolean subprocs) {
        for (Activity activity : getActivities()) {
            if (activity.getLogicalId().equals(logicalId)) {
                activity.setProcessName(getName());
                return activity;
            }
        }
        if (subprocs) {
            for (Process subProc : this.subprocesses) {
                for (Activity activity : subProc.getActivities()) {
                    if (activity.getLogicalId().equals(logicalId)) {
                        activity.setProcessName(getName() + ":" + subProc.getName());
                        return activity;
                    }
                }
            }
        }

        return null;
    }

    public List<Activity> getActivities() {
        return activities;
    }

    /**
     * @return the activities order by sequence id
     */
    public List<Activity> getActivitiesOrderBySeq() {
        List<Activity> sorted = new ArrayList<>(activities);
        sorted.sort(Comparator.comparingInt(Activity::getSequenceId));
        return sorted;
    }

    /**
     * @param activities the activities to set
     */
    public void setActivities(List<Activity> activities) {
        this.activities = activities;
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

    @Override
    public boolean equals(Object other) {
        return other instanceof Process && ((Process)other).getId().equals(getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    /**
     * Finds one work transition for this process matching the specified parameters
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
                if (entrycode == null || entrycode.isEmpty()) {
                    if (completionCode == null || completionCode.isEmpty())
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
                if (entrycode == null || entrycode.isEmpty())
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
            returnSet = new ArrayList<>();
            for (Transition trans : allTransitions) {
                if (trans.getEventType().equals(EventType.RESUME))
                    returnSet.add(trans);
            }
        }
        return returnSet;
    }

    public List<Transition> getAllTransitions(Long fromWorkId) {
        List<Transition> allTransitions = new ArrayList<>();
        for (Transition workTransitionVO : getTransitions()) {
            if (workTransitionVO.getFromId().equals(fromWorkId)) {
                allTransitions.add(workTransitionVO);
            }
        }
        return allTransitions;
    }

    private List<Transition> findTransitions(List<Transition> all,
            Integer eventType, String compcode) {
        List<Transition> set = new ArrayList<>();
        for (Transition trans : all) {
            if (trans.match(eventType, compcode)) set.add(trans);
        }
        return set;
    }

    /**
     * Find a work transition based on its id value
     * @return the matching transition, or null if not found
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
     */
    public void setAttribute(String attrname, String value) {
        if (attributes == null)
            attributes = new ArrayList<>();
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
        for (Activity activity : getActivities()) {
            if (Data.Implementors.START_IMPL.equals(activity.getImplementor()))
                return activity;
        }
        // assume first activity in process is start
        return getActivities().get(0);
    }

    public List<Activity> getDownstreamActivities(Activity activity) {
        List<Activity> downstreamActivities = new ArrayList<>();
        for (Transition transition : getAllTransitions(activity.getId())) {
            Activity downstreamActivity = getActivity(transition.getToId());
            if (downstreamActivity != null)
                downstreamActivities.add(downstreamActivity);
        }
        return downstreamActivities;
    }

    private int sequenceId;
    public int getSequenceId() { return sequenceId; }
    public void setSequenceId(int sequenceId) { this.sequenceId = sequenceId; }

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
        List<Attribute> toKeep = new ArrayList<>();
        for (Attribute attr : attrs) {
            if (attr.getName() != null && attr.getName().trim().length() > 0 &&
                    attr.getValue() != null && attr.getValue().trim().length() > 0 &&
                    !WorkAttributeConstant.isOverrideAttribute(attr.getName()) &&
                    !attr.getName().equals("REFERENCED_ACTIVITIES") &&
                    !attr.getName().equals("REFERENCED_PROCESSES")) {
                toKeep.add(attr);
            }
        }
        return toKeep;
    }

    public Process(JSONObject json) throws JSONException {
        if (json.has("name"))
            setName(json.getString("name"));
        if (json.has("description"))
            setDescription(json.getString("description"));
        if (json.has("attributes")) {
            this.attributes = Attribute.getAttributes(json.getJSONObject("attributes"));
        }
        // many places don't check for null arrays, so we must instantiate
        this.activities = new ArrayList<>();
        this.transitions = new ArrayList<>();
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
        this.subprocesses = new ArrayList<>();
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
        this.textNotes = new ArrayList<>();
        if (json.has("textNotes")) {
            JSONArray textNotesJson = json.getJSONArray("textNotes");
            for (int i = 0; i < textNotesJson.length(); i++)
                this.textNotes.add(new TextNote(textNotesJson.getJSONObject(i)));
        }
        this.variables = new ArrayList<>();
        if (json.has("variables")) {
            JSONObject variablesJson = json.getJSONObject("variables");
            Map<String,JSONObject> objects = getJsonObjects(variablesJson);
            for (String name : objects.keySet()) {
                Variable variable = new Variable(objects.get(name));
                variable.setName(name);
                this.variables.add(variable);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public Process(Map<String,Object> yaml) {
        if (yaml.containsKey("name"))
            setName((String)yaml.get("name"));
        if (yaml.containsKey("description"))
            setDescription((String)yaml.get("description"));
        if (yaml.containsKey("attributes")) {
            this.attributes = Attribute.getAttributes((Map<String,Object>)yaml.get("attributes"));
        }
        this.activities = new ArrayList<>();
        this.transitions = new ArrayList<>();
        if (yaml.containsKey("activities")) {
            List<Map<String,Object>> activitiesList = (List<Map<String,Object>>) yaml.get("activities");
            for (Map<String,Object> activityYaml : activitiesList) {
                Activity activity = new Activity(activityYaml);
                this.activities.add(activity);
                if (activityYaml.containsKey("transitions")) {
                    List<Map<String,Object>> transitionsList = (List<Map<String,Object>>) activityYaml.get("transitions");
                    for (Map<String,Object> transitionYaml : transitionsList) {
                        Transition transition = new Transition(transitionYaml);
                        transition.setFromId(activity.getId());
                        this.transitions.add(transition);
                    }
                }
            }
        }
        this.subprocesses = new ArrayList<>();
        if (yaml.containsKey("subprocesses")) {
            List<Map<String,Object>> subprocsList = (List<Map<String,Object>>) yaml.get("subprocesses");
            for (Map<String,Object> subprocYaml : subprocsList) {
                Process subproc = new Process(subprocYaml);
                String logicalId = (String)subprocYaml.get("id");
                if (logicalId.startsWith("SubProcess"))
                    logicalId = "P" + logicalId.substring(10);
                subproc.setId(Long.valueOf(logicalId.substring(1)));
                subproc.setAttribute(WorkAttributeConstant.LOGICAL_ID, (String)subprocYaml.get("id"));
                this.subprocesses.add(subproc);
            }
        }
        this.textNotes = new ArrayList<>();
        if (yaml.containsKey("textNotes")) {
            List<Map<String,Object>> textNotesList = (List<Map<String,Object>>)yaml.get("textNotes");
            for (Map<String,Object> textNoteYaml : textNotesList)
                this.textNotes.add(new TextNote(textNoteYaml));
        }
        this.variables = new ArrayList<>();
        if (yaml.containsKey("variables")) {
            Map<String,Object> variablesMap = (Map<String,Object>) yaml.get("variables");
            for (String name : variablesMap.keySet()) {
                Variable variable = new Variable((Map<String,Object>)variablesMap.get(name));
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
            json.put("attributes", Attribute.getAttributesJson(attributes));
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
                            transitionJson.put("to", getActivity(transition.getToId()).getLogicalId());
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

    @Override
    public Map<String,Object> getYaml() {
        // ensure correct ordering
        Map<String,Object> yaml = Yamlable.create();

        if (getDescription() != null && !getDescription().isEmpty())
            yaml.put("description", getDescription());
        if (activities != null && !activities.isEmpty()) {
            List<Map<String,Object>> activitiesList = new ArrayList<>();
            for (Activity activity : activities) {
                Map<String,Object> activityYaml = activity.getYaml();
                List<Transition> transitions = getAllTransitions(activity.getId());
                if (transitions != null && !transitions.isEmpty()) {
                    List<Map<String,Object>> transitionsList = new ArrayList<>();
                    for (Transition transition : transitions) {
                        Map<String,Object> transitionYaml = transition.getYaml();
                        if (transition.getToId() < 0) // newly created
                            transitionYaml.put("to", getActivity(transition.getToId()).getLogicalId());
                        transitionsList.add(transitionYaml);
                    }
                    activityYaml.put("transitions", transitionsList);
                }
                activitiesList.add(activityYaml);
            }
            yaml.put("activities", activitiesList);
        }
        if (subprocesses != null && !subprocesses.isEmpty()) {
            List<Map<String,Object>> subprocsList = new ArrayList<>();
            for (Process subproc : subprocesses) {
                String logicalId = subproc.getAttribute(WorkAttributeConstant.LOGICAL_ID);
                Map<String,Object> subprocYaml = Yamlable.create();
                subprocYaml.put("id", logicalId);
                subprocYaml.put("name", subproc.getName());
                subprocYaml.putAll(subproc.getYaml());
                if (subprocYaml.containsKey("version"))
                    subprocYaml.remove("version");
                subprocsList.add(subprocYaml);
            }
            yaml.put("subprocesses", subprocsList);
        }
        if (variables != null && !variables.isEmpty()) {
            Map<String,Object> variablesYaml = new TreeMap<>(); // sorted
            for (Variable variable : variables)
                variablesYaml.put(variable.getName(), variable.getYaml());
            yaml.put("variables", variablesYaml);
        }
        if (attributes != null && !attributes.isEmpty()) {
            yaml.put("attributes", Attribute.getAttributesYaml(attributes));
        }
        if (textNotes != null && !textNotes.isEmpty()) {
            List<Map<String,Object>> textNotesList = new ArrayList<>();
            for (TextNote textNote : textNotes)
                textNotesList.add(textNote.getYaml());
            yaml.put("textNotes", textNotesList);
        }

        return yaml;
    }

    public JSONObject getSummaryJson() {
        JSONObject json = create();
        json.put("id", getId());
        json.put("name", getName());
        json.put("packageName", getPackageName());
        json.put("version", getVersionString());
        return json;
    }

    @Override
    public JSONObject getSummaryJson(int detail) {
        return getSummaryJson();
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
            AssetRequest request = new AssetRequest(assetPath, httpMethod, path, params);
            request.setSummary(getAttribute("requestSummary"));
            return request;
        }
        else {
            return null;
        }
    }

    public static Map<String,JSONObject> getJsonObjects(JSONObject json) throws JSONException {
        Map<String,JSONObject> objects = new HashMap<>();
        Iterator<?> keys = json.keys();
        while (keys.hasNext()) {
            String key = keys.next().toString();
            objects.put(key, json.getJSONObject(key));
        }
        return objects;
    }

    @Override
    public String getContentType() {
        return "application/json";
    }

    /**
     * For runtime use only since value is cached.
     */
    private Boolean hasDelayTransition;
    public boolean hasDelayTransition() {
        if (hasDelayTransition == null) {
            for (Transition transition : getTransitions()) {
                String delayAttr = transition.getAttribute(WorkTransitionAttributeConstant.TRANSITION_DELAY);
                if (delayAttr != null && !"0".equals(delayAttr)) {
                    return hasDelayTransition = true;
                }
            }
        }
        return hasDelayTransition = false;
    }

    public boolean invokesSubprocess(Process subprocess) {
        if (activities != null) {
            for (Activity activity : activities) {
                if (activity.invokesSubprocess(subprocess))
                    return true;
            }
        }
        if (subprocesses != null) {
            for (Process embedded : subprocesses) {
                if (embedded.getActivities() != null) {
                    for (Activity activity : embedded.getActivities()) {
                        if (activity.invokesSubprocess(subprocess))
                            return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Find all subprocesses invoked by me.
     */
    public List<Process> findInvoked(List<Process> processes) {
        List<Process> invoked = new ArrayList<>();
        if (activities != null) {
            for (Activity activity : activities) {
                for (Process subproc : activity.findInvoked(processes)) {
                    if (!invoked.contains(subproc))
                        invoked.add(subproc);
                }
            }
        }
        if (subprocesses != null) {
            for (Process embedded : subprocesses) {
                if (embedded.getActivities() != null) {
                    for (Activity activity : embedded.getActivities()) {
                        for (Process subproc : activity.findInvoked(processes)) {
                            if (invoked.contains(subproc))
                                invoked.add(subproc);
                        }
                    }
                }
            }
        }
        return invoked;
    }

    public Linked<Activity> getLinkedActivities() {
        return getLinkedActivities(getStartActivity());
    }

    public Linked<Activity> getLinkedActivities(Activity start) {
        Linked<Activity> linked = new Linked<>(start);
        linkActivities(linked);
        return linked;
    }

    private void linkActivities(Linked<Activity> parent) {
        Activity parentActivity = parent.get();
        parentActivity.setProcessId(getId());
        parentActivity.setProcessName(getName());
        parentActivity.setProcessVersion(getVersionString());
        parentActivity.setPackageName(getPackageName());
        parentActivity.setMilestoneName(parentActivity.milestoneName());
        parentActivity.setMilestoneGroup(parentActivity.milestoneGroup());
        if (!parent.get().isStop()) {
            for (Activity downstream : getDownstreamActivities(parent.get())) {
                boolean deadend = !downstream.isStop() && getDownstreamActivities(downstream).isEmpty();
                if (!deadend) {
                    Linked<Activity> child = new Linked<>(downstream);
                    child.setParent(parent);
                    parent.getChildren().add(child);
                    if (!child.checkCircular()) {
                        linkActivities(child);
                    }
                }
            }
        }
    }
}
