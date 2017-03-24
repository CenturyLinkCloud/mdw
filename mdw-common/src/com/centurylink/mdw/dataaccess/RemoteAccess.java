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
package com.centurylink.mdw.dataaccess;

public class RemoteAccess {

    public static final char REMOTE_NAME_DELIMITER = '@';

    private DatabaseAccess db;
    private String logicalServerName;
    private int schemaVersion;

    /**
     * This constructor is used by designer as well as engine
     * @param logicalServerName
     * @param databaseUrl database access url (jdbc:....)
     * @throws DataAccessException
     */
    public RemoteAccess(String logicalServerName, String databaseUrl)
            throws DataAccessException {
        this.logicalServerName = logicalServerName;
        db = new DatabaseAccess(databaseUrl);
        int[] versions = DataAccess.getDatabaseSchemaVersion(db);
        schemaVersion = versions[0];
    }

    public String getLogicalServerName() {
        return logicalServerName;
    }

    public int getSchemaVersion() {
        return schemaVersion;
    }
}
