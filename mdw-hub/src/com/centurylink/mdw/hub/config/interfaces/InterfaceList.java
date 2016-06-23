/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.hub.config.interfaces;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.centurylink.mdw.workflow.Interface;

/**
 * Represents a collection of workflow interfaces.
 */
public class InterfaceList {
    private List<Interface> interfaces;

    public List<Interface> getInterfaces() {
        return interfaces;
    }

    public InterfaceList(List<Interface> interfaces) {
        // create a new List in order to sort
        this.interfaces = new ArrayList<Interface>();
        for (Interface iface : interfaces)
            this.interfaces.add(iface);
        Collections.sort(this.interfaces, new Comparator<Interface>() {
            public int compare(Interface i1, Interface i2) {
                return i1.getName().compareTo(i2.getName());
            }
        });
    }

    public Interface getInterface(String name) {
        for (Interface iface : interfaces) {
            if (iface.getName().equals(name))
                return iface;
        }
        return null; // not found
    }
}
