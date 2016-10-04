/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.script;

public class JsonSlurper implements Slurper {

    private String name;
    public String getName() { return name; }

    private Object input;
    public Object getInput() { return input; }

    public JsonSlurper(String name, String input) {
        this.name = name;
        this.input = new groovy.json.JsonSlurper().parseText(input);
    }
}
