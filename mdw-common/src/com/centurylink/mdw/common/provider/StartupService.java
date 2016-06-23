/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.provider;

import com.centurylink.mdw.common.service.RegisteredService;
import com.centurylink.mdw.common.utilities.startup.StartupClass;

/**
 * Implemented by workflow bundles that perform startup functionality (especially custom listeners).
 */
public interface StartupService extends StartupClass, RegisteredService {
    
    public boolean isEnabled();

    @Override
    public void onStartup() throws StartupException;
    
}
