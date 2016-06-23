/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services.pooling;

import com.centurylink.mdw.common.exception.PropertyException;
import com.centurylink.mdw.common.utilities.property.PropertyManager;
import com.centurylink.mdw.common.utilities.property.PropertyUtil;

public abstract class Config {
    
    public abstract void initialize(String configXml) throws Exception;
    
    protected String getResourcePropertyValue(String group, String property) throws PropertyException {
       PropertyManager appProp = PropertyUtil.getInstance().getPropertyManager();
       return appProp.getStringProperty(group, property);
    }
}
