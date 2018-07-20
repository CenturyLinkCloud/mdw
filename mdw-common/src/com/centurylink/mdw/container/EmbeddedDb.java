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
