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

import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Wraps a DatabaseAccess instance for autocloseability
 */
public class DbAccess implements AutoCloseable {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

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

    /**
     * Utility method for showing parameterized query result
     */
    public static String substitute(String sql, Object... params) {
        try {
            if (params == null || params.length == 0)
                return sql;
            String subst = sql;
            int start = 0;
            int q;
            for (int i = 0; start < subst.length() && (q = subst.indexOf('?', start)) >= 0; i++) {
                Object param = params[i];
                String p = String.valueOf(param);
                if (param != null && !(param instanceof Integer) && !(param instanceof Long))
                    p = "'" + p + "'";
                subst = subst.substring(0, q) + p + subst.substring(q + 1);
                start = q + p.length();
            }
            return subst;
        }
        catch (Throwable t) {
            logger.severeException(t.getMessage(), t);
            return sql;
        }
    }
}
