/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.script;

public class JsonBuilder extends groovy.json.JsonBuilder implements Builder {

    private String name;
    public String getName() { return name; }

    public JsonBuilder(String name) {
        this.name = name;
    }

    public String getString() {
        return toString();
    }
}
