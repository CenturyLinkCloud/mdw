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

    String SYSTEM = "SYSTEM";
    String TESTER = "TESTER";

    String PROCESS = "PROCESS";
    String PROCESS_INSTANCE = "PROCESS_INSTANCE";
    String MAIN_PROCESS_INSTANCE = "MAIN_PROCESS_INSTANCE";    // for embedded proc inst

    String ACTIVITY = "ACTIVITY";
    String ACTIVITY_INSTANCE = "ACTIVITY_INSTANCE";
    String ACTIVITY_IMPLEMENTOR = "ACTIVITY_IMPLEMENTOR";

    String TASK = "TASK";
    String TASK_INSTANCE = "TASK_INSTANCE";

    String WORK_TRANSITION = "WORK_TRANSITION";
    String WORK_TRANSITION_INSTANCE = "WORK_TRANSITION_INSTANCE";

    String USER = "USER";
    String USER_GROUP = "USER_GROUP";
    String USER_GROUP_MAP = "USER_GROUP_MAP";

    String INTERNAL_EVENT = "INTERNAL_EVENT";

    String VARIABLE_INSTANCE = "VARIABLE_INSTANCE";

    String ADAPTER = "ADAPTER"; // owner ID is activity instance ID
    String ADAPTER_REQUEST = "ADAPTER_REQUEST"; // owner ID is activity instance ID
    String ADAPTER_RESPONSE = "ADAPTER_RESPONSE"; // owner ID is activity instance ID
    String ADAPTER_REQUEST_META = "ADAPTER_REQUEST_META"; // owner ID is document ID of ADAPTER_REQUEST
    String ADAPTER_RESPONSE_META = "ADAPTER_RESPONSE_META"; // owner ID is document ID of ADAPTER_RESPONSE
    String LISTENER_REQUEST = "LISTENER_REQUEST";   // owner ID is processInstanceId (if present)
    String LISTENER_RESPONSE = "LISTENER_RESPONSE"; // owner ID is the document ID of LISTENER_REQUEST
    String LISTENER_REQUEST_META = "LISTENER_REQUEST_META"; // owner ID is the document ID of the LISTENER_REQUEST
    String LISTENER_RESPONSE_META = "LISTENER_RESPONSE_META"; // owner ID is the document ID of the LISTENER_RESPONSE

    String DOCUMENT = "DOCUMENT";
    String ERROR = "ERROR";
    String SLA = "SLA";

    String SOLUTION = "SOLUTION";

    String PROCESS_RUN = "PROCESS_RUN";

    String PROCESS_INSTANCE_DEF = "PROCESS_INSTANCE_DEF";

    String COMMENT = "COMMENT";
}
