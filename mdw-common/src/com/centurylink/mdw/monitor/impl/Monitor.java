/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.monitor.impl;

import java.util.HashMap;
import java.util.Map;

public class Monitor {

    private Map<String,String> properties;
    public String getProperty(String name) {
        if (properties == null)
            return null;
        return properties.get(name);
    }
    public void setProperty(String name, String value) {
        if (properties == null)
            properties = new HashMap<String,String>();
        properties.put(name, value);
    }
}
