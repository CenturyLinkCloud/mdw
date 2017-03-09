/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.config;

import java.util.Properties;

public class MockPropertyManager extends PropertyManager {

    private Properties properties;

    public MockPropertyManager() {
        properties = new Properties();
    }

    public void refreshCache() throws PropertyException {
        properties.clear();
    }

    public void clearCache() {
        properties.clear();
    }

    public String getStringProperty(String group, String name) {
        return this.getStringProperty(group + "/" + name);
    }

    public Properties getProperties(String group) {
        Properties props = new Properties();
        int k = group.length();
        for (Object key : properties.keySet()) {
            String propname = (String)key;
            int l = propname.length();
            char ch = l>k?propname.charAt(k):' ';
            if ((ch=='.'||ch=='/') && propname.startsWith(group)) {
                if (ch=='.') props.put(propname, properties.get(propname));
                else props.put(propname.substring(k+1), properties.get(propname));
            }
        }
        return props;
    }

    public String getStringProperty(String name) {
        return (String)properties.get(name);
    }

    public Properties getAllProperties() {
        return properties;
    }

    public void setStringProperty(String name, String value) {
        properties.put(name, value);
    }


}