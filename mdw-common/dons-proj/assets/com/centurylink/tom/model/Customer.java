/**
 * Copyright (c) 2017 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.tom.model;

import org.json.JSONObject;

import com.centurylink.mdw.model.Jsonable;

import io.swagger.annotations.ApiModel;

@ApiModel(value="Customer", description="Customer for an order")
public class Customer implements Jsonable {
    
    public Customer(JSONObject json) {
        bind(json);    
    }
    
    private String customerType;
    public String getCustomerType() { return customerType; }
    public void setCustomerType(String customerType) { this.customerType = customerType; }

    private String accountType;
    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }

    private Name name;
    public Name getName() { return name; }
    public void setName(Name name) { this.name = name; }
    
    private Agent agent;
    public Agent getAgent() { return agent; }
    public void setAgent(Agent agent) { this.agent = agent; }
}
