/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.dataaccess.version5;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.xmlbeans.XmlException;

import com.centurylink.mdw.bpm.ApplicationPropertiesDocument.ApplicationProperties;
import com.centurylink.mdw.bpm.MDWActivity;
import com.centurylink.mdw.bpm.MDWActivityImplementor;
import com.centurylink.mdw.bpm.MDWAttribute;
import com.centurylink.mdw.bpm.MDWPackage;
import com.centurylink.mdw.bpm.MDWProcess;
import com.centurylink.mdw.bpm.MDWProcessDefinition;
import com.centurylink.mdw.bpm.MDWRuleSet;
import com.centurylink.mdw.bpm.MDWTransition;
import com.centurylink.mdw.bpm.PackageDocument;
import com.centurylink.mdw.bpm.ProcessDefinitionDocument;
import com.centurylink.mdw.common.Compatibility;
import com.centurylink.mdw.common.SchemaTypeTranslator;
import com.centurylink.mdw.common.constant.WorkAttributeConstant;
import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.dataaccess.DataAccess;
import com.centurylink.mdw.dataaccess.version4.ProcessImporterExporterV4;
import com.centurylink.mdw.model.value.activity.ActivityVO;
import com.centurylink.mdw.model.value.attribute.AttributeVO;
import com.centurylink.mdw.model.value.attribute.CustomAttributeVO;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;
import com.centurylink.mdw.model.value.process.PackageVO;
import com.centurylink.mdw.model.value.process.ProcessVO;
import com.centurylink.mdw.model.value.task.TaskVO;
import com.centurylink.mdw.model.value.work.WorkTransitionVO;
import com.centurylink.mdw.task.TaskTemplate;

/**
 * ProcessImporterExporterV5
 */
public class ProcessImporterExporterV5 extends ProcessImporterExporterV4 {


    public ProcessImporterExporterV5() {
        super();
    }

    public ProcessImporterExporterV5(SchemaTypeTranslator schemaTypeTranslator) {
        super(schemaTypeTranslator);
    }

    @Override
    public String exportPackages(List<PackageVO> packages, boolean includeTaskTemplates) throws DataAccessException, XmlException {
        PackageDocument pkgDoc = PackageDocument.Factory.newInstance();
        MDWPackage pkg = pkgDoc.addNewPackage();
        for (PackageVO pkgVo : packages) {
            preparePackageForExport(pkgVo);
            MDWProcessDefinition procDef = pkg.addNewProcessDefinition();
            mapPackage(procDef, pkgVo, includeTaskTemplates);
        }
        return pkgDoc.xmlText(getXmlOptions());
    }

    /**
     * RuleSet attributes purposely don't get exported since they are specified
     * on a per-environment basis.  This is the mechanism used by custom attributes
     * to allow end-users to set values dynamically.
     */
    @Override
    protected void exportRuleSets(MDWProcessDefinition procDefn, List<RuleSetVO> rulesets) {
    	for (RuleSetVO ruleset : rulesets) {
    		MDWRuleSet xmlruleset = procDefn.addNewRuleSet();
    		xmlruleset.setRuleSetName(ruleset.getName());
    		xmlruleset.setRuleSetLanguage(ruleset.getLanguage());
    		xmlruleset.setVersion(createVersionString(ruleset.getVersion()));
    		xmlruleset.setRuleSetContent(ruleset.getRuleSet());
    	}
    }

    protected void exportTaskTemplates(MDWProcessDefinition procDefn, List<TaskVO> taskVos) {
        for (TaskVO taskVo : taskVos) {
            if (taskVo.getVersion() > 0)
                procDefn.getTaskTemplateList().add(taskVo.toTemplate());
        }
    }

    @Override
    protected void importRuleSets(PackageVO packageVO, MDWProcessDefinition processDefn) {
    	List<RuleSetVO> rulesets = new ArrayList<RuleSetVO>();
    	packageVO.setRuleSets(rulesets);
    	for (MDWRuleSet xmlruleset : processDefn.getRuleSetList()) {
    		RuleSetVO ruleset = new RuleSetVO();
    		rulesets.add(ruleset);
    		ruleset.setName(xmlruleset.getRuleSetName());
    		ruleset.setVersion(RuleSetVO.parseVersion(xmlruleset.getVersion()));
    		ruleset.setLanguage(xmlruleset.getRuleSetLanguage());
    		ruleset.setRuleSet(xmlruleset.getRuleSetContent());
    	}
    }

    @Override
    protected void importTaskTemplates(PackageVO packageVO, MDWProcessDefinition processDefn) {
        List<TaskVO> taskVos = new ArrayList<TaskVO>();
        packageVO.setTaskTemplates(taskVos);
        for (TaskTemplate taskTemplate : processDefn.getTaskTemplateList()) {
            if (taskTemplate.getVersion() != null && !"0".equals(taskTemplate.getVersion())) {
                TaskVO taskVo = new TaskVO(taskTemplate);
                taskVos.add(taskVo);
            }
        }
    }

    @Override
    protected void exportPackageAttributes(MDWProcessDefinition procDefn, List<AttributeVO> packageAttrs) {
        for (AttributeVO attr : packageAttrs) {
            MDWAttribute defAttr = procDefn.addNewAttribute();
            defAttr.setName(attr.getAttributeName());
            defAttr.setValue(attr.getAttributeValue());
        }
    }

    @Override
    protected void exportCustomAttributes(MDWProcessDefinition procDefn, List<CustomAttributeVO> customAttrs) {
        for (CustomAttributeVO customAttr : customAttrs) {
            MDWAttribute defAttr = procDefn.addNewCustomAttribute();
            defAttr.setName(CustomAttributeVO.DEFINITION);
            defAttr.setValue(customAttr.getDefinition());
            defAttr.setOwner(customAttr.getDefinitionAttrOwner());
            MDWAttribute rolesAttr = procDefn.addNewCustomAttribute();
            rolesAttr.setName(CustomAttributeVO.ROLES);
            rolesAttr.setValue(customAttr.getRolesString());
            rolesAttr.setOwner(customAttr.getRolesAttrOwner());
        }
    }

    protected void importPackageAttributes(PackageVO packageVO, MDWProcessDefinition processDefn) {
        List<AttributeVO> attrs = new ArrayList<AttributeVO>();
        for (MDWAttribute defAttr : processDefn.getAttributeList()) {
            AttributeVO attr = new AttributeVO(defAttr.getName(), defAttr.getValue());
            attrs.add(attr);
        }
        packageVO.setAttributes(attrs);
    }

    @Override
    protected void importCustomAttributes(PackageVO packageVO, MDWProcessDefinition processDefn) {
        List<CustomAttributeVO> customAttrs = new ArrayList<CustomAttributeVO>();
        Map<String,CustomAttributeVO> customAttrMap = new HashMap<String,CustomAttributeVO>();
        for (MDWAttribute attribute : processDefn.getCustomAttributeList()) {
            String owner = attribute.getOwner();
            CustomAttributeVO customAttr = customAttrMap.get(owner);
            if (customAttr == null) {
                String ownerType = owner;
                String categorizer = null;
                int colon = owner.indexOf(':');
                if (colon > 0) {
                    ownerType = owner.substring(0, colon);
                    categorizer = owner.substring(colon + 1);
                }
                customAttr = new CustomAttributeVO(ownerType, categorizer);
                customAttrMap.put(owner, customAttr);
            }
            if (attribute.getName().equals(CustomAttributeVO.DEFINITION))
                customAttr.setDefinition(attribute.getValue());
            else if (attribute.getName().equals(CustomAttributeVO.ROLES))
                customAttr.setRolesString(attribute.getValue());
        }
        for (CustomAttributeVO customAttribute : customAttrMap.values()) {
            customAttrs.add(customAttribute);
        }
        packageVO.setCustomAttributes(customAttrs);
    }

	@Override
	protected void exportConfiguration(MDWProcessDefinition procDefn, PackageVO pkg) throws DataAccessException  {
		if (pkg.getVoXML()==null || pkg.getVoXML().length()==0) return;
        try {
            ApplicationProperties props = null;
            if (pkg.getVoXML().startsWith("<bpm:package") || pkg.getVoXML().startsWith("<package")) {
                PackageDocument pkgDefDoc = PackageDocument.Factory.parse(pkg.getVoXML());
                props = pkgDefDoc.getPackage().getApplicationProperties();
            }
            else {
                ProcessDefinitionDocument oldDefn = ProcessDefinitionDocument.Factory.parse(pkg.getVoXML(), Compatibility.namespaceOptions());
                props = oldDefn.getProcessDefinition().getApplicationProperties();
            }
            if (props != null) {
                procDefn.setApplicationProperties(props);
            }
        } catch (XmlException e) {
			throw new DataAccessException(-1, e.getMessage(), e);
		}
	}

	@Override
	protected void importConfiguration(PackageVO packageVO, MDWProcessDefinition def) {
		ApplicationProperties props = def.getApplicationProperties();
		if (props != null) {
		    if (packageVO.getSchemaVersion() >= DataAccess.schemaVersion55) {
		        PackageDocument pkgDefDoc = PackageDocument.Factory.newInstance();
		        MDWPackage pkgDef = pkgDefDoc.addNewPackage();
		        pkgDef.setApplicationProperties(props);
		        packageVO.setVoXML(pkgDefDoc.xmlText());
		    }
		    else {
		        ProcessDefinitionDocument defDoc = ProcessDefinitionDocument.Factory.newInstance();
	            MDWProcessDefinition configDefn = defDoc.addNewProcessDefinition();
	            configDefn.setApplicationProperties(props);
	            packageVO.setVoXML(defDoc.xmlText());
		    }
		}
	}

    public String exportOverrideAttributes(String attrPrefix, PackageVO packageVO) throws DataAccessException, XmlException {
        ProcessDefinitionDocument defnDoc = ProcessDefinitionDocument.Factory.newInstance();
        MDWProcessDefinition procDefn = defnDoc.addNewProcessDefinition();
        int mainProcessCounter = 1;
        for (ProcessVO processVO : packageVO.getProcesses()) {
            MDWProcess process = procDefn.addNewProcess();
            String exportedId = "MainProcess"+(mainProcessCounter++);
            mapProcessOverrideAttributes(processVO, process, attrPrefix, exportedId);
        }
        procDefn.setImplementorArray(new MDWActivityImplementor[0]);
        int schemaVersion = packageVO.getSchemaVersion();
        procDefn.setSchemaVersion(createVersionString(schemaVersion));
        procDefn.setPackageVersion(createPackageVersionString(packageVO.getVersion()));
        procDefn.setPackageWorkgroup(packageVO.getGroup());
        procDefn.setPackageName(packageVO.getPackageName());
        return defnDoc.xmlText(getXmlOptions());
    }

    public String exportOverrideAttributes(String attrPrefix, ProcessVO processVO, int schemaVersion) throws DataAccessException, XmlException {
        ProcessDefinitionDocument defnDoc = ProcessDefinitionDocument.Factory.newInstance();
        MDWProcessDefinition procDefn = defnDoc.addNewProcessDefinition();
        MDWProcess process = procDefn.addNewProcess();
        int mainProcessCounter = 1;
        String exportedId = "MainProcess"+(mainProcessCounter++);
        mapProcessOverrideAttributes(processVO, process, attrPrefix, exportedId);
        procDefn.setImplementorArray(new MDWActivityImplementor[0]);
        procDefn.setSchemaVersion(createVersionString(schemaVersion));
        procDefn.setPackageVersion("0.0");
        procDefn.setPackageName(processVO.getPackageName());
        return defnDoc.xmlText(getXmlOptions());
    }

    private void mapProcessOverrideAttributes(ProcessVO processVO, MDWProcess process, String attrPrefix, String exportedId){

        process.setId(exportedId);
        process.setName(processVO.getProcessName());
        process.setVersion(createVersionString(processVO.getVersion()));

        List<MDWAttribute> mdwAttrs = new ArrayList<MDWAttribute>(processVO.getAttributes().size());

        for (AttributeVO attr : processVO.getAttributes()) {
            if (WorkAttributeConstant.isAttrNameFor(attr.getAttributeName(), attrPrefix)) {
                MDWAttribute mdwAttr = MDWAttribute.Factory.newInstance();
                mdwAttr.setName(attr.getAttributeName());
                mdwAttr.setValue(attr.getAttributeValue());
                mdwAttrs.add(mdwAttr);
            }
        }

        process.setAttributeArray(mdwAttrs.toArray(new MDWAttribute[0]));

        List<ActivityVO> activities = processVO.getActivities();
        for(ActivityVO activityVo: activities){
            List<MDWAttribute> mdwActivityAttrs = new ArrayList<MDWAttribute>(activityVo.getAttributes().size());
            for(AttributeVO attr: activityVo.getAttributes()){
                if (WorkAttributeConstant.isAttrNameFor(attr.getAttributeName(), attrPrefix)) {
                    MDWAttribute mdwAttr = MDWAttribute.Factory.newInstance();
                    mdwAttr.setName(attr.getAttributeName());
                    mdwAttr.setValue(attr.getAttributeValue());
                    mdwActivityAttrs.add(mdwAttr);
                }
            }
            if(mdwActivityAttrs.size() > 0){
                MDWActivity activity = process.addNewActivity();
                activity.setId(activityVo.getLogicalId());
                activity.setAttributeArray(mdwActivityAttrs.toArray(new MDWAttribute[0]));
            }
        }


        for(WorkTransitionVO transitionVo: processVO.getTransitions()){
            List<MDWAttribute> mdwTransitionAttrs = new ArrayList<MDWAttribute>(transitionVo.getAttributes().size());
            for(AttributeVO attr: transitionVo.getAttributes()){
                if (WorkAttributeConstant.isAttrNameFor(attr.getAttributeName(), attrPrefix)) {
                    MDWAttribute mdwAttr = MDWAttribute.Factory.newInstance();
                    mdwAttr.setName(attr.getAttributeName());
                    mdwAttr.setValue(attr.getAttributeValue());
                    mdwTransitionAttrs.add(mdwAttr);
                }
            }
            if(mdwTransitionAttrs.size() > 0){
                MDWTransition transition = process.addNewTransition();
                transition.setId("Transition"+String.valueOf(transitionVo.getWorkTransitionId()));
                transition.setAttributeArray(mdwTransitionAttrs.toArray(new MDWAttribute[0]));
            }

        }

        int i=1;
        if(processVO.getSubProcesses() != null){
            for(ProcessVO subProcessVO: processVO.getSubProcesses()){
                MDWProcess subprocess = process.addNewSubProcess();
                mapProcessOverrideAttributes(subProcessVO, subprocess, attrPrefix, "SubProcess"+i++);
            }
        }

    }

    public ProcessVO importProcess(MDWProcess pProcess){
        ProcessVO processVO = importProcessBase(pProcess);
        importProcessUsingLogicalId(pProcess, processVO, false);
        return processVO;
    }
}
