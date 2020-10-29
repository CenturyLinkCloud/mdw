package com.centurylink.mdw.workflow.activity.transform;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.activity.types.ScriptActivity;
import com.centurylink.mdw.annotations.Activity;
import com.centurylink.mdw.model.variable.Variable;
import com.centurylink.mdw.translator.DocumentReferenceTranslator;
import com.centurylink.mdw.util.log.StandardLogger.LogLevel;
import com.centurylink.mdw.util.timer.Tracked;
import com.centurylink.mdw.variable.VariableTranslator;
import com.centurylink.mdw.workflow.activity.script.ScriptExecutorActivity;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import groovy.util.Node;
import groovy.util.XmlNodePrinter;
import groovy.util.XmlParser;
import groovy.util.XmlSlurper;
import groovy.xml.MarkupBuilder;
import org.apache.commons.lang.StringUtils;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Transforms an input xml document into an output document.
 */
@Tracked(LogLevel.TRACE)
@Activity(value="XML Transform", category=ScriptActivity.class, icon="com.centurylink.mdw.base/xml.jpg",
        pagelet="com.centurylink.mdw.base/transform.pagelet")
public class TransformActivity extends ScriptExecutorActivity {
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


            if (StringUtils.isBlank(transformLanguage)) {
                throw new ActivityException("Transform language has not been specified.");
            }
            inputDocument = getAttributeValue(INPUT_DOCUMENTS);
            if (StringUtils.isBlank(inputDocument)) {
                throw new ActivityException("Input document has not been specified.");
            }
            String outputDocument = getAttributeValue(OUTPUTDOCS);
            if (StringUtils.isBlank(outputDocument)) {
                throw new ActivityException("Output document has not been specified.");
            }
            setOutputDocuments(getAttributes().getList(OUTPUTDOCS).toArray(new String[0]));

            if (transformLanguage.equals(GPATH)) {
                executeGPath();
            }
            else if (transformLanguage.equals(XSLT)) {
                executeXSLT();
            }
        }
        catch (ActivityException ex) {
            getLogger().error(ex.getMessage(), ex);
            throw ex;
        }
        catch (Exception ex) {
            getLogger().error(ex.getMessage(), ex);
            throw new ActivityException(ex.getMessage(), ex);
        }
    }

    private void executeGPath() throws ActivityException {
        String transform = (String) getAttributeValue(RULE);
        if (StringUtils.isBlank(transform)) {
            getLogger().info("No transform defined for activity: " + getActivityName());
            return;
        }

        GroovyShell shell = new GroovyShell(getClass().getClassLoader());
        Script script = shell.parse(transform);
        Binding binding = new Binding();

        Variable inputVar = getMainProcessDefinition().getVariable(inputDocument);
        if (inputVar == null)
          throw new ActivityException("Input document variable not found: " + inputDocument);
        String inputVarName = inputVar.getName();
        Object inputVarValue = getGPathParamValue(inputVarName, inputVar.getType());
        binding.setVariable(inputVarName, inputVarValue);

        Variable outputVar = getMainProcessDefinition().getVariable(getOutputDocuments()[0]);
        String outputVarName = outputVar.getName();
        Object outputVarValue = getGPathParamValue(outputVarName, outputVar.getType());
        binding.setVariable(outputVarName, outputVarValue);

        script.setBinding(binding);

        script.run();

        Object groovyVarValue = binding.getVariable(outputVarName);
        setGPathParamValue(outputVarName, outputVar.getType(), groovyVarValue);
    }

    private void executeXSLT() throws ActivityException {
        String transform = (String) getAttributeValue(RULE);
        if (StringUtils.isBlank(transform)) {
            getLogger().info("No transform defined for activity: " + getActivityName());
            return;
        }

        String output = null;

        Variable inputVar = getMainProcessDefinition().getVariable(inputDocument);
        if (inputVar == null)
          throw new ActivityException("Input document variable not found: " + inputDocument);
        String inputVarName = inputVar.getName();
        String inputVarType = inputVar.getType();
        VariableTranslator inTranslator = getPackage().getTranslator(inputVarType);
        if (inTranslator instanceof DocumentReferenceTranslator) {
            DocumentReferenceTranslator docRefTranslator = (DocumentReferenceTranslator) inTranslator;
            String input = docRefTranslator.toString(getValue(inputVarName), inputVarType);
            output = transform(input, transform);
        }
        else {
            throw new ActivityException("Input does not appear to be a document: " + inputVarName);
        }

        Variable outputVar = getMainProcessDefinition().getVariable(getOutputDocuments()[0]);
        String outputVarName = outputVar.getName();
        String outputVarType = outputVar.getType();
        VariableTranslator outTranslator = getPackage().getTranslator(outputVarType);
        if (!isOutputDocument(outputVarName))
          throw new ActivityException("Output document is not writable: " + outputVarName);
        if (outTranslator instanceof DocumentReferenceTranslator) {
            DocumentReferenceTranslator docRefTranslator = (DocumentReferenceTranslator) outTranslator;
            Object doc = docRefTranslator.toObject(output, outputVarType);
            setVariableValue(outputVarName, outputVarType, doc);
        }
        else {
          throw new ActivityException("Output does not appear to be a document: " + outputVarName);
        }
    }

    private Object getGPathParamValue(String varName, String varType) throws ActivityException {
        Object value = getValue(varName);
        VariableTranslator translator = getPackage().getTranslator(varType);
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
                        value = new XmlParser().parseText(docRefTranslator.toString(value, varType));
                    }
                }
                else {
                    value = new XmlSlurper().parseText(docRefTranslator.toString(value, varType));
                }
            }
            catch (Exception ex) {
                getLogger().error(ex.getMessage(), ex);
                throw new ActivityException(ex.getMessage(), ex);
            }
        }

        return value;
    }

    private void setGPathParamValue(String varName, String varType, Object value) throws ActivityException {
        if (isOutputDocument(varName)) {
            VariableTranslator translator = getPackage().getTranslator(varType);
            DocumentReferenceTranslator docRefTranslator = (DocumentReferenceTranslator) translator;
            try {
                if (value instanceof Node)
                  new XmlNodePrinter(new PrintWriter(outputDocumentWriter)).print((Node)value);
                Object doc = docRefTranslator.toObject(outputDocumentWriter.toString(), varType);
                super.setVariableValue(varName, varType, doc);
            }
            catch (Exception ex) {
                getLogger().error(ex.getMessage(), ex);
                throw new ActivityException(ex.getMessage(), ex);
            }
        }
        else {
            super.setVariableValue(varName, varType, value);
        }
    }

    private String transform(String xml, String xsl) {
        try {
            @SuppressWarnings("squid:S4435") // false positive
            TransformerFactory tFactory = TransformerFactory.newInstance();
            tFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

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
