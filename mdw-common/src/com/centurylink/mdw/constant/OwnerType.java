/*
 * Copyright (C) 2017 CenturyLink, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

    public static final String ADAPTER = "ADAPTER"; // owner ID is activity instance ID
    public static final String ADAPTER_REQUEST = "ADAPTER_REQUEST"; // owner ID is activity instance ID
    public static final String ADAPTER_RESPONSE = "ADAPTER_RESPONSE"; // owner ID is activity instance ID
    public static final String ADAPTER_REQUEST_META = "ADAPTER_REQUEST_META"; // owner ID is document ID of ADAPTER_REQUEST
    public static final String ADAPTER_RESPONSE_META = "ADAPTER_RESPONSE_META"; // owner ID is document ID of ADAPTER_RESPONSE
    public static final String LISTENER_REQUEST = "LISTENER_REQUEST";   // owner ID is processInstanceId (if present)
    public static final String LISTENER_RESPONSE = "LISTENER_RESPONSE"; // owner ID is the document ID of LISTENER_REQUEST
    public static final String LISTENER_REQUEST_META = "LISTENER_REQUEST_META"; // owner ID is the document ID of the LISTENER_REQUEST
    public static final String LISTENER_RESPONSE_META = "LISTENER_RESPONSE_META"; // owner ID is the document ID of the LISTENER_RESPONSE

    public static final String PACKAGE = "PACKAGE";

    public static final String DOCUMENT = "DOCUMENT";
    public static final String ERROR = "ERROR";
    public static final String SLA = "SLA";

    public static final String SOLUTION = "SOLUTION";

    public static final String PROCESS_RUN = "PROCESS_RUN";

    public static final String PROCESS_INSTANCE_DEF = "PROCESS_INSTANCE_DEF";

    public static final String COMMENT = "COMMENT";
}
