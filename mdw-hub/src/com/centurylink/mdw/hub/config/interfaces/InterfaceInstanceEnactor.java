/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.hub.config.interfaces;

import java.io.IOException;

import org.apache.xmlbeans.XmlException;

import com.centurylink.mdw.workflow.Interface;
import com.centurylink.mdw.workflow.InterfaceInstance;

public interface InterfaceInstanceEnactor {
    /**
     * Implemented with version-specific logic.
     */
    public abstract InterfaceInstance getInterfaceInstance(Interface iface) throws IOException, XmlException;

    public abstract void setInterfaceInstance(Interface iface, InterfaceInstance instance)
    throws IOException, XmlException;
}
