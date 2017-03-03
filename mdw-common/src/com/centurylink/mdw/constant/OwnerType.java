/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.constant;

/**
 * This file contains all the generic or common owner types for diff entities
 * This can be extended for more customization
 */
public interface OwnerType {

    public static final String SYSTEM = "SYSTEM";
    public static final String TESTER = "TESTER";

    public static final String PROCESS = "PROCESS";
    public static final String PROCESS_INSTANCE = "PROCESS_INSTANCE";
    public static final String MAIN_PROCESS_INSTANCE = "MAIN_PROCESS_INSTANCE";    // for embedded proc inst
    public static final String EXTERNAL = "EXTERNAL";

    public static final String ACTIVITY = "ACTIVITY";
    public static final String ACTIVITY_INSTANCE = "ACTIVITY_INSTANCE";
    public static final String ACTIVITY_IMPLEMENTOR = "ACTIVITY_IMPLEMENTOR";

    public static final String TASK = "TASK";
    public static final String TASK_INSTANCE = "TASK_INSTANCE";

    public static final String WORK_TRANSITION = "WORK_TRANSITION";
    public static final String WORK_TRANSITION_INSTANCE = "WORK_TRANSITION_INSTANCE";

    public static final String USER = "USER";
    public static final String USER_GROUP = "USER_GROUP";
    public static final String USER_GROUP_MAP = "USER_GROUP_MAP";

    public static final String INTERNAL_EVENT = "INTERNAL_EVENT";

    public static final String VARIABLE_INSTANCE = "VARIABLE_INSTANCE";

    public static final String ADAPTER_REQUEST = "ADAPTER_REQUEST"; // owner ID is activity instance ID
    public static final String ADAPTER_RESPONSE = "ADAPTER_RESPONSE"; // owner ID is activity instance ID
    public static final String LISTENER_REQUEST = "LISTENER_REQUEST";   // owner ID is processInstanceId (if present)
    public static final String LISTENER_RESPONSE = "LISTENER_RESPONSE"; // owner ID is the document ID of LISTENER_REQUEST

    public static final String NOTIFICATION_ACTIVITY = "NOTIFICATION_ACTIVITY"; // owner ID is activity instance ID

    public static final String PACKAGE = "PACKAGE";

    public static final String DOCUMENT = "DOCUMENT";
    public static final String ERROR = "ERROR";
    public static final String SLA = "SLA";

    public static final String MASTER_REQUEST = "MASTER_REQUEST";
    public static final String SOLUTION = "SOLUTION";

    public static final String PROCESS_RUN = "PROCESS_RUN";
}
