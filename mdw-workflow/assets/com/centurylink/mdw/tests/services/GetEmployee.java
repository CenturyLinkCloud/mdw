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
