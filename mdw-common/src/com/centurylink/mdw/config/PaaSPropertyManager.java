/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.config;

import java.util.Iterator;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;

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
            JSONObject json = new JSONObject(mdwProperties);
            Iterator<?> keys = json.keys();
            while (keys.hasNext()) {
                String key = keys.next().toString();
                setStringProperty(key, json.getString(key));
            }
        }
        catch (JSONException ex) {
            ex.printStackTrace();
            throw new StartupException(StartupException.FAIL_TO_LOAD_PROPERTIES, ex.getMessage(), ex);
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
     * @param pGroupName
     * @return Properties for the group
     * @throws PropertyException
     */
    @Override
    public Properties getProperties(String pGroupName) {
        Properties props = new Properties();
        int k = pGroupName.length();
        for (Object key : properties.keySet()) {
            String propname = (String) key;
            int l = propname.length();
            char ch = l > k ? propname.charAt(k) : ' ';
            if ((ch == '.' || ch == '/') && propname.startsWith(pGroupName)) {
                if (ch == '.')
                    props.put(propname, properties.get(propname));
                else
                    props.put(propname.substring(k + 1), properties.get(propname));
            }
        }
        return props;
    }

    @Override
    public String getStringProperty(String pGroupName, String pPropertyName)
            throws PropertyException {
        if (pGroupName != null)
            return this.getStringProperty(pGroupName + "/" + pPropertyName);
        else
            return this.getStringProperty(pPropertyName);
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
    public void setStringProperty(String property_name, String value) {
        if (value == null || value.length() == 0)
            properties.remove(property_name);
        else
            properties.put(property_name, value);

    }
}
