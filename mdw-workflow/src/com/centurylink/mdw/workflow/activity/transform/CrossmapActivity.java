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
package com.centurylink.mdw.workflow.activity.transform;

import javax.xml.transform.TransformerException;

import org.codehaus.groovy.control.CompilerConfiguration;
import org.json.JSONObject;
import org.w3c.dom.Document;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.cache.impl.AssetCache;
import com.centurylink.mdw.common.translator.impl.DomDocumentTranslator;
import com.centurylink.mdw.model.JsonObject;
import com.centurylink.mdw.model.asset.Asset;
import com.centurylink.mdw.model.asset.AssetVersionSpec;
import com.centurylink.mdw.model.variable.Variable;
import com.centurylink.mdw.model.workflow.ActivityRuntimeContext;
import com.centurylink.mdw.script.Builder;
import com.centurylink.mdw.script.CrossmapScript;
import com.centurylink.mdw.script.JsonBuilder;
import com.centurylink.mdw.script.JsonSlurper;
import com.centurylink.mdw.script.Slurper;
import com.centurylink.mdw.script.XmlBuilder;
import com.centurylink.mdw.script.XmlSlurper;
import com.centurylink.mdw.translator.JsonTranslator;
import com.centurylink.mdw.translator.VariableTranslator;
import com.centurylink.mdw.workflow.activity.DefaultActivityImpl;
import com.centurylink.mdw.xml.DomHelper;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;

public class CrossmapActivity extends DefaultActivityImpl {

    protected static final String MAPPER = "Mapper";
    protected static final String MAPPER_VERSION = "Mapper_assetVersion";
    protected static final String INPUT = "Input";
    protected static final String OUTPUT = "Output";

    @Override
    public Object execute(ActivityRuntimeContext runtimeContext) throws ActivityException {
        try {
            String mapper = getAttributeValueSmart(MAPPER);
            if (mapper == null)
                throw new ActivityException("Missing attribute: " + MAPPER);
            String mapperVer = getAttributeValueSmart(MAPPER_VERSION);
            AssetVersionSpec spec = new AssetVersionSpec(mapper, mapperVer == null ? "0" : mapperVer);
            Asset mapperScript = AssetCache.getAsset(spec);
            if (mapperScript == null)
                throw new ActivityException("Cannot load mapping script: " + spec);
            if (!Asset.GROOVY.equals(mapperScript.getLanguage()))
                throw new ActivityException("Unsupported mapper language: " + mapperScript.getLanguage());

            // input
            String inputAttr = getAttributeValueSmart(INPUT);
            if (inputAttr == null)
                throw new ActivityException("Missing attribute: " + INPUT);
            Variable inputVar = runtimeContext.getProcess().getVariable(inputAttr);
            if (inputVar == null)
                throw new ActivityException("Input variable not defined: " + inputAttr);
            com.centurylink.mdw.variable.VariableTranslator inputTrans
              = VariableTranslator.getTranslator(getPackage(), inputVar.getVariableType());
            Object inputObj = getVariableValue(inputAttr);
            if (inputObj == null)
                throw new ActivityException("Input variable is null: " + inputAttr);
            Slurper slurper;
            // XML is always tried first since XML can now be represented as JSON
            if (inputTrans instanceof DomDocumentTranslator) {
                Document input = ((DomDocumentTranslator)inputTrans).toDomDocument(inputObj);
                slurper = new XmlSlurper(inputVar.getName(), DomHelper.toXml((Document)input));
            }
            else if (inputTrans instanceof JsonTranslator) {
                JSONObject input = ((JsonTranslator)inputTrans).toJson(inputObj);
                slurper = new JsonSlurper(inputVar.getName(), input.toString());
            }
            else {
                throw new ActivityException("Unsupported input variable type: " + inputVar.getVariableType());
            }

            // output
            String outputAttr = getAttributeValueSmart(OUTPUT);
            if (outputAttr == null)
                throw new ActivityException("Missing attribute: " + OUTPUT);
            Variable outputVar = runtimeContext.getProcess().getVariable(outputAttr);
            if (outputVar == null)
                throw new ActivityException("Output variable not defined: " + outputVar);
            com.centurylink.mdw.variable.VariableTranslator outputTrans
                = VariableTranslator.getTranslator(getPackage(), outputVar.getVariableType());
            Builder builder;
            if (outputTrans instanceof DomDocumentTranslator)
                builder = new XmlBuilder(outputVar.getName());
            else if (outputTrans instanceof JsonTranslator)
                builder = new JsonBuilder(outputVar.getName());
            else
                throw new ActivityException("Unsupported output variable type: " + outputVar.getVariableType());

            runScript(mapperScript.getStringContent(), slurper, builder);

            if (outputTrans instanceof DomDocumentTranslator) {
                Object output = ((DomDocumentTranslator)outputTrans).fromDomNode(DomHelper.toDomDocument(builder.getString()));
                setVariableValue(outputVar.getName(), output);
            }
            else if (outputTrans instanceof JsonTranslator) {
                Object output = ((JsonTranslator)outputTrans).fromJson(new JsonObject(builder.getString()));
                setVariableValue(outputVar.getName(), output);
            }

            return null;
        }
        catch (Exception ex) {
            throw new ActivityException(ex.getMessage(), ex);
        }
    }

    /**
     * Returns the builder object for creating new output variable value.
     */
    protected void runScript(String mapperScript, Slurper slurper, Builder builder)
            throws ActivityException, TransformerException {

        CompilerConfiguration compilerConfig = new CompilerConfiguration();
        compilerConfig.setScriptBaseClass(CrossmapScript.class.getName());

        Binding binding = new Binding();
        binding.setVariable("runtimeContext", getRuntimeContext());
        binding.setVariable(slurper.getName(), slurper.getInput());
        binding.setVariable(builder.getName(), builder);
        GroovyShell shell = new GroovyShell(getPackage().getCloudClassLoader(), binding, compilerConfig);
        Script gScript = shell.parse(mapperScript);
        // gScript.setProperty("out", getRuntimeContext().get);
        gScript.run();
    }

}
