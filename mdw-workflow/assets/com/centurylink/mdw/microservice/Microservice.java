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
package com.centurylink.mdw.microservice;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.model.Jsonable;

/**
 * Represents a microservice's invocations for one master request.
 */
public class Microservice implements Jsonable {

    private String microservice;
    public String getMicroservice() { return microservice; }
    public void setMicroservice(String microservice) { this.microservice = microservice; }

    private String transactionId;
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String id) { this.transactionId = id; }

    private List<Invocation> invocations;
    public List<Invocation> getInvocations() { return invocations; }
    public void setInvocations(List<Invocation> invocations) { this.invocations = invocations; }

    private List<Update> updates;
    public List<Update> getUpdates() { return updates; }
    public void setUpdates(List<Update> updates) { this.updates = updates; }

    public Microservice(String microservice) {
        this.microservice = microservice;
        this.invocations = new ArrayList<Invocation>();
    }

    public Microservice(String microservice, JSONObject json) throws JSONException {
        this.microservice = microservice;
        if (json.has("transactionId")) {
            this.transactionId = json.getString("transactionId");
        }
        if (json.has("invocations")) {
            this.invocations = new ArrayList<Invocation>();
            JSONArray invocationsArr = json.getJSONArray("invocations");
            for (int i = 0; i < invocationsArr.length(); i++) {
                this.invocations.add(new Invocation(invocationsArr.getJSONObject(i)));
            }
        }
        if (json.has("updates")) {
            this.updates = new ArrayList<Update>();
            JSONArray updatesArr = json.getJSONArray("updates");
            for (int i = 0; i < updatesArr.length(); i++) {
                this.updates.add(new Update(updatesArr.getJSONObject(i)));
            }
        }
    }

    public JSONObject getJson() throws JSONException {
        JSONObject json = create();
        if (transactionId != null)
            json.put("transactionId", transactionId);
        if (invocations != null) {
            JSONArray invocationsArr = new JSONArray();
            for (Invocation invocation : invocations) {
                invocationsArr.put(invocation.getJson());
            }
            json.put("invocations", invocationsArr);
        }
        if (updates != null) {
            JSONArray updatesArr = new JSONArray();
            for (Update update : updates) {
                updatesArr.put(update.getJson());
            }
            json.put("updates", updatesArr);
        }
        return json;
    }

    public String getJsonName() {
        return microservice;
    }
}
