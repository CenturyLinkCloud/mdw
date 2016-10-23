/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.service.types;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for Task complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="Task">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="MasterRequestId" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="Priority" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/>
 *         &lt;element name="DueInSeconds" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/>
 *         &lt;element name="User" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="Assignee" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="Workgroups" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="Comments" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="OwnerApplicationName" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="AssociatedTaskInstanceId" type="{http://www.w3.org/2001/XMLSchema}long" minOccurs="0"/>
 *         &lt;element name="ProcessInstanceId" type="{http://www.w3.org/2001/XMLSchema}long" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="logicalId" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="instanceId" type="{http://www.w3.org/2001/XMLSchema}long" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Task", propOrder = {
    "masterRequestId",
    "priority",
    "dueInSeconds",
    "user",
    "assignee",
    "workgroups",
    "comments",
    "ownerApplicationName",
    "associatedTaskInstanceId",
    "processInstanceId"
})
public class Task {

    @XmlElement(name = "MasterRequestId")
    protected String masterRequestId;
    @XmlElement(name = "Priority")
    protected Integer priority;
    @XmlElement(name = "DueInSeconds")
    protected Integer dueInSeconds;
    @XmlElement(name = "User")
    protected String user;
    @XmlElement(name = "Assignee")
    protected String assignee;
    @XmlElement(name = "Workgroups")
    protected String workgroups;
    @XmlElement(name = "Comments")
    protected String comments;
    @XmlElement(name = "OwnerApplicationName")
    protected String ownerApplicationName;
    @XmlElement(name = "AssociatedTaskInstanceId")
    protected Long associatedTaskInstanceId;
    @XmlElement(name = "ProcessInstanceId")
    protected Long processInstanceId;
    @XmlAttribute
    protected String logicalId;
    @XmlAttribute
    protected Long instanceId;

    /**
     * Gets the value of the masterRequestId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getMasterRequestId() {
        return masterRequestId;
    }

    /**
     * Sets the value of the masterRequestId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setMasterRequestId(String value) {
        this.masterRequestId = value;
    }

    /**
     * Gets the value of the priority property.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getPriority() {
        return priority;
    }

    /**
     * Sets the value of the priority property.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setPriority(Integer value) {
        this.priority = value;
    }

    /**
     * Gets the value of the dueInSeconds property.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getDueInSeconds() {
        return dueInSeconds;
    }

    /**
     * Sets the value of the dueInSeconds property.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setDueInSeconds(Integer value) {
        this.dueInSeconds = value;
    }

    /**
     * Gets the value of the user property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getUser() {
        return user;
    }

    /**
     * Sets the value of the user property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setUser(String value) {
        this.user = value;
    }

    /**
     * Gets the value of the assignee property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getAssignee() {
        return assignee;
    }

    /**
     * Sets the value of the assignee property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setAssignee(String value) {
        this.assignee = value;
    }

    /**
     * Gets the value of the workgroups property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getWorkgroups() {
        return workgroups;
    }

    /**
     * Sets the value of the workgroups property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setWorkgroups(String value) {
        this.workgroups = value;
    }

    /**
     * Gets the value of the comments property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getComments() {
        return comments;
    }

    /**
     * Sets the value of the comments property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setComments(String value) {
        this.comments = value;
    }

    /**
     * Gets the value of the ownerApplicationName property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getOwnerApplicationName() {
        return ownerApplicationName;
    }

    /**
     * Sets the value of the ownerApplicationName property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setOwnerApplicationName(String value) {
        this.ownerApplicationName = value;
    }

    /**
     * Gets the value of the associatedTaskInstanceId property.
     * 
     * @return
     *     possible object is
     *     {@link Long }
     *     
     */
    public Long getAssociatedTaskInstanceId() {
        return associatedTaskInstanceId;
    }

    /**
     * Sets the value of the associatedTaskInstanceId property.
     * 
     * @param value
     *     allowed object is
     *     {@link Long }
     *     
     */
    public void setAssociatedTaskInstanceId(Long value) {
        this.associatedTaskInstanceId = value;
    }

    /**
     * Gets the value of the processInstanceId property.
     * 
     * @return
     *     possible object is
     *     {@link Long }
     *     
     */
    public Long getProcessInstanceId() {
        return processInstanceId;
    }

    /**
     * Sets the value of the processInstanceId property.
     * 
     * @param value
     *     allowed object is
     *     {@link Long }
     *     
     */
    public void setProcessInstanceId(Long value) {
        this.processInstanceId = value;
    }

    /**
     * Gets the value of the logicalId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getLogicalId() {
        return logicalId;
    }

    /**
     * Sets the value of the logicalId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setLogicalId(String value) {
        this.logicalId = value;
    }

    /**
     * Gets the value of the instanceId property.
     * 
     * @return
     *     possible object is
     *     {@link Long }
     *     
     */
    public Long getInstanceId() {
        return instanceId;
    }

    /**
     * Sets the value of the instanceId property.
     * 
     * @param value
     *     allowed object is
     *     {@link Long }
     *     
     */
    public void setInstanceId(Long value) {
        this.instanceId = value;
    }

}
