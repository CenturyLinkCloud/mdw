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
