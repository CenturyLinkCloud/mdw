/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.translator;

public class TranslationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public TranslationException(String pMessage){
        super(pMessage);
    }
	
	public TranslationException(String message, Throwable cause) {
	    super(message, cause);
	}
}
