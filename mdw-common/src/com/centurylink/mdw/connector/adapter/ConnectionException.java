/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.connector.adapter;



/**
 *
 */
public class ConnectionException extends Exception {

	private static final long serialVersionUID = 1L;
	
	public static final int CONNECTION_DOWN = 41290;
	public static final int POOL_DISABLED = 41291;	// try to be unique, along with AdapterException codes
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
