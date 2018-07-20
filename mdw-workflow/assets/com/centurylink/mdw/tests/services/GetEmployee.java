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
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = { "workstationId", "sapId" })
@XmlRootElement(name = "GetEmployee")
public class GetEmployee {

    protected String workstationId;
    protected String sapId;

    /**
     * Gets the value of the workstationId property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getWorkstationId() {
        return workstationId;
    }

    /**
     * Sets the value of the workstationId property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setWorkstationId(String value) {
        this.workstationId = value;
    }

    /**
     * Gets the value of the sapId property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getSapId() {
        return sapId;
    }

    /**
     * Sets the value of the sapId property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setSapId(String value) {
        this.sapId = value;
    }

}
