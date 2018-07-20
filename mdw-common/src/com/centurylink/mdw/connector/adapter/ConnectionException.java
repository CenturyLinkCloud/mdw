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
package com.centurylink.mdw.connector.adapter;

public class ConnectionException extends Exception {

    public static final int CONNECTION_DOWN = 41290;
    public static final int CONFIGURATION_WRONG = 41286;

    private int code;

    public ConnectionException(String message) {
        super(message);
        this.code = CONNECTION_DOWN;
    }

    public ConnectionException(int code, String message) {
        super(message);
        this.code = code;
    }

    public ConnectionException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConnectionException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public int getCode(){
        return code;
    }

}
