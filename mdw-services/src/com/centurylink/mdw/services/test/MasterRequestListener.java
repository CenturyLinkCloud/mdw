package com.centurylink.mdw.services.test;

public interface MasterRequestListener {

    public void syncMasterRequestId(String oldId, String newId);
}
