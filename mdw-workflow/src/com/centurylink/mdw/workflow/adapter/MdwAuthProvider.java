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

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.auth.AuthTokenProvider;
import com.centurylink.mdw.auth.MdwAuthenticator;
import com.centurylink.mdw.auth.MdwSecurityException;
import com.centurylink.mdw.cache.CacheService;
import com.centurylink.mdw.services.cache.CacheRegistration;
import com.centurylink.mdw.services.util.AuthUtils;

public class MdwAuthProvider implements AuthTokenProvider {

    @Override
    public byte[] getToken(URL endpoint, String user, String password) throws MdwSecurityException {

        try {
            byte[] token = getToken(endpoint, user);
            if (token == null) {
                // not found in cache -- invoke auth call
                token = invokeAuth(user, password);
                putToken(endpoint, user, token);
            }
            return token;
        }
        catch (ReflectiveOperationException ex) {
            throw new MdwSecurityException(ex.getMessage(), ex);
        }
    }

    protected byte[] getToken(URL endpoint, String user) throws ReflectiveOperationException {
        CacheService jwtTokenCacheInstance = CacheRegistration.getInstance().getCache(AuthUtils.JWTTOKENCACHE);
        Method tokenGetter = jwtTokenCacheInstance.getClass().getMethod("getToken", URL.class, String.class);
        Object token = tokenGetter.invoke(jwtTokenCacheInstance, endpoint, user);
        return token == null ? null : token.toString().getBytes();
    }

    protected void putToken(URL endpoint, String user, byte[] token) throws ReflectiveOperationException {
        CacheService jwtTokenCacheInstance = CacheRegistration.getInstance().getCache(AuthUtils.JWTTOKENCACHE);
        Method tokenPutter = jwtTokenCacheInstance.getClass().getMethod("putToken", URL.class, String.class, String.class);
        tokenPutter.invoke(jwtTokenCacheInstance, endpoint, user, new String(token));
    }

    protected byte[] invokeAuth(String user, String password) throws MdwSecurityException {
        String token = new MdwAuthenticator(ApplicationContext.getAppId()).doAuthentication(user, password);
        return token == null ? null : token.getBytes();
    }
}
