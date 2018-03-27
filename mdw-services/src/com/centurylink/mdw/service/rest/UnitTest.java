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
package com.centurylink.mdw.service.rest;

import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.JsonService;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.model.JsonObject;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;

public class UnitTest implements JsonService {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    public static final String SCRIPT = "Script";

    public String getJson(JSONObject request, Map<String,String> metaInfo) throws ServiceException {
        if (request == null)
            throw new ServiceException("Missing parameter: " + SCRIPT);
        String testName = null;
        try {
            testName = request.getString("name");
            logger.info("Executing unit test: " + testName);
            String groovy = request.getString("groovy");

            Binding binding = new Binding();
            binding.setVariable("unitTest", this);
            binding.setVariable("onServer", true);
            GroovyShell shell = new GroovyShell(this.getClass().getClassLoader(), binding);
            Script gScript = shell.parse(groovy);
            gScript.run();

            return null;
        }
        catch (Exception ex) {
            logger.severeException("Unit test errored: " + testName, ex);
            try {
                return createAssertionErrorResponse(testName, ex);
            }
            catch (JSONException jex) {
                throw new ServiceException(jex.getMessage(), jex);
            }
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

    public String getText(Object requestObj, Map<String,String> metaInfo) throws ServiceException {
        return getJson((JSONObject)requestObj, metaInfo);
    }

    private String createAssertionErrorResponse(String testName, Throwable err) throws JSONException {
        StackTraceElement[] stes = err.getStackTrace();
        int i = 0;
        StackTraceElement scriptTraceElement = null;
        while (i < stes.length && scriptTraceElement == null) {
            StackTraceElement ste = stes[i];
            if (ste.getClassName() != null && ste.getClassName().startsWith("Script"))
              scriptTraceElement = ste;
            i++;
        }
        JSONObject resp = new JsonObject();
        JSONObject status = new JsonObject();
        resp.put("status", status);
        status.put("code", 1001);
        status.put("message", err.toString());
        if (scriptTraceElement != null)
            status.put("location", scriptTraceElement.toString());
        return resp.toString(2);
    }

}
