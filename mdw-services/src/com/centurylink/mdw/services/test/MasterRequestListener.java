/**
 * Copyright (c) 2017 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services.test;

public interface MasterRequestListener {

    public void syncMasterRequestId(String oldId, String newId);
}
