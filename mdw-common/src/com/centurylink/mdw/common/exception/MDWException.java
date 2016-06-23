/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.exception;


public class MDWException extends Exception {
    private static final long serialVersionUID = 1L;
    private int errorCode;

    public MDWException(String pMessage){
        super(pMessage);
    }

    public MDWException(int pCode, String pMessage){
        super(pMessage);
        this.errorCode = pCode;
    }

    public MDWException(int pCode, String pMessage, Throwable pTh){
        super(pMessage, pTh);
        this.errorCode = pCode;
    }

    public MDWException(String pMessage, Throwable pTh){
        super(pMessage, pTh);
        this.errorCode = -1;
    }
    
    public int getErrorCode(){
    	return this.errorCode;
    }
    
    public String getStackTraceDetails() {
      return getStackTrace(this);
    }
    
    public static String getStackTrace(Throwable t) {
        StackTraceElement[] elems = t.getStackTrace();
        StringBuffer sb = new StringBuffer();
        sb.append(t.toString());
        sb.append("\n");
        for (int i = 0; i < elems.length; i++) {
            sb.append(elems[i].toString());
            sb.append("\n");
        }

        if (t.getCause() != null) {
            sb.append("\n\nCaused by:\n");
            sb.append(getStackTrace(t.getCause()));
        }

        return sb.toString();
    }

    public Throwable findCause() {
        return findCause(this);
    }
    
    public static Throwable findCause(Throwable t) {
        if (t.getCause() == null)
            return t;
        else
            return findCause(t.getCause());
    }
}
