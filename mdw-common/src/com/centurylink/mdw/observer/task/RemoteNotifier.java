/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.observer.task;

/**
 * Marker interface indicating that a Notifier is for Remote Summary TaskManager.
 * In that case creation notices are delayed until after the Summary TaskManager knows about
 * the task instance.
 */
public interface RemoteNotifier {

}
