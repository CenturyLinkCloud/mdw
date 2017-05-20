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
package com.centurylink.mdw.model;

import org.json.JSONException;
import org.json.JSONObject;

public class Status implements Jsonable {
    private int code;
    public int getCode() { return code; }

    private String message;
    public String getMessage() { return message; }

    public Status(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public Status(Status status, String message) {
        this.code = status.code;
        this.message = message;
    }

    public Status(JSONObject json) throws JSONException {
        if (json.has("code"))
            this.code = json.getInt("code");
        if (json.has("message"))
            this.message = json.getString("message");
    }

    public JSONObject getJson() throws JSONException {
        JSONObject json = create();
        if (code > 0)
            json.put("code", code);
        if (message != null)
            json.put("message", message);
        return json;
    }

    public String getJsonName() {
        return "status";
    }

    /**
     * For convenience, reproduced from javax.ws.rs.core.Response.Status
     */
    public static final Status OK = new Status(200, "OK");
    public static final Status CREATED = new Status(201, "Created");
    public static final Status ACCEPTED = new Status(202, "Accepted");
    public static final Status NO_CONTENT = new Status(204, "No Content");
    public static final Status RESET_CONTENT = new Status(205, "Reset Content");
    public static final Status PARTIAL_CONTENT = new Status(206, "Partial Content");

    public static final Status MOVED_PERMANENTLY = new Status(301, "Moved Permanently");
    public static final Status FOUND = new Status(302, "Found");
    public static final Status SEE_OTHER = new Status(303, "See Other");
    public static final Status NOT_MODIFIED = new Status(304, "Not Modified");
    public static final Status USE_PROXY = new Status(305, "Use Proxy");
    public static final Status TEMPORARY_REDIRECT = new Status(307, "Temporary Redirect");

    public static final Status BAD_REQUEST = new Status(400, "Bad Request");
    public static final Status UNAUTHORIZED = new Status(401, "Unauthorized");
    public static final Status PAYMENT_REQUIRED = new Status(402, "Payment Required");
    public static final Status FORBIDDEN = new Status(403, "Forbidden");
    public static final Status NOT_FOUND = new Status(404, "Not Found");
    public static final Status METHOD_NOT_ALLOWED = new Status(405, "Method Not Allowed");
    public static final Status NOT_ACCEPTABLE = new Status(406, "Not Acceptable");
    public static final Status PROXY_AUTHENTICATION_REQUIRED = new Status(407, "Proxy Authentication Required");
    public static final Status REQUEST_TIMEOUT = new Status(408, "Request Timeout");
    public static final Status CONFLICT = new Status(409, "Conflict");
    public static final Status GONE = new Status(410, "Gone");
    public static final Status LENGTH_REQUIRED = new Status(411, "Length Required");
    public static final Status PRECONDITION_FAILED = new Status(412, "Precondition Failed");
    public static final Status REQUEST_ENTITY_TOO_LARGE = new Status(413, "Request Entity Too Large");
    public static final Status REQUEST_URI_TOO_LONG = new Status(414, "Request-URI Too Long");
    public static final Status UNSUPPORTED_MEDIA_TYPE = new Status(415, "Unsupported Media Type");
    public static final Status REQUESTED_RANGE_NOT_SATISFIABLE = new Status(416, "Requested Range Not Satisfiable");
    public static final Status EXPECTATION_FAILED = new Status(417, "Expectation Failed");

    public static final Status INTERNAL_SERVER_ERROR = new Status(500, "Internal Server Error");
    public static final Status INTERNAL_ERROR = new Status(500, "Internal Error");
    public static final Status NOT_IMPLEMENTED = new Status(501, "Not Implemented");
    public static final Status BAD_GATEWAY = new Status(502, "Bad Gateway");
    public static final Status SERVICE_UNAVAILABLE = new Status(503, "Service Unavailable");
    public static final Status GATEWAY_TIMEOUT = new Status(504, "Gateway Timeout");
    public static final Status HTTP_VERSION_NOT_SUPPORTED = new Status(505, "HTTP Version Not Supported");

}
