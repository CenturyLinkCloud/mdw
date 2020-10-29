package com.centurylink.mdw.workflow.adapter.http;

import com.centurylink.mdw.util.HttpConnection;
import com.centurylink.mdw.util.HttpHelper;

/**
 * Auth provider for HTTP Basic.  Does not implement AuthTokenProvider.
 */
public class BasicAuthProvider {

    private String user;
    private String password;

    public BasicAuthProvider(String user, String password) {
        this.user = user;
        this.password = password;
    }

    public HttpHelper getHttpHelper(HttpConnection connection) {
        HttpHelper helper = new HttpHelper((HttpConnection)connection);
        helper.getConnection().setUser(user);
        helper.getConnection().setPassword(password);
        return helper;
    }
}
