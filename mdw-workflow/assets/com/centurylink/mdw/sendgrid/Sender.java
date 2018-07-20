/*
 * Copyright (C) 2018 CenturyLink, Inc.
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
package com.centurylink.mdw.sendgrid;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import com.centurylink.mdw.util.HttpHelper;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

public class Sender {
    
    private static StandardLogger logger = LoggerUtil.getStandardLogger();
    public static final String SENDGRID_API_KEY = "SENDGRID_API_KEY";
    
    private String payload;
    public String getPayload() throws IOException {
        return payload;
    }
    
    public Sender(Email email) {
        this.payload = email.getJson().toString(2);
    }
    
    public Sender(String payload) {
        this.payload = payload;
    }
    
    public void send() throws IOException {
        HttpHelper helper = new HttpHelper(getUrl());
        helper.setHeaders(getRequestHeaders());
        String response = helper.post(getPayload());
        if (helper.getResponseCode() != 202) {
            String message = "Error response from SendGrid (" + helper.getResponseCode() + ")";
            logger.severe(message + ":\n" + response);
            if (logger.isDebugEnabled()) {
                logger.severe("SendGrid request:\n" + getPayload());
            }
            throw new IOException(message);
        }
    }
    
    protected Map<String,String> getRequestHeaders() throws IOException {
        String key = System.getenv(SENDGRID_API_KEY);
        if (key == null)
            throw new IOException("Missing environment variable: " + SENDGRID_API_KEY);
        Map<String,String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + key);
        headers.put("Content-Type", "application/json");
        return headers;
    }
    
    protected URL getUrl() throws MalformedURLException {
        return new URL("https://api.sendgrid.com/v3/mail/send");
    }
}
