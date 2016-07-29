/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.workflow.activity.rules;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.drools.KnowledgeBase;
import org.drools.command.CommandFactory;
import org.drools.runtime.StatelessKnowledgeSession;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.activity.types.RuleActivity;
import com.centurylink.mdw.common.cache.impl.PackageVOCache;
import com.centurylink.mdw.common.exception.PropertyException;
import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.common.utilities.logger.StandardLogger.LogLevel;
import com.centurylink.mdw.common.utilities.timer.Tracked;
import com.centurylink.mdw.model.value.attribute.AssetVersionSpec;
import com.centurylink.mdw.model.value.process.PackageVO;
import com.centurylink.mdw.model.value.process.ProcessVO;
import com.centurylink.mdw.model.value.variable.VariableInstanceInfo;
import com.centurylink.mdw.model.value.variable.VariableVO;
import com.centurylink.mdw.workflow.activity.DefaultActivityImpl;
import com.centurylink.mdw.workflow.drools.cache.DroolsKnowledgeBaseCache;
import com.centurylink.mdw.workflow.drools.cache.KnowledgeBaseRuleSet;

@Tracked(LogLevel.TRACE)
public class DroolsActivity extends DefaultActivityImpl implements RuleActivity {

    public static final String KNOWLEDGE_BASE = "KnowledgeBase";
    public static final String KNOWLEDGE_BASE_ASSET_VERSION = "KnowledgeBase_assetVersion";
    public static final String CUSTOM_ATTRIBUTES = "CustomAttributes";
    public static final String RULE_VERSION_VAR = "RuleVersionVar";


    @Override
    public void execute() throws ActivityException {

        String knowledgeBaseName = null;
        String knowledgeBaseVersion = null;
        try {
            knowledgeBaseName = getAttributeValueSmart(KNOWLEDGE_BASE);
            knowledgeBaseVersion = getAttributeValueSmart(KNOWLEDGE_BASE_ASSET_VERSION);
        }
        catch (PropertyException ex) {
            throw new ActivityException(ex.getMessage(), ex);
        }

        if (StringHelper.isEmpty(knowledgeBaseName))
            throw new ActivityException("Missing attribute: " + KNOWLEDGE_BASE);

        KnowledgeBase knowledgeBase = getKnowledgeBase(knowledgeBaseName, knowledgeBaseVersion);
        if (knowledgeBase == null)
            throw new ActivityException("Cannot load KnowledgeBase: " + knowledgeBaseName);

        // TODO stateful session option
        StatelessKnowledgeSession knowledgeSession = knowledgeBase.newStatelessKnowledgeSession();

        List<Object> facts = new ArrayList<Object>();
        Map<String,Object> values = new HashMap<String,Object>();

        for (VariableInstanceInfo variableInstance : getParameters()) {
            Object value = getVariableValue(variableInstance.getName());
            values.put(variableInstance.getName(), value);
        }
        facts.add(values);

        setGlobalValues(knowledgeSession);

        knowledgeSession.execute(CommandFactory.newInsertElements(facts));

        String temp = getAttributeValue(OUTPUTDOCS);
        setOutputDocuments(temp == null ? new String[0] : temp.split("#"));

        // TODO handle document variables
        ProcessVO processVO = getProcessDefinition();
        for (VariableVO variable : processVO.getVariables()) {
            Object newValue = values.get(variable.getVariableName());
            if (newValue != null)
                setVariableValue(variable.getVariableName(), variable.getVariableType(), newValue);
        }
    }


    /**
     * Get knowledge base with no modifiers.
     */
    protected KnowledgeBase getKnowledgeBase(String name, String version) throws ActivityException {
        return getKnowledgeBase(name, version, null);
    }


    /**
     * Returns the latest version whose attributes match the custom attribute
     * criteria specified via "CustomAttributes".
     * Override to apply additional or non-standard conditions.
     */
   /* protected KnowledgeBase getKnowledgeBase(String name, String modifier) throws ActivityException {
        Map<String,String> customAttrs = null;
        String customAttrString = getAttributeValue(CUSTOM_ATTRIBUTES);
        if (!StringHelper.isEmpty(customAttrString)) {
            customAttrs = StringHelper.parseMap(customAttrString);
        }

        KnowledgeBaseRuleSet kbrs = DroolsKnowledgeBaseCache.getKnowledgeBaseRuleSet(name, modifier, customAttrs);

        if (kbrs == null) {
            return null;
        }
        else {
            super.loginfo("Using Knowledge Base: " + kbrs.getRuleSet().getLabel());

            String versionLabelVarName = getAttributeValue(RULE_VERSION_VAR);
            if (versionLabelVarName != null)
                setParameterValue(versionLabelVarName, kbrs.getRuleSet().getLabel());
            return kbrs.getKnowledgeBase();
        }
    }*/

    /**
     * Returns the asset based on specified version/range whose attributes match the custom attribute
     * Returns the latest version whose attributes match the custom attribute when you dont specify version/range
     * criteria specified via "CustomAttributes".
     * Override to apply additional or non-standard conditions.
     */
    protected KnowledgeBase getKnowledgeBase(String name, String assetVersion, String modifier) throws ActivityException {
        Map<String,String> customAttrs = null;
        KnowledgeBaseRuleSet kbrs;
        String customAttrString = getAttributeValue(CUSTOM_ATTRIBUTES);
        if (!StringHelper.isEmpty(customAttrString)) {
            customAttrs = StringHelper.parseMap(customAttrString);
        }

        if (assetVersion == null)
            kbrs = DroolsKnowledgeBaseCache.getKnowledgeBaseRuleSet(name, modifier, customAttrs, getClassLoader());
        else
            kbrs = DroolsKnowledgeBaseCache.getKnowledgeBaseRuleSet(new AssetVersionSpec(name, assetVersion), modifier, customAttrs, getClassLoader());

        if (kbrs == null) {
            return null;
        }
        else {
            super.loginfo("Using Knowledge Base: " + kbrs.getRuleSet().getLabel());

            String versionLabelVarName = getAttributeValue(RULE_VERSION_VAR);
            if (versionLabelVarName != null)
                setParameterValue(versionLabelVarName, kbrs.getRuleSet().getLabel());
            return kbrs.getKnowledgeBase();
        }
    }

    /**
     * Get the class loader based on package bundle version spec
     * default is mdw-workflow loader
     * @return
     */
    protected ClassLoader getClassLoader() {
        ClassLoader loader = null;
        PackageVO pkg =PackageVOCache.getProcessPackage(getProcessId());
        if (pkg != null) {
            loader = pkg.getClassLoader();
        }
        if (loader == null) {
            loader = getClass().getClassLoader();
        }
        return loader;
    }

    protected void setGlobalValues(StatelessKnowledgeSession knowledgeSession) throws ActivityException {
        knowledgeSession.setGlobal("activity", this); // TODO deprecate
        knowledgeSession.setGlobal("runtimeContext", getRuntimeContext());
        knowledgeSession.setGlobal("now", new Date());
    }

    // expose this publicly
    public void setReturnCode(String code) {
        super.setReturnCode(code);
    }

}
