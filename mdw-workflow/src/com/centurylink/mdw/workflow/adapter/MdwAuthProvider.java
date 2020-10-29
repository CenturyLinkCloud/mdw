package com.centurylink.mdw.workflow.adapter;

import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.centurylink.mdw.auth.AuthTokenProvider;
import com.centurylink.mdw.auth.MdwAuthenticator;
import com.centurylink.mdw.auth.MdwSecurityException;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

public class MdwAuthProvider implements AuthTokenProvider {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();
    private static Map<String,String> tokenMap = new ConcurrentHashMap<>();

    private Map<String,String> options = null;

    @Override
    public byte[] getToken(URL endpoint, String user, String password) throws MdwSecurityException {
        String token = getToken(endpoint, user);
        if (token == null) {
            // not found in cache -- invoke auth call
            token = invokeAuth(user, password);
            if (token == null)
                return null;
            putToken(endpoint, user, token);
        }
        return token.getBytes();
    }

    protected String getToken(URL endpoint, String user) {
        return tokenMap.get(getEndpointKey(endpoint, user));
    }

    protected void putToken(URL endpoint, String user, String token) {
        tokenMap.put(getEndpointKey(endpoint, user), token);
    }

    protected String invokeAuth(String user, String password) throws MdwSecurityException {
        if (options == null || options.get("appId") == null) {
            logger.error("Missing AppId value for MdwAuthenticator");
            return null;
        }
        String token = new MdwAuthenticator(options.get("appId")).doAuthentication(user, password);
        return token == null ? null : token;
    }

    @Override
    public void setOptions(Map<String, String> options) {
        this.options = options;
    }

    @Override
    public void invalidateToken(URL endpoint, String user) {
        tokenMap.remove(getEndpointKey(endpoint, user));
    }

    private static String getEndpointKey(URL endpoint, String user) {
        return endpoint.getProtocol() + "://" + user + "@" + endpoint.getHost() + ":" + endpoint.getPort() + "/" + endpoint.getPath();
    }
}
