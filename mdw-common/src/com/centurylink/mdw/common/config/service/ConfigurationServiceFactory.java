/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.config.service;

import com.centurylink.mdw.common.utilities.property.PropertyManager;

public class ConfigurationServiceFactory {

    public ConfigurationService getConfigurationService() {
        return (ConfigurationService) PropertyManager.getInstance();
    }
}
