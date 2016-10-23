/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.activity.types;


/**
 * Interface for all Finish Activities
 */
public interface FinishActivity extends GeneralActivity{

    String getProcessCompletionCode();
    
    boolean doNotNotifyCaller();
}
