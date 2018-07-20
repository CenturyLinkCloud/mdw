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
package com.centurylink.mdw.auth;

/**
 * Authentication API for Designer and MDWHub.
 */
public interface Authenticator {

    /**
     * AuthenticationException indicates bad credentials, whereas general MdwSecurityException
     * indicates some other type of error.
     */
    public void authenticate(String user, String password) throws MdwSecurityException;

    /**
     * @return Identifies this authenticator versus others of the same type to allow clients
     * to avoid re-prompting for credentials when the user has already logged in to the same location.
     */
    public String getKey();
}
