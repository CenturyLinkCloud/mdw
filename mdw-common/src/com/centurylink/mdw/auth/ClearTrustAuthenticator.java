/**
 * Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.auth;

import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.common.constant.PropertyGroups;
import com.centurylink.mdw.common.exception.PropertyException;
import com.centurylink.mdw.common.utilities.property.PropertyManager;
import com.qwest.appsec.actrl.AccessControl;
import com.qwest.appsec.actrl.AccessControlFactory;
import com.qwest.appsec.actrl.BasicCredential;

public class ClearTrustAuthenticator implements Authenticator {

    public static final String DEFAULT_APP_NAME = "MDW";
    public static final String DEFAULT_CT_MODE = "prod";

    private String appName;
    private String ctMode;

    public ClearTrustAuthenticator() {
        this(DEFAULT_APP_NAME, DEFAULT_CT_MODE);
    }

    public ClearTrustAuthenticator(String appName, String ctMode) {
        this.appName = appName;
        this.ctMode = ctMode;
    }

    public void authenticate(String cuid, String pass) throws MdwSecurityException {
        AccessControl accessControl = null;
        BasicCredential basicCred = null;
        try
        {
            if (appName == null) {
                PropertyManager appProp = PropertyManager.getInstance();
                appName = appProp.getStringProperty(PropertyGroups.APPLICATION_DETAILS, "MALApplicationAcronym");
                if (appName == null)
                    appName = ApplicationContext.getApplicationName();
                String applmode = appProp.getStringProperty(PropertyGroups.APPLICATION_DETAILS, "EnvironmentName");
                if (applmode == null)
                  applmode = System.getProperty("runtimeEnv");
                if (applmode.equals("dev") || applmode.equals("prod"))
                    ctMode = applmode;
                else
                    ctMode = "test";
            }
            String existingCtMode = System.getProperty("com.qwest.appsec.actrl.ctenv");
            if (!("employee." + ctMode).equals(existingCtMode))
                System.out.println("Using ClearTrust LDAP authentication with environment setting: " + "employee." + ctMode);
            System.setProperty("com.qwest.appsec.actrl.ctenv", "employee." + ctMode);
            System.setProperty("com.qwest.appsec.actrl.applName", appName);
            // appName needs to be MAL name, but CT does not check
            accessControl = AccessControlFactory.getInstance();

            basicCred = new BasicCredential(cuid, pass);
            accessControl.authenticate(basicCred);
        }
        catch (com.qwest.appsec.actrl.exception.AuthenticationException authExc) {
            throw new AuthenticationException("Authentication failed", authExc);
        }
        catch (PropertyException ex) {
            String msg = "Cannot read properties for ClearTrust";
            throw new MdwSecurityException(msg, ex);
        }
        catch (Exception e) {
            throw new MdwSecurityException(e.getMessage(), e);
        }
    }

    public String getKey() {
        return appName + "_" + ctMode;
    }

    public static void main(String[] args) {
        if (args.length != 2)
            throw new RuntimeException("args: <user> <password>");
        Authenticator auth = new ClearTrustAuthenticator("MDW", "test");
        try {
            auth.authenticate(args[0], args[1]);
            System.out.print("authenticated user " + args[0]);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}
