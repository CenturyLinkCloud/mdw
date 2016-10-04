/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.script;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

public class XmlSlurper implements Slurper {

    private String name;
    public String getName() { return name; }

    private Object input;
    public Object getInput() { return input; }

    public XmlSlurper(String name, String input)
            throws IOException, SAXException, ParserConfigurationException {
        this.name = name;
        this.input = new groovy.util.XmlSlurper().parseText(input);
    }

}
