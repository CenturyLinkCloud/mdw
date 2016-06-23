/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services.workflow;

import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.dataaccess.DataAccess;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.dataaccess.RuntimeDataAccess;
import com.centurylink.mdw.model.value.event.ExternalMessageVO;
import com.centurylink.mdw.services.ExternalMessageServices;

public class ExternalMessageServicesImpl implements ExternalMessageServices {

    public ExternalMessageVO getExternalMessage(Long activityId, Long activityInstId, Long eventInstId)
    throws DataAccessException {
        return getRuntimeDataAccess().getExternalMessage(activityId, activityInstId, eventInstId);
    }

    private RuntimeDataAccess getRuntimeDataAccess() throws DataAccessException {
        return DataAccess.getRuntimeDataAccess(new DatabaseAccess(null));
    }
}
