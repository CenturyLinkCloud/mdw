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
package com.centurylink.mdw.model.listener;

import java.util.Map;

/**
 * The interface needs to be implemented if it is desired
 * that the class needs to listen to a specific message
 * at start up.
 */

public interface Listener {

    // tomcat makes headers lower-case, so we'll start out that way
    String METAINFO_REQUEST_ID = "request-id";
    String METAINFO_MDW_REQUEST_ID = "mdw-request-id";
    String METAINFO_MDW_PROCESS_INSTANCE_ID = "mdw-process-instance-id";
    String METAINFO_CORRELATION_ID = "correlation-id";
    String METAINFO_DOCUMENT_ID = "document-id";

    String METAINFO_CONTENT_TYPE = "Content-Type";
    String METAINFO_ACCEPT = "Accept";

    String METAINFO_PROTOCOL = "Protocol";
    String METAINFO_SERVICE_CLASS = "ServiceClass";
    String METAINFO_START_ACTIVITY_ID = "StartActivityID";
    String METAINFO_PROCESS_NAME = "ProcessName";
    String METAINFO_EVENT_NAME = "EventName";
    String METAINFO_EVENT_MESSAGE = "EventMessage";
    String METAINFO_EVENT_ID = "EventID";
    String METAINFO_HEX_CORRELATION_ID = "HexadecimalCorrelationID";
    String METAINFO_HEX_REQUEST_ID = "HexadecimalRequestID";
    String METAINFO_NO_PERSISTENCE = "NoPersistence";
    String METAINFO_NO_META_PERSISTENCE = "NoMetaPersistence";
    String METAINFO_REQUEST_PAYLOAD = "RequestPayload"; // for altering/extracting payload

    // Custom Error Response
    String METAINFO_ERROR_RESPONSE = "ErrorResponse";
    String METAINFO_ERROR_RESPONSE_VALUE = Boolean.TRUE.toString();

    String METAINFO_PROTOCOL_JMS = "Jms";
    String METAINFO_PROTOCOL_WEBSERVICE = "WebService";
    String METAINFO_PROTOCOL_REST = "REST";
    String METAINFO_PROTOCOL_SOAP = "SOAP";
    String METAINFO_PROTOCOL_RMI = "Rmi";
    String METAINFO_PROTOCOL_EMAIL = "Email";
    String METAINFO_PROTOCOL_INTERNAL = "Internal";    // called directly
    String METAINFO_PROTOCOL_KAFKA = "Kafka";

    String METAINFO_REQUEST_PATH = "RequestPath";
    String METAINFO_RESOURCE_SUBPATH = "ResourceSubPath"; // after any '/' in RequestPath

    String AUTHORIZATION_HEADER_NAME = "Authorization";
    String X_HUB_SIGNATURE = "x-hub-signature";

    // Will be populated when authentication happens
    String AUTHENTICATED_USER_HEADER = "AuthenticatedUser";
    // Will be populated when authentication fails
    String AUTHENTICATION_FAILED = "AuthenticationFailed";

    String AUTHENTICATED_JWT = "AuthenticatedJWT";

    String METAINFO_DOWNLOAD_FORMAT = "DownloadFormat";
    String METAINFO_MASTER_OP = "mdw-master-op";
    String DOWNLOAD_FORMAT_EXCEL = "xlsx";
    String DOWNLOAD_FORMAT_ZIP = "zip";
    String DOWNLOAD_FORMAT_JSON = "json";
    String DOWNLOAD_FORMAT_XML = "xml";
    String DOWNLOAD_FORMAT_TEXT = "text";
    String DOWNLOAD_FORMAT_FILE = "file";
    String METAINFO_DOWNLOAD_FILE = "download-file";
    String METAINFO_ZIP_CONTENT = "ZipContent";
    String METAINFO_REQUEST_URL = "RequestURL";
    String METAINFO_REQUEST_QUERY_STRING = "RequestQueryString";
    String METAINFO_HTTP_METHOD = "HttpMethod";
    String METAINFO_HTTP_STATUS_CODE = "HttpStatusCode";

    String METAINFO_TOPIC = "Topic"; // Topic based routing like Kafka, TibcoBus, etc

    String METAINFO_REMOTE_ADDR = "RemoteAddr";
    String METAINFO_REMOTE_HOST = "RemoteHost";
    String METAINFO_REMOTE_PORT = "RemotePort";
    String METAINFO_REMOTE_USER = "RemoteUser";

    String CONTENT_TYPE_JSON = "application/json";
    String CONTENT_TYPE_XML = "text/xml";
    String CONTENT_TYPE_TEXT = "text/plain";
    String CONTENT_TYPE_EXCEL = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    String CONTENT_TYPE_ZIP = "application/zip";
    String CONTENT_TYPE_DOWNLOAD = "application/octet-stream";

    String METAINFO_CLOUD_ROUTING = "cloud-routing";
    String METAINFO_MDW_APP_ID = "mdw-app-id";
    String METAINFO_MDW_APP_TOKEN = "mdw-app-token";

    static boolean isHealthCheck(Map<String,String> headers) {
        return "AppSummary".equals(headers.get(METAINFO_REQUEST_PATH));
    }
}