package com.centurylink.mdw.services;

import com.centurylink.mdw.common.MdwException;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.model.workflow.ProcessRuntimeContext;
import org.json.JSONException;
import org.json.JSONObject;

public class ProcessException extends MdwException {

    public ProcessException(String message) {
        super(message);
    }

    public ProcessException(int code, String message) {
        super(code, message);
    }

    public ProcessException(int code, String message, Throwable cause) {
        super(code, message, cause);
    }

    public ProcessException(String message, Throwable cause) {
        super(message, cause);
    }

    private ProcessRuntimeContext runtimeContext;
    public ProcessRuntimeContext getRuntimeContext() { return runtimeContext; }
    public void setRuntimeContext(ProcessRuntimeContext context) { this.runtimeContext = context; }

    public ProcessException(JSONObject json) throws ServiceException {
        super(json);
        if (json.has("runtimeContext"))
            this.runtimeContext = new ProcessRuntimeContext(json.getJSONObject("runtimeContext"));
    }

    @Override
    public JSONObject getJson() throws JSONException {
        JSONObject json = super.getJson();
        if (runtimeContext != null)
            json.put("runtimeContext", runtimeContext.getJson());
        return json;
    }
}
