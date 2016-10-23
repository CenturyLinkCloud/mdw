/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
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
