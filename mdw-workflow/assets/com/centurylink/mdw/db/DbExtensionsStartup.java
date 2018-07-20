/*
 * Copyright (C) 2017 CenturyLink, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.centurylink.mdw.db;

import java.sql.SQLException;

import com.centurylink.mdw.annotations.RegisteredService;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.dataaccess.EmbeddedDataAccess;
import com.centurylink.mdw.startup.StartupException;
import com.centurylink.mdw.startup.StartupService;

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
