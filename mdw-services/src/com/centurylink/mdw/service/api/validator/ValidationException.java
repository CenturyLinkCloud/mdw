/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */

package com.centurylink.mdw.service.api.validator;

/**
 * Dynamic Java workflow asset.
 */
public class ValidationException extends Exception
{
    private ValidationMessage message;

    public ValidationException(ValidationMessage message, Throwable cause) {
        super(message.getMessage(), cause);
    }

    public ValidationException message(ValidationMessage message) {
        this.message = message;
        return this;
    }

    public ValidationMessage getValidationMessage() {
        return message;
    }

}
