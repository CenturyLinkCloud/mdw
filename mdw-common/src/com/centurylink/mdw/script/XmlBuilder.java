/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.script;

import groovy.lang.Closure;
import groovy.lang.Writable;
import groovy.xml.StreamingMarkupBuilder;
import groovy.xml.XmlUtil;

public class XmlBuilder extends StreamingMarkupBuilder implements Builder {

    private String name;
    public String getName() { return name; }

    public XmlBuilder(String name) {
        this.name = name;
    }

    public String getString() {
        return XmlUtil.serialize(result);
    }

    private Writable result;
    public Object call(Closure<?> closure) {
        result = (Writable) bind(closure);
        return result;
    }
}
