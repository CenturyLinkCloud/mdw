/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model.workflow;

public interface TransitionStatus {

    public static final Integer STATUS_INITIATED = new Integer(1);
    public static final Integer STATUS_COMPLETED = new Integer(6);
    
    public static final Integer[] allStatusCodes = {STATUS_INITIATED, STATUS_COMPLETED};
    public static final String[] allStatusNames = {"Initiated", "Completed" };
    
}