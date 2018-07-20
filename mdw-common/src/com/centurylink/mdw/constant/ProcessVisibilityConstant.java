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
 * This class hold all the constants for process visibility
 *
 */
public class ProcessVisibilityConstant  {

     public static final String PUBLIC = "PUBLIC";

     public static final String PRIVATE = "PRIVATE";
     
     public static final String EMBEDDED = "EMBEDDED";

     public static final String SERVICE = "SERVICE";
     
     public static final String REGULAR = PUBLIC;
      
     // subtype for embedded process (EMBEDDED_PROCESS_TYPE)
     public static final String EMBEDDED_ERROR_PROCESS = "Exception Handler";
     public static final String EMBEDDED_DELAY_PROCESS = "Delay Handler";
     public static final String EMBEDDED_ABORT_PROCESS = "Cancellation Handler";
     public static final String EMBEDDED_CORRECT_PROCESS = "Correction Handler";


}
