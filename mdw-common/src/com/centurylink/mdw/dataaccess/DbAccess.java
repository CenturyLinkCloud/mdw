/*
 * Copyright (C) 2018 CenturyLink, Inc.
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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Wraps a DatabaseAccess instance for autocloseability
 */
public class DbAccess implements AutoCloseable {

    private DatabaseAccess databaseAccess;

    public DbAccess() throws SQLException {
        this(new DatabaseAccess(null));
    }

    public DbAccess(DatabaseAccess databaseAccess) throws SQLException {
        this.databaseAccess = databaseAccess;
        databaseAccess.openConnection();
    }

    public Connection getConnection() {
        return databaseAccess.getConnection();
    }

    @Override
    public void close() {
        databaseAccess.closeConnection();
    }

    public ResultSet runSelect(String query, Object... arguments) throws SQLException {
        return databaseAccess.runSelect(query, arguments);
    }

    public int runUpdate(String query, Object... arguments) throws SQLException {
        return databaseAccess.runUpdate(query, arguments);
    }

    public Long runInsertReturnId(String query, Object... arguments) throws SQLException {
        return databaseAccess.runInsertReturnId(query, arguments);
    }
}
