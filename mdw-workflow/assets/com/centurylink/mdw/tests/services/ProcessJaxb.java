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
package com.centurylink.mdw.tests.services;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="elementOne" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="elementTwo" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="attributeOne" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="attributeTwo" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "elementOne",
    "elementTwo"
})
@XmlRootElement(name = "ProcessJaxb")
public class ProcessJaxb {

    @XmlElement(required = true)
    protected String elementOne;
    protected String elementTwo;
    @XmlAttribute(name = "attributeOne", required = true)
    protected String attributeOne;
    @XmlAttribute(name = "attributeTwo")
    protected String attributeTwo;

    /**
     * Gets the value of the elementOne property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getElementOne() {
        return elementOne;
    }

    /**
     * Sets the value of the elementOne property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setElementOne(String value) {
        this.elementOne = value;
    }

    /**
     * Gets the value of the elementTwo property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getElementTwo() {
        return elementTwo;
    }

    /**
     * Sets the value of the elementTwo property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setElementTwo(String value) {
        this.elementTwo = value;
    }

    /**
     * Gets the value of the attributeOne property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getAttributeOne() {
        return attributeOne;
    }

    /**
     * Sets the value of the attributeOne property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setAttributeOne(String value) {
        this.attributeOne = value;
    }

    /**
     * Gets the value of the attributeTwo property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getAttributeTwo() {
        return attributeTwo;
    }

    /**
     * Sets the value of the attributeTwo property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setAttributeTwo(String value) {
        this.attributeTwo = value;
    }

}
