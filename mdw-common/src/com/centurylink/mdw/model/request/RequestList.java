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
package com.centurylink.mdw.model.request;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.model.InstanceList;
import com.centurylink.mdw.model.Jsonable;
import com.centurylink.mdw.util.StringHelper;

public class RequestList implements Jsonable, InstanceList<Request> {

    public static final String MASTER_REQUESTS = "masterRequests";
    public static final String INBOUND_REQUESTS = "inboundRequests";
    public static final String OUTBOUND_REQUESTS = "outboundRequests";

    private Date retrieveDate;
    public Date getRetrieveDate() { return retrieveDate; }
    public void setRetrieveDate(Date d) { this.retrieveDate = d; }

    private int count;
    public int getCount() { return count; }
    public void setCount(int ct) { this.count = ct; }

    private long total = -1;
    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }

    private String name;
    public String getName() { return name;}
    public void setName(String name) { this.name = name; }

    private List<Request> requests = new ArrayList<Request>();
    public List<Request> getRequests() { return requests; }
    public void setRequests(List<Request> requests) { this.requests = requests; }

    public RequestList(String name, JSONObject jsonObj) throws JSONException, ParseException {
        this.name = name;
        if (jsonObj.has("retrieveDate"))
            retrieveDate = StringHelper.serviceStringToDate(jsonObj.getString("retrieveDate"));
        if (jsonObj.has("count"))
            count = jsonObj.getInt("count");
        if (jsonObj.has("requests")) {
            JSONArray reqList = jsonObj.getJSONArray("requests");
            for (int i = 0; i < reqList.length(); i++)
                requests.add(new Request((JSONObject)reqList.get(i)));
        }
    }

    public RequestList(String name, List<Request> requests) {
        this.name = name;
        this.requests = requests;
    }

    public List<Request> getItems() {
        return requests;
    }

    public int getIndex(String id) {
        for (int i = 0; i < requests.size(); i++) {
            if (requests.get(i).getId().equals(id))
                return i;
        }
        return -1;
    }

    public JSONObject getJson() throws JSONException {
        JSONObject json = create();
        json.put("retrieveDate", StringHelper.serviceDateToString(getRetrieveDate()));
        json.put("count", count);
        if (total != -1)
            json.put("total", total);
        JSONArray array = new JSONArray();
        if (requests != null) {
            for (Request request : requests)
                array.put(request.getJson());
        }
        json.put("requests", array);
        return json;
    }

    public String getJsonName() {
        return name;
    }

}
