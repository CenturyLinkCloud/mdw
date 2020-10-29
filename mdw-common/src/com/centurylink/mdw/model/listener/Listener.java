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
    String METAINFO_PROCESS_NAME = "ProcessName";
    String METAINFO_EVENT_NAME = "EventName";
    String METAINFO_EVENT_ID = "EventID";
    String METAINFO_NO_PERSISTENCE = "NoPersistence";
    String METAINFO_NO_META_PERSISTENCE = "NoMetaPersistence";
    String METAINFO_REQUEST_PAYLOAD = "RequestPayload"; // for altering/extracting payload

    // Custom Error Response
    String METAINFO_ERROR_RESPONSE = "ErrorResponse";
    String METAINFO_ERROR_RESPONSE_VALUE = Boolean.TRUE.toString();

    String METAINFO_PROTOCOL_JMS = "Jms";
    String METAINFO_PROTOCOL_REST = "REST";
    String METAINFO_PROTOCOL_SOAP = "SOAP";
    String METAINFO_PROTOCOL_RMI = "Rmi";
    String METAINFO_PROTOCOL_KAFKA = "Kafka";

    String METAINFO_REQUEST_PATH = "RequestPath";

    String AUTHORIZATION_HEADER_NAME = "Authorization";
    String X_HUB_SIGNATURE = "x-hub-signature";

    // Will be populated when authentication happens
    String AUTHENTICATED_USER_HEADER = "AuthenticatedUser";
    // Will be populated when authentication fails
    String AUTHENTICATION_FAILED = "AuthenticationFailed";

    String AUTHENTICATED_JWT = "AuthenticatedJWT";

    String METAINFO_DOWNLOAD_FORMAT = "DownloadFormat";
    String DOWNLOAD_FORMAT_EXCEL = "xlsx";
    String DOWNLOAD_FORMAT_ZIP = "zip";
    String DOWNLOAD_FORMAT_JSON = "json";
    String DOWNLOAD_FORMAT_XML = "xml";
    String DOWNLOAD_FORMAT_TEXT = "text";
    String DOWNLOAD_FORMAT_FILE = "file";
    String METAINFO_DOWNLOAD_FILE = "download-file";
    String METAINFO_REQUEST_URL = "RequestURL";
    String METAINFO_REQUEST_QUERY_STRING = "RequestQueryString";
    String METAINFO_HTTP_METHOD = "HttpMethod";
    String METAINFO_HTTP_STATUS_CODE = "HttpStatusCode";

    String METAINFO_TOPIC = "Topic"; // Topic based routing like Kafka, TibcoBus, etc

    String METAINFO_REMOTE_ADDR = "RemoteAddr";
    String METAINFO_REMOTE_HOST = "RemoteHost";
    String METAINFO_REMOTE_PORT = "RemotePort";

    String CONTENT_TYPE_JSON = "application/json";
    String CONTENT_TYPE_XML = "text/xml";
    String CONTENT_TYPE_TEXT = "text/plain";
    String CONTENT_TYPE_DOWNLOAD = "application/octet-stream";

    String METAINFO_CLOUD_ROUTING = "cloud-routing";
    String METAINFO_MDW_APP_ID = "mdw-app-id";
    String METAINFO_MDW_APP_TOKEN = "mdw-app-token";

    static boolean isHealthCheck(Map<String,String> headers) {
        return "AppSummary".equals(headers.get(METAINFO_REQUEST_PATH));
    }
}