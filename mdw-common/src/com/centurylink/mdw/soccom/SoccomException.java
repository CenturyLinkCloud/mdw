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
package com.centurylink.mdw.soccom;

public class SoccomException extends Exception {

    public static final int CREATE_SOCKET = -986;
    public static final int HOSTNAME = -987;
    public static final int CONNECT = -988;
    public static final int REQUEST = -989;
    public static final int POLL_TIMEOUT = -990;
    public static final int RECV_ERROR = -992;
    public static final int RECV_HEADER = -993;
    public static final int SOCKET_CLOSED = -995;
    public static final int MSGID_MISMATCH = -996;

    public static final int ENDM_LENGTH = -1030;

    private int _errcode;

    public SoccomException(int code) {
        super(errdesc(code));
        _errcode = code;
    }

    public SoccomException(int code, Throwable cause) {
        super(errdesc(code), cause);
        _errcode = code;
    }

    public int getErrorCode() {
        return _errcode;
    }

    public static String errdesc(int code) {
        String msg;
        switch (code) {
        case RECV_ERROR:
            msg = "error during recv";
            break;
        case CONNECT:
            msg = "cannot connect the server";
            break;
        case RECV_HEADER:
            msg = "error in recv header";
            break;
        case POLL_TIMEOUT:
            msg = "timeout";
            break;
        default:
            msg = "Unspecified error code " + code;
        }
        return msg;
    }

    public String errdesc() {
        return errdesc(_errcode);
    }

}
