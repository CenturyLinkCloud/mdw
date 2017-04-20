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
package com.centurylink.mdw.drools;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.kie.internal.command.CommandFactory;
import org.kie.api.KieBase;
import org.kie.api.runtime.StatelessKieSession;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.activity.types.RuleActivity;
import com.centurylink.mdw.cache.impl.PackageCache;
import com.centurylink.mdw.config.PropertyException;
import com.centurylink.mdw.model.asset.AssetVersionSpec;
import com.centurylink.mdw.model.variable.Variable;
import com.centurylink.mdw.model.variable.VariableInstance;
import com.centurylink.mdw.model.workflow.Package;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.util.StringHelper;
import com.centurylink.mdw.util.log.StandardLogger.LogLevel;
import com.centurylink.mdw.util.timer.Tracked;
import com.centurylink.mdw.workflow.activity.DefaultActivityImpl;

@Tracked(LogLevel.TRACE)
public class DroolsActivity extends DefaultActivityImpl implements RuleActivity {

    public static final String KNOWLEDGE_BASE = "KnowledgeBase";
    public static final String KNOWLEDGE_BASE_ASSET_VERSION = "KnowledgeBase_assetVersion";
    public static final String RULE_VERSION_VAR = "RuleVersionVar";


    @Override
    @SuppressWarnings("unchecked")
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

        KieBase knowledgeBase = getKnowledgeBase(knowledgeBaseName, knowledgeBaseVersion);
        if (knowledgeBase == null)
            throw new ActivityException("Cannot load KnowledgeBase: " + knowledgeBaseName);

        // TODO stateful session option
        StatelessKieSession kSession = knowledgeBase.newStatelessKieSession();

        List<Object> facts = new ArrayList<Object>();
        Map<String,Object> values = new HashMap<String,Object>();

        for (VariableInstance variableInstance : getParameters()) {
            Object value = getVariableValue(variableInstance.getName());
            values.put(variableInstance.getName(), value);
        }
        facts.add(values);

        setGlobalValues(kSession);

        kSession.execute(CommandFactory.newInsertElements(facts));

        String temp = getAttributeValue(OUTPUTDOCS);
        setOutputDocuments(temp == null ? new String[0] : temp.split("#"));

        // TODO handle document variables
        Process processVO = getProcessDefinition();
        for (Variable variable : processVO.getVariables()) {
            Object newValue = values.get(variable.getVariableName());
            if (newValue != null)
                setVariableValue(variable.getVariableName(), variable.getVariableType(), newValue);
        }
    }


    /**
     * Get knowledge base with no modifiers.
     */
    protected KieBase getKnowledgeBase(String name, String version) throws ActivityException {
        return getKnowledgeBase(name, version, null);
    }

    /**
     * Returns the asset based on specified version/range whose attributes match the custom attribute
     * Override to apply additional or non-standard conditions.
     */
    protected KieBase getKnowledgeBase(String name, String assetVersion, String modifier) throws ActivityException {
        Map<String,String> customAttrs = null;
        KnowledgeBaseAsset kbrs;
        if (assetVersion == null)
            kbrs = DroolsKnowledgeBaseCache.getKnowledgeBaseAsset(name, modifier, customAttrs, getClassLoader());
        else
            kbrs = DroolsKnowledgeBaseCache.getKnowledgeBaseAsset(new AssetVersionSpec(name, assetVersion), modifier, customAttrs, getClassLoader());

        if (kbrs == null) {
            return null;
        }
        else {
            super.loginfo("Using Knowledge Base: " + kbrs.getAsset().getLabel());

            String versionLabelVarName = getAttributeValue(RULE_VERSION_VAR);
            if (versionLabelVarName != null)
                setParameterValue(versionLabelVarName, kbrs.getAsset().getLabel());
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
        Package pkg = PackageCache.getProcessPackage(getProcessId());
        if (pkg != null) {
            loader = pkg.getCloudClassLoader();
        }
        if (loader == null) {
            loader = getClass().getClassLoader();
        }
        return loader;
    }

    protected void setGlobalValues(StatelessKieSession knowledgeSession) throws ActivityException {
        knowledgeSession.setGlobal("activity", this); // TODO deprecate
        knowledgeSession.setGlobal("runtimeContext", getRuntimeContext());
        knowledgeSession.setGlobal("now", new Date());
    }

    // expose this publicly
    public void setReturnCode(String code) {
        super.setReturnCode(code);
    }

}
