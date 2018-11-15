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
package com.centurylink.mdw.model.asset;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.centurylink.mdw.model.Jsonable;
import com.centurylink.mdw.util.JsonUtil;

public class Pagelet implements Jsonable {

    private Map<String,String> attributes;
    public Map<String,String> getAttributes() { return attributes; }
    public void setAttributes(Map<String,String> attrs) { this.attributes = attrs; }

    public String getAttribute(String name) {
        if (attributes == null)
            return null;
        else
            return attributes.get(name);
    }

    public void setAttribute(String name, String val) {
        if (attributes == null)
            attributes = new HashMap<String,String>();
        this.attributes.put(name, val);
    }

    private String implCategory;

    private List<Widget> widgets = new ArrayList<Widget>();
    public void addWidget(Widget widget) {
        widgets.add(widget);
    }
    public List<Widget> getWidgets() {
        List<Widget> allWidgets = new ArrayList<>(widgets);
        if (widgetProviders != null) {
            for (WidgetProvider provider : widgetProviders) {
                List<Widget> providedWidgets = provider.getWidgets(implCategory);
                if (providedWidgets != null) {
                    allWidgets.addAll(providedWidgets);
                }
            }
        }
        return allWidgets;
    }

    private List<WidgetProvider> widgetProviders;
    public void addWidgetProvider(WidgetProvider widgetProvider) {
        if (widgetProviders == null) {
            widgetProviders = new ArrayList<>();
        }
        widgetProviders.add(widgetProvider);
    }
    public void removeWidgetProvider(WidgetProvider widgetProvider) {
        if (widgetProviders != null) {
            widgetProviders.remove(widgetProvider);
        }
    }

    public Pagelet(String source) throws Exception {
        this(null, source);
    }

    /**
     * Note: changes widget type and attribute names into lower case.
     */
    public Pagelet(String implCategory, String source) throws Exception {
        this.implCategory = implCategory;

        if (source.startsWith("{")) {
            fromJson(new JSONObject(source));
            return;
        }

        InputSource inputSource = new InputSource(new ByteArrayInputStream(source.getBytes()));
        SAXParserFactory parserFactory = SAXParserFactory.newInstance();

        SAXParser parser = parserFactory.newSAXParser();
        parser.parse(inputSource, new DefaultHandler() {
            private Stack<Widget> widgs = new Stack<>();
            private Map<String,String> widgNameToElem = new HashMap<>();
            private boolean inOpt;
            private boolean hasParam;

            // attributes for workflow project
            public void startElement(String uri, String localName, String name, Attributes attrs)
                    throws SAXException {
                if (name.equals("PAGELET")) {
                    for (int i = 0; i < attrs.getLength(); i++) {
                        setAttribute(attrs.getQName(i).toLowerCase(), attrs.getValue(i));
                    }
                }
                else if (name.equals("OPTION")) {
                    Map<String,String> attrMap = new HashMap<>();
                    for (int i = 0; i < attrs.getLength(); i++) {
                        attrMap.put(attrs.getQName(i).toLowerCase(), attrs.getValue(i));
                    }
                    if (!attrMap.containsKey("parameter"))
                        inOpt = true;
                    else {
                        hasParam = true;
                        String widgName = attrMap.get("parameter");
                        String translatedType;
                        if (attrMap.size() == 1) {
                            translatedType = "text";
                            attrMap.put("vw" , "100");
                        }
                        else {
                            translatedType = translateType(widgName, attrMap);
                        }
                        Widget widget = new Widget(widgName, translatedType);
                        attrMap.put("label" , widgName);
                        widgNameToElem.put(widget.getName(), name);
                        if (widgs.isEmpty())
                            widgets.add(widget);
                        else
                            widgs.peek().addWidget(widget);
                        widget.setAttributes(attrMap);
                        widgs.push(widget);
                    }
                }
                else {
                    Map<String,String> attrsMap = new HashMap<>();
                    for (int i = 0; i < attrs.getLength(); i++) {
                        attrsMap.put(attrs.getQName(i).toLowerCase(), attrs.getValue(i));
                    }
                    String type = translateType(name.toLowerCase(), attrsMap);
                    Widget widget = new Widget(attrsMap.get("name"), type);
                    attrsMap.remove("name");
                    attrsMap.remove("type");
                    widget.setAttributes(attrsMap);
                    if (widget.getName() != null)
                        widgNameToElem.put(widget.getName(), name);
                    if (widgs.isEmpty())
                        widgets.add(widget);
                    else
                        widgs.peek().addWidget(widget);
                    widgs.push(widget);
                }
            }

            @Override
            public void endElement(String uri, String localName, String name) throws SAXException {
                if (name.equals("OPTION") && this.inOpt) {
                    inOpt = false;
                }
                else {
                    if (!widgs.isEmpty()) {
                        String elemName = widgNameToElem.get(widgs.peek().getName());
                        // assumes unnamed widgets can't have children
                        if (elemName == null || elemName.equals(name))
                            widgs.pop();
                    }
                }
            }

            public void characters(char[] ch, int start, int length) throws SAXException {
                if (!widgs.isEmpty()) {
                    if (inOpt) {
                        widgs.peek().addOption(new String(ch).substring(start, start + length).trim());
                    }
                    else if (hasParam) {
                        String option = new String(ch).substring(start, start + length).trim();
                        if (!option.isEmpty()) {
                            widgets.get(widgets.size() - 1).addOption(option);
                            widgs.peek().setAttribute("parentValue", option);
                        }
                        hasParam = false;
                    }
                    else if (widgs.peek().getName() == null) {
                        widgs.peek().setName(new String(ch).substring(start, start + length).trim());
                    }
                }
            }
        });

        adjustWidgets(implCategory);
    }

    /**
     * Translate widget type.
     */
    private String translateType(String type, Map<String,String> attrs) {
        String translated = type.toLowerCase();
        if ("select".equals(type))
            translated = "radio";
        else if ("boolean".equals(type))
            translated = "checkbox";
        else if ("list".equals(type)) {
            translated = "picklist";
            String lbl = attrs.get("label");
            if (lbl == null)
                lbl = attrs.get("name");
            if ("Output Documents".equals(lbl)) {
                attrs.put("label", "Documents");
                attrs.put("unselectedLabel", "Read-Only");
                attrs.put("selectedLabel", "Writable");
            }
        }
        else if ("hyperlink".equals(type)) {
            if (attrs.containsKey("url"))
                translated = "link";
            else
                translated = "text";
        }
        else if ("rule".equals(type)) {
            if ("EXPRESSION".equals(attrs.get("type"))) {
                translated = "expression";
            }
            else if ("TRANSFORM".equals(attrs.get("type"))) {
                translated = "edit";
                attrs.put("label", "Transform");
            }
            else  {
                translated = "edit";
                attrs.put("label", "Script");
            }
            attrs.remove("type");
        }
        else if ("Java".equals(attrs.get("name"))) {
            translated = "edit";
        }
        else {
            // asset-driven
            String source = attrs.get("source");
            if ("Process".equals(source)) {
                translated = "asset";
                attrs.put("source", "proc");
            }
            else if ("TaskTemplates".equals(source)) {
                translated = "asset";
                attrs.put("source", "task");
            }
            else if ("RuleSets".equals(source)) {
                String format = attrs.get("type");
                if (format != null) {
                    String exts = "";
                    String[] formats = format.split(",");
                    for (int i = 0; i < formats.length; i++) {
                        String ext = Asset.getFileExtension(formats[i]);
                        if (ext != null) {
                            if (exts.length() > 0)
                                exts += ",";
                            exts += ext.substring(1);
                        }
                    }
                    if (exts.length() > 0) {
                        translated = "asset";
                        attrs.put("source", exts);
                    }
                }
            }
        }
        return translated;
    }

    /**
     * Adds companion widgets as needed.
     */
    private void adjustWidgets(String implCategory) {
        // adjust to add script language options param
        Map<Integer,Widget> companions = new HashMap<>();
        for (int i = 0; i < widgets.size(); i++) {
            Widget widget = widgets.get(i);
            if ("expression".equals(widget.type) || ("edit".equals(widget.type) && !"Java".equals(widget.name))) {
                Widget companion = new Widget("SCRIPT", "dropdown");
                companion.setAttribute("label", "Language");
                companion.options = Arrays.asList(widget.getAttribute("languages").split(","));
                if (companion.options.contains("Groovy"))
                    companion.setAttribute("default", "Groovy");
                else if (companion.options.contains("Kotlin Script"))
                    companion.setAttribute("default", "Kotlin Script");
                String section = widget.getAttribute("section");
                if (section != null)
                    companion.setAttribute("section", section);
                companions.put(i, companion);
            }
        }
        int offset = 0;
        for (int idx : companions.keySet()) {
            widgets.add(idx + offset, companions.get(idx));
            offset++;
        }
    }

    public Pagelet(JSONObject json) {
        fromJson(json);
    }

    private void fromJson(JSONObject json) {
        if (json.has("widgets")) {
            JSONArray widgetsJson = json.getJSONArray("widgets");
            for (int i = 0; i < widgetsJson.length(); i++) {
                widgets.add(new Widget(widgetsJson.getJSONObject(i)));
            }
            if (json.has("attributes"))
                attributes = JsonUtil.getMap(json.getJSONObject("attributes"));
        }
    }

    public JSONObject getJson() throws JSONException {
        JSONObject json = create();
        JSONObject attrsJson = JsonUtil.getJson(attributes);
        if (attrsJson != null)
            json.put("attributes", attrsJson);
        JSONArray widgetsJson = new JSONArray();
        for (Widget w : getWidgets()) {
            widgetsJson.put(w.getJson());
        }
        json.put("widgets", widgetsJson);
        return json;
    }

    public String getJsonName() {
        return "pagelet";
    }

    public static class Widget implements Jsonable {

        public Widget(String name, String type) {
            this.name = name;
            this.type = type;
        }

        private String type;
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }


        private String name;
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        private Object value;
        public Object getValue() { return value; }
        public void setValue(Object value) { this.value = value; }

        private Map<String,String> attributes;
        /**
         * everything but name, type and options
         */
        public Map<String,String> getAttributes() { return attributes; }
        public void setAttributes(Map<String,String> attrs) { this.attributes = attrs; }

        private List<String> options;
        public List<String> getOptions() { return options; }
        public void setOptions(List<String> options) { this.options = options; }

        private List<Widget> widgets;
        public List<Widget> getWidgets() { return widgets; }
        public void setWidgets(List<Widget> widgets) { this.widgets = widgets; }

        private WidgetAdapter adapter;
        public WidgetAdapter getAdapter() { return adapter; }
        public void setAdapter(WidgetAdapter adapter) { this.adapter = adapter; }

        public void addWidget(Widget widget) {
            if (widgets == null)
                widgets = new ArrayList<>();
            widgets.add(widget);
        }

        public String getAttribute(String name) {
            if (attributes == null)
                return null;
            else
                return attributes.get(name);
        }

        public void setAttribute(String name, String val) {
            if (attributes == null)
                attributes = new HashMap<String,String>();
            attributes.put(name, val);
        }

        public void addOption(String option) {
            if (options == null)
                options = new ArrayList<>();
            options.add(option);
        }

        public Widget(JSONObject json) throws JSONException {
            // name and type are mandatory
            name = json.getString("name");
            type = json.getString("type");

            for (String key : json.keySet()) {
                if (key.equals("options")) {
                    options = new ArrayList<>();
                    JSONArray arr = json.getJSONArray("options");
                    for (int i = 0; i < arr.length(); i++)
                        options.add(arr.getString(i));
                }
                else if (key.equals("widgets")) {
                    widgets = new ArrayList<>();
                    JSONArray arr = json.getJSONArray("widgets");
                    for (int i = 0; i < arr.length(); i++)
                        widgets.add(new Widget(arr.getJSONObject(i)));
                }
                else if (!key.equals("name") && !key.equals("type")) {
                    setAttribute(key, json.get(key).toString());
                }
            }
        }

        public JSONObject getJson() throws JSONException {
            JSONObject json = create();
            json.put("name", name);
            json.put("type", type);
            // don't populate label or hub will display it as an attribute

            if (attributes != null) {
                for (String key : attributes.keySet())
                    json.put(key, attributes.get(key));
            }

            if (options != null) {
                JSONArray arr = new JSONArray();
                for (String option : options)
                    arr.put(option);
                json.put("options", arr);
            }

            if (widgets != null) {
                JSONArray arr = new JSONArray();
                for (Widget widget : widgets)
                    arr.put(widget.getJson());
                json.put("widgets", arr);
            }

            return json;
        }

        public String getJsonName() {
            return "widget";
        }


    }

}
