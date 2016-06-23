/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.service;

/**
 * Indicates that one MDW component is awaiting startup of another.
 */
public class AwaitingStartupException extends Exception {

    public AwaitingStartupException(String message) {
        super(message);
    }

    public AwaitingStartupException(String message, Throwable cause) {
        super(message, cause);
    }

}