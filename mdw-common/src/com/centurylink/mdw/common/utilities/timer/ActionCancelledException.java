/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.utilities.timer;

public class ActionCancelledException extends Exception {

    private static final long serialVersionUID = 1L;

    public ActionCancelledException() {
      super();
    }
    
    public ActionCancelledException(String message) {
        super(message);
    }
  
    public ActionCancelledException(String message, Throwable cause) {
        super(message, cause);
    }
}
