/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model.listener;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RMIListener extends Remote {
	
	String JNDI_NAME = "mdw_rmi_listener";
	String BROADCAST_MARKER = "@BROADCAST@";
	int PORT_DIFF = 5;	// for Tomcat, a different port is needed for RMI, add this to HTTP port number 

	void login(String cuid, String pass)
    throws RemoteException;
 
    public String invoke(String meta, String message)
    throws RemoteException;

}
