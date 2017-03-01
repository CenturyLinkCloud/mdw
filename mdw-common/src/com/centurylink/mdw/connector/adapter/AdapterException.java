/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
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
