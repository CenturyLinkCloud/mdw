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
package com.centurylink.mdw.model.listener;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RMIListener extends Remote {
    
    String JNDI_NAME = "mdw_rmi_listener";
    String BROADCAST_MARKER = "@BROADCAST@";
    int PORT_DIFF = 5;    // for Tomcat, a different port is needed for RMI, add this to HTTP port number 

    void login(String cuid, String pass)
    throws RemoteException;
 
    public String invoke(String meta, String message)
    throws RemoteException;

}
