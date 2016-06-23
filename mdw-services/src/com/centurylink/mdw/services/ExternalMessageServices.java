/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services;

import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.model.value.event.ExternalMessageVO;

public interface ExternalMessageServices {

    public ExternalMessageVO getExternalMessage(Long activityId, Long activityInstId, Long eventInstId)
    throws DataAccessException;

}
