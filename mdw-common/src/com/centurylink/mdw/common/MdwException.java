/**
 * Copyright (c) 2017 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.Jsonable;
import com.centurylink.mdw.model.ThrowableJsonable;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

public class MdwException extends Exception implements Jsonable {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    private int code;
    public int getCode(){
        return this.code;
    }

    public MdwException(String code){
        super(code);
    }

    public MdwException(int code, String message) {
        super(message);
        this.code = code;
    }

    public MdwException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public MdwException(String message, Throwable cause){
        super(message, cause);
        this.code = -1;
    }

    public String getStackTraceDetails() {
      return getStackTrace(this);
    }

    public static String getStackTrace(Throwable t) {
        StackTraceElement[] elems = t.getStackTrace();
        StringBuffer sb = new StringBuffer();
        sb.append(t.toString());
        sb.append("\n");
        for (int i = 0; i < elems.length; i++) {
            sb.append(elems[i].toString());
            sb.append("\n");
        }

        if (t.getCause() != null) {
            sb.append("\n\nCaused by:\n");
            sb.append(getStackTrace(t.getCause()));
        }

        return sb.toString();
    }

    public Throwable findCause() {
        return findCause(this);
    }

    public static Throwable findCause(Throwable t) {
        if (t.getCause() == null)
            return t;
        else
            return findCause(t.getCause());
    }

    public MdwException(JSONObject json) throws JSONException {
        this(getCode(json), getMessage(json), getCause(json));
    }

    @Override
    public JSONObject getJson() throws JSONException {
        JSONObject json = new JSONObject();
        if (code > 0)
            json.put("code", code);
        if (getMessage() != null)
            json.put("message", getMessage());
        if (getCause() != null)
            json.put("cause", new ThrowableJsonable(getCause()).getJson());
        return json;
    }

    @Override
    public String getJsonName() {
        String className = getClass().getSimpleName();
        return className.substring(0, 1).toLowerCase() + className.substring(1);
    }

    private static int getCode(JSONObject json) throws JSONException {
        return json.has("code") ? json.getInt("code") : 0;
    }
    private static String getMessage(JSONObject json) throws JSONException {
        return json.has("message") ? json.getString("message") : null;
    }
    private static Throwable getCause(JSONObject json) throws JSONException {
        Throwable cause = null;
        if (json.has("cause")) {
            ThrowableJsonable jsonable = new ThrowableJsonable(json.getJSONObject("cause"));
            try {
                cause = jsonable.toThrowable();
            }
            catch (Exception ex) {
                logger.severeException("Cannot instantiate throwable: " + jsonable.getThrowable(), ex);
                throw new JSONException(ex);
            }
        }
        return cause;
    }
}
