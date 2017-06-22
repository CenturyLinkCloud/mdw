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
package com.centurylink.mdw.config;

import java.util.Iterator;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.model.JsonObject;
import com.centurylink.mdw.startup.StartupException;
import com.centurylink.mdw.util.MiniEncrypter;

/**
 * Handles environment variables deployed on a Cloud Foundry env
 *
 * @author aa70413
 *
 */
public class PaaSPropertyManager extends PropertyManager {

    protected Properties properties = new Properties();
    public static final char GROUP_SEPARATOR = '-';

    public PaaSPropertyManager() throws StartupException {
        properties.clear();
        setStringProperty("mdw.asset.location", System.getenv("MDW_ASSET_LOCATION")); // default value
        String mdwProperties = System.getenv("mdw_properties");
        try {
            JSONObject json = new JsonObject(mdwProperties);
            Iterator<?> keys = json.keys();
            while (keys.hasNext()) {
                String key = keys.next().toString();
                setStringProperty(key, json.getString(key));
            }
        }
        catch (JSONException ex) {
            ex.printStackTrace();
            throw new StartupException(ex.getMessage(), ex);
        }
    }

    @Override
    public void refreshCache() throws Exception {

        /**
         * How do we refresh Cache ? Maybe we just restage ?
         */

    }

    @Override
    public void clearCache() {
        properties.clear();

    }

    /**
     * returns the properties for group. When the group is specified with
     * ApplicationProperties.xml (old style), the names of the properties
     * returned will not contain the group names and '/', for backward
     * compatibility. When the group is in the new style, where a group is any
     * property name prefix terminated by a '.', the property names returned
     * will contain the whole property names including the group names.
     *
     * @param group
     * @return Properties for the group
     */
    @Override
    public Properties getProperties(String group) {
        Properties props = new Properties();
        int k = group.length();
        for (Object key : properties.keySet()) {
            String propname = (String) key;
            int l = propname.length();
            char ch = l > k ? propname.charAt(k) : ' ';
            if ((ch == '.' || ch == '/') && propname.startsWith(group)) {
                if (ch == '.')
                    props.put(propname, properties.get(propname));
                else
                    props.put(propname.substring(k + 1), properties.get(propname));
            }
        }
        return props;
    }

    @Override
    public String getStringProperty(String group, String name)
            throws PropertyException {
        if (group != null)
            return this.getStringProperty(group + "/" + name);
        else
            return this.getStringProperty(name);
    }

    @Override
    public String getStringProperty(String name) {
        int slash = name.indexOf('/');
        String propName = slash == -1 ? name : name.replace('/', GROUP_SEPARATOR);
        String value = properties.getProperty(propName);
        if (value != null && value.startsWith("###"))
            value = MiniEncrypter.decrypt(value.substring(3));
        return value;
    }

    @Override
    public Properties getAllProperties() {
        return properties;
    }

    @Override
    public void setStringProperty(String name, String value) {
        if (value == null || value.length() == 0)
            properties.remove(name);
        else
            properties.put(name, value);

    }
}
