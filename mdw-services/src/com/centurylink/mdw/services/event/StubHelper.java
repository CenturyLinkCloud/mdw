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
package com.centurylink.mdw.services.event;

import java.net.InetAddress;

import org.json.JSONObject;

import com.centurylink.mdw.activity.types.AdapterActivity;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.connector.adapter.AdapterException;
import com.centurylink.mdw.connector.adapter.ConnectionException;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.model.JsonObject;
import com.centurylink.mdw.model.Response;
import com.centurylink.mdw.model.event.AdapterStubResponse;
import com.centurylink.mdw.model.workflow.ActivityStubResponse;
import com.centurylink.mdw.soccom.SoccomClient;

public class StubHelper {

    private String stubServerSpec;

    public StubHelper() {
        this.stubServerSpec = PropertyManager.getProperty(PropertyNames.MDW_STUB_SERVER);
    }

    public boolean isStubbing() {
        return stubServerSpec!=null;
    }

    public Response getStubResponse(String masterRequestId, String request)
    throws AdapterException,ConnectionException {
        SoccomClient client = null;
        try {
            String[] spec = stubServerSpec.split(":");
            String host = spec.length > 0 ? spec[0] : InetAddress.getLocalHost().getHostAddress();
            String port = spec.length > 1 ? spec[1] : AdapterActivity.DEFAULT_STUBBER_PORT;
            int timeoutSecs = spec.length > 2 ? Integer.parseInt(spec[2]) : AdapterActivity.DEFAULT_STUBBER_TIMEOUT;
            client = new SoccomClient(host, port, null);
            client.putreq(request);
            String responseString = client.getresp(timeoutSecs);
            JSONObject json = new JsonObject(responseString);
            Response response;
            int delaySecs = 0;
            if (json.has("AdapterStubResponse")) {
                response = new AdapterStubResponse(json);
                delaySecs = ((AdapterStubResponse)response).getDelay();
            }
            else {
                response = new ActivityStubResponse(json);
                delaySecs = ((ActivityStubResponse)response).getDelay();
            }
            if (delaySecs > 0)
                Thread.sleep(delaySecs * 1000);
            return response;
        }
        catch (Exception e) {
            String errmsg = "Failed to get response from stub server";
            throw new ConnectionException(ConnectionException.CONNECTION_DOWN, errmsg, e);
        }
        finally {
            if (client != null)
                client.close();
        }
    }
}
