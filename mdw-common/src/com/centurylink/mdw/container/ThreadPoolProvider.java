/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.container;

public interface ThreadPoolProvider {

	// following are provider names
	String MDW = "MDW";

	// following are worker names
	String WORKER_ENGINE = "Engine";
	String WORKER_LISTENER = "Listener";
	String WORKER_SCHEDULER = "Scheduler";
    String WORKER_MONITOR = "Monitor";
	String WORKER_DEFAULT = "Default";

	void start();

	void stop();

	boolean hasAvailableThread(String workerName);

	boolean execute(String workerName, String assignee, Runnable command);

	String currentStatus();

}
