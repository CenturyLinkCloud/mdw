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

import com.centurylink.mdw.activity.types.GeneralActivity;
import com.centurylink.mdw.app.Compatibility;
import com.centurylink.mdw.cloud.CloudClassLoader;
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
import org.json.JSONException;
import org.json.JSONObject;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Package implements Serializable, Jsonable {

    public static final String MDW = "com.centurylink.mdw";

    private static Package defaultPackage = null;

    private Long id;
    private String name;
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
    // runtime information
    private ClassLoader classloader;
    private String group;

    public String getGroup() {
        return group;
    }
    public void setGroup(String group) {
        this.group = group;
    }

    public Package() {
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

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    @Deprecated
    public Long getPackageId() { return getId(); }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    @Deprecated
    public String getPackageName() { return getName(); }

    public List<Process> getProcesses() {
        return this.processes;
    }
    public void setProcesses(List<Process> pProcesses) {
        this.processes = pProcesses;
    }

    public boolean containsProcess(Long processId) {
        if (processes == null)
          return false;

        for (Process process : processes) {
            if (process.getId().equals(processId))
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

    public List<Attribute> getMetaAttributes() {
        if (metaContent == null || metaContent.isEmpty())
            return null;
        if (metaContent.trim().startsWith("{")) {
            Package metaPkg = new Package(new JsonObject(metaContent));
            return metaPkg.getAttributes();
        } else {
            Yaml yaml= new Yaml();
            Map<String,Object> map= (Map<String, Object>) yaml.load(metaContent);
            Package metaPkg = new Package(map);
            return metaPkg.getAttributes();
        }
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

    public String getLabel() {
        return getName() + " v" + getVersionString();
    }

    public static Package getDefaultPackage() {
        if (defaultPackage == null) {
            defaultPackage = new Package();
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

    public GeneralActivity getActivityImplementor(String className)
    throws InstantiationException, IllegalAccessException, ClassNotFoundException, IOException, MdwJavaException {
        // try dynamic java first (preferred in case patch override is needed)
        try {
            ClassLoader parentLoader = getCloudClassLoader();
            return (GeneralActivity) CompiledJavaCache.getInstance(className, parentLoader, this);
        }
        catch (ClassNotFoundException ex) {
            // not located as dynamic java
        }
        String implClass = Compatibility.getActivityImplementor(className);
        GeneralActivity injected = SpringAppContext.getInstance().getActivityImplementor(implClass, this);
        if (injected != null)
            return injected;
        if (getCloudClassLoader().hasClass(implClass))
          return getCloudClassLoader().loadClass(implClass).asSubclass(GeneralActivity.class).newInstance();
        return getClassLoader().loadClass(implClass).asSubclass(GeneralActivity.class).newInstance();
    }

    public EventHandler getEventHandler(String classname)
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
        if (version < 0) // Negative version means fake pkg (for when retrieving assets from Git history)
            return "-" + formatVersion((version * -1));

        int major = version/1000;
        int minor = version%1000;
        int point = minor%100;
        return major + "." + minor/100 + "." + (point >= 10 ? point : "0" + point);
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

    @SuppressWarnings("unchecked")
    public Package(Map<?, ?> map) {
        if (map.get("name") != null)
            this.setName((String)map.get("name"));
        if (map.get("version") != null)
            this.setVersion(parseVersion((String)map.get("version")));
        if (map.get("schemaVersion") != null)
            this.setVersion(Asset.parseVersion((String)map.get("schemaVersion")));
        if (map.get("workgroup") != null)
            this.setGroup((String)map.get("workgroup"));
        if (map.get("attributes") != null) {
            List<Attribute> attributes = new ArrayList<Attribute>();
            Map<String, Object> attrs =  (Map<String, Object>) map.get("attributes");
            for (Map.Entry<String, Object> entry : attrs.entrySet()) {
                Attribute attr = new Attribute(entry.getKey(), entry.getValue().toString());
                attributes.add(attr);
            }
            this.attributes = attributes;
        }
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
            this.attributes = Attribute.getAttributes(json.getJSONObject("attributes"));
        }
    }

    public JSONObject getJson() throws JSONException {
        JSONObject json = create();
        json.put("version", getVersionString());
        json.put("schemaVersion", Asset.formatVersion(getSchemaVersion()));
        if (group != null)
            json.put("workgroup", group);
        if (attributes != null && !attributes.isEmpty()) {
            json.put("attributes", Attribute.getAttributesJson(attributes, true));
        }
        json.put("name", getName());

        return json;
    }

    public String getJsonName() {
        return getName();
    }
}
