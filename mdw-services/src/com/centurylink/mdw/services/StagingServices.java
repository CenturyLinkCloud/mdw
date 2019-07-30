package com.centurylink.mdw.services;

import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.dataaccess.file.GitBranch;

public interface StagingServices {

    /**
     * Return Git branch if it exists on remote.  Otherwise null.
     */
    GitBranch getStagingBranch(String cuid) throws ServiceException;

}
