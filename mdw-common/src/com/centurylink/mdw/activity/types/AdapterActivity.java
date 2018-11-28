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
package com.centurylink.mdw.activity.types;


/**
 * Interface distinguishing new adapter activities.
 * This is to replace ControlledAdapterActivity eventually.
 */
public interface AdapterActivity extends GeneralActivity {

    String PROP_MAX_TRIES = "max_tries";        // including the first try; 0 is retreated the same as 1
    String PROP_TIMEOUT = "timeout";
    String PROP_RETRY_INTERVAL = "retry_interval";
    String PROP_SYNCHRONOUS_RESPONSE = "synchronous";
    String DO_LOGGING = "DO_LOGGING";
    String REQUEST_VARIABLE = "REQUEST_VARIABLE";
    String RESPONSE_VARIABLE = "RESPONSE_VARIABLE";
    String REQUEST_XSD = "REQUEST_XSD";
    String RESPONSE_XSD = "RESPONSE_XSD";

    String COMPCODE_AUTO_RETRY = "Automatic Retry";

    String MAKE_ACTUAL_CALL = "(MAKE_ACTUAL_CALL)";
    int DEFAULT_STUBBER_PORT = 7182;
    int DEFAULT_STUBBER_TIMEOUT = 120;

    /**
     * Determine if the adapter itself is synchronous, i.e.
     * waiting for responses.
     *
     * @return true if it needs to wait for responses
     */
    boolean isSynchronous();

}
