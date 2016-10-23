/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.activity.types;


/**
 * This interface is just to declare a special type and contains
 * no additional method.
 *
 * The method needToWait() of the super class WaitActivity
 * is used to determine whether the condition has been satisfied to move on.
 *
 */
public interface SynchronizationActivity extends GeneralActivity, SuspendibleActivity {

}
