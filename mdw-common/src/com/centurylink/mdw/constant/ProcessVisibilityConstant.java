/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
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
