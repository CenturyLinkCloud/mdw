/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.activity.types;


/**
 * Interface distinguishing new adapter activities.
 * This is to replace ControlledAdapterActivity eventually.
 */
public interface AdapterActivity extends GeneralActivity {
    
    String PROP_MAX_TRIES = "max_tries";        // including the first try; 0 is retreated the same as 1
    String PROP_TIMEOUT = "timeout";
    String PROP_RETRY_INTERVAL = "retry_interval";
    String PROP_SYNCHRONOUS_RESPONSE = "synchronous";
    String DO_LOGGING = "DO_LOGGING";
    String REQUEST_VARIABLE = "REQUEST_VARIABLE";
    String RESPONSE_VARIABLE = "RESPONSE_VARIABLE";
    String REQUEST_XSD = "REQUEST_XSD";
    String RESPONSE_XSD = "RESPONSE_XSD";
    
    String COMPCODE_AUTO_RETRY = "Automatic Retry";
    
    String MAKE_ACTUAL_CALL = "(MAKE_ACTUAL_CALL)";
    String DEFAULT_STUBBER_HOST = "localhost";
    String DEFAULT_STUBBER_PORT = "7182";
    int DEFAULT_STUBBER_TIMEOUT = 120;
    
    /**
     * Determine if the adapter itself is synchronous, i.e.
     * waiting for responses.
     * 
     * @return true if it needs to wait for responses
     */
    boolean isSynchronous();
        
}
