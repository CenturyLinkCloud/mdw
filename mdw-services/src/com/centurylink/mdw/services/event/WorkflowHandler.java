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
package com.centurylink.mdw.services.event;

import java.util.Map;

import com.centurylink.mdw.services.EventException;


/**
 * Abstraction of a workflow processor (analogous to an adapter activity) that can be 
 * registered to perform actions in response to a step in a process flow.
 * Register based on name/value pairs returned by the getParameters() method.
 * (See EventManager.registerWorkflowHandler()). 
 */
public interface WorkflowHandler {
    
    /**
     * Performs the actions associated with this workflow processor.request and returns a response,
     * which currently must be a String.  Could be used to configure a threadpool
     * to handle the actual work.
     * @param runtimeContext the runtime context containing the workflow instance state
     * @return object indicating result code back to the workflow
     */
    public Object invoke(Object message, Map<String,Object> headers) throws EventException;
    
    /**
     * The designated workflow asset and parameters uniquely determine which activities the handler should respond to.
     * Returned value should include the workflow package (eg: MyPackage/MyCamelRoute.xml). 
     */
    public String getAsset();
    
    /**
     * The designated workflow asset and parameters uniquely determine which activities the handler should respond to.
     */
    public Map<String,String> getParameters();

    
}
