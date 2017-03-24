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
package com.centurylink.mdw.model;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.Jsonable;

/**
 * Represents a Throwable as a Jsonable for JSON serialization.
 */
public class JsonableThrowable implements Jsonable {

    private String throwable;
    public String getThrowable() { return throwable; }

    private String message;
    public String getMessage() { return message; }

    private JsonableThrowable cause;
    public JsonableThrowable getCause() { return cause; }

    private StackTraceElement[] stackElements;
    public StackTraceElement[] getStackElements() { return stackElements; }

    public JsonableThrowable(Throwable th) {
        this.throwable = th.getClass().getName();
        this.message = th.getMessage();
        if (th.getCause() != null)
            this.cause = new JsonableThrowable(th.getCause());
        this.stackElements = th.getStackTrace();
    }

    public JsonableThrowable(JSONObject json) throws JSONException {
        if (json.has("throwable"))
            throwable = json.getString("throwable");
        if (json.has("message"))
            message = json.getString("message");
        if (json.has("cause"))
            cause = new JsonableThrowable(json.getJSONObject("cause"));
        if (json.has("stackElements"))
            stackElements = getStackElements(json.getJSONArray("stackElements"));
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
        JSONArray stackElementsJson = getStackElementsJson(stackElements);
        if (stackElementsJson != null)
            json.put("stackElements", stackElementsJson);

        return json;
    }

    public static StackTraceElement[] getStackElements(JSONArray stackElemArr) throws JSONException {
        if (stackElemArr == null) {
            return null;
        }
        else {
            List<StackTraceElement> elements = new ArrayList<>();
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
            return elements.toArray(new StackTraceElement[0]);
        }
    }

    public static JSONArray getStackElementsJson(StackTraceElement[] stackElements) throws JSONException {
        JSONArray stackElemArr = null;
        if (stackElements != null) {
            stackElemArr = new JSONArray();
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
                stackElemArr.put(stackElem);
            }
        }
        return stackElemArr;
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

        Throwable th;
        Constructor<? extends Throwable> ctor;
        try {
            if (cause == null) {
                if (message == null) {
                    th = clazz.newInstance();
                }
                else {
                    ctor = clazz.getConstructor(String.class);
                    th = ctor.newInstance(message);
                }
            }
            else {
                if (message == null) {
                    ctor = clazz.getConstructor(Throwable.class);
                    th =  ctor.newInstance(cause.toThrowable());
                }
                else {
                    ctor = clazz.getConstructor(String.class, Throwable.class);
                    th = ctor.newInstance(message, cause.toThrowable());
                }
            }
        }
        catch (NoSuchMethodException ex) {
            // no appropriate constructor
            th = clazz.newInstance();
        }
        if (stackElements != null)
            th.setStackTrace(stackElements);
        return th;
    }
}
