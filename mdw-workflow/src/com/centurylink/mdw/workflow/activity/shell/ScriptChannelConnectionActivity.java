/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.workflow.activity.shell;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlOptions;
import org.w3c.dom.Document;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.bpm.MDWStatusMessageDocument;
import com.centurylink.mdw.bpm.MDWStatusMessageDocument.MDWStatusMessage;
import com.centurylink.mdw.common.exception.PropertyException;
import com.centurylink.mdw.common.translator.VariableTranslator;
import com.centurylink.mdw.common.utilities.HttpHelper;
import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.common.utilities.logger.StandardLogger.LogLevel;
import com.centurylink.mdw.common.utilities.timer.Tracked;
import com.centurylink.mdw.model.value.variable.VariableVO;
import com.centurylink.mdw.service.Action;
import com.centurylink.mdw.service.ActionRequestDocument;
import com.centurylink.mdw.service.ActionRequestDocument.ActionRequest;
import com.centurylink.mdw.service.Parameter;
import com.centurylink.mdw.workflow.activity.DefaultActivityImpl;
import com.qwest.mbeng.DomDocument;

/**
 * MDW general activity.
 */
@Tracked(LogLevel.TRACE)
public class ScriptChannelConnectionActivity extends DefaultActivityImpl
{
  private static String BORROW_OR_RETURN = "BorrowOrReturn";
  private static String CONFIG_STATEMENTS = "Config Stmts";
  private static String CONNECTION_HOSTS = "Hosts";
  private static String CONFIG_FILE = "Config File";
  private static String RULE = "Rule";
  private static String SHELL_PROMPT = "Prompt";
  private static String USERNAME = "UserName";
  private static String PASSWORD = "Password";
  private static String SCRIPTCHANNEL = "SCRIPT_CHANNEL";
  public static final String OUTPUTDOCS = "Output Documents";

  protected String[] outputDocuments;

  private String actionType;
  private String configStatements;
  private String configFile;
  private String expression;
  private String language;
  private String scriptChannel;
  private String host;
  private String prompt;
  private String userNm;
  private String password;

  private static final String PROPERTY_GROUP = "MDWFramework.ScriptChannelConnectionActivity";

  @Override
  public void execute() throws ActivityException
  {
	setActivityAttributes();
	validateAttributes();
	executeGroovyScript();
	callConnectionService();
  }

  private void setActivityAttributes() throws ActivityException {

	  try {

		  this.actionType = (String)this.getAttributeValueSmart(BORROW_OR_RETURN);
		  this.configStatements = (String)this.getAttributeValue(CONFIG_STATEMENTS);
		  this.configFile = (String)this.getAttributeValue(CONFIG_FILE);
		  this.expression = (String)this.getAttributeValue(RULE);
		  this.language = (String)this.getAttributeValue("SCRIPT");
		  this.host = (String)this.getAttributeValueSmart(CONNECTION_HOSTS);
		  this.prompt = (String)this.getAttributeValue(SHELL_PROMPT);
  		  String temp = (String)this.getAttributeValueSmart(OUTPUTDOCS);
  		  this.outputDocuments = temp==null?new String[0]:temp.split("#");
  		  this.userNm = (String)this.getAttributeValueSmart(USERNAME);
  		  this.password = (String)this.getAttributeValueSmart(PASSWORD);
  		  this.scriptChannel = (String)this.getAttributeValueSmart(SCRIPTCHANNEL);

	  } catch (PropertyException propertyexception) {
//		  logger.severeException(propertyexception.getMessage(), propertyexception);
		  throw new ActivityException(-1, propertyexception.getMessage(), propertyexception);
	  }
  }

  private void validateAttributes() throws ActivityException {
	  if (this.language != null) {
		  if (!this.language.equals("Groovy")) throw new ActivityException ("Invalid language : " + this.language + ", Only Groovy Language supported");
	  }
	  this.actionType  = this.actionType.equals("Borrow") ? "OPEN" : "RETURN";
  }

  protected boolean isOutputDocument(String variableName) {
      for (String outputDoc : outputDocuments) {
          if (outputDoc.equals(variableName))
              return true;
      }
      return false;
  }

  protected void setParamValue(String varName, String varType, Object v)
  throws ActivityException {
  	if (v==null) return;
  	if (VariableTranslator.isDocumentReferenceVariable(varType)) {
  		if (isOutputDocument(varName)) {
  			if (v instanceof DomDocument) {
  					if (varType.equals(Document.class.getName())) { // DOM document
  						v = ((DomDocument)v).getXmlDocument();
  						this.setParameterValueAsDocument(varName, varType, v);
  					} else {    // XML Bean
  						this.setParameterValueAsDocument(varName, varType, v);
  					}
  			} else this.setParameterValueAsDocument(varName, varType, v);
  		}
  	} else this.setParameterValue(varName, v);
  }

	private void executeGroovyScript() throws ActivityException {
      String rule = (String)getAttributeValue(RULE);
      if(StringHelper.isEmpty(rule)){
        return;
      }
      try {
          GroovyShell shell = new GroovyShell(getClass().getClassLoader());
          Script script = shell.parse(rule);
          Binding binding = new Binding();
          List<VariableVO> vos = getProcessDefinition().getVariables();

          for (VariableVO variableVO: vos) {
            String variableName = variableVO.getVariableName();
            Object variableValue = getVariableValue(variableName);
            binding.setVariable(variableName, variableValue);
          }
          script.setBinding(binding);
          script.run();

          for (VariableVO variableVO: vos) {
              String variableName = variableVO.getVariableName();
              Object groovyVarValue = binding.getVariable(variableName);
              setParamValue(variableName, variableVO.getVariableType(), groovyVarValue);
          }
      }catch(Exception ex){
//        logger.severeException(ex.getMessage(), ex);
        throw new ActivityException(-1, ex.getMessage(), ex);
      }
  }

  private void callConnectionService() throws ActivityException  {

	  ActionRequestDocument actionRequestDoc = ActionRequestDocument.Factory.newInstance();
	  ActionRequest actionRequest = actionRequestDoc.addNewActionRequest();
	  Action action = actionRequest.addNewAction();
	  action.setName("NETWORK_REQUEST");
	  Parameter param = action.addNewParameter();
	  param.setName("ACTION");
	  param.setStringValue(this.actionType);

	  param = action.addNewParameter();
	  param.setName("HOST");
	  param.setStringValue(this.host);

	  param = action.addNewParameter();
	  param.setName("PROMPT");
	  param.setStringValue(this.prompt);

	  if (this.actionType.equals("RETURN")) {
		  param = action.addNewParameter();
		  param.setName("ID");
		  param.setStringValue(getResourceID());
	  } else {
		  param = action.addNewParameter();
		  param.setName("CONFIG_STATEMENTS");
		  param.setStringValue(this.configStatements);

		  param = action.addNewParameter();
		  param.setName("CONFIG_FILE");
		  param.setStringValue(this.configFile);

		  param = action.addNewParameter();
		  param.setName("USER_NM");
		  param.setStringValue(this.userNm);

		  param = action.addNewParameter();
		  param.setName("PASSWORD");
		  param.setStringValue(this.password);

		  param = action.addNewParameter();
		  param.setName("SCRIPT_CHANNEL_TYPE");
		  param.setStringValue(this.scriptChannel);
	  }

	  try
	  {
		  HttpHelper httpHelper = new HttpHelper(new URL(getConnectionServiceLocation()));
	      String response = httpHelper.post(actionRequestDoc.xmlText(new    XmlOptions().setSavePrettyPrint().setSavePrettyPrintIndent(2)));

	      MDWStatusMessageDocument statusMessageDoc = MDWStatusMessageDocument.Factory.parse(response);
	      MDWStatusMessage statusMessage = statusMessageDoc.getMDWStatusMessage();
	      if (statusMessage.getStatusMessage().indexOf("Success") >= 0) {
	    	  this.setParameterValue("resourceID", statusMessage.getRequestID());
	      } else {
	    	  throw new Exception ("Problem in connection service: " + statusMessage.getStatusMessage());
	      }
	  } catch (IOException e)  {
//		  logger.severeException(e.getMessage(), e);
		  throw new ActivityException (-1, "Problem invoking connection service:" + e.getMessage(), e);
	  } catch (XmlException e) {
//		  logger.severeException(e.getMessage(), e);
		  throw new ActivityException (-1, "Problem in parsing MDWStatusMessageDocument after calling connection service:"
				  + e.getMessage(), e);
	  } catch (Exception e) {
//		  logger.severeException(e.getMessage(), e);
		  throw new ActivityException (-1, "other exception in callConnectionService", e);
	  }
  }

  private String getConnectionServiceLocation () throws ActivityException {
	  String connectionServiceLocation = getProperty(PROPERTY_GROUP + "/ConnectionServiceLocation");
      if (connectionServiceLocation==null)
    	  throw new ActivityException("Property ConnectionServiceURL not defined");
	  return connectionServiceLocation;
  }

  private String getResourceID () {
	  return (String)this.getParameterValue("resourceID");
  }

  public String getActionType() {
  	return actionType;
  }
  public void setActionType(String actionType) {
  	this.actionType = actionType;
  }

  public String getConfigStatements() {
  	return configStatements;
  }
  public void setConfigStatements(String configStatements) {
  	this.configStatements = configStatements;
  }

  public String getConfigFile() {
  	return configFile;
  }
  public void setConfigFile(String configFile) {
  	this.configFile = configFile;
  }

  public String getExpression() {
	return expression;
  }
  public void setExpression(String expression) {
	this.expression = expression;
  }

  public String getLanguage() {
	return language;
  }
  public void setLanguage(String language) {
	this.language = language;
  }

  public String getHost() {
	return host;
  }
  public void setHost(String host) {
	this.host = host;
  }

  public String getPrompt() {
	return prompt;
  }
  public void setPrompt(String prompt) {
	this.prompt = prompt;
  }

public String getScriptChannel() {
	return scriptChannel;
}

public void setScriptChannel(String scriptChannel) {
	this.scriptChannel = scriptChannel;
}

}
