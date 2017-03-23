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
 * Constants relating to process variables.
 */
public class VariableConstants {

    /**
     * Variable mode constants
     */
    public static final Integer VARIABLE_CATEGORY_LOCAL = new Integer(0);
    public static final Integer VARIABLE_CATEGORY_INPUT = new Integer(1);
    public static final Integer VARIABLE_CATEGORY_OUTPUT = new Integer(2);
    public static final Integer VARIABLE_CATEGORY_IN_OUT = new Integer(3);

    /**
     * variable name constants
     */
    public static final String MASTER_DOCUMENT = "MasterDocument";
    public static final String REQUEST = "request";
    public static final String REQUEST_HEADERS = "requestHeaders";
    public static final String RESPONSE = "response";
    public static final String RESPONSE_HEADERS = "responseHeaders";

}
