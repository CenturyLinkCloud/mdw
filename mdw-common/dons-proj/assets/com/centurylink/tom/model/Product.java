/**
 * Copyright (c) 2017 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.tom.model;

import org.json.JSONObject;

import com.centurylink.mdw.model.Jsonable;

public class Product implements Jsonable {
    
    public Product(JSONObject json) {
        bind(json);    
    }
    
    private Service service;
    public Service getService() { return service; }
    public void setService(Service service) { this.service = service; }
    
}
