/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.ui;

import java.sql.SQLException;

import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.dataaccess.DataAccess;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.dataaccess.ProcessLoader;
import com.centurylink.mdw.dataaccess.ProcessPersister;
import com.centurylink.mdw.dataaccess.RuntimeDataAccess;

public class MDWDataAccess
{
  private RuntimeDataAccess runtimeDataAccess;
  public RuntimeDataAccess getRuntimeDataAccess() throws SQLException, DataAccessException
  {
    if (runtimeDataAccess == null)
    {
      DatabaseAccess databaseAccess = new DatabaseAccess(null);
      runtimeDataAccess = DataAccess.getRuntimeDataAccess(databaseAccess);
    }
    return runtimeDataAccess;
  }

  private ProcessLoader processLoader;
  public ProcessLoader getProcessLoader() throws SQLException, DataAccessException
  {
    if (processLoader == null)
    {
      processLoader = DataAccess.getProcessLoader();
    }

    return processLoader;
  }

  private ProcessPersister processPersister;
  public ProcessPersister getProcessPersister() throws SQLException, DataAccessException
  {
    if (processPersister == null)
    {
      DatabaseAccess databaseAccess = new DatabaseAccess(null);
      int[] dbVersions = DataAccess.getDatabaseSchemaVersion(databaseAccess);
      processPersister = DataAccess.getProcessPersister(dbVersions[0], dbVersions[1], databaseAccess, null);
    }

    return processPersister;
  }

}
