/**
 * Copyright (c) 2017 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.tom.model;

import java.util.Date;
import java.util.List;

import org.json.JSONObject;

import com.centurylink.mdw.model.Jsonable;

import io.swagger.annotations.ApiModelProperty;

public class Order implements Jsonable {
    
    public Order(JSONObject json) {
        bind(json);    
    }
    
    public Order(String id){
        this.id = id;
    }
    
    private String id;
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    @ApiModelProperty(allowableValues="N,C,X")    
    private String action;
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    private int version;
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
    
    public Date created;
    public Date getCreated() { return created; }
    public void setCreated(Date created) { this.created = created; }
    
    public Date dueDate;
    public Date getDueDate() { return dueDate; }
    public void setDueDate(Date dueDate) { this.dueDate = dueDate; }
    
    private List<Remark> remarks;
    public List<Remark> getRemarks() { return remarks; }
    public void setRemarks(List<Remark> remarks) { this.remarks = remarks; }
    
    private String mtn;
    public String getMtn() { return mtn; }
    public void setMtn(String mtn) { this.mtn = mtn; }
    
    private String newField;
    public String getNewField() { return newField; }
    public void setNewField(String newField) { this.newField = newField; }
    
    private String projectId;
    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    
    private String originatorId;
    public String getOriginatorId() { return originatorId; }
    public void setOriginatorId(String origId) { this.originatorId = origId; }
    
    private Customer customer;
    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }
    
    // TODO: should this be List<WorkItem>?
    private WorkItem workItem;
    public WorkItem getWorkItem() { return workItem; }
    public void setWorkItem(WorkItem workItem) { this.workItem = workItem; }
    
    // TODO: should this be List<OrderItem>?
    private OrderItem orderItem;
    public OrderItem getOrderItem() { return orderItem; }
    public void setOrderItem(OrderItem orderItem) { this.orderItem = orderItem; }
    
    
}
