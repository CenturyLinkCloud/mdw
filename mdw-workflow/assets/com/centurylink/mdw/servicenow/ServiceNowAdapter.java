package com.centurylink.mdw.servicenow;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.activity.types.AdapterActivity;
import com.centurylink.mdw.activity.types.NotificationActivity;
import com.centurylink.mdw.config.PropertyException;
import com.centurylink.mdw.model.workflow.ActivityRuntimeContext;
import com.centurylink.mdw.workflow.activity.DefaultActivityImpl;
import com.centurylink.mdw.annotations.Activity;
import com.centurylink.mdw.workflow.adapter.http.BasicAuthProvider;
import com.centurylink.mdw.workflow.adapter.rest.RestServiceAdapter;

import java.util.Map;

@Activity(value="ServiceNow Adapter", category=AdapterActivity.class,
        icon="com.centurylink.mdw.servicenow/servicenow.png",
        pagelet="com.centurylink.mdw.servicenow/adapter.pagelet")
public class ServiceNowAdapter extends RestServiceAdapter {

    @Override
    protected String getHttpMethod() throws ActivityException {
        return "POST";
    }

    @Override
    public Map<String,String> getRequestHeaders() {
        Map<String,String> requestHeaders = super.getRequestHeaders();
        requestHeaders.put("Content-Type", "application/json");
        return requestHeaders;
    }

    @Override
    public Object getAuthProvider() throws ActivityException {
        String user = getAttribute(AUTH_USER);
        String password = getAttribute(AUTH_PASSWORD);
        return new BasicAuthProvider(user, password);
    }
}
