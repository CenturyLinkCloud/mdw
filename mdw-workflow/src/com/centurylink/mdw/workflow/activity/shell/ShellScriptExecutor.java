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
package com.centurylink.mdw.workflow.activity.shell;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.oro.text.regex.MalformedPatternException;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.config.PropertyException;
import com.centurylink.mdw.model.variable.Variable;
import com.centurylink.mdw.util.StringHelper;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import com.centurylink.mdw.util.log.StandardLogger.LogLevel;
import com.centurylink.mdw.util.timer.Tracked;
import com.centurylink.mdw.workflow.activity.DefaultActivityImpl;

import expect4j.Closure;
import expect4j.Expect4j;
import expect4j.ExpectState;
import expect4j.ExpectUtils;
import expect4j.matches.RegExpMatch;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;

@Tracked(LogLevel.TRACE)
public class ShellScriptExecutor extends DefaultActivityImpl  {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    private Expect4j session;
    private Closure closure;

    private int    port;
    private long   timeout;
    private long   delay;
    private String userName;
    private String password;
    private String hostName;
    private String command;
    private String prompt;
    private String pattern;
    private String expression;
    private String language;
    private String protocol;
    private static final String VRESPONSE = "response";

    private List<String> response ;

    public ShellScriptExecutor() {
        super();
    }

    public void execute() throws ActivityException {

        logger.info("Inside ShellScriptExecutor.execute()");


        try {
           this.userName = (String)this.getAttributeValueSmart("UserName");
           this.password = (String)this.getAttributeValueSmart("Password");
           this.hostName = (String)this.getAttributeValueSmart("HostName");

           Object portObj = this.getAttributeValueSmart("Port");
           this.port     = (portObj == null ) ? -1 : portNumeric((String)portObj) ? Integer.valueOf((String)portObj) : -1;

           this.command  = (String)this.getAttributeValueSmart("Commamd");

           Object timeoutObj = this.getAttributeValueSmart("Timeout");
           this.timeout  =  ( timeoutObj == null) ? -999 : (this.timeInLong((String)timeoutObj)) ? Long.valueOf((String)timeoutObj) : -999L;

           Object delayObj = this.getAttributeValueSmart("Delay");
           this.delay = (delayObj == null) ? -999 : (this.timeInLong((String)delayObj)) ? Long.valueOf((String)delayObj) : -999;

           this.prompt   = (String)this.getAttributeValueSmart("Prompt");
           this.pattern  = (String)this.getAttributeValueSmart("RegularExpression");
           this.expression = (String)this.getAttributeValueSmart("Rule");
           this.language = (String)this.getAttributeValueSmart("SCRIPT");
           this.protocol = (String)this.getAttributeValueSmart("Protocol");
        } catch (PropertyException propertyexception) {
           logger.debugException(propertyexception.getMessage(), propertyexception);
           throw new ActivityException(propertyexception.getMessage());
        }

        if (this.expression != null && !this.language.equals("Groovy")) {

           throw new ActivityException("Only Groovy Scripting language is supported in Shell Script Executor Activity");
        }


        boolean usingTelnet = this.protocol.equals("Telnet") ? true : false;

        try {
            try {
                createExpect4JSession(usingTelnet);
                executeShellScript();
            }
            catch (Exception e) {
                logger.severeException(e.getMessage(), e);
                throw new ActivityException(e.getMessage());
            } finally {
                try  {
                    this.closeSession();
                 } catch (IOException ioexception) {
                   logger.debugException(ioexception.getMessage(), ioexception);
                 }
             }
            executeGroovyScript();
        } catch (Exception exception) {
            logger.severeException(exception.getMessage(), exception);
            throw new ActivityException(exception.getMessage());
        }
    }

    private boolean timeInLong (String str) {

        try {
            Long.parseLong(str);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;

    }
    private boolean portNumeric (String str) {

        try {
            Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;

    }

    private void executeShellScript () throws Exception {

        if (this.timeout == -1) {
            this.session.setDefaultTimeout(Expect4j.TIMEOUT_FOREVER);
        } else if (this.timeout == -999) {
          this.session.setDefaultTimeout(Expect4j.TIMEOUT_DEFAULT);
        } else {
            this.session.setDefaultTimeout(this.timeout * 1000);
        }

        if (this.delay != -999) {
           pause(this.delay * 1000);
        }

        String cmd =  this.command + "\r\n";
        session.send(cmd);

        List<RegExpMatch> pairs = new ArrayList<RegExpMatch>();

        String patternStr =   "(" + this.command + ")+(: +.*)(not found|cannot execute)(\r\n+)" + this.prompt ;
        pairs.add( new RegExpMatch(patternStr , this.closure ));
        pairs.add( new RegExpMatch(this.pattern , this.closure ));

        int index = this.session.expect(pairs);

        if (index < 0) {
            throw new RuntimeException("Pattern :"  + patternStr + " did not match");
        }

        if ( this.session.getLastState().getMatch().contains("not found")) {
            throw new RuntimeException("The script " + this.command + " does not exist");
        }

        if ( this.session.getLastState().getMatch().contains("cannot execute")) {
            throw new RuntimeException("The script " + this.command + " may not have execute permission");
        }
        fillResponseList();
    }

    private void fillResponseList () {
        boolean retry = true;
        int counter = 1;
        response = new ArrayList<String>();
        while (retry) {
            String tempStr = this.session.getLastState().getMatch(counter);
            if(tempStr != null) {
                this.response.add(tempStr);
                ++counter;
            } else retry = false;
        }

        for (String str: response) {
            System.out.println("Str:" + str);
        }
    }

    private void closeSession() throws IOException {
        this.session.send("exit");
    }

    private  void createExpect4JSession (boolean usingTelnet) throws Exception {
        this.session = (usingTelnet) ? ExpectUtils.telnet (this.hostName, (this.port == -1) ? 23 : this.port)
                                     : ExpectUtils.SSH(this.hostName, this.userName, this.password, (this.port ==  -1) ? 22 : this.port);

        if (usingTelnet) {
            handleTelnetProtocol();
        } else {
          if (this.session.expect(this.prompt) < 0)
               throw new RuntimeException ("Did not recieve prompt");
        }
        closure = new Closure() { public void run(ExpectState state) throws Exception {}; };
    }

    private void handleTelnetProtocol () throws MalformedPatternException, Exception {
        String patternStr = "([Pp]olicy.)(\r\n*)(#*)(\r\n)(#*)(\r\n.*)(login:)";
        List<RegExpMatch> pairs = new ArrayList<RegExpMatch>();
        pairs.add( new RegExpMatch(patternStr , this.closure ));


        int id = this.session.expect(pairs);
        if (id < 0) {
            throw new RuntimeException ("Did not recieve login prompt");
        }

        this.session.send(this.userName + "\r\n");
        this.session.expect("[Pp]assword:");

        this.session.send(this.password + "\r\n");

        pause(5000L);

        pairs = new ArrayList<RegExpMatch>();


        String str = "Login incorrect";
        pairs.add( new RegExpMatch((String)str , this.closure ));
        pairs.add( new RegExpMatch(this.prompt , this.closure ));



        id = this.session.expect(pairs);

        if (this.session.getLastState().getMatch().equals(str)) {
            throw new RuntimeException ("Incorrect password");
        }
    }

    private synchronized void executeGroovyScript() throws ActivityException {
        if(StringHelper.isEmpty(expression)){
            throw new  ActivityException("Groovy Script has not been defined");
        }

        try {
            GroovyShell shell = new GroovyShell();
            Script script = shell.parse(this.expression);

            Binding binding = new Binding();
            List<Variable> vos = getProcessDefinition().getVariables();

            for (Variable variableVO: vos) {
              String variableName = variableVO.getVariableName();
              Object variableValue = getParameterValue(variableName);
              binding.setVariable(variableName, variableValue);
            }

            binding.setVariable(VRESPONSE, response);

            script.setBinding(binding);
            script.run();

            for (Object id: binding.getVariables().keySet()) {
                String groovyVarName = (String)id;
                Object groovyVarValue = binding.getVariable(groovyVarName);
                System.out.println("Variable Name:" + groovyVarName + " Variable Value:" + groovyVarValue);
                if (!groovyVarName.equals(VRESPONSE))
                  super.setParameterValue(groovyVarName, groovyVarValue);
             }

            String returnCodeAttrValue = (String)getAttributeValue("RETURN_CODE");

            if (returnCodeAttrValue != null ) {
              super.setReturnCode((String)binding.getVariable(returnCodeAttrValue));
            }
        }catch(Exception ex){
          logger.severeException(ex.getMessage(), ex);
          throw new ActivityException(ex.getMessage());
        }
    }

    public List<String> getResponse() {
        return response;
    }

    public void setResponse(List<String> response) {
        this.response = response;
    }

    public void pause( long numberMillis )
    {
        long exittime = System.currentTimeMillis() + numberMillis;
        while(true){
           if(System.currentTimeMillis() > exittime) {  return; }
        }
    }
}
