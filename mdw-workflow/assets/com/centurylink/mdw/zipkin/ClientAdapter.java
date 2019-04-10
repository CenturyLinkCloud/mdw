package com.centurylink.mdw.zipkin;

import brave.http.HttpClientAdapter;
import com.centurylink.mdw.util.HttpRequest;
import com.centurylink.mdw.util.HttpResponse;

public class ClientAdapter extends HttpClientAdapter<HttpRequest,HttpResponse> {

    @Override
    public String url(HttpRequest request) {
        return request.getConnection().getUrl().toString();
    }

    @Override
    public String method(HttpRequest request) {
        return request.getConnection().getMethod();
    }

    @Override
    public String requestHeader(HttpRequest request, String name) {
        return request.getConnection().getHeaders().get(name);
    }

    @Override
    public Integer statusCode(HttpResponse response) {
        return response.getCode();
    }
}
