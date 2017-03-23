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
package com.centurylink.mdw.connector.adapter;



/**
 *
 */
public class ConnectionException extends Exception {

    private static final long serialVersionUID = 1L;
    
    public static final int CONNECTION_DOWN = 41290;
    public static final int POOL_DISABLED = 41291;    // try to be unique, along with AdapterException codes
    public static final int POOL_EXHAUSTED = 41292;
    public static final int POOL_BORROW = 41293;
    public static final int CONFIGURATION_WRONG = 41286;

    private int code;
    private Throwable cause;

    /**
     * @param pMessage
     */
    public ConnectionException(String pMessage) {
        super(pMessage);
        this.code = CONNECTION_DOWN;
    }

    /**
     * @param pMessage
     */
    public ConnectionException(int pCode, String pMessage) {
        super(pMessage);
        this.code = pCode;

    }

    /**
     * @param pMessage
     */
    public ConnectionException(int pCode, String pMessage, Throwable pCause) {
        super(pMessage);
        this.code = pCode;
        this.cause = pCause;

    }

    /**
     * Returns code
     */
    public int getCode(){
        return code;
    }

    /**
     * Returns Cause
     */
    public Throwable getCause(){
        return cause;
    }


}
