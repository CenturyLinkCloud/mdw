/**
 * Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.service.action;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;

import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.JsonService;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.common.service.types.ResponseCodes;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

public class UnitTest implements JsonService {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    public static final String SCRIPT = "Script";

    public String getJson(Map<String,Object> parameters, Map<String,String> metaInfo) throws ServiceException {
        JSONObject jsonObject = (JSONObject) parameters.get(SCRIPT);
        if (jsonObject == null)
            throw new ServiceException("Missing parameter: " + SCRIPT);
        String testName = null;
        try {
            testName = jsonObject.getString("name");
            logger.info("Executing unit test: " + testName);
            String groovy = jsonObject.getString("groovy");

            Binding binding = new Binding();
            binding.setVariable("unitTest", this);
            binding.setVariable("onServer", true);
            GroovyShell shell = new GroovyShell(this.getClass().getClassLoader(), binding);
            Script gScript = shell.parse(groovy);
            gScript.run();

            return null;
        }
        catch (Exception ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
        catch (AssertionError err) {
            logger.severeException("Unit test failed: " + testName, err);
            try {
                return createAssertionErrorResponse(testName, err);
            }
            catch (JSONException ex) {
                throw new ServiceException(ex.getMessage(), ex);
            }
        }
    }

    public String getText(Map<String,Object> parameters, Map<String,String> metaInfo) throws ServiceException {
        return getJson(parameters, metaInfo);
    }

    private String createAssertionErrorResponse(String testName, AssertionError err) throws JSONException {
        StackTraceElement[] stes = err.getStackTrace();
        int i = 0;
        StackTraceElement scriptTraceElement = null;
        while (i < stes.length && scriptTraceElement == null) {
            StackTraceElement ste = stes[i];
            if (ste.getClassName() != null && ste.getClassName().startsWith("Script"))
              scriptTraceElement = ste;
            i++;
        }
        JSONObject resp = new JSONObject();
        JSONObject status = new JSONObject();
        resp.put("status", status);
        status.put("code", ResponseCodes.UNIT_TEST_FAILED);
        status.put("message", err.toString());
        if (scriptTraceElement != null)
            status.put("location", scriptTraceElement.toString());
        return resp.toString(2);
    }

}
