/**
 * Copyright (c) 2017 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.Jsonable;

/**
 * Wraps a Throwable as a Jsonable for JSON serialization.
 */
public class ThrowableJsonable implements Jsonable {

    private String throwable;
    public String getThrowable() { return throwable; }

    private String message;
    public String getMessage() { return message; }

    private ThrowableJsonable cause;
    public ThrowableJsonable getCause() { return cause; }

    private StackTraceElement[] stackElements;
    public StackTraceElement[] getStackElements() { return stackElements; }

    public ThrowableJsonable(Throwable th) {
        this.throwable = th.getClass().getName();
        this.message = th.getMessage();
        if (th.getCause() != null)
            this.cause = new ThrowableJsonable(th.getCause());
        this.stackElements = th.getStackTrace();
    }

    public ThrowableJsonable(JSONObject json) throws JSONException {
        if (json.has("throwable"))
            throwable = json.getString("throwable");
        if (json.has("message"))
            message = json.getString("message");
        if (json.has("cause"))
            cause = new ThrowableJsonable(json.getJSONObject("cause"));
        if (json.has("stackElements")) {
            List<StackTraceElement> elements = new ArrayList<>();
            JSONArray stackElemArr = json.getJSONArray("stackElements");
            for (int i = 0; i < stackElemArr.length(); i++) {
                JSONObject stackElem = stackElemArr.getJSONObject(i);
                String clazz = null;
                if (stackElem.has("class"))
                    clazz = stackElem.getString("class");
                String method = null;
                if (stackElem.has("method"))
                    method = stackElem.getString("method");
                String file = null;
                if (stackElem.has("file"))
                    file = stackElem.getString("file");
                int line = 0;
                if (stackElem.has("native") && stackElem.getBoolean("native"))
                    line = -2;
                else if (stackElem.has("line"))
                    line = stackElem.getInt("line");
                elements.add(new StackTraceElement(clazz, method, file, line));
            }
            stackElements = elements.toArray(new StackTraceElement[0]);
        }
    }

    @Override
    public JSONObject getJson() throws JSONException {
        JSONObject json = new JSONObject();
        if (throwable != null)
            json.put("throwable", throwable);
        if (message != null)
            json.put("message", message);
        if (cause != null)
            json.put("cause", cause.getJson());
        if (stackElements != null) {
            JSONArray stackElemArr = new JSONArray();
            for (int i = 0; i < stackElements.length; i++) {
                StackTraceElement stackElement = stackElements[i];
                JSONObject stackElem = new JSONObject();
                if (stackElement.getClassName() != null)
                    stackElem.put("class", stackElement.getClassName());
                if (stackElement.getMethodName() != null)
                    stackElem.put("method", stackElement.getMethodName());
                if (stackElement.getFileName() != null)
                    stackElem.put("file", stackElement.getFileName());
                if (stackElement.getLineNumber() > 0)
                    stackElem.put("line", stackElement.getLineNumber());
                if (stackElement.isNativeMethod())
                    stackElem.put("native", stackElement.isNativeMethod());
            }
            json.put("stackElements", stackElemArr);
        }

        return json;
    }

    @Override
    public String getJsonName() {
        return "throwable";
    }

    public Throwable toThrowable() throws Exception {
        return toThrowable(null);
    }

    public Throwable toThrowable(ClassLoader classLoader) throws Exception {
        Class<? extends Throwable> clazz;
        if (classLoader == null)
            clazz = Class.forName(throwable).asSubclass(Throwable.class);
        else
            clazz = Class.forName(throwable, true, classLoader).asSubclass(Throwable.class);

        Constructor<? extends Throwable> ctor;
        try {
            if (cause == null) {
                if (message == null) {
                    return clazz.newInstance();
                }
                else {
                    ctor = clazz.getConstructor(String.class);
                    return ctor.newInstance(message);
                }
            }
            else {
                if (message == null) {
                    ctor = clazz.getConstructor(Throwable.class);
                    return ctor.newInstance(cause.toThrowable());
                }
                else {
                    ctor = clazz.getConstructor(String.class, Throwable.class);
                    return ctor.newInstance(message, cause.toThrowable());
                }
            }
        }
        catch (NoSuchMethodException ex) {
            // no appropriate constructor
            return clazz.newInstance();
        }
    }
}
