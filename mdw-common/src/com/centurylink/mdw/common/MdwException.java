/**
 * Copyright (c) 2017 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common;

public class MdwException extends Exception {

    private int code;

    public MdwException(String code){
        super(code);
    }

    public MdwException(int code, String message) {
        super(message);
        this.code = code;
    }

    public MdwException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public MdwException(String message, Throwable cause){
        super(message, cause);
        this.code = -1;
    }

    public int getCode(){
        return this.code;
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
