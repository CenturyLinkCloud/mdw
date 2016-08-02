/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.designer.testing;

/**
 * Filter applied to expected results before parsing according to old expressions syntax.
 */
public interface PreFilter {
    public String apply(String before);
}
