/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.listener;

import java.util.Hashtable;

import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.UserManager;
import com.centurylink.mdw.util.StringHelper;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

public class ListenerAuthHelper {
    // CONSTANTS ------------------------------------------------------
    private static final String SEPERATOR = "~";

    // CLASS VARIABLES ------------------------------------------------
    private static Hashtable<String,String> authenticatedClients = new Hashtable<String,String>();

    protected static StandardLogger logger = LoggerUtil.getStandardLogger();

    // INSTANCE VARIABLES ---------------------------------------------

    // PUBLIC METHODS --------------------------------------------------
    /**
     * Method that checks if the client has access to the service
     *
     * @param pServiceName
     * @param pClientName
     * @param pPassword
     * @return boolean status
     */
    public static boolean isAuthenticated(String pServiceName, String pClientName, String pPassword) {
        if (StringHelper.isEmpty(pServiceName) || StringHelper.isEmpty(pClientName)
                || StringHelper.isEmpty(pPassword)) {
            return false;
        }
        boolean isSuccess = false;
        String key = pServiceName + SEPERATOR + pClientName;
        String authenticated = authenticatedClients.get(key);
        if (StringHelper.isEqualIgnoreCase(authenticated, pPassword)) {
            return true;
        } else {
            // Make a call to the authentication service and validate the user
            isSuccess = authenticate(pServiceName, pClientName, pPassword);
            if (isSuccess) {
                authenticatedClients.put(key, pPassword);
            }
        }
        return isSuccess;
    }

    /**
     * Method that checks if the client has access to the service
     *
     * @param pServiceName
     * @param pClientName
     * @param pPassword
     * @return boolean status
     */
    private static boolean authenticate(String pServiceName, String pClientName, String pPassword) {
        String defPass = StringHelper.reverseString(pClientName);
        if (!StringHelper.isEqual(pPassword, defPass)) {
            logger.info("Invaid Password Entered by the Client. ClientName:" + pClientName + " ServiceName="
                    + pServiceName);
            return false;
        }
        try {
            UserManager userMgr = ServiceLocator.getUserManager();
            boolean belongs = userMgr.doesUserBelongToGroup(pClientName, pServiceName);
            return belongs;
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            return false;
        }
    }
}
