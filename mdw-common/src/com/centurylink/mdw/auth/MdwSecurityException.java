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

import com.centurylink.mdw.common.MdwException;

public class MdwSecurityException extends MdwException {

    public static final int UNTRUSTED_CERT = 101;

    private int errorCode;

    public int getCode() {
        return errorCode;
    }

    public MdwSecurityException(String message) {
        super(-1, message);
    }

    public MdwSecurityException(String message, Throwable cause) {
        super(-1, message, cause);
    }

    public MdwSecurityException(int code, String message) {
        super(code, message);
        errorCode = code;
    }

    public MdwSecurityException(int code, String message, Throwable cause) {
        super(code, message, cause);
        errorCode = code;
    }
}
