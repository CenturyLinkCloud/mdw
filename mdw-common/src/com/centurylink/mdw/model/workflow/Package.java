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

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.xmlbeans.XmlException;
import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.activity.types.GeneralActivity;
import com.centurylink.mdw.app.Compatibility;
import com.centurylink.mdw.cloud.CloudClassLoader;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.event.EventHandler;
import com.centurylink.mdw.java.CompiledJavaCache;
import com.centurylink.mdw.java.MdwJavaException;
import com.centurylink.mdw.model.JsonObject;
import com.centurylink.mdw.model.Jsonable;
import com.centurylink.mdw.model.asset.Asset;
import com.centurylink.mdw.model.attribute.Attribute;
import com.centurylink.mdw.model.event.ExternalEvent;
import com.centurylink.mdw.model.task.TaskTemplate;
import com.centurylink.mdw.model.variable.Variable;
import com.centurylink.mdw.spring.SpringAppContext;
import com.centurylink.mdw.util.JsonUtil;

public class Package implements Serializable, Jsonable {

    public static final String MDW = "com.centurylink.mdw";
    public static final String MDW_HUB = MDW + ".hub";

    private static Package defaultPackage = null;

    private Long packageId;
    private String packageName;
    private String packageDescription;
    private String metaContent;
    private List<Attribute> attributes;
    private List<ActivityImplementor> implementors;
    private List<ExternalEvent> externalEvents;
    private List<Variable> variables;
    private List<Process> processes;
    private List<Asset> assets;
    private List<TaskTemplate> taskTemplates;
    private int schemaVersion;
    private int version;
    private boolean exported;
    private Date modifyDate;
    // runtime information
    private Map<String,String> properties;
    private ClassLoader classloader;
    private String group;

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public Package(){
    }

    /**
     * Only set for VCS assets.
     */
    private boolean archived;
    public boolean isArchived() { return archived; }
    public void setArchived(boolean archived) { this.archived = archived; }

    /**
     * @return the attributes
     */
    public List<Attribute> getAttributes() {
        return attributes;
    }

    public List<Attribute> getAttributes(String attributeGroup) {
        if (attributes == null)
            return null;
        List<Attribute> groupAttributes = new ArrayList<>();
        for (Attribute attribute : attributes) {
            if (attributeGroup == null) {
                if (attribute.getAttributeGroup() == null)
                    groupAttributes.add(attribute);
            }
            else if (attributeGroup.equals(attribute.getAttributeGroup())) {
                groupAttributes.add(attribute);
            }
        }
        return groupAttributes;
    }

    public Map<String,List<Attribute>> getAttributesByGroup() {
        if (attributes == null)
            return null;
        Map<String,List<Attribute>> grouped = new HashMap<String,List<Attribute>>();
        for (Attribute attribute : attributes) {
            String group = attribute.getAttributeGroup();
            List<Attribute> groupAttrs = grouped.get(group);
            if (groupAttrs == null) {
                groupAttrs = new ArrayList<Attribute>();
                grouped.put(group, groupAttrs);
            }
            groupAttrs.add(attribute);
        }
        return grouped;
    }

    /**
     * @param attributes the attributes to set
     */
    public void setAttributes(List<Attribute> attributes) {
        this.attributes = attributes;
    }

    public String getAttribute(String name) {
        if (attributes != null) {
            for (Attribute attr : attributes) {
                if (attr.getAttributeName().equals(name))
                    return attr.getAttributeValue();
            }
        }
        return null;
    }

    public Long getPackageId() {
        return packageId;
    }

    public Long getId() {
        return getPackageId();
    }

    /**
     * @param processId the processId to set
     */
    public void setPackageId(Long packageId) {
        this.packageId = packageId;
    }

    public void setId(Long id) {
        setPackageId(id);
    }

    /**
     * @return the processName
     */
    public String getPackageName() {
        return packageName;
    }

    public String getName() {
        return getPackageName();
    }

    /**
     * @param processName the processName to set
     */
    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public void setName(String name) {
        setPackageName(name);
    }

    /**
     * @return the processDescription
     */
    public String getPackageDescription() {
        return packageDescription;
    }

    /**
     * @param processDescription the processDescription to set
     */
    public void setPackageDescription(String packageDescription) {
        this.packageDescription = packageDescription;
    }

    /**
     * @return the processes
     */
    public List<Process> getProcesses() {
        return this.processes;
    }

    /**
     * @param processes the processes to set
     */
    public void setProcesses(List<Process> pProcesses) {
        this.processes = pProcesses;
    }

    public boolean containsProcess(Long processId) {
        if (processes == null)
          return false;

        for (Process processVO : processes) {
            if (processVO.getProcessId().equals(processId))
                return true;
        }
        return false;
    }

    public boolean containsExternalEvent(Long externalEventId) {
        if (externalEvents == null)
            return false;

        for (ExternalEvent externalEventVO : externalEvents) {
            if (externalEventVO.getId().equals(externalEventId))
                return true;
        }
        return false;
    }

    public boolean containsTaskTemplate(Long taskId) {
        if (taskTemplates == null)
            return false;

        for (TaskTemplate taskTemplate : taskTemplates) {
            if (taskTemplate.getTaskId().equals(taskId))
                return true;
        }
        return false;
    }

    public boolean containsActivityImpl(Long activityImplId) {
        if (implementors == null)
            return false;

        for (ActivityImplementor activityImplVO : implementors) {
            if (activityImplVO.getImplementorId().equals(activityImplId))
                return true;
        }
        return false;
    }

    public boolean containsActivityImpl(String implClass) {
        if (implementors == null)
            return false;

        for (ActivityImplementor activityImplVO : implementors) {
            if (activityImplVO.getImplementorClassName().equals(implClass))
                return true;
        }
        return false;
    }

    public boolean containsAsset(Long assetId) {
        if (assets == null)
            return false;

        for (Asset asset : assets) {
            if (asset.getId().equals(assetId))
                return true;
        }
        return false;
    }
    /**
     * @return the variables
     */
    public List<Variable> getVariables() {
        return variables;
    }

    /**
     * @param variables the variables to set
     */
    public void setVariables(List<Variable> variables) {
        this.variables = variables;
    }

    public List<ActivityImplementor> getImplementors() {
        return this.implementors;
    }
    public void setImplementors(List<ActivityImplementor> imps){
        this.implementors = imps;
    }

    public List<ExternalEvent> getExternalEvents(){
        return this.externalEvents;
    }
    public void setExternalEvents(List<ExternalEvent> externalEvents){
        this.externalEvents = externalEvents;
    }

    public List<TaskTemplate> getTaskTemplates(){
        return this.taskTemplates;
    }
    public void setTaskTemplates(List<TaskTemplate> taskTemplates){
        this.taskTemplates = taskTemplates;
    }

    /**
     * Method that returns the version
     */
    public int getVersion(){
        return this.version;
    }

    /**
     * method that sets the version
     */
    public void setVersion(int pVersion){
        this.version = pVersion;
    }

    public int getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(int schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public boolean isExported() {
        return exported;
    }

    public boolean isDefaultPackage() {
      return getName() == null;
    }

    public void setExported(boolean exported) {
        this.exported = exported;
    }

    public List<Asset> getAssets() {
        return assets;
    }

    public void setAssets(List<Asset> assets) {
        this.assets = assets;
    }

    public Asset getAsset(String name) {
        if (assets != null) {
            for (Asset asset : assets) {
                if (asset.getName().equals(name))
                    return asset;
            }
        }
        return null;
    }

    public String getMetaContent() {
        return metaContent;
    }

    public void setMetaContent(String metaContent) {
        this.metaContent = metaContent;
    }

    public List<Attribute> getMetaAttributes() throws JSONException, XmlException {
        if (metaContent == null || metaContent.isEmpty())
            return null;
        List<Attribute> metaAttributes = new ArrayList<Attribute>();
        if (metaContent.trim().startsWith("{")) {
            Package metaPkg = new Package(new JsonObject(metaContent));
            return metaPkg.getAttributes();
        }
        return metaAttributes;
    }

    public String getVersionString() {
        return formatVersion(version);
    }

    public int getNewVersion(boolean major) {
        if (major)
            return (version/1000 + 1) * 1000;
        else
            return version + 1;
    }

    public String getNewVersionString(boolean major) {
        int version = getNewVersion(major);
        return version/1000 + "." + version%1000;
    }

    public String getLabel() {
        return getPackageName() + " v" + getVersionString();
    }

    public Date getModifyDate() {
        return modifyDate;
    }

    public void setModifyDate(Date modifyDate) {
        this.modifyDate = modifyDate;
    }

    public void hashProperties() {
        properties = new HashMap<String,String>();
        if (attributes!=null) {
            for (Attribute attr: attributes) {
                properties.put(attr.getAttributeName(), attr.getAttributeValue());
            }
        }
    }

    public String getProperty(String propertyName) {
        if (properties == null)
            hashProperties();
        String v = properties.get(propertyName);
        if (v==null) v = PropertyManager.getProperty(propertyName);
        return v;
    }

    public static Package getDefaultPackage() {
        if (defaultPackage==null) {
            defaultPackage = new Package() {
                @Override
                public String getProperty(String propertyName) {
                    return PropertyManager.getProperty(propertyName);
                }
            };
        }
        return defaultPackage;
    }

    /**
     * TODO : dynamic java classloader
     * @return the bundle classloader specified by the package config
     */
    public ClassLoader getClassLoader() {
        if (classloader == null) {
            classloader = getClass().getClassLoader();
        }
        return classloader;
    }

    private CloudClassLoader cloudClassLoader = null;
    public CloudClassLoader getCloudClassLoader() {
        if (cloudClassLoader == null)
            cloudClassLoader = new CloudClassLoader(this);
        return cloudClassLoader;
    }

    public Class<?> loadClass(String classname) throws ClassNotFoundException {
        ClassLoader classloader = getClassLoader();
        return classloader.loadClass(classname);
    }

    public GeneralActivity getActivityImplementor(Activity activity)
    throws InstantiationException, IllegalAccessException, ClassNotFoundException, IOException, MdwJavaException {
        // try dynamic java first (preferred in case patch override is needed)
        try {
            ClassLoader parentLoader = getCloudClassLoader();
            return (GeneralActivity) CompiledJavaCache.getInstance(activity.getImplementorClassName(), parentLoader, this);
        }
        catch (ClassNotFoundException ex) {
            // not located as dynamic java
        }
        String implClass = Compatibility.getActivityImplementor(activity.getImplementorClassName());
        GeneralActivity injected = SpringAppContext.getInstance().getActivityImplementor(implClass, this);
        if (injected != null)
            return injected;
        if (getCloudClassLoader().hasClass(implClass))
          return getCloudClassLoader().loadClass(implClass).asSubclass(GeneralActivity.class).newInstance();
        return getClassLoader().loadClass(implClass).asSubclass(GeneralActivity.class).newInstance();
    }

    public EventHandler getEventHandler(String classname, String content, Map<String,String> metaInfo)
    throws InstantiationException, IllegalAccessException, ClassNotFoundException, IOException, MdwJavaException {
        // try dynamic java first (preferred in case patch override is needed)
        try {
            ClassLoader parentLoader = getCloudClassLoader();
            return (EventHandler) CompiledJavaCache.getInstance(classname, parentLoader, this);
        }
        catch (ClassNotFoundException ex) {
            // not located as dynamic java
        }
        String handlerClass = Compatibility.getEventHandler(classname);
        EventHandler injected = SpringAppContext.getInstance().getEventHandler(handlerClass, this);
        if (injected != null)
            return injected;
        if (getCloudClassLoader().hasClass(handlerClass))
          return getCloudClassLoader().loadClass(handlerClass).asSubclass(EventHandler.class).newInstance();
        return getClassLoader().loadClass(handlerClass).asSubclass(EventHandler.class).newInstance();
    }

    public static String formatVersion(int version) {
        int major = version/1000;
        int minor = version%1000;
        int point = minor%100;
        return major + "." + minor/100 + "." + (point > 10 ? point : "0" + point);
    }

    public static String formatVersionOld(int version) {
        return version/1000 + "." + version%1000;
    }

    public static int parseVersion(String versionString) throws NumberFormatException {
        if (versionString == null)
            return 0;
        int firstDot = versionString.indexOf('.');
        int major, minor;
        if (firstDot > 0) {
            major = Integer.parseInt(versionString.substring(0, firstDot));
            int secondDot = versionString.indexOf('.', firstDot + 1);
            if (secondDot > 0)
                minor = Integer.parseInt(versionString.substring(firstDot + 1, secondDot)) * 100 + Integer.parseInt(versionString.substring(secondDot + 1));
            else
                minor = Integer.parseInt(versionString.substring(firstDot + 1));
        }
        else {
            major = 0;
            minor = Integer.parseInt(versionString);
        }
        return major*1000 + minor;
    }

    @Override
    public String toString() {
        return getLabel();
    }

    public Package(JSONObject json) throws JSONException {
        if (json.has("name"))
            this.setName(json.getString("name"));
        if (json.has("version"))
            this.setVersion(parseVersion(json.getString("version")));
        if (json.has("schemaVersion"))
            this.setSchemaVersion(Asset.parseVersion(json.getString("schemaVersion")));
        if (json.has("workgroup"))
            this.setGroup(json.getString("workgroup"));
        if (json.has("attributes")) {
            this.attributes = JsonUtil.getAttributes(json.getJSONObject("attributes"));
        }
        // many places don't check for null arrays, so we must instantiate
        this.implementors = new ArrayList<ActivityImplementor>();
        if (json.has("activityImplementors")) {
            JSONObject implementorsJson = json.getJSONObject("activityImplementors");
            for (JSONObject implementorJson : JsonUtil.getJsonObjects(implementorsJson).values())
                this.implementors.add(new ActivityImplementor(implementorJson));
        }
        this.externalEvents = new ArrayList<ExternalEvent>();
        if (json.has("eventHandlers")) {
            JSONObject eventHandlersJson = json.getJSONObject("eventHandlers");
            Map<String, JSONObject> objects = JsonUtil.getJsonObjects(eventHandlersJson);
            for (String name : objects.keySet()) {
                ExternalEvent externalEvent = new ExternalEvent(objects.get(name));
                externalEvent.setEventName(name);
                this.externalEvents.add(externalEvent);
            }
        }
        this.processes = new ArrayList<Process>();
        if (json.has("processes")) {
            JSONObject processesJson = json.getJSONObject("processes");
            Map<String,JSONObject> objects = JsonUtil.getJsonObjects(processesJson);
            for (String name : objects.keySet()) {
                Process process = new Process(objects.get(name));
                process.setName(name);
                this.processes.add(process);
            }
        }
        this.assets = new ArrayList<Asset>();
        if (json.has("assets")) {
            JSONObject assetsJson = json.getJSONObject("assets");
            Map<String,JSONObject> objects = JsonUtil.getJsonObjects(assetsJson);
            for (String name : objects.keySet()) {
                Asset asset = new Asset(objects.get(name));
                asset.setName(name);
                asset.setLanguage(Asset.getFormat(name));
                this.assets.add(asset);
            }
        }
        this.taskTemplates = new ArrayList<TaskTemplate>();
        if (json.has("taskTemplates")) {
            JSONObject taskTemplatesJson = json.getJSONObject("taskTemplates");
            Map<String,JSONObject> objects = JsonUtil.getJsonObjects(taskTemplatesJson);
            for (String name : objects.keySet()) {
                TaskTemplate taskTemplate = new TaskTemplate(objects.get(name));
                taskTemplate.setName(name);
                this.taskTemplates.add(taskTemplate);
            }
        }
    }

    public JSONObject getJson() throws JSONException {
        return getJson(true);
    }

    public JSONObject getJson(boolean deep) throws JSONException {
        JSONObject json = create();
        json.put("version", getVersionString());
        json.put("schemaVersion", Asset.formatVersion(getSchemaVersion()));
        if (group != null)
            json.put("workgroup", group);
        if (attributes != null && !attributes.isEmpty()) {
            json.put("attributes", JsonUtil.getAttributesJson(attributes, true));
        }
        if (deep) {
            // name is not included since it's the JSON name.
            if (implementors != null && !implementors.isEmpty()) {
                JSONObject implementorsJson = create();
                for (ActivityImplementor implementor : implementors)
                    implementorsJson.put(implementor.getJsonName(), implementor.getJson());
                json.put("activityImplementors", implementorsJson);
            }
            if (externalEvents != null && !externalEvents.isEmpty()) {
                JSONObject eventHandlersJson = create();
                for (ExternalEvent eventHandler : externalEvents)
                    eventHandlersJson.put(eventHandler.getJsonName(), eventHandler.getJson());
                json.put("eventHandlers", eventHandlersJson);
            }
            if (processes != null && !processes.isEmpty()) {
                JSONObject processesJson = create();
                for (Process process : processes)
                    processesJson.put(process.getJsonName(), process.getJson());
                json.put("processes", processesJson);
            }
            if (assets != null && !assets.isEmpty()) {
                JSONObject assetsJson = create();
                for (Asset asset : assets)
                    assetsJson.put(asset.getJsonName(), asset.getJson());
                json.put("assets", assetsJson);
            }
            if (taskTemplates != null && !taskTemplates.isEmpty()) {
                JSONObject taskTemplatesJson = create();
                for (TaskTemplate taskTemplate : taskTemplates)
                    taskTemplatesJson.put(taskTemplate.getJsonName(), taskTemplate.getJson());
                json.put("taskTemplates", taskTemplatesJson);
            }
        }
        else {
            json.put("name", getName());
        }

        return json;
    }

    public String getJsonName() {
        return getName();
    }
}
