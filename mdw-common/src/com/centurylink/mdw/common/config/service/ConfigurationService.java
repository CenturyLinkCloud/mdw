/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.config.service;

import com.centurylink.mdw.common.utilities.property.PropertyManager;

/**
 * Service-based PropertyManager access for OSGI bundles.
 */
public interface ConfigurationService {
    
    public PropertyManager getPropertyManager();
    
}
