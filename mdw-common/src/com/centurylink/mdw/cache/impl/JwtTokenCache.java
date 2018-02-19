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
package com.centurylink.mdw.cache.impl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.centurylink.mdw.cache.CacheService;
import com.centurylink.mdw.cache.CachingException;

public class JwtTokenCache implements CacheService {

    private static volatile Map<String,String> tokenMap = new ConcurrentHashMap<String,String>();

    public void refreshCache() throws CachingException {
            clearCache();
    }

    @Override
    public void clearCache() {
        tokenMap.clear();
    }

    public static String getToken(String credentials) throws CachingException {
        return tokenMap.get(credentials);
    }

    public static String setToken(String credentials, String token) throws CachingException {
        return tokenMap.put(credentials, token);
    }
}
