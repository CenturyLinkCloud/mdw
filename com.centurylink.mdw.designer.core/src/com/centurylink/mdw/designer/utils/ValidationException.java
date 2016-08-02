/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.designer.utils;

import java.util.ArrayList;
import java.util.List;

public class ValidationException extends Exception {
  
    private static final long serialVersionUID = 3509358139080915727L;
    
    private List<String> errors;
    public List<String> getErrors() { return errors; }
    
    public ValidationException(List<String> errors, Throwable cause) {
        super(cause);
        this.errors = errors;
    }
  
    public ValidationException(List<String> errors) {
        this.errors = errors;
    }
    
    public ValidationException(String message) {
        super(message);
        errors = new ArrayList<String>();
        errors.add(message);
    }
    
    public void fillInErrors(StringBuffer sb, int show_max_errors) {
        int n = errors.size();
        if (n>show_max_errors) {
            sb.append("There are " + n + " errors; the first " +
                    show_max_errors + " are:\n\n");
            n = show_max_errors;
        } else if (n==1) {
            sb.append("There is 1 error:\n\n");
        } else sb.append("There are " + n + " errors:\n\n");
        for (int i=0; i<n; i++) {
            sb.append("  ").append(i+1).append(". ").append(errors.get(i)).append("\n");
        }
    }
}
