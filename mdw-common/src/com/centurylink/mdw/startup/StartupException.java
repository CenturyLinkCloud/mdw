/*
 * Copyright (C) 2017 CenturyLink, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.centurylink.mdw.startup;


import com.centurylink.mdw.common.MdwException;

/**
 * ServiceLocatorException
 *
  */
public class StartupException extends MdwException {

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
