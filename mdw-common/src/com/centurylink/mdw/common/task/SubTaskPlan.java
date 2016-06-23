/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.task;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
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
 *         &lt;element name="SubTask" type="{http://mdw.centurylink.com/task}SubTask" maxOccurs="unbounded"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "subTask"
})
@XmlRootElement(name = "SubTaskPlan")
public class SubTaskPlan {

    @XmlElement(name = "SubTask", required = true)
    protected List<SubTask> subTask;

    /**
     * Gets the value of the subTask property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the subTask property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getSubTask().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link SubTask }
     * 
     * 
     */
    public List<SubTask> getSubTask() {
        if (subTask == null) {
            subTask = new ArrayList<SubTask>();
        }
        return this.subTask;
    }
    
    /**
     * Better method name (pass-through).
     */
    public List<SubTask> getSubTasks() {
        return getSubTask();
    }
    
    public void addSubTasks(String logicalId, int count) {
        SubTask subTask = new SubTask();
        subTask.setLogicalId(logicalId);
        subTask.setCount(new BigInteger(String.valueOf(count)));
        getSubTasks().add(subTask);
    }
    
    public void addSubTask(String logicalId) {
        SubTask subTask = new SubTask();
        subTask.setLogicalId(logicalId);
        getSubTasks().add(subTask);
    }
}
