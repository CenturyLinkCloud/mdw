/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.properties.convert;

public class ConversionException extends RuntimeException {
    public ConversionException(String message) {
        super(message);
    }

    public ConversionException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConversionException(Throwable cause) {
        super(cause);
    }
}
