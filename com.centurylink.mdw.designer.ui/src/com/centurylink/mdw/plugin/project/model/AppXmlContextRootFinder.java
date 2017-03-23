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
package com.centurylink.mdw.plugin.project.model;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Parses the application.xml file to extract the specified context root.
 */
public class AppXmlContextRootFinder extends DefaultHandler {
    private boolean inModule = false;
    private boolean inWeb = false;
    private boolean inContextRoot = false;
    private String moduleId;
    private String contextRoot;

    public String getContextRoot() {
        return contextRoot;
    }

    private String moduleToFind;

    AppXmlContextRootFinder(String moduleToFind) {
        this.moduleToFind = moduleToFind;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attrs)
            throws SAXException {
        if (qName.equals("module")) {
            inModule = true;
            moduleId = attrs.getValue("id");
        }
        else if (qName.equals("web")) {
            inWeb = true;
        }
        else if (qName.equals("context-root")) {
            inContextRoot = true;
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        if (inModule && inWeb && inContextRoot && moduleToFind != null
                && moduleToFind.equals(moduleId)) {
            contextRoot = new String(ch).substring(start, start + length);
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (qName.equals("module")) {
            inModule = false;
            moduleId = null;
        }
        else if (qName.equals("web")) {
            inWeb = false;
        }
        else if (qName.equals("context-root")) {
            inContextRoot = false;
        }
    }
}
