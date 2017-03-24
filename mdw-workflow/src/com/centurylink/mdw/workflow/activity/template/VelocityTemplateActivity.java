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
package com.centurylink.mdw.workflow.activity.template;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.tools.view.XMLToolboxManager;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.cache.impl.AssetCache;
import com.centurylink.mdw.config.PropertyException;
import com.centurylink.mdw.model.asset.Asset;
import com.centurylink.mdw.model.variable.Variable;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.util.StringHelper;
import com.centurylink.mdw.util.file.FileHelper;
import com.centurylink.mdw.util.log.StandardLogger.LogLevel;
import com.centurylink.mdw.util.timer.Tracked;
import com.centurylink.mdw.workflow.activity.DefaultActivityImpl;

@Tracked(LogLevel.TRACE)
public class VelocityTemplateActivity extends DefaultActivityImpl {

    // attribute name constants
    private static final String VELOCITY_TEMPLATE_NAME = "Template Name";
    private static final String VELOCITY_OUTPUT = "velocityOutput";
    private static final String VELOCITY_TOOLBOX_FILE = "velocityToolboxFile";

    // filename for velocity.properties (should be available on classpath)
    private static final String VELOCITY_PROP_FILE = "velocity.properties";

    private static final String RULE = "Rule";
    private static final String LANGUAGE = "Script";

    private String templateName;
    private String velocityOutput;

    @Override
    public void execute() throws ActivityException {
        try {
            templateName = getAttributeValueSmart(VELOCITY_TEMPLATE_NAME);
            if (templateName == null)
              throw new ActivityException(-1, "Missing attribute: " + VELOCITY_TEMPLATE_NAME);

        }
        catch (PropertyException ex) {
            throw new ActivityException(-1, ex.getMessage(), ex);
        }

        mergeTemplate();

        String script = getAttributeValue(RULE);
        if (!StringHelper.isEmpty(script))
            executeScript(script);
    }

    private static VelocityEngine velocityEngine;
    private static VelocityEngine getVelocityEngine() throws Exception {
        if (velocityEngine == null) {
            velocityEngine = new VelocityEngine();

            Properties vProps = new Properties();

            if (FileHelper.fileExistsOnClasspath(VELOCITY_PROP_FILE)) {
                vProps.load(FileHelper.fileInputStreamFromClasspath(VELOCITY_PROP_FILE));
            }

            velocityEngine.init(vProps);
        }
        return velocityEngine;
    }

    protected void mergeTemplate() throws ActivityException {
        try {
            String temp = getAttributeValue(OUTPUTDOCS);
            setOutputDocuments(temp == null ? new String[0] : temp.split("#"));

            // get the template from the cache
            Asset templateVO = getTemplate(templateName);

            // Get the template text from the AssetVO Object
            String templateContent = templateVO.getStringContent();

            if (isLogDebugEnabled()) {
                logdebug(templateVO.getDescription() + " Contains:\n" + templateContent);
            }

            VelocityContext context = createVelocityContext();

            StringWriter writer = new StringWriter();
            try {
                if (getVelocityEngine().evaluate(context, writer, templateName, templateContent)) {
                    if (isLogDebugEnabled())
                        logdebug("Evaluation of Template was successful.");
                }
                else {
                    throw new ActivityException("Evaluation of Template was NOT successful.");
                }
            }
            catch (ParseErrorException pe) {
                logsevere("Velocity Parsing error in " + templateName + ":  " + pe.getMessage() + "\n" + pe.getInvalidSyntax());
                throw new ActivityException(-1, pe.getMessage(), pe);
            }


            if (isLogDebugEnabled()) {
                logdebug("\n***Results of Velocity Template Merge***\n" + writer.toString());
            }

            velocityOutput = writer.toString();
        }
        catch (Exception e) {
            throw new ActivityException(-1, e.getMessage(), e);
        }
    }

    protected Asset getTemplate(String name) throws ActivityException {
        Asset template = null;

        template = AssetCache.getAsset(name, Asset.VELOCITY);
        if (template == null)
          throw new ActivityException("Unable to load velocity template '" + name + "'");
        else
          return template;
    }

    protected void executeScript(String script) throws ActivityException {
        String language = getAttributeValue(LANGUAGE);
        if (language == null)
            language = GROOVY;

        Map<String,Object> addlBindings = getAdditionalScriptBindings();
        addlBindings.put("activity", this);
        Object retObj = executeScript(script, language, addlBindings);
        if (null != retObj) {
            setReturnCode(retObj.toString());
        }
    }

    /**
     * Creates the velocity context, adding process variables as parameters.
     */
    protected VelocityContext createVelocityContext() throws ActivityException {
        try {
            VelocityContext context = null;
            String toolboxFile = getAttributeValueSmart(VELOCITY_TOOLBOX_FILE);
            if (toolboxFile != null && FileHelper.fileExistsOnClasspath(toolboxFile)) {
                XMLToolboxManager toolboxManager = new XMLToolboxManager();
                toolboxManager.load(FileHelper.fileInputStreamFromClasspath(toolboxFile));
                context = new VelocityContext(toolboxManager.getToolbox(toolboxFile));
            }
            else {
                context = new VelocityContext();
            }

            Process processVO = getMainProcessDefinition();
            List<Variable> varVOs = processVO.getVariables();
            for (Variable variableVO : varVOs) {
              String variableName = variableVO.getVariableName();
              Object variableValue = getVariableValue(variableName);
              context.put(variableName, variableValue);
            }

            return context;
        }
        catch (Exception ex) {
            throw new ActivityException(-1, ex.getMessage(), ex);
        }
    }

    /**
     * Gets additional bindings for script execution, adding the velocity
     * output string as a special bind value.
     */
    protected Map<String,Object> getAdditionalScriptBindings() {
        Map<String,Object> addlBindings = new HashMap<String,Object>(1);
        addlBindings.put(VELOCITY_OUTPUT, velocityOutput);
        return addlBindings;
    }
}