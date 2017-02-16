/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.model;

public interface AttributeHolder {
    public String getAttribute(String name);

    public void setAttribute(String name, String value);
}
