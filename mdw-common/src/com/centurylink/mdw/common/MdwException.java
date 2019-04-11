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
package com.centurylink.mdw.common;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.model.Jsonable;
import com.centurylink.mdw.model.JsonableThrowable;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

public class MdwException extends Exception implements Jsonable {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    private int code;
    public int getCode() { return this.code; }

    public MdwException(String code) {
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
    }

    @Override
    public String toString() {
        String s = getClass().getName();
        if (code > 0)
            s += ":(code=" + code + ")";
        String message = getLocalizedMessage();
        return (message != null) ? (s + ": " + message) : s;
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
        if (json.has("stackElements")) {
            StackTraceElement[] stackElements = JsonableThrowable.getStackElements(json.getJSONArray("stackElements"));
            if (stackElements != null)
                setStackTrace(stackElements);
        }
    }

    @Override
    public JSONObject getJson() throws JSONException {
        JSONObject json = create();
        json.put("throwable", this.getClass().getName());
        if (code > 0)
            json.put("code", code);
        if (getMessage() != null)
            json.put("message", getMessage());
        if (getCause() != null)
            json.put("cause", new JsonableThrowable(getCause()).getJson());
        JSONArray stackElementsJson = JsonableThrowable.getStackElementsJson(getStackTrace());
        if (stackElementsJson != null)
            json.put("stackElements", stackElementsJson);
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
            JsonableThrowable jsonable = new JsonableThrowable(json.getJSONObject("cause"));
            try {
                cause = jsonable.toThrowable();
            }
            catch (Exception ex) {
                logger.severeException("Cannot instantiate throwable: " + jsonable.getThrowable(), ex);
            }
        }
        return cause;
    }

    public boolean equals(Object obj) {
        if (super.equals(obj))
            return true;
        if (!(obj instanceof MdwException))
            return false;
        MdwException mdwException = (MdwException) obj;
        if (!toString().equals(mdwException.toString()))
            return false;  // fail fast
        try {
            return (getJson().equals(mdwException.getJson()));
        }
        catch (JSONException ex) {
            return false;
        }
    }
}
