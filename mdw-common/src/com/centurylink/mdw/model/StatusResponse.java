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

import com.centurylink.mdw.model.Jsonable;

/**
 * Standard status response.
 * Lighter-weight, compatible alternative to StatusMessage object.
 */
public class StatusResponse implements Jsonable {

    private Status status;
    public Status getStatus() { return status; }

    public StatusResponse(Status status) {
        this.status = status;
    }

    public StatusResponse(int code, String message) {
        this.status = new Status(code, message);
    }

    public StatusResponse(JSONObject json) throws JSONException {
        this.status = new Status(json.getJSONObject("status"));
    }

    public StatusResponse(Status status, String message) {
        this.status = new Status(status, message);
    }

    public JSONObject getJson() throws JSONException {
        JSONObject json = create();
        json.put("status", status.getJson());
        return json;
    }

    public String getJsonName() {
        return "statusResponse";
    }

    public static final StatusResponse forCode(int code) {
        return new StatusResponse(code, getMessage(code));

    }

    public static final String getMessage(int code) {
        switch (code) {
            case 200: return Status.OK.getMessage();
            case 201: return Status.CREATED.getMessage();
            case 202: return Status.ACCEPTED.getMessage();
            case 204: return Status.NO_CONTENT.getMessage();
            case 205: return Status.RESET_CONTENT.getMessage();
            case 206: return Status.PARTIAL_CONTENT.getMessage();
            case 301: return Status.MOVED_PERMANENTLY.getMessage();
            case 302: return Status.FOUND.getMessage();
            case 303: return Status.SEE_OTHER.getMessage();
            case 304: return Status.NOT_MODIFIED.getMessage();
            case 305: return Status.USE_PROXY.getMessage();
            case 307: return Status.TEMPORARY_REDIRECT.getMessage();
            case 400: return Status.BAD_REQUEST.getMessage();
            case 401: return Status.UNAUTHORIZED.getMessage();
            case 402: return Status.PAYMENT_REQUIRED.getMessage();
            case 403: return Status.FORBIDDEN.getMessage();
            case 404: return Status.NOT_FOUND.getMessage();
            case 405: return Status.METHOD_NOT_ALLOWED.getMessage();
            case 406: return Status.NOT_ACCEPTABLE.getMessage();
            case 407: return Status.PROXY_AUTHENTICATION_REQUIRED.getMessage();
            case 408: return Status.REQUEST_TIMEOUT.getMessage();
            case 409: return Status.CONFLICT.getMessage();
            case 410: return Status.GONE.getMessage();
            case 411: return Status.LENGTH_REQUIRED.getMessage();
            case 412: return Status.PRECONDITION_FAILED.getMessage();
            case 413: return Status.REQUEST_ENTITY_TOO_LARGE.getMessage();
            case 414: return Status.REQUEST_URI_TOO_LONG.getMessage();
            case 415: return Status.UNSUPPORTED_MEDIA_TYPE.getMessage();
            case 416: return Status.REQUESTED_RANGE_NOT_SATISFIABLE.getMessage();
            case 417: return Status.EXPECTATION_FAILED.getMessage();
            case 500: return Status.INTERNAL_SERVER_ERROR.getMessage();
            case 501: return Status.NOT_IMPLEMENTED.getMessage();
            case 502: return Status.BAD_GATEWAY.getMessage();
            case 503: return Status.SERVICE_UNAVAILABLE.getMessage();
            case 504: return Status.GATEWAY_TIMEOUT.getMessage();
            case 505: return Status.HTTP_VERSION_NOT_SUPPORTED.getMessage();
        }
        return "Unrecognized HTTP code";
    }
}
