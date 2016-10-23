/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.config;

import java.util.Properties;

public class PropertyManagerUnitTest extends PropertyManager {

    private Properties properties;

    public PropertyManagerUnitTest() {
        properties = new Properties();
    }

    public void refreshCache() throws PropertyException {
        properties.clear();
    }

    public void clearCache() {
        properties.clear();
    }

    public String getStringProperty(String pGroupName, String pPropertyName) {
        return this.getStringProperty(pGroupName + "/" + pPropertyName);
    }

    public Properties getProperties(String pGroupName) {
        Properties props = new Properties();
        int k = pGroupName.length();
        for (Object key : properties.keySet()) {
            String propname = (String)key;
            int l = propname.length();
            char ch = l>k?propname.charAt(k):' ';
            if ((ch=='.'||ch=='/') && propname.startsWith(pGroupName)) {
                if (ch=='.') props.put(propname, properties.get(propname));
                else props.put(propname.substring(k+1), properties.get(propname));
            }
        }
        return props;
    }

    public String getStringProperty(String propertyName) {
        return (String)properties.get(propertyName);
    }

    public Properties getAllProperties() {
        return properties;
    }

    public void setStringProperty(String property_name, String value) {
        properties.put(property_name, value);
    }


}