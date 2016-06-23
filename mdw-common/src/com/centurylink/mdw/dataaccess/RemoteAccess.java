/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.dataaccess;

import com.centurylink.mdw.common.exception.DataAccessException;

public class RemoteAccess {

    public static final char REMOTE_NAME_DELIMITER = '@';

    private DatabaseAccess db;
    private String logicalServerName;
    private int schemaVersion, supportedVersion;
    private ProcessLoader loader;
    private RuntimeDataAccess runtimeDataAccess;

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
        supportedVersion = versions[1];
        loader = null;
        runtimeDataAccess = null;
    }

    public String getLogicalServerName() {
        return logicalServerName;
    }

    /**
     * Used by designer only
     * @return
     */
    public ProcessLoader getLoader() throws DataAccessException {
        if (loader==null) loader = DataAccess.getProcessLoader(schemaVersion, supportedVersion, db);
        return loader;
    }

    /**
     * Used by designer only
     * @return
     */
    public RuntimeDataAccess getRuntimeDataAccess() {
        if (runtimeDataAccess==null) runtimeDataAccess =
        	DataAccess.getRuntimeDataAccess(schemaVersion, supportedVersion, db);
        return runtimeDataAccess;
    }

    public int getSchemaVersion() {
    	return schemaVersion;
    }


}
