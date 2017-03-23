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


import com.centurylink.mdw.common.MdwException;

public class AdapterException extends MdwException {
    private static final long serialVersionUID = 1L;

    public static final int EXCEED_MAXTRIES = 41282;
    public static final int CONFIGURATION_WRONG = ConnectionException.CONFIGURATION_WRONG;

    private boolean isRetryableError;
    public AdapterException(String pMessage){
        super(pMessage);
    }

     public AdapterException(String pMessage, boolean pIsRetryableError){
        super(pMessage);
        this.isRetryableError = pIsRetryableError;
    }

    public AdapterException(int pCode, String pMessage){
        super(pCode, pMessage);

    }

     public AdapterException(int pCode, String pMessage, boolean pIsRetryableError){
        super(pCode, pMessage);
        this.isRetryableError = pIsRetryableError;
    }


     public AdapterException(String pMessage, Throwable pTh) {
         super(pMessage, pTh);

     }

    public AdapterException(int pCode, String pMessage, Throwable pTh){
        super(pCode, pMessage, pTh);

    }

    public AdapterException(int pCode, String pMessage, Throwable pTh, boolean pIsRetryableError){
        super(pCode, pMessage, pTh);
        this.isRetryableError = pIsRetryableError;
    }

    public boolean isRetryableError(){
        return isRetryableError;
    }

    public void setIsRetryableError(boolean pIsRetryableError){
        this.isRetryableError = pIsRetryableError;
    }
}
