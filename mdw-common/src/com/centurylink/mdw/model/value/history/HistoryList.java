/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model.value.history;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.InstanceList;
import com.centurylink.mdw.common.service.Jsonable;
import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.model.value.user.UserActionVO;

/**
 * Also maintains a static list of History reference data loaded
 * from the db.
 */
public class HistoryList implements Jsonable, InstanceList<UserActionVO>
{
  public static final String ALL_HISTORY = "allHistory";

  public HistoryList(String name, List<UserActionVO> history) {
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
              history.add(new UserActionVO((JSONObject)assetList.get(i)));
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

  private List<UserActionVO> history = new ArrayList<UserActionVO>();
  public List<UserActionVO> getHistory() { return history; }
  public void setHistory(List<UserActionVO> history) { this.history = history; }

  public List<UserActionVO> getItems() {
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
          for (UserActionVO historyItem : history)
              array.put(historyItem.getHistoryJson());
      }
      json.put(name, array);
      return json;
  }

  public String getJsonName() {
      return "History";
  }
}
