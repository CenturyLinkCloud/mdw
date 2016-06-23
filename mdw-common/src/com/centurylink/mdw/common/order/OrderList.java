/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.order;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.InstanceList;
import com.centurylink.mdw.common.service.Jsonable;
import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.model.value.process.OrderVO;

public class OrderList implements Jsonable, InstanceList<OrderVO> {

    public OrderList(String name, String json) throws JSONException, ParseException {
        this.name = name;
        JSONObject jsonObj = new JSONObject(json);
        if (jsonObj.has("retrieveDate"))
            retrieveDate = StringHelper.serviceStringToDate(jsonObj.getString("retrieveDate"));
        if (jsonObj.has("count"))
            count = jsonObj.getInt("count");
        if (jsonObj.has(name)) {
            JSONArray orderList = jsonObj.getJSONArray(name);
            for (int i = 0; i < orderList.length(); i++)
                orders.add(new OrderVO((JSONObject)orderList.get(i)));
        }
    }

    public OrderList(String name, List<OrderVO> orders) {
        this.name = name;
        this.orders = orders;
    }

    public JSONObject getJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("retrieveDate", StringHelper.serviceDateToString(getRetrieveDate()));
        json.put("count", count);
        JSONArray array = new JSONArray();
        if (orders != null) {
            for (OrderVO order : orders)
                array.put(order.getJson());
        }
        json.put(name, array);
        return json;
    }

    public String getJsonName() {
        return name;
    }

    private String name;
    public String getName() { return name;}
    public void setName(String name) { this.name = name; }

    private Date retrieveDate;
    public Date getRetrieveDate() { return retrieveDate; }
    public void setRetrieveDate(Date d) { this.retrieveDate = d; }

    private int count;
    public int getCount() { return count; }
    public void setCount(int ct) { this.count = ct; }

    public long getTotal() {
        return count; // TODO: pagination
    }

    private List<OrderVO> orders = new ArrayList<OrderVO>();
    public List<OrderVO> getOrders() { return orders; }
    public void setOrders(List<OrderVO> orders) { this.orders = orders; }

    public List<OrderVO> getItems() {
        return orders;
    }

    public void addOrder(OrderVO order) {
        orders.add(order);
    }

    public OrderVO getOrder(Long orderInstanceId) {
        for (OrderVO order : orders) {
            if (order.getId()!= null)
                return order;
        }
        return null;
    }

    public int getIndex(Long orderInstanceId) {
        for (int i = 0; i < orders.size(); i++) {
            if (orders.get(i).getId().equals(orderInstanceId))
                return i;
        }
        return -1;
    }

    public int getIndex(String id) {
        return (getIndex(Long.parseLong(id)));
    }
}
