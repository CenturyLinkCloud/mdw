package com.centurylink.mdw.auth;

import java.net.URL;
import java.util.Map;

public interface AuthTokenProvider {

    byte[] getToken(URL endpoint, String user, String password) throws MdwSecurityException;
    void setOptions(Map<String,String> options);
    void invalidateToken(URL endpoint, String user);
}
