/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.db;

import java.sql.SQLException;

import com.centurylink.mdw.annotations.RegisteredService;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.dataaccess.EmbeddedDataAccess;
import com.centurylink.mdw.provider.StartupException;
import com.centurylink.mdw.provider.StartupService;

/**
 * Startup service for embedded db extensions.
 */
@RegisteredService(StartupService.class)
public class DbExtensionsStartup implements StartupService {

    public boolean isEnabled() {
        return true;
    }

    public void onStartup() throws StartupException {
        EmbeddedDataAccess embedded = DatabaseAccess.getEmbedded();
        if (embedded != null) {
            try {
                embedded.initializeExtensions();
            }
            catch (SQLException ex) {
                throw new StartupException(this.getClass().getName(), ex);
            }
        }
    }

    public void onShutdown() {
    }

}
