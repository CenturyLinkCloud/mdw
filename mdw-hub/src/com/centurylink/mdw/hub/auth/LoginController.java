/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.hub.auth;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.faces.context.FacesContext;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ActionEvent;
import javax.faces.event.ActionListener;

import com.centurylink.mdw.auth.Authenticator;
import com.centurylink.mdw.auth.LdapAuthenticator;
import com.centurylink.mdw.auth.OAuthAuthenticator;
import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.common.constant.AuthConstants;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.model.value.user.AuthenticatedUser;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.util.RemoteLocator;

public class LoginController implements ActionListener {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    public void processAction(ActionEvent actionEvent) throws AbortProcessingException {
        Login login = (Login) FacesVariableUtil.getValue("login");
        if (actionEvent.getComponent().getId().equals("loginButton")) {
            try {
                Authenticator auth;
                if (AuthConstants.getOAuthTokenLocation() != null)
                    auth = new OAuthAuthenticator();
                else // ldap is default
                    auth = new LdapAuthenticator();

                auth.authenticate(login.getUser(), login.getPassword());

                AuthenticatedUser user = RemoteLocator.getUserManager().loadUser(login.getUser());
                if (user == null && AuthConstants.isAllowAnyAuthenticatedUser())
                    user = new AuthenticatedUser(login.getUser());
                FacesVariableUtil.setValue("authenticatedUser", user);
                logger.info("Authenticated User: " + user.getCuid());
                logger.mdwDebug("Auth User Details:\n" + user);
                FacesVariableUtil.navigate(getWelcomeUrl());
            }
            catch (Exception ex) {
                logger.severeException(ex.getMessage(), ex);
                try {
                    FacesContext.getCurrentInstance().getExternalContext().redirect(getErrorPath());
                }
                catch (IOException ex2) {
                    logger.severeException(ex2.getMessage(), ex2);
                    throw new AbortProcessingException(ex2.getMessage(), ex2);
                }
            }
        }
    }

    public URL getWelcomeUrl() throws MalformedURLException {
        return new URL(ApplicationContext.getMdwHubUrl());
    }

    public String getErrorPath() {
        return "/" + ApplicationContext.getMdwHubContextRoot() + "/loginError";
    }
}
