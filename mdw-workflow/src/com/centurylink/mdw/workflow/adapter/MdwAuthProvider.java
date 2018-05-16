/*
 * Copyright (C) 2018 CenturyLink, Inc.
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
package com.centurylink.mdw.workflow.adapter;

import java.lang.reflect.Method;
import java.net.URL;

import com.centurylink.mdw.auth.AuthTokenProvider;
import com.centurylink.mdw.auth.MdwAuthenticator;
import com.centurylink.mdw.auth.MdwSecurityException;
import com.centurylink.mdw.cache.CacheService;
import com.centurylink.mdw.services.cache.CacheRegistration;
import com.centurylink.mdw.services.util.AuthUtils;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

public class MdwAuthProvider implements AuthTokenProvider {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();
    private String appId = null;

    @Override
    public byte[] getToken(URL endpoint, String user, String password) throws MdwSecurityException {

        try {
            String token = getToken(endpoint, user);
            if (token == null) {
                // not found in cache -- invoke auth call
                token = invokeAuth(user, password);
                putToken(endpoint, user, token);
            }
            return token.getBytes();
        }
        catch (ReflectiveOperationException ex) {
            throw new MdwSecurityException(ex.getMessage(), ex);
        }
    }

    protected String getToken(URL endpoint, String user) throws ReflectiveOperationException {
        CacheService jwtTokenCacheInstance = CacheRegistration.getInstance().getCache(AuthUtils.JWTTOKENCACHE);
        if (jwtTokenCacheInstance == null)  {
            logger.warn("Missing package com.centurylink.mdw.authCTL");
            return null;
        }
        Method tokenGetter = jwtTokenCacheInstance.getClass().getMethod("getToken", URL.class, String.class);
        String token = (String)tokenGetter.invoke(jwtTokenCacheInstance, endpoint, user);
        return token == null ? null : token;
    }

    protected void putToken(URL endpoint, String user, String token) throws ReflectiveOperationException {
        CacheService jwtTokenCacheInstance = CacheRegistration.getInstance().getCache(AuthUtils.JWTTOKENCACHE);
        if (jwtTokenCacheInstance == null)  {
            logger.warn("Missing package com.centurylink.mdw.authCTL");
            return;
        }
        Method tokenPutter = jwtTokenCacheInstance.getClass().getMethod("putToken", URL.class, String.class, String.class);
        tokenPutter.invoke(jwtTokenCacheInstance, endpoint, user, token);
    }

    protected String invokeAuth(String user, String password) throws MdwSecurityException {
        String token = new MdwAuthenticator(appId).doAuthentication(user, password);
        return token == null ? null : token;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }
}
