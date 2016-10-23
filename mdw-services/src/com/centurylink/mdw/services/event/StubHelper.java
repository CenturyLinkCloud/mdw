/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services.event;

import com.centurylink.mdw.activity.types.AdapterActivity;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.connector.adapter.AdapterException;
import com.centurylink.mdw.connector.adapter.ConnectionException;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.soccom.SoccomClient;

public class StubHelper {
    
    private String stubServerSpec;
    
    public StubHelper() {
        this.stubServerSpec = PropertyManager.getProperty(PropertyNames.MDW_STUB_SERVER);
    }
    
    public boolean isStubbing() {
        return stubServerSpec!=null;
    }
    
    public String getStubResponse(String masterRequestId, String request) 
            throws AdapterException,ConnectionException {
        String response=null;
        SoccomClient client = null;
        try {
            String[] spec = stubServerSpec.split(":");
            String host = spec.length>0?spec[0]:AdapterActivity.DEFAULT_STUBBER_HOST;
            String port = spec.length>1?spec[1]:AdapterActivity.DEFAULT_STUBBER_PORT;
            int timeout_seconds = spec.length>2?Integer.parseInt(spec[2]):AdapterActivity.DEFAULT_STUBBER_TIMEOUT;
            client = new SoccomClient(host, port, null);
            client.putreq("REQUEST~"+masterRequestId+"~"+request);
            String response_string = client.getresp(timeout_seconds);
            String[] parsed = response_string.split("~", 3);
            if (parsed.length==3) {
                int delay = Integer.parseInt(parsed[1]);
                if (delay>0) Thread.sleep(delay*1000L);
                response = parsed[2];
            } else response = response_string;
        } catch (Exception e) {
            String errmsg = "Failed to get response from stub server";
            throw new ConnectionException(ConnectionException.CONNECTION_DOWN, errmsg, e);
        } finally {
            if (client!=null) client.close();
        }
        return response;
    }
    
    

}
