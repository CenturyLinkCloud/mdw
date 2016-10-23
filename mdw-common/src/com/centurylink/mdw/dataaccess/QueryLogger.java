/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.dataaccess;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public interface QueryLogger {

    ResultSet executeQuery(PreparedStatement ps, String query)
    throws SQLException;
    
    int executeUpdate(PreparedStatement ps, String query)
    throws SQLException;
    
    int [] executeBatch(PreparedStatement ps, String query)
    throws SQLException;
    
    List<String> getLoggedQueries();

}
