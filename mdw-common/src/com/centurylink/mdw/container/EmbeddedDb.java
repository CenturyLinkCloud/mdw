/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.container;

import java.sql.SQLException;

import com.centurylink.mdw.common.service.RegisteredService;
import com.centurylink.mdw.model.user.User;

public interface EmbeddedDb extends RegisteredService {

    public static final String DB_ASSET_PACKAGE = "com.centurylink.mdw.db";

    public void init(String url, String user, String password, String assetLocation, String baseLocation, String dataLocation);

    /**
     * This should block until the db is available.
     */
    public void startup() throws SQLException;

    /**
     * Should block until database has been shutdown.
     */
    public void shutdown() throws SQLException;

    public boolean checkRunning() throws SQLException;

    public boolean checkMdwSchema() throws SQLException;

    /**
     * Create the schema
     */
    public void createMdwSchema() throws SQLException;

    public void source(String contents) throws Exception;

    public void insertUser(User user) throws SQLException;

    public String getDriverClass();
}
