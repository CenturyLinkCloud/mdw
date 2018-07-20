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

import com.centurylink.mdw.common.MdwException;

public class AdapterException extends MdwException {

    public static final int EXCEED_MAXTRIES = 41282;

    private boolean retryable;

    public AdapterException(String message) {
        super(message);
    }

    public AdapterException(String message, boolean retryable) {
        super(message);
        this.retryable = retryable;
    }

    public AdapterException(int code, String message) {
        super(code, message);

    }

    public AdapterException(int code, String message, boolean retryable) {
        super(code, message);
        this.retryable = retryable;
    }

    public AdapterException(String message, Throwable cause) {
        super(message, cause);

    }

    public AdapterException(int code, String message, Throwable cause) {
        super(code, message, cause);

    }

    public AdapterException(int code, String message, Throwable cause, boolean retryable) {
        super(code, message, cause);
        this.retryable = retryable;
    }

    public boolean isRetryable() {
        return retryable;
    }

    public void setRetryable(boolean retryable) {
        this.retryable = retryable;
    }
}
