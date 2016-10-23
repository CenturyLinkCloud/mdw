/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.startup;


import com.centurylink.mdw.common.MDWException;

/**
 * ServiceLocatorException
 *
  */
public class StartupException extends MDWException {

    private static final long serialVersionUID = 1L;
    
    public static final int NO_PROPERTY_FILE_FOUND = 1;
    public static final int FAIL_TO_LOAD_PROPERTIES = 2;
    public static final int FAIL_INIT_APPLICATION_CONTEXT = 3;
    public static final int FAIL_TO_START_DATABASE_POOL = 4;
    public static final int FAIL_TO_START_RMI_LISTENER = 5;
    public static final int FAIL_TO_LOAD_STARTUP_CLASSES = 6;

    public StartupException(String pMessage){
        super(pMessage);
    }

    public StartupException(int pCode, String pMessage){
        super(pCode, pMessage);

    }

    public StartupException(int pCode, String pMessage, Throwable pTh){
        super(pCode, pMessage, pTh);

    }



}
