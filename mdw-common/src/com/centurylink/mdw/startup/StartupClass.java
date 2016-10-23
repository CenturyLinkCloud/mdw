/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.startup;

/**
 * The interface needs to be implemented if it is desired
 * that the class is loaded and executed when the server is started
 * The framework will load this class and execute the onStartup method
 * at start up.
 */

public interface StartupClass {


	/**
	 * Method that gets invoked when the server comes up
     * The impl class will have logic to that gets
     * executed when the server starts up
     * @throws StartupException
	 */
	public void onStartup() throws StartupException;

	/**
	 * Method that gets invoked when the server
     * shuts down
	 */
	public void onShutdown() ;

}