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