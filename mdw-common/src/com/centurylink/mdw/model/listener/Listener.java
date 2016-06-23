/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model.listener;

/**
 * The interface needs to be implemented if it is desired
 * that the class needs to listen to a specific message
 * at start up.
 */

public interface Listener {

    public static final String METAINFO_PROTOCOL = "Protocol";
    public static final String METAINFO_SERVICE_CLASS = "ServiceClass";
    public static final String METAINFO_REQUEST_ID = "RequestID";
    public static final String METAINFO_HEX_REQUEST_ID = "HexadecimalRequestID";
    public static final String METAINFO_START_ACTIVITY_ID = "StartActivityID";
    public static final String METAINFO_PROCESS_NAME = "ProcessName";
    public static final String METAINFO_EVENT_NAME = "EventName";
    public static final String METAINFO_EVENT_MESSAGE = "EventMessage";
    public static final String METAINFO_EVENT_ID = "EventID";
    public static final String METAINFO_DOCUMENT_ID = "DocumentID";
//    public static final String METAINFO_EVENT_INSTANCE_ID = METAINFO_DOCUMENT_ID;
    public static final String METAINFO_CORRELATION_ID = "CorrelationID";
    public static final String METAINFO_HEX_CORRELATION_ID = "HexadecimalCorrelationID";
    public static final String METAINFO_MASTER_REQUEST_ID = "MasterRequestID";
    public static final String METAINFO_REQUEST_CATEGORY = "RequestCategory";
    public static final String METAINFO_PACKAGE_NAME = "PackageName";
    public static final String METAINFO_NO_PERSISTENCE = "NoPersistence";
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
    public static final String METAINFO_PROTOCOL_INTERNAL = "Internal";	// called directly

    public static final String METAINFO_REQUEST_PATH = "RequestPath";
    public static final String METAINFO_RESOURCE_SUBPATH = "ResourceSubPath"; // after any '/' in RequestPath

    public static final String REQUEST_CATEGORY_CREATE = "Create";
    public static final String REQUEST_CATEGORY_READ = "Read";
    public static final String REQUEST_CATEGORY_UPDATE = "Update";
    public static final String REQUEST_CATEGORY_DELETE = "Delete";

    public static final String AUTHORIZATION_HEADER_NAME = "Authorization";
    // Will be populated when authentication happens
    public static final String AUTHENTICATED_USER_HEADER = "AuthenticatedUser";
    // Will be populated when authentication fails
    public static final String AUTHENTICATION_FAILED = "AuthenticationFailed";
    public static final String AUTHORIZATION_WORKGROUP = "AuthorizationWorkgroup";

    public static final String METAINFO_FORMAT = "Format"; // TODO: just use content-type
    public static final String METAINFO_DOWNLOAD_FORMAT = "DownloadFormat";
    public static final String METAINFO_CONTENT_TYPE = "ContentType";
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
    public static final String CONTENT_TYPE_DOWNLOAD = "application/octet-stream";

}