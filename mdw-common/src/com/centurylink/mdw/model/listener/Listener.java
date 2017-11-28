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

/**
 * The interface needs to be implemented if it is desired
 * that the class needs to listen to a specific message
 * at start up.
 */

public interface Listener {

    // tomcat makes headers lower-case, so we'll start out that way
    public static final String METAINFO_REQUEST_ID = "request-id";
    public static final String METAINFO_MDW_REQUEST_ID = "mdw-request-id";
    public static final String METAINFO_MDW_PROCESS_INSTANCE_ID = "mdw-process-instance-id";
    public static final String METAINFO_CORRELATION_ID = "correlation-id";
    public static final String METAINFO_DOCUMENT_ID = "document-id";

    public static final String METAINFO_CONTENT_TYPE = "Content-Type";
    public static final String METAINFO_ACCEPT = "Accept";

    public static final String METAINFO_PROTOCOL = "Protocol";
    public static final String METAINFO_SERVICE_CLASS = "ServiceClass";
    public static final String METAINFO_START_ACTIVITY_ID = "StartActivityID";
    public static final String METAINFO_PROCESS_NAME = "ProcessName";
    public static final String METAINFO_EVENT_NAME = "EventName";
    public static final String METAINFO_EVENT_MESSAGE = "EventMessage";
    public static final String METAINFO_EVENT_ID = "EventID";
    public static final String METAINFO_HEX_CORRELATION_ID = "HexadecimalCorrelationID";
    public static final String METAINFO_HEX_REQUEST_ID = "HexadecimalRequestID";
    public static final String METAINFO_PACKAGE_NAME = "PackageName";
    public static final String METAINFO_NO_PERSISTENCE = "NoPersistence";
    public static final String METAINFO_NO_META_PERSISTENCE = "NoMetaPersistence";
    //Custom Error Response
    public static final String METAINFO_ERROR_RESPONSE = "ErrorResponse";
    public static final String METAINFO_ERROR_RESPONSE_VALUE = Boolean.TRUE.toString();

    public static final String METAINFO_PROTOCOL_JMS = "Jms";
    public static final String METAINFO_PROTOCOL_BUS = "Bus";
    public static final String METAINFO_PROTOCOL_WEBSERVICE = "WebService";
    public static final String METAINFO_PROTOCOL_REST = "REST";
    public static final String METAINFO_PROTOCOL_RESTFUL_WEBSERVICE = METAINFO_PROTOCOL_REST;
    public static final String METAINFO_PROTOCOL_SOAP = "SOAP";
    public static final String METAINFO_PROTOCOL_EJB = "Ejb";
    public static final String METAINFO_PROTOCOL_RMI = "Rmi";
    public static final String METAINFO_PROTOCOL_MQSERIES = "MqSeries";
    public static final String METAINFO_PROTOCOL_EMAIL = "Email";
    public static final String METAINFO_PROTOCOL_INTERNAL = "Internal";    // called directly
    public static final String METAINFO_PROTOCOL_KAFKA = "Kafka";

    public static final String METAINFO_REQUEST_PATH = "RequestPath";
    public static final String METAINFO_RESOURCE_SUBPATH = "ResourceSubPath"; // after any '/' in RequestPath

    public static final String AUTHORIZATION_HEADER_NAME = "Authorization";
    public static final String X_HUB_SIGNATURE = "x-hub-signature";

    // Will be populated when authentication happens
    public static final String AUTHENTICATED_USER_HEADER = "AuthenticatedUser";
    // Will be populated when authentication fails
    public static final String AUTHENTICATION_FAILED = "AuthenticationFailed";
    public static final String AUTHORIZATION_WORKGROUP = "AuthorizationWorkgroup";

    public static final String METAINFO_DOWNLOAD_FORMAT = "DownloadFormat";
    public static final String METAINFO_MASTER_OP = "mdw-master-op";
    public static final String DOWNLOAD_FORMAT_EXCEL = "xlsx";
    public static final String DOWNLOAD_FORMAT_ZIP = "zip";
    public static final String DOWNLOAD_FORMAT_JSON = "json";
    public static final String DOWNLOAD_FORMAT_XML = "xml";
    public static final String DOWNLOAD_FORMAT_TEXT = "text";
    public static final String METAINFO_ZIP_CONTENT = "ZipContent";
    public static final String METAINFO_REQUEST_URL = "RequestURL";
    public static final String METAINFO_REQUEST_QUERY_STRING = "RequestQueryString";
    public static final String METAINFO_HTTP_METHOD = "HttpMethod";
    public static final String METAINFO_HTTP_STATUS_CODE = "HttpStatusCode";

    public static final String METAINFO_REMOTE_ADDR = "RemoteAddr";
    public static final String METAINFO_REMOTE_HOST = "RemoteHost";
    public static final String METAINFO_REMOTE_PORT = "RemotePort";
    public static final String METAINFO_REMOTE_USER = "RemoteUser";

    public static final String CONTENT_TYPE_JSON = "application/json";
    public static final String CONTENT_TYPE_XML = "text/xml";
    public static final String CONTENT_TYPE_TEXT = "text/plain";
    public static final String CONTENT_TYPE_EXCEL = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    public static final String CONTENT_TYPE_ZIP = "application/zip";
    public static final String CONTENT_TYPE_DOWNLOAD = "application/octet-stream";
}