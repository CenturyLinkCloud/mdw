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
package com.centurylink.mdw.model.event;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.InstanceList;
import com.centurylink.mdw.common.service.Jsonable;
import com.centurylink.mdw.model.user.UserAction;
import com.centurylink.mdw.util.StringHelper;

/**
 * Also maintains a static list of History reference data loaded
 * from the db.
 */
public class HistoryList implements Jsonable, InstanceList<UserAction>
{
  public static final String ALL_HISTORY = "allHistory";

  public HistoryList(String name, List<UserAction> history) {
      this.name = name;
      this.history = history;
      this.count = history.size();
  }

  public HistoryList(String name, String json) throws JSONException {
      this.name = name;
      JSONObject jsonObj = new JSONObject(json);
      if (jsonObj.has("retrieveDate"))
          retrieveDate = StringHelper.serviceStringToDate(jsonObj.getString("retrieveDate"));
      if (jsonObj.has("count"))
          count = jsonObj.getInt("count");
      if (jsonObj.has(name)) {
          JSONArray assetList = jsonObj.getJSONArray(name);
          for (int i = 0; i < assetList.length(); i++)
              history.add(new UserAction((JSONObject)assetList.get(i)));
      }
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

  public long getTotal() { return count; }  // no pagination

  private List<UserAction> history = new ArrayList<UserAction>();
  public List<UserAction> getHistory() { return history; }
  public void setHistory(List<UserAction> history) { this.history = history; }

  public List<UserAction> getItems() {
      return history;
  }

  public int getIndex(String id) {
      for (int i = 0; i < history.size(); i++) {
          if (history.get(i).getId().equals(id))
              return i;
      }
      return -1;
  }

  public JSONObject getJson() throws JSONException {
      JSONObject json = new JSONObject();
      json.put("retrieveDate", StringHelper.serviceDateToString(getRetrieveDate()));
      json.put("count", count);
      JSONArray array = new JSONArray();
      if (history != null) {
          for (UserAction historyItem : history)
              array.put(historyItem.getHistoryJson());
      }
      json.put(name, array);
      return json;
  }

  public String getJsonName() {
      return "History";
  }
}
