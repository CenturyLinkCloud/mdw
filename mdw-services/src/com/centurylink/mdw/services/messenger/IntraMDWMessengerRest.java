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
package com.centurylink.mdw.services.messenger;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.connector.adapter.AdapterException;
import com.centurylink.mdw.services.ProcessException;
import com.centurylink.mdw.util.HttpHelper;

public class IntraMDWMessengerRest extends IntraMDWMessenger {

    protected IntraMDWMessengerRest(String destination, String serviceContext) {
        super(destination);
    }

    /**
     *
     * @param jndi
     * @return
     * @throws MalformedURLException
     */
    private URL getURL(String engineUrl) throws MalformedURLException {
        if (engineUrl == null)
            return new URL(ApplicationContext.getServicesUrl() + "/Services/REST");
        else
            return new URL(engineUrl + "/Services/REST");
    }

    /**
     * jndi can be null, in which case send to the same site
     */
    public void sendMessage(String message)
    throws ProcessException {
        try {
            HttpHelper httpHelper = new HttpHelper(getURL(destination));
            httpHelper.post(message);
        } catch (Exception e) {
            throw new ProcessException(-1, e.getMessage(), e);
        }
    }

    /**
     * jndi can be null, in which case send to the same site
     */
    public String invoke(String message, int timeoutSeconds)
    throws ProcessException {
        try {
            HttpHelper httpHelper = new HttpHelper(getURL(destination));
            httpHelper.getConnection().setReadTimeout(timeoutSeconds * 1000);
            httpHelper.getConnection().setConnectTimeout(timeoutSeconds * 1000);
            String res = httpHelper.post(message);
            return res==null?null:res.trim();
        } catch (Exception e) {
            throw new ProcessException(-1, e.getMessage(), e);
        }
    }

    public void sendCertifiedMessage(String message, String msgid, int ackTimeoutSeconds)
        throws ProcessException,AdapterException {
        String acknowledgment;
        try {
            HttpHelper httpHelper = new HttpHelper(getURL(destination));
            HashMap<String,String> headers = new HashMap<String,String>();
            headers.put("MDWCertifiedMessageId", msgid);
            httpHelper.setHeaders(headers);
            httpHelper.getConnection().setReadTimeout(ackTimeoutSeconds * 1000);
            httpHelper.getConnection().setConnectTimeout(ackTimeoutSeconds * 1000);
            acknowledgment = httpHelper.post(message);
        } catch (Exception e) {
            throw new ProcessException(0, "Faile to send certified message", e);
        }
        if (!msgid.equals(acknowledgment.trim()))
            throw new AdapterException("Incorrect acknowledgment for certified message");
    }

}
