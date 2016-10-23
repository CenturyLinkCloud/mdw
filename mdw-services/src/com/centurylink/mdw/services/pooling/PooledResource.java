/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services.pooling;

import java.util.UUID;

public interface PooledResource {
    public void openResource(Config config) throws Exception ;
    public UUID getUuid() ;
    public String executeCommand (String commandInfoXml) throws Exception;
    public void closeResource() throws Exception;
}
