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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.centurylink.mdw.common.service.Jsonable;
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

    private List<Widget> widgets = new ArrayList<Widget>();

    /**
     * Note: changes widget type and attribute names into lower case.
     */
    public Pagelet(String xml) throws Exception {
        InputSource src = new InputSource(new ByteArrayInputStream(xml.getBytes()));
        SAXParserFactory parserFactory = SAXParserFactory.newInstance();

        SAXParser parser = parserFactory.newSAXParser();
        parser.parse(src, new DefaultHandler() {
            private Widget curWidg = null;
            private boolean inOpt;

            // attributes for workflow project
            public void startElement(String uri, String localName, String name, Attributes attrs)
            throws SAXException {
                if (name.equals("PAGELET")) {
                    for (int i = 0; i < attrs.getLength(); i++) {
                        setAttribute(attrs.getQName(i).toLowerCase(), attrs.getValue(i));
                    }
                }
                else if (name.equals("OPTION")) {
                    inOpt = true;
                }
                else {
                    curWidg = new Widget(name.toLowerCase());
                    for (int i = 0; i < attrs.getLength(); i++) {
                        curWidg.setAttribute(attrs.getQName(i).toLowerCase(), attrs.getValue(i));
                    }
                    widgets.add(curWidg);
                }
            }

            @Override
            public void endElement(String uri, String localName, String name) throws SAXException {
                if (name.equals("OPTION"))
                    inOpt = false;
                else
                    curWidg = null;
            }

            public void characters(char[] ch, int start, int length) throws SAXException {
                if (curWidg != null) {
                    if (inOpt) {
                        curWidg.addOption(new String(ch).substring(start, start + length).trim());
                    }
                    else if (curWidg.getAttribute("name") == null) {
                        curWidg.setAttribute("name", new String(ch).substring(start, start + length).trim());
                    }
                }
            }
        });
    }

    public Pagelet(JSONObject json) throws JSONException {
        JSONArray widgetsJson = json.getJSONArray("widgets");
        for (int i = 0; i < widgetsJson.length(); i++) {
            widgets.add(new Widget(widgetsJson.getJSONObject(i)));
        }
        if (json.has("attributes"))
            attributes = JsonUtil.getMap(json.getJSONObject("attributes"));
    }


    public JSONObject getJson() throws JSONException {
        JSONObject json = new JSONObject();
        JSONObject attrsJson = JsonUtil.getJson(attributes);
        if (attrsJson != null)
            json.put("attributes", attrsJson);
        JSONArray widgetsJson = new JSONArray();
        for (Widget w : widgets) {
            widgetsJson.put(w.getJson());
        }
        json.put("widgets", widgetsJson);
        return json;
    }

    public String getJsonName() {
        return "pagelet";
    }

    public class Widget implements Jsonable {

        public Widget(String type) {
            this.type = type;
        }

        private String type;
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        private Map<String,String> attributes;
        public Map<String,String> getAttributes() { return attributes; }
        public void setAttributes(Map<String,String> attrs) { this.attributes = attrs; }

        private List<String> options;
        public List<String> getOptions() { return options; }
        public void setOptions(List<String> options) { this.options = options; }

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


        public void addOption(String option) {
            if (options == null)
                options = new ArrayList<>();
            options.add(option);
        }

        public Widget(JSONObject json) throws JSONException {
            type = json.getString("type");
            if (json.has("attributes"))
                attributes = JsonUtil.getMap(json.getJSONObject("attributes"));
            if (json.has("options")) {
                options = new ArrayList<>();
                JSONArray arr = json.getJSONArray("options");
                for (int i = 0; i < arr.length(); i++)
                    options.add(arr.getString(i));
            }
        }

        public JSONObject getJson() throws JSONException {
            JSONObject json = new JSONObject();
            json.put("type", type);
            JSONObject attrsJson = JsonUtil.getJson(attributes);
            if (attrsJson != null)
                json.put("attributes", attrsJson);
            if (options != null) {
                JSONArray arr = new JSONArray();
                for (String option : options)
                    arr.put(option);
                json.put("options", arr);
            }
            return json;
        }

        public String getJsonName() {
            return "widget";
        }
    }

}
