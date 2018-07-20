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
package com.centurylink.mdw.tests.cloud;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

// asset-defined jaxb asset java class

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "requiredElement",
    "optionalElement"
})
@XmlRootElement(name = "JaxbAsset")
public class JaxbAsset implements java.io.Serializable {

    @XmlElement(name = "RequiredElement", required = true)
    protected String requiredElement;
    public String getRequiredElement() {
        return requiredElement;
    }
    public void setRequiredElement(String value) {
        this.requiredElement = value;
    }

    @XmlElement(name = "OptionalElement")
    protected String optionalElement;
    public String getOptionalElement() {
        return optionalElement;
    }
    public void setOptionalElement(String value) {
        this.optionalElement = value;
    }

    @XmlAttribute(required = true)
    protected String requiredAttribute;
    public String getRequiredAttribute() {
        return requiredAttribute;
    }
    public void setRequiredAttribute(String value) {
        this.requiredAttribute = value;
    }

    @XmlAttribute
    protected String optionalAttribute;
    public String getOptionalAttribute() {
        return optionalAttribute;
    }
    public void setOptionalAttribute(String value) {
        this.optionalAttribute = value;
    }
}
