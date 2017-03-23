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
package com.centurylink.mdw.plugin.designer.model;

import java.util.ArrayList;
import java.util.List;

import com.centurylink.mdw.model.value.attribute.RuleSetVO;

public class XmlDoc extends WorkflowAsset {
    public XmlDoc() {
        super();
    }

    public XmlDoc(RuleSetVO ruleSetVO, WorkflowPackage packageVersion) {
        super(ruleSetVO, packageVersion);
    }

    public XmlDoc(XmlDoc cloneFrom) {
        super(cloneFrom);
    }

    @Override
    public String getTitle() {
        return "XML Document";
    }

    @Override
    public String getIcon() {
        if (RuleSetVO.WSDL.equals(getLanguage()))
            return "wsdl.gif";
        else if (RuleSetVO.XSD.equals(getLanguage()))
            return "xsd.gif";
        else if (RuleSetVO.XSL.equals(getLanguage()))
            return "xsl.gif";
        else
            return "xml.gif";
    }

    @Override
    public String getDefaultExtension() {
        return RuleSetVO.getFileExtension(RuleSetVO.XML);
    }

    private static List<String> xmlDocLanguages;

    @Override
    public List<String> getLanguages() {
        if (xmlDocLanguages == null) {
            xmlDocLanguages = new ArrayList<String>();
            xmlDocLanguages.add(RuleSetVO.XML);
            xmlDocLanguages.add(RuleSetVO.WSDL);
            xmlDocLanguages.add(RuleSetVO.XSL);
            xmlDocLanguages.add(RuleSetVO.XSD);
        }
        return xmlDocLanguages;
    }
}
