/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.workflow.activity.transform;

import javax.xml.transform.TransformerException;

import org.codehaus.groovy.control.CompilerConfiguration;
import org.json.JSONObject;
import org.w3c.dom.Document;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.common.cache.impl.RuleSetCache;
import com.centurylink.mdw.common.translator.JsonTranslator;
import com.centurylink.mdw.common.translator.VariableTranslator;
import com.centurylink.mdw.common.translator.impl.DomDocumentTranslator;
import com.centurylink.mdw.model.value.activity.ActivityRuntimeContext;
import com.centurylink.mdw.model.value.attribute.AssetVersionSpec;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;
import com.centurylink.mdw.model.value.variable.VariableVO;
import com.centurylink.mdw.script.Builder;
import com.centurylink.mdw.script.CrossmapScript;
import com.centurylink.mdw.script.JsonBuilder;
import com.centurylink.mdw.script.JsonSlurper;
import com.centurylink.mdw.script.Slurper;
import com.centurylink.mdw.script.XmlBuilder;
import com.centurylink.mdw.script.XmlSlurper;
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
            RuleSetVO mapperScript = RuleSetCache.getRuleSet(spec);
            if (!RuleSetVO.GROOVY.equals(mapperScript.getLanguage()))
                throw new ActivityException("Unsupported mapper language: " + mapperScript.getLanguage());

            // input
            String inputAttr = getAttributeValueSmart(INPUT);
            if (inputAttr == null)
                throw new ActivityException("Missing attribute: " + INPUT);
            VariableVO inputVar = runtimeContext.getProcess().getVariable(inputAttr);
            if (inputVar == null)
                throw new ActivityException("Input variable not defined: " + inputAttr);
            com.centurylink.mdw.variable.VariableTranslator inputTrans
              = VariableTranslator.getTranslator(getPackage(), inputVar.getVariableType());
            Object inputObj = getVariableValue(inputAttr);
            if (inputObj == null)
                throw new ActivityException("Input variable is null: " + inputAttr);
            Slurper slurper;
            if (inputTrans instanceof JsonTranslator) {
                JSONObject input = ((JsonTranslator)inputTrans).toJson(inputObj);
                slurper = new JsonSlurper(inputVar.getName(), input.toString());
            }
            else if (inputTrans instanceof DomDocumentTranslator) {
                Document input = ((DomDocumentTranslator)inputTrans).toDomDocument(inputObj);
                slurper = new XmlSlurper(inputVar.getName(), DomHelper.toXml((Document)input));
            }
            else {
                throw new ActivityException("Unsupported input variable type: " + inputVar.getVariableType());
            }

            // output
            String outputAttr = getAttributeValueSmart(OUTPUT);
            if (outputAttr == null)
                throw new ActivityException("Missing attribute: " + OUTPUT);
            VariableVO outputVar = runtimeContext.getProcess().getVariable(outputAttr);
            if (outputVar == null)
                throw new ActivityException("Output variable not defined: " + outputVar);
            com.centurylink.mdw.variable.VariableTranslator outputTrans
                = VariableTranslator.getTranslator(getPackage(), outputVar.getVariableType());
            Builder builder;
            if (outputTrans instanceof JsonTranslator)
                builder = new JsonBuilder(outputVar.getName());
            else if (outputTrans instanceof DomDocumentTranslator)
                builder = new XmlBuilder(outputVar.getName());
            else
                throw new ActivityException("Unsupported output variable type: " + outputVar.getVariableType());

            runScript(mapperScript.getRuleSet(), slurper, builder);

            if (outputTrans instanceof JsonTranslator) {
                Object output = ((JsonTranslator)outputTrans).fromJson(new JSONObject(builder.getString()));
                setVariableValue(outputVar.getName(), output);
            }
            else if (outputTrans instanceof DomDocumentTranslator) {
                Object output = ((DomDocumentTranslator)outputTrans).fromDomNode(DomHelper.toDomDocument(builder.getString()));
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
