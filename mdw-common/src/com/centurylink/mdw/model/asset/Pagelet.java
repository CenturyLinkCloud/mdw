/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
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


    private Map<String,String> attributes = null;
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
     * Changes widget type and attribute names into lower case.
     */
    public Pagelet(String xml) throws Exception {
        InputSource src = new InputSource(new ByteArrayInputStream(xml.getBytes()));
        SAXParserFactory parserFactory = SAXParserFactory.newInstance();

        SAXParser parser = parserFactory.newSAXParser();
        parser.parse(src, new DefaultHandler() {
            // attributes for workflow project
            public void startElement(String uri, String localName, String name, Attributes attrs)
            throws SAXException {
                if (name.equals("PAGELET")) {
                    for (int i = 0; i < attrs.getLength(); i++) {
                        setAttribute(attrs.getQName(i).toLowerCase(), attrs.getValue(i));
                    }
                }
                else {
                    Widget w = new Widget(name.toLowerCase());
                    for (int i = 0; i < attrs.getLength(); i++) {
                        w.setAttribute(attrs.getQName(i).toLowerCase(), attrs.getValue(i));
                    }
                    widgets.add(w);
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

        private Map<String,String> attributes = null;
        public Map<String,String> getAttributes() { return attributes; }
        public void setAttributes(Map<String,String> attrs) { this.attributes = attrs; }

        public String getAttribute(String name) {
            if (attributes == null)
                return null;
            else
                return attributes.get(name);
        }

        public Widget(JSONObject json) throws JSONException {
            type = json.getString("type");
            if (json.has("attributes"))
                attributes = JsonUtil.getMap(json.getJSONObject("attributes"));
        }

        public void setAttribute(String name, String val) {
            if (attributes == null)
                attributes = new HashMap<String,String>();
            this.attributes.put(name, val);
        }

        public JSONObject getJson() throws JSONException {
            JSONObject json = new JSONObject();
            json.put("type", type);
            JSONObject attrsJson = JsonUtil.getJson(attributes);
            if (attrsJson != null)
                json.put("attributes", attrsJson);
            return json;
        }
        public String getJsonName() {
            return "widget";
        }
    }

}
