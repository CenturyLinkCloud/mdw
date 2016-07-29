/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.workflow.activity.shell;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.apache.xmlbeans.XmlOptions;
import org.w3c.dom.Document;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.common.exception.PropertyException;
import com.centurylink.mdw.common.translator.VariableTranslator;
import com.centurylink.mdw.common.utilities.HttpHelper;
import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.common.utilities.logger.StandardLogger.LogLevel;
import com.centurylink.mdw.common.utilities.timer.Tracked;
import com.centurylink.mdw.model.data.event.EventType;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;
import com.centurylink.mdw.model.value.event.EventWaitInstanceVO;
import com.centurylink.mdw.model.value.variable.VariableVO;
import com.centurylink.mdw.service.Action;
import com.centurylink.mdw.service.ActionRequestDocument;
import com.centurylink.mdw.service.ActionRequestDocument.ActionRequest;
import com.centurylink.mdw.service.Parameter;
import com.centurylink.mdw.workflow.activity.event.EventWaitActivity;
import com.qwest.mbeng.DomDocument;

@Tracked(LogLevel.TRACE)
public class ScriptChannelActivityImpl extends EventWaitActivity {

	public static final String CONFIG_STATEMENTS = "Config Stmts";
    public static final String RULE = "Rule";
	public static final String SHELL_PROMPT = "Prompt";
	public static final String EXPECT_PAIRS = "ExpectPairs";
	public static final String SCRIPT_COMMAND = "Command";
	public static final String CHANNEL_COMMAND_TIMEOUT="Timeout";
    public static final String OUTPUTDOCS = "Output Documents";
    //public static final String OUTPUT_VARIABLE = "OutputVariable";
    public static final String RETURNCODE = "vReturnCode";
    private static final String VRESPONSE = "response";

    protected String[] outputDocuments;

    private String response;
    private String resourceID;
	private String configStatements;
	private String command;
	private String prompt;
	private String expectPairs;
	private String expression;
	private String language;
	private String timeout;
	//private String outputVar;
	private GroovyShell shell;

	private static String SCRIPT_CHANNEL_ACTIVITY = "SCRIPT_CHANNEL_ACTIVITY";
	private static final String PROPERTY_GROUP = "MDWFramework.ScriptChannelConnectionActivity";

	private String getWaitEventOwner() throws ActivityException {
		return SCRIPT_CHANNEL_ACTIVITY;
	}

	private Long getWaitEventOwnerId() throws ActivityException {
		return super.getActivityInstanceId();
	}

    public String getType() {
    	return "<PAGELET>"
    	    + " <TEXT NAME='Timeout'/>"
    	    + " <TEXT NAME='Config Stmts'  MULTILINE='true'/>"
    	    + " <TEXT NAME='Command' />"
            + " <TEXT NAME='Prompt' LABEL='Prompt'/>"
            + " <TEXT NAME='OutputVariable' LABEL='Output Variable' /> "
            + " <RULE NAME='Rule'  TYPE='ACTION' />"
            + "   <LIST NAME='Output Documents' SOURCE='DocumentVariables'/> "
    		+ " <TABLE NAME='" + "ExpectPairs" + "' LABEL='Expect Pairs'>"
    		+ "    <TEXT LABEL='Expect Pair/>"
    		+ "    <TEXT LABEL='KeyData'/> "
            + "    <TEXT LABEL='GroupNum'/>"
    		+ "    <TEXT LABEL='Response'/>"
            + " </TABLE>"
            + "</PAGELET>";
    }

    public void execute() throws ActivityException {
    	getChannelResourceID();
    	setActivityAttributes();
    	//executeGroovyScript();
    	translateScriptPlaceHolders();
    	registerWaitEvents();
        callService();
    }

    private void translateScriptPlaceHolders () throws ActivityException  {
   		executeGroovyScript (SCRIPT_COMMAND, true, false);
    }

    private void setActivityAttributes() throws ActivityException {

  	  try {
  		  this.timeout = (String)this.getAttributeValue(CHANNEL_COMMAND_TIMEOUT);
  		  this.configStatements = (String)this.getAttributeValue(CONFIG_STATEMENTS);
  		  this.expression = (String)this.getAttributeValue(RULE);
  		  this.language = (String)this.getAttributeValue("SCRIPT");
  		  this.prompt = (String)this.getAttributeValue(SHELL_PROMPT);
  		  this.expectPairs = (String)this.getAttributeValue(EXPECT_PAIRS);
  		 // this.outputVar = (String)this.getAttributeValueSmart(OUTPUT_VARIABLE);
  		  String temp = (String)this.getAttributeValueSmart(OUTPUTDOCS);
  		  this.outputDocuments = temp==null?new String[0]:temp.split("#");



  	  } catch (PropertyException propertyexception) {
//  		  logger.severeException(propertyexception.getMessage(), propertyexception);
  		  throw new ActivityException(-1, "exception in setActivityAttributes",
  				  propertyexception);
  	  }
    }
    /**
     * Method that registers the wait events
     *
     * @throws ActivityException
     */
    protected EventWaitInstanceVO registerWaitEvents() throws ActivityException {
    	super.loginfo("Inside ControlledScriptChannelActivityImpl.registerWaitEvents() :" + resourceID );
//    	logger.info("Inside ControlledScriptChannelActivityImpl.registerWaitEvents() :" + resourceID );
        String eventName = getWaitEventOwner() + ":" + getWaitEventOwnerId() + "_" + this.resourceID;
        String eventType = EventType.getEventTypes().get(EventType.FINISH);
        boolean recurring = false;

        try {
            return getEngine().createEventWaitInstance(
            		getActivityInstanceId(), eventName, eventType, recurring, true);
        }
        catch (Exception ex) {
            throw new ActivityException(-1, "Exception in registerWaitEvents", ex);
        }
    }

    private void callService () throws ActivityException{
    	logdebug("Inside ControlledScriptChannelActivityImpl.callService() :" + resourceID );
    	ActionRequestDocument actionRequestDoc = ActionRequestDocument.Factory.newInstance();
        ActionRequest actionRequest = actionRequestDoc.addNewActionRequest();
    	Action action = actionRequest.addNewAction();
    	action.setName("NETWORK_REQUEST");

    	Parameter param = action.addNewParameter();
    	param.setName("ACTION");
    	param.setStringValue("COMMAND");

    	param = action.addNewParameter();
    	param.setName("ID");
    	param.setStringValue(resourceID);

    	param = action.addNewParameter();
    	param.setName("EXECUTABLE_COMMAND");
    	param.setStringValue(this.command);

    	param = action.addNewParameter();
    	param.setName("EXECUTABLE_COMMAND_TIMEOUT");
    	param.setStringValue( (String)this.timeout);

    	param = action.addNewParameter();
    	param.setName("PROMPT");
    	param.setStringValue( (String)this.prompt);

    	param = action.addNewParameter();
		param.setName("CONFIG_STATEMENTS");
		param.setStringValue(this.configStatements);

    	param = action.addNewParameter();
		param.setName("EXPECT_PAIRS");
		param.setStringValue(this.expectPairs);

/*    	param = action.addNewParameter();
		param.setName("RESPONSE_VARIABLE");
		param.setStringValue(this.outputVar);*/

		param = action.addNewParameter();
		param.setName("OWNER_ID");
		param.setStringValue(Long.toString(this.getWaitEventOwnerId()));

		HttpHelper httpHelper;
		try {
			httpHelper = new HttpHelper(new URL(getConnectionServiceLocation()));
		} catch (MalformedURLException e) {
			throw new ActivityException ("Malformed URL Exception in invoking service:" + e.getMessage());
		}

		try {
			if (isLogInfoEnabled())
				loginfo("****Action Request XML for Wait Owner ID:" + this.getWaitEventOwnerId() + "****\n " + actionRequestDoc.xmlText());
//			logger.info("\n****Action Request XML for Wait Owner ID:" + this.getWaitEventOwnerId() + "****\n " + actionRequestDoc.xmlText());
			httpHelper.post(actionRequestDoc.xmlText(new XmlOptions().setSavePrettyPrint().setSavePrettyPrintIndent(2)));
		} catch (Exception e) {
			throw new ActivityException ("Exception in invoking service:" + e.getMessage());
		}

    }

    private void getChannelResourceID () throws ActivityException {
       	resourceID =  (String)getParameterValue("resourceID");
        if (resourceID == null) {
        	 throw new ActivityException ("Unable to find resource id for channel");
        }
        logdebug("Inside ControlledScriptChannelActivityImpl.getChannelResourceID() :" + resourceID );
    }

    @Override
    protected void processMessage(String responseXML) throws ActivityException {
      setActivityAttributes();
      if (isLogInfoEnabled()) loginfo("****Response:" + responseXML);
      String closureStr = "";
      try {
          ActionRequestDocument actionRequestDoc = ActionRequestDocument.Factory.parse(responseXML);
          for (Parameter param : actionRequestDoc.getActionRequest().getAction().getParameterList()) {
              if (param.getName().equals("CLOSURE_STR")) {
                  closureStr = param.getStringValue();
              } else if (param.getName().equals("RESPONSE")) {
                  this.response = param.getStringValue();
              }
          }
      } catch (Exception ex) {

      }

      if (isLogInfoEnabled()) loginfo("****Closure Str:" + closureStr);
      String localReturnCode = null;
      if (closureStr != null && closureStr.length() > 0) {
			RuleSetVO ruleSet = getRuleSet(closureStr, RuleSetVO.GROOVY, 0);
            String ruleStr = (ruleSet != null) ? ruleSet.getRuleSet(): null;

            if (isLogInfoEnabled()) loginfo("****Rule:" + ruleStr);
            if (ruleStr != null)   {
                localReturnCode =  executeGroovyScript(ruleStr, false, true);
                if (localReturnCode != null && (
                    localReturnCode.indexOf("ABORT:") >=0  ||
                            localReturnCode.indexOf("ERROR:") >=0  ||
                            localReturnCode.indexOf("CORRECT:") >=0 ||
                            localReturnCode.indexOf("DELAY:") >=0 )) {
                        this.setReturnCode(localReturnCode);
                } else {
                    localReturnCode = executeGroovyScript(RULE, false, false);
                    this.setReturnCode(localReturnCode);
                }
            } else if (closureStr.equals("CORRECTION")) {
                this.setReturnCode(EventType.getEventTypeName(EventType.CORRECT));
            } else if (closureStr.equals("ERROR")) {
                this.setReturnCode(EventType.getEventTypeName(EventType.ERROR));
            } else if (closureStr.equals("CANCEL")) {
                this.setReturnCode(EventType.getEventTypeName(EventType.ABORT));
            }
      }  else {
          localReturnCode = executeGroovyScript(RULE, false, false);
          this.setReturnCode(localReturnCode);
      }

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

	private String executeGroovyScript(String attribute, boolean transform, boolean scriptPassed) throws ActivityException {
	    if (shell == null)
	        shell = new GroovyShell(getClass().getClassLoader());
        String rule = "";
        if (!scriptPassed)
            rule = (String)getAttributeValue(attribute);
        else
            rule = attribute;

        if (transform) {
        	rule = "command = (String)\"" + rule + "\";";
        	System.out.println ("\n\nRule:" + rule );
        }
        if(StringHelper.isEmpty(rule)){
          return null;
        }
        try {

            Script script = shell.parse(rule);
            Binding binding = new Binding();

            List<VariableVO> vos = getMainProcessDefinition().getVariables();
            for (VariableVO variableVO: vos) {
              String variableName = variableVO.getVariableName();
              Object variableValue = getVariableValue(variableName);
              binding.setVariable(variableName, variableValue);
            }

            if (transform) {
            	binding.setVariable(command, null);
            } else {
                binding.setVariable("activity", this);
                binding.setVariable(VRESPONSE, response);
            }


            script.setBinding(binding);
            script.run();

            for (VariableVO variableVO: vos) {
                String variableName = variableVO.getVariableName();
                Object groovyVarValue = binding.getVariable(variableName);
                if (!variableName.equals(VRESPONSE))
                    setParamValue(variableName, variableVO.getVariableType(), groovyVarValue);

            }

            if (transform) {
             	this.command = (String) binding.getVariable("command");
             	if (isLogInfoEnabled()) loginfo("****Command:" + this.command);
            } else {
               String returnCodeAttrValue = getAttributeValue("RETURN_CODE");
               if (returnCodeAttrValue != null  && binding.getVariables().containsKey(returnCodeAttrValue)) {
            	if (returnCodeAttrValue != null) {
            		Object object = binding.getVariable(returnCodeAttrValue);
            		if (object != null)
            		return (object.toString());
            	}
               } else  if (binding.getVariables().containsKey(RETURNCODE)){
                Object  object = binding.getVariable(RETURNCODE);
                if ( object != null) {
                  return (object.toString());
                }
               }
            }
        }catch(Exception ex){
//          logger.severeException(ex.getMessage(), ex);
          throw new ActivityException(-1, "Exception in executeGroovyScript", ex);
        }

        return null;
    }

	private String getConnectionServiceLocation () throws ActivityException {
		  String connectionServiceLocation = getProperty(PROPERTY_GROUP + "/ConnectionServiceLocation");
	      if (connectionServiceLocation==null)
	    	  throw new ActivityException("Property ConnectionServiceURL not defined");
		  return connectionServiceLocation;
	}

	public String getResourceID() {
		return resourceID;
	}

	public void setResourceID(String resourceID) {
		this.resourceID = resourceID;
	}

	public String getConfigStatements() {
		return configStatements;
	}

	public void setConfigStatements(String configStatements) {
		this.configStatements = configStatements;
	}

	public String getCommand() {
		return command;
	}

	public void setCommand(String command) {
		this.command = command;
	}

	public String getPrompt() {
		return prompt;
	}

	public void setPrompt(String prompt) {
		this.prompt = prompt;
	}

	public String getExpectPairs() {
		return expectPairs;
	}

	public void setExpectPairs(String expectPairs) {
		this.expectPairs = expectPairs;
	}

	public String getTimeout() {
		return timeout;
	}

	public void setTimeout(String timeout) {
		this.timeout = timeout;
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

/*	public String getOutputVar() {
		return outputVar;
	}

	public void setOutputVar(String outputVar) {
		this.outputVar = outputVar;
	}*/

	   //
    // following methods are exposed so that they can be accessed in Groovy.
    //

    @Override
    public void setProcessInstanceCompletionCode(String code) throws ActivityException {
        super.setProcessInstanceCompletionCode(code);
    }

    @Override
    public Long getProcessInstanceId() {
        return super.getProcessInstanceId();
    }

    @Override
    public boolean needSuspend() {
      return true;
    }

}
