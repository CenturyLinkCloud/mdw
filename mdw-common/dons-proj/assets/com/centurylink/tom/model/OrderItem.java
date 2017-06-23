/**
 * Copyright (c) 2017 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.tom.model;

import org.json.JSONObject;

import com.centurylink.mdw.model.Jsonable;

public class OrderItem implements Jsonable {
    
    public OrderItem(JSONObject json) {
        bind(json);    
    }
    
    private Product product;
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
}
