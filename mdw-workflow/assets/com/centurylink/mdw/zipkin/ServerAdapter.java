package com.centurylink.mdw.zipkin;

import brave.http.HttpServerAdapter;
import com.centurylink.mdw.model.listener.Listener;

import java.util.Map;

public class ServerAdapter extends HttpServerAdapter<ServerAdapter.ServerRequest,ServerAdapter.ServerResponse> {

    @Override
    public String method(ServerRequest request) {
        return request.headers.get(Listener.METAINFO_HTTP_METHOD);
    }

    @Override
    public String url(ServerRequest request) {
        return request.headers.get(Listener.METAINFO_REQUEST_URL);
    }

    @Override
    public String requestHeader(ServerRequest request, String name) {
        return request.headers.get(name);
    }

    @Override
    public Integer statusCode(ServerResponse response) {
        return response.code;
    }

    public static class ServerRequest {
        Map<String,String> headers;
        public ServerRequest(Map<String,String> headers) {
            this.headers = headers;
        }
        public String getHeader(String name) {
            String value = headers.get(name);
            if (value == null) // tomcat converts to lower case
                value = headers.get(name.toLowerCase());
            return value;
        }
    }

    public static class ServerResponse {
        int code;
        public ServerResponse(int code) {
            this.code = code;
        }
    }
}
