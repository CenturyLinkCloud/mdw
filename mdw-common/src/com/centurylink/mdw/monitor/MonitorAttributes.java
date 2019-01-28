/*
 * Copyright (C) 2018 CenturyLink, Inc.
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
package com.centurylink.mdw.monitor;

import org.json.JSONArray;

import com.centurylink.mdw.annotations.Monitor;
import com.centurylink.mdw.model.asset.AssetInfo;

public class MonitorAttributes {

    private JSONArray rows;

    public MonitorAttributes(String value) {
        rows = new JSONArray(value);
    }

    public boolean isEnabled(String className) {
        JSONArray row = getRow(className);
        if (row != null && row.length() > 0)
            return row.getString(0).equals("true");
        return false;
    }

    public void setEnabled(String implAsset, String label, boolean enabled) {
        String implClass = implAsset.substring(0, implAsset.lastIndexOf('.')).replace('/', '.');
        JSONArray row = getRow(implClass);
        if (row == null) {
            row = new JSONArray();
            row.put(String.valueOf(enabled));
            row.put(label);
            row.put(implAsset);
            row.put("");
            rows.put(row);
        }
        row.put(0, String.valueOf(enabled));
    }

    public JSONArray getRow(String className) {
        for (int i = 0; i < rows.length(); i++) {
            JSONArray row = rows.getJSONArray(i);
            if (row.length() > 2) {
                String implAsset = row.getString(2);
                String implClass = implAsset.substring(0, implAsset.lastIndexOf('.')).replace('/', '.');
                if (implClass.equals(className))
                    return row;
            }
        }
        return null;
    }

    public static JSONArray getRowDefault(AssetInfo implAsset, Class<? extends com.centurylink.mdw.monitor.Monitor> monitorClass) {
        Monitor monitorAnnotation = monitorClass.getAnnotation(Monitor.class);
        if (monitorAnnotation == null)
            return null;
        JSONArray row = new JSONArray();
        row.put(String.valueOf(monitorAnnotation.defaultEnabled()));
        row.put(monitorAnnotation.value());
        String className = monitorClass.getName();
        if (implAsset == null)
            row.put("");
        else
            row.put(className.substring(0, className.lastIndexOf(".")) + "/" + implAsset.getName());
        row.put(monitorAnnotation.defaultOptions());
        return row;
    }

    @Override
    public String toString() {
        return rows == null ? "[]" : rows.toString();
    }
}
