package com.centurylink.mdw.services.system;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * MDW Datasource MBean attributes of interest.
 */
public class DatasourceAttributes {

    private static final LinkedHashMap<String,String> ATTRIBUTES = new LinkedHashMap<>();
    static {
        ATTRIBUTES.put("MDWDataSource/DefaultTransactionIsolation", "Default Tx Isolation");

        ATTRIBUTES.put("MDWDataSource/NumActive", "Active");
        ATTRIBUTES.put("MDWDataSource/NumIdle", "Idle");
        ATTRIBUTES.put("MDWDataSource/NumWaiters", "Waiting");

        ATTRIBUTES.put("MDWDataSource/BorrowedCount", "Borrowed");
        ATTRIBUTES.put("MDWDataSource/CreatedCount", "Created");
        ATTRIBUTES.put("MDWDataSource/DestroyedCount", "Destroyed");
        ATTRIBUTES.put("MDWDataSource/ReturnedCount", "Returned");
    }
    public static Map<String,String> getAttributes() { return ATTRIBUTES; }
}
