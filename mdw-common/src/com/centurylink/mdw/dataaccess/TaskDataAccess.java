/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.dataaccess;

import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.model.value.process.ProcessVO;

public interface TaskDataAccess {

    public void saveTaskTemplates(ProcessVO process) throws DataAccessException;

}
