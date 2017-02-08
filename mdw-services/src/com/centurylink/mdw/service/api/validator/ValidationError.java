/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */

package com.centurylink.mdw.service.api.validator;

/**
 * Dynamic Java workflow asset.
 */
public enum ValidationError {
    MISSING_REQUIRED,
    VALUE_UNDER_MINIMUM,
    VALUE_OVER_MAXIMUM,
    INVALID_FORMAT,
    INVALID_BOOLEAN,
    UNACCEPTABLE_VALUE
  }