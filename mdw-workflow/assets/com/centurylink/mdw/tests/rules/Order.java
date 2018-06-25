package com.centurylink.mdw.tests.rules;

import java.util.Date;

import org.json.JSONObject;

import com.centurylink.mdw.model.Jsonable;

import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiModelProperty.AccessMode;

public class Order implements Jsonable {
    
    public Order(JSONObject json) {
        bind(json);
    }
    
    public enum Region {
        Eastern,
        Western
    }
    
    private Region region;
    public Region getRegion() { return region; }
    public void setRegion(Region region) { this.region = region; }
    @ApiModelProperty(accessMode=AccessMode.READ_ONLY)
    public String getRegionString() { return region == null ? null : region.toString(); }
    
    private Date dueDate;
    public Date getDueDate() { return dueDate; }
    public void setDueDate(Date dueDate) { this.dueDate = dueDate; }
    
    private int quotedPrice;
    public int getQuotedPrice() { return quotedPrice; }
    public void setQuotedPrice(int price) { this.quotedPrice = price; }
    
    private String orderNumber;
    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }
    
    
}
