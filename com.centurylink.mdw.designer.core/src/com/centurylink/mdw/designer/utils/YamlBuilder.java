/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.designer.utils;

public class YamlBuilder {

    private StringBuilder stringBuilder;

    private String newLine = "\n";

    public YamlBuilder() {
        stringBuilder = new StringBuilder();
    }

    public YamlBuilder(String newLine) {
        this();
        this.newLine = newLine;
    }

    public YamlBuilder append(String s) {
        if (s != null)
            stringBuilder.append(s);
        return this;
    }

    public YamlBuilder append(long l) {
        return append(String.valueOf(l));
    }

    public YamlBuilder appendMulti(String indent, String value) {
        if (value != null) {
            String[] lines = value.split("\r\n");
            if (lines.length == 1)
                lines = value.split("\n");
            if (lines.length == 1) {
                stringBuilder.append(value);
            }
            else {
                stringBuilder.append("|").append(newLine);
                for (int i = 0; i < lines.length; i++) {
                    stringBuilder.append(indent).append(lines[i]);
                    if (i < lines.length - 1)
                        stringBuilder.append(newLine);
                }
            }
        }
        return this;
    }

    public YamlBuilder newLine() {
        return append(newLine);
    }

    public int length() {
        return stringBuilder.length();
    }

    public String toString() {
        // trim extra newline if present
        String str = stringBuilder.toString();
        if (str.endsWith(newLine))
          str = str.substring(0, str.length() - newLine.length());
        return str;
    }
}
