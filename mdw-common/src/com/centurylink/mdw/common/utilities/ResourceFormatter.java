/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.utilities;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.json.JSONException;
import org.json.JSONObject;

public class ResourceFormatter {

    public enum Format {
        xml,
        json,
        text
    }

    private Format format;
    private int indent = 0;

    public ResourceFormatter(Format format) {
        this(format, 0);
    }

    public ResourceFormatter(Format format, int indent) {
        this.format = format;
        this.indent = indent;
    }

    public String format(Object obj) throws FormatException {
        if (format == Format.xml) {
            if (obj instanceof XmlObject) {
                XmlObject xmlBean = (XmlObject) obj;
                return xmlBean.xmlText(getXmlOptions());
            }
            else {
                throw new IllegalArgumentException("TODO: JavaBean XML support");
            }
        }
        else if (format == Format.json) {
            if (obj instanceof XmlObject) {
                XmlObject xmlBean = (XmlObject) obj;
                try {
                    JSONObject jsonObject = org.json.XML.toJSONObject(xmlBean.xmlText());
                    return indent == 0 ? jsonObject.toString() : jsonObject.toString(indent);
                }
                catch (JSONException ex) {
                    throw new FormatException(ex.getMessage(), ex);
                }
            }
            else if (obj instanceof JSONObject) {
                JSONObject jsonObject = (JSONObject) obj;
                try {
                    return indent == 0 ? jsonObject.toString() : jsonObject.toString(indent);
                }
                catch (JSONException ex) {
                    throw new FormatException(ex.getMessage(), ex);
                }
            }
            else {
                throw new FormatException("TODO: JavaBean XML support");
            }
        }
        else if (format == Format.text) {
            return String.valueOf(obj);
        }
        else {
            throw new IllegalStateException("Unsupported resource format: " + format);
        }
    }

    public Object parse(String text) throws ParseException {
        if (format == Format.xml) {
            try {
                return XmlObject.Factory.parse(text);
            }
            catch (XmlException ex) {
                throw new ParseException(ex.getMessage(), ex);
            }
        }
        else if (format == Format.json) {
            try {
                return new JSONObject(text);
            }
            catch (JSONException ex) {
                throw new ParseException(ex.getMessage(), ex);
            }
        }
        else if (format == Format.text) {
            return text;
        }
        else {
            throw new IllegalStateException("Unsupported resource format: " + format);
        }
    }

    public XmlOptions getXmlOptions() {
        XmlOptions options = new XmlOptions();
        if (indent != 0)
            options.setSavePrettyPrint().setSavePrettyPrintIndent(indent);
        return options;
    }

}
