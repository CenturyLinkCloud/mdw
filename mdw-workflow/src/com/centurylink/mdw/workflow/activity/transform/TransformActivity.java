/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.workflow.activity.transform;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import groovy.util.Node;
import groovy.util.XmlNodePrinter;
import groovy.util.XmlParser;
import groovy.util.XmlSlurper;
import groovy.xml.MarkupBuilder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.model.variable.Variable;
import com.centurylink.mdw.translator.DocumentReferenceTranslator;
import com.centurylink.mdw.translator.VariableTranslator;
import com.centurylink.mdw.util.StringHelper;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import com.centurylink.mdw.util.log.StandardLogger.LogLevel;
import com.centurylink.mdw.util.timer.Tracked;
import com.centurylink.mdw.workflow.activity.script.ScriptExecutorActivity;

/**
 * Transforms an input xml document into an output document.
 */
@Tracked(LogLevel.TRACE)
public class TransformActivity extends ScriptExecutorActivity {
    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    public static final String TRANSFORM_LANGUAGE = "Transform Language";
    public static final String INPUT_DOCUMENTS = "Input Documents";
    public static final String GPATH = "GPath";
    public static final String XSLT = "XSLT";

    private String transformLanguage;
    private String inputDocument;

    private StringWriter outputDocumentWriter;

    @Override
    public void execute() throws ActivityException {
        try {
            transformLanguage = getAttributeValue(SCRIPT_LANGUAGE);  // backward compatibility
            if (transformLanguage == null || (!transformLanguage.equals("GPath") && !transformLanguage.equals("XSLT"))) {
                transformLanguage = getAttributeValue(TRANSFORM_LANGUAGE);
            }


            if (StringHelper.isEmpty(transformLanguage)) {
                throw new ActivityException("Transform language has not been specified.");
            }
            inputDocument = getAttributeValue(INPUT_DOCUMENTS);
            if (StringHelper.isEmpty(inputDocument)) {
                throw new ActivityException("Input document has not been specified.");
            }
            String outputDocument = getAttributeValue(OUTPUTDOCS);
            if (StringHelper.isEmpty(outputDocument)) {
                throw new ActivityException("Output document has not been specified.");
            }
            setOutputDocuments(new String[]{outputDocument});

            if (transformLanguage.equals(GPATH)) {
                executeGPath();
            }
            else if (transformLanguage.equals(XSLT)) {
                executeXSLT();
            }
        }
        catch (ActivityException ex) {
            logger.severeException(ex.getMessage(), ex);
            throw ex;
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            throw new ActivityException(ex.getMessage(), ex);
        }
    }

    private void executeGPath() throws ActivityException {
        String transform = (String) getAttributeValue(RULE);
        if (StringHelper.isEmpty(transform)) {
            logger.info("No transform defined for activity: " + getActivityName());
            return;
        }

        GroovyShell shell = new GroovyShell(getClass().getClassLoader());
        Script script = shell.parse(transform);
        Binding binding = new Binding();

        Variable inputVar = getMainProcessDefinition().getVariable(inputDocument);
        if (inputVar == null)
          throw new ActivityException("Input document variable not found: " + inputDocument);
        String inputVarName = inputVar.getVariableName();
        Object inputVarValue = getGPathParamValue(inputVarName, inputVar.getVariableType());
        binding.setVariable(inputVarName, inputVarValue);

        Variable outputVar = getMainProcessDefinition().getVariable(getOutputDocuments()[0]);
        String outputVarName = outputVar.getVariableName();
        Object outputVarValue = getGPathParamValue(outputVarName, outputVar.getVariableType());
        binding.setVariable(outputVarName, outputVarValue);

        script.setBinding(binding);

        script.run();

        Object groovyVarValue = binding.getVariable(outputVarName);
        setGPathParamValue(outputVarName, outputVar.getVariableType(), groovyVarValue);
    }

    private void executeXSLT() throws ActivityException {
        String transform = (String) getAttributeValue(RULE);
        if (StringHelper.isEmpty(transform)) {
            logger.info("No transform defined for activity: " + getActivityName());
            return;
        }

        String output = null;

        Variable inputVar = getMainProcessDefinition().getVariable(inputDocument);
        if (inputVar == null)
          throw new ActivityException("Input document variable not found: " + inputDocument);
        String inputVarName = inputVar.getVariableName();
        String inputVarType = inputVar.getVariableType();
        com.centurylink.mdw.variable.VariableTranslator inTranslator = VariableTranslator.getTranslator(getPackage(), inputVarType);
        if (inTranslator instanceof DocumentReferenceTranslator) {
            DocumentReferenceTranslator docRefTranslator = (DocumentReferenceTranslator) inTranslator;
            String input = docRefTranslator.realToString(getVariableValue(inputVarName));
            output = transform(input, transform);
        }
        else {
            throw new ActivityException("Input does not appear to be a document: " + inputVarName);
        }

        Variable outputVar = getMainProcessDefinition().getVariable(getOutputDocuments()[0]);
        String outputVarName = outputVar.getVariableName();
        String outputVarType = outputVar.getVariableType();
        com.centurylink.mdw.variable.VariableTranslator outTranslator = VariableTranslator.getTranslator(getPackage(), outputVarType);
        if (!isOutputDocument(outputVarName))
          throw new ActivityException("Output document is not writable: " + outputVarName);
        if (outTranslator instanceof DocumentReferenceTranslator) {
            DocumentReferenceTranslator docRefTranslator = (DocumentReferenceTranslator) outTranslator;
            Object doc = docRefTranslator.realToObject(output);
            setVariableValue(outputVarName, outputVarType, doc);
        }
        else {
          throw new ActivityException("Output does not appear to be a document: " + outputVarName);
        }
    }

    private Object getGPathParamValue(String varName, String varType) throws ActivityException {
        Object value = super.getVariableValue(varName);
        com.centurylink.mdw.variable.VariableTranslator translator = VariableTranslator.getTranslator(getPackage(), varType);
        if (translator instanceof DocumentReferenceTranslator) {
            try {
                DocumentReferenceTranslator docRefTranslator = (DocumentReferenceTranslator) translator;
                outputDocumentWriter = new StringWriter();
                if (isOutputDocument(varName)) {
                    if (value == null) {
                        MarkupBuilder builder = new MarkupBuilder(outputDocumentWriter);
                        builder.setDoubleQuotes(true);
                        value = builder;
                    }
                    else {
                        value = new XmlParser().parseText(docRefTranslator.realToString(value));
                    }
                }
                else {
                    value = new XmlSlurper().parseText(docRefTranslator.realToString(value));
                }
            }
            catch (Exception ex) {
                logger.severeException(ex.getMessage(), ex);
                throw new ActivityException(ex.getMessage(), ex);
            }
        }

        return value;
    }

    private void setGPathParamValue(String varName, String varType, Object value) throws ActivityException {
        if (isOutputDocument(varName)) {
            com.centurylink.mdw.variable.VariableTranslator translator = VariableTranslator.getTranslator(getPackage(), varType);
            DocumentReferenceTranslator docRefTranslator = (DocumentReferenceTranslator) translator;
            try {
                if (value instanceof Node)
                  new XmlNodePrinter(new PrintWriter(outputDocumentWriter)).print((Node)value);
                Object doc = docRefTranslator.realToObject(outputDocumentWriter.toString());
                super.setVariableValue(varName, varType, doc);
            }
            catch (Exception ex) {
                logger.severeException(ex.getMessage(), ex);
                throw new ActivityException(ex.getMessage(), ex);
            }
        }
        else {
            super.setVariableValue(varName, varType, value);
        }
    }

    private String transform(String xml, String xsl) {
        try {
            TransformerFactory tFactory = TransformerFactory.newInstance();

            Source xslSource = new StreamSource(new ByteArrayInputStream(xsl.getBytes()));
            Transformer transformer = tFactory.newTransformer(xslSource);

            Source xmlSource = new StreamSource(new ByteArrayInputStream(xml.getBytes()));
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            transformer.transform(xmlSource, new StreamResult(outputStream));

            return new String(outputStream.toByteArray());

        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
