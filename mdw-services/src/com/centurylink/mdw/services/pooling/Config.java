/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services.pooling;

import com.centurylink.mdw.config.PropertyException;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.config.PropertyUtil;

public abstract class Config {
    
    public abstract void initialize(String configXml) throws Exception;
    
    protected String getResourcePropertyValue(String group, String property) throws PropertyException {
       PropertyManager appProp = PropertyUtil.getInstance().getPropertyManager();
       return appProp.getStringProperty(group, property);
    }
}
