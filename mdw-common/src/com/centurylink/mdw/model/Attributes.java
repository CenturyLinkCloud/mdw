package com.centurylink.mdw.model;

import com.centurylink.mdw.constant.WorkAttributeConstant;
import com.centurylink.mdw.util.JsonUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;

public class Attributes extends HashMap<String,String> implements Jsonable {

    public Attributes() {
    }

    public Attributes(Attributes attributes) {
        super(attributes);
    }

    public Attributes(Map<String,Object> yaml) {
        for (String name : yaml.keySet()) {
            put(name, (String)yaml.get(name));
        }
    }

    public Attributes(JSONObject json) {
        for (String name : JSONObject.getNames(json)) {
            put(name, json.optString(name));
        }
    }

    @Override
    public JSONObject getJson() throws JSONException {
        JSONObject json = create();
        for (String name : keySet()) {
           json.put(name, get(name));
        }
        return json;
    }

    /**
     * Removes null or empty names/values.  Also prevents junk.
     */
    @Override
    public String put(String key, String value) {
        if (key == null || key.isEmpty() || value == null || value.isEmpty()) {
            return remove(key);
        }
        else {
            return super.put(key, value);
        }
    }

    public Attributes clean() {
        Attributes cleanAttributes = new Attributes(this);
        cleanAttributes.remove(WorkAttributeConstant.LOGICAL_ID);
        if ("0".equals(cleanAttributes.get(WorkAttributeConstant.SLA)))
            cleanAttributes.remove(WorkAttributeConstant.SLA);
        if (!cleanAttributes.containsKey(WorkAttributeConstant.SLA))
            cleanAttributes.remove(WorkAttributeConstant.SLA_UNIT);
        return cleanAttributes;
    }

    public List<String> getList(String key) {
        String value = get(key);
        List<String> list = new ArrayList<>();
        if (value != null) {
            if (value.startsWith("[")) {
                JSONArray jsonArr = new JSONArray(value);
                for (int i = 0; i < jsonArr.length(); i++)
                    list.add(jsonArr.getString(i));
            } else {
                StringTokenizer st = new StringTokenizer(value, "#");
                while (st.hasMoreTokens())
                    list.add(st.nextToken());
            }
        }
        return list;
    }

    public Map<String,String> getMap(String key) {
        String value = get(key);
        HashMap<String,String> map = new LinkedHashMap<>();
        if (value != null) {
            if (value.startsWith("{")) {
                return JsonUtil.getMap(new JsonObject(value));
            } else {
                int name_start = 0;
                int n = value.length();
                int m;
                while (name_start < n) {
                    m = name_start;
                    char ch = value.charAt(m);
                    while (ch != '=' && ch != ';' && m < n - 1) {
                        m++;
                        ch = value.charAt(m);
                    }
                    if (ch == '=') {
                        int value_start = m + 1;
                        boolean escaped = false;
                        for (m = value_start; m < n; m++) {
                            if (escaped) escaped = false;
                            else {
                                ch = value.charAt(m);
                                if (ch == '\\') escaped = true;
                                else if (ch == ';') break;
                            }
                        }
                        map.put(value.substring(name_start, value_start - 1).trim(),
                                value.substring(value_start, m).trim());
                        name_start = m + 1;
                    } else if (ch == ';') {
                        if (m > name_start) {
                            map.put(value.substring(name_start, m).trim(), null);
                        }
                        name_start = m + 1;
                    } else {    // m == n-1
                        if (m > name_start) {
                            map.put(value.substring(name_start, m).trim(), null);
                        }
                        name_start = m + 1;
                    }
                }
            }
        }
        return map;
    }

    public List<String[]> getTable(String key, char fieldDelimiter, char rowDelimiter, int columnCount) {
        String value = get(key);
        List<String[]> table = new ArrayList<>();
        if (value != null) {
            if (value.startsWith("[")) {
                List<String[]> rows = new ArrayList<>();
                JSONArray outer = new JSONArray(value);
                for (int i = 0; i < outer.length(); i++) {
                    String[] row = new String[columnCount];
                    JSONArray inner = outer.getJSONArray(i);
                    for (int j = 0; j < row.length; j++) {
                        if (inner.length() > j)
                            row[j] = inner.getString(j);
                        else
                            row[j] = "";
                    }
                    rows.add(row);
                }
                return rows;
            } else {
                int row_start = 0;
                int field_start;
                int n = value.length();
                String[] row;
                int m, j;
                StringBuffer sb;
                while (row_start < n) {
                    row = new String[columnCount];
                    table.add(row);
                    j = 0;
                    field_start = row_start;
                    char ch = fieldDelimiter;
                    while (ch == fieldDelimiter) {
                        sb = new StringBuffer();
                        boolean escaped = false;
                        for (m = field_start; m < n; m++) {
                            ch = value.charAt(m);
                            if (ch == '\\' && !escaped) {
                                escaped = true;
                            } else {
                                if (!escaped && (ch == fieldDelimiter || ch == rowDelimiter)) {
                                    break;
                                } else {
                                    sb.append(ch);
                                    escaped = false;
                                }
                            }
                        }
                        if (j < columnCount)
                            row[j] = sb.toString();
                        if (m >= n || ch == rowDelimiter) {
                            row_start = m + 1;
                            break;
                        } else {  // ch==field_delimiter
                            field_start = m + 1;
                            j++;
                        }
                    }
                }
            }
        }
        return table;
    }

}
