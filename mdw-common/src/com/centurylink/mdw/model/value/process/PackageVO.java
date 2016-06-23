/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model.value.process;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.centurylink.mdw.activity.types.GeneralActivity;
import com.centurylink.mdw.cloud.CloudClassLoader;
import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.common.Compatibility;
import com.centurylink.mdw.common.constant.WorkAttributeConstant;
import com.centurylink.mdw.common.provider.ProviderRegistry;
import com.centurylink.mdw.common.spring.SpringAppContext;
import com.centurylink.mdw.common.utilities.form.FormAction;
import com.centurylink.mdw.common.utilities.property.PropertyManager;
import com.centurylink.mdw.event.ExternalEventHandler;
import com.centurylink.mdw.java.CompiledJavaCache;
import com.centurylink.mdw.java.MdwJavaException;
import com.centurylink.mdw.model.value.activity.ActivityImplementorVO;
import com.centurylink.mdw.model.value.activity.ActivityVO;
import com.centurylink.mdw.model.value.attribute.AttributeVO;
import com.centurylink.mdw.model.value.attribute.CustomAttributeVO;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;
import com.centurylink.mdw.model.value.event.ExternalEventVO;
import com.centurylink.mdw.model.value.task.TaskVO;
import com.centurylink.mdw.model.value.variable.VariableVO;
import com.centurylink.mdw.osgi.BundleLocator;
import com.centurylink.mdw.osgi.BundleSpec;

public class PackageVO implements Serializable {

    public static final String DEFAULT_PACKAGE_NAME = "(default package)";
    public static final String BASELINE_PACKAGE_NAME = "MDW Baseline";
    public static final String MDW = "com.centurylink.mdw";
    public static final String MDW_HUB = MDW + ".hub";


    private static PackageVO defaultPackage = null;

    private Long packageId;
    private String packageName;
    private String packageDescription;
    private String voXML;
    private List<AttributeVO> attributes;
    private List<ActivityImplementorVO> implementors;
    private List<ExternalEventVO> externalEvents;
    private List<VariableVO> variables;
    private List<ProcessVO> processes;
    private List<PoolVO> pools;
    private List<RuleSetVO> rulesets;
    private List<TaskVO> taskTemplates;
    private int schemaVersion;
    private int version;
    private boolean exported;
    private Date modifyDate;
    private List<CustomAttributeVO> customAttributes;
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

    public PackageVO(){
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
	public List<AttributeVO> getAttributes() {
		return attributes;
	}

	/**
	 * @param attributes the attributes to set
	 */
	public void setAttributes(List<AttributeVO> attributes) {
		this.attributes = attributes;
	}

	public String getAttribute(String name) {
	    if (attributes != null) {
	        for (AttributeVO attr : attributes) {
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
	public List<ProcessVO> getProcesses() {
	    return this.processes;
	}

	/**
	 * @param processes the processes to set
	 */
	public void setProcesses(List<ProcessVO> pProcesses) {
	    this.processes = pProcesses;
	}

	public boolean containsProcess(Long processId) {
	    if (processes == null)
	      return false;

	    for (ProcessVO processVO : processes) {
	        if (processVO.getProcessId().equals(processId))
	            return true;
	    }
	    return false;
	}

    public boolean containsExternalEvent(Long externalEventId) {
        if (externalEvents == null)
            return false;

        for (ExternalEventVO externalEventVO : externalEvents) {
            if (externalEventVO.getId().equals(externalEventId))
                return true;
        }
        return false;
    }

    public boolean containsTaskTemplate(Long taskId) {
        if (taskTemplates == null)
            return false;

        for (TaskVO taskTemplate : taskTemplates) {
            if (taskTemplate.getTaskId().equals(taskId))
                return true;
        }
        return false;
    }

    public boolean containsActivityImpl(Long activityImplId) {
        if (implementors == null)
            return false;

        for (ActivityImplementorVO activityImplVO : implementors) {
            if (activityImplVO.getImplementorId().equals(activityImplId))
                return true;
        }
        return false;
    }

    public boolean containsActivityImpl(String implClass) {
        if (implementors == null)
            return false;

        for (ActivityImplementorVO activityImplVO : implementors) {
            if (activityImplVO.getImplementorClassName().equals(implClass))
                return true;
        }
        return false;
    }

    public boolean containsRuleSet(Long ruleSetId) {
        if (rulesets == null)
            return false;

        for (RuleSetVO ruleSetVO : rulesets) {
            if (ruleSetVO.getId().equals(ruleSetId))
                return true;
        }
        return false;
    }
    /**
	 * @return the variables
	 */
	public List<VariableVO> getVariables() {
		return variables;
	}

	/**
	 * @param variables the variables to set
	 */
	public void setVariables(List<VariableVO> variables) {
		this.variables = variables;
	}

    public List<ActivityImplementorVO> getImplementors() {
        return this.implementors;
    }
    public void setImplementors(List<ActivityImplementorVO> imps){
        this.implementors = imps;
    }

    public List<ExternalEventVO> getExternalEvents(){
        return this.externalEvents;
    }
    public void setExternalEvents(List<ExternalEventVO> externalEvents){
        this.externalEvents = externalEvents;
    }

    public List<TaskVO> getTaskTemplates(){
        return this.taskTemplates;
    }
    public void setTaskTemplates(List<TaskVO> taskTemplates){
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
      return getName() == null || getName().equals(PackageVO.DEFAULT_PACKAGE_NAME);
    }

    public void setExported(boolean exported) {
        this.exported = exported;
    }

    /**
     * Returns collection of pools associated with this package
     * @param
     * @return java.util.List<com.centurylink.mdw.model.value.process.PoolVO>
     */
    public List<PoolVO> getPools() {
        return pools;
    }

    /**
     * Sets collection of pools associated with this package
     * @param  java.util.List<com.centurylink.mdw.model.value.process.PoolVO>
     * @return
     */
    public void setPools(List<PoolVO> pools) {
        this.pools = pools;
    }

    public List<RuleSetVO> getRuleSets() {
        return rulesets;
    }

    public void setRuleSets(List<RuleSetVO> rulesets) {
        this.rulesets = rulesets;
    }

    public RuleSetVO getRuleSet(String name) {
        if (rulesets != null) {
            for (RuleSetVO ruleset : rulesets) {
                if (ruleset.getName().equals(name))
                    return ruleset;
            }
        }
        return null;
    }

    public List<LaneVO> getParticipants() {
        if (pools==null || pools.size()==0) return null;
        return pools.get(0).getLanes();
    }

    public void setParticipants(List<LaneVO> participants) {
        if (pools==null) pools = new ArrayList<PoolVO>(1);
        PoolVO pool;
        if (pools.size()==0) {
            pool = new PoolVO();
            pools.add(pool);
        } else pool = pools.get(0);
        pool.setLanes(participants);
    }

    public String getVoXML() {
        return voXML;
    }

    public void setVoXML(String voXML) {
        this.voXML = voXML;
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

    public List<CustomAttributeVO> getCustomAttributes() {
        return customAttributes;
    }

    public void setCustomAttributes(List<CustomAttributeVO> customAttrs) {
        this.customAttributes = customAttrs;
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
    		for (AttributeVO attr: attributes) {
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

    public static PackageVO getDefaultPackage() {
    	if (defaultPackage==null) {
    		defaultPackage = new PackageVO() {
    			@Override
    			public String getProperty(String propertyName) {
    				return PropertyManager.getProperty(propertyName);
    			}
    		};
    		// defaultPackage.setPackageName(DEFAULT_PACKAGE_NAME);
    		// should leave package name to null - other places assume it
        }
    	return defaultPackage;
    }

    /**
     * TODO : dynamic java classloader
     * @return the bundle classloader specified by the package config
     */
    public ClassLoader getClassLoader() {
    	if (classloader == null) {
    	    if (ApplicationContext.isOsgi()) {
                BundleSpec bundleSpec = getBundleSpec();
                if (bundleSpec != null)
        	        classloader = new BundleLocator(ApplicationContext.getOsgiBundleContext()).getClassLoader(bundleSpec);
                if (classloader == null) {
                    classloader = new BundleLocator(ApplicationContext.getOsgiBundleContext()).getMDWWorkflowClassLoader();
                }
    	    }
            if (classloader == null)
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

    public GeneralActivity getActivityImplementor(ActivityVO activity)
    throws InstantiationException, IllegalAccessException, ClassNotFoundException, IOException, MdwJavaException {
        // try dynamic java first (preferred in case patch override is needed)
        try {
            ClassLoader parentLoader = getCloudClassLoader();
            if (ApplicationContext.isOsgi())
                parentLoader = ProviderRegistry.getInstance().getMdwActivityProvider().getClass().getClassLoader();
            return (GeneralActivity) CompiledJavaCache.getInstance(activity.getImplementorClassName(), parentLoader, this);
        }
        catch (ClassNotFoundException ex) {
            // not located as dynamic java
        }
        String implClass = Compatibility.getActivityImplementor(activity.getImplementorClassName());
        if (ApplicationContext.isOsgi()) {
            String bsn = activity.getAttribute(WorkAttributeConstant.OSGI_BSN);
            return  ProviderRegistry.getInstance().getActivityInstance(this, implClass, bsn);
        }
        else if (ApplicationContext.isCloud()) {
            GeneralActivity injected = SpringAppContext.getInstance().getActivityImplementor(implClass, this);
            if (injected != null)
                return injected;
            if (getCloudClassLoader().hasClass(implClass))
              return getCloudClassLoader().loadClass(implClass).asSubclass(GeneralActivity.class).newInstance();
        }
        return getClassLoader().loadClass(implClass).asSubclass(GeneralActivity.class).newInstance();
    }

    public ExternalEventHandler getEventHandler(String classname, String content, Map<String,String> metaInfo)
    throws InstantiationException, IllegalAccessException, ClassNotFoundException, IOException, MdwJavaException {
        // try dynamic java first (preferred in case patch override is needed)
        try {
            ClassLoader parentLoader = getCloudClassLoader();
            if (ApplicationContext.isOsgi())
                parentLoader = ProviderRegistry.getInstance().getMdwEventHandlerProvider().getClass().getClassLoader();
            return (ExternalEventHandler) CompiledJavaCache.getInstance(classname, parentLoader, this);
        }
        catch (ClassNotFoundException ex) {
            // not located as dynamic java
        }
        String handlerClass = Compatibility.getEventHandler(classname);
        if (ApplicationContext.isOsgi()) {
            return ProviderRegistry.getInstance().getExternalEventHandlerInstance(handlerClass, content, metaInfo);
        }
        else if (ApplicationContext.isCloud()) {
            ExternalEventHandler injected = SpringAppContext.getInstance().getEventHandler(handlerClass, this);
            if (injected != null)
                return injected;
            if (getCloudClassLoader().hasClass(handlerClass))
              return getCloudClassLoader().loadClass(handlerClass).asSubclass(ExternalEventHandler.class).newInstance();
        }
    	return getClassLoader().loadClass(handlerClass).asSubclass(ExternalEventHandler.class).newInstance();
    }

    public FormAction getFormAction(String classname)
    throws InstantiationException, IllegalAccessException, ClassNotFoundException {
    	ClassLoader classloader = getClassLoader();
    	return classloader.loadClass(classname).asSubclass(FormAction.class).newInstance();
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

    private BundleSpec bundleSpec;
    /**
     * Return null if no BSN package config.
     */
    public BundleSpec getBundleSpec() {
        if (bundleSpec == null && ApplicationContext.isOsgi()) {
            String bsn = getProperty(WorkAttributeConstant.OSGI_BSN);
            if (bsn != null)
              bundleSpec = new BundleSpec(bsn, getProperty(WorkAttributeConstant.OSGI_BUNDLE_VERSION));
        }
        return bundleSpec;
    }

    @Override
    public String toString() {
        return getLabel();
    }
}
