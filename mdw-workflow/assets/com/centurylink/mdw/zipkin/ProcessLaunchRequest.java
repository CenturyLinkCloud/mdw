package com.centurylink.mdw.zipkin;

import brave.ScopedSpan;
import brave.Span;
import brave.Tracer;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Brave client request for launching an MDW workflow process.
 */
public class ProcessLaunchRequest {

    // TODO requestMap needs to cross clustered instances?
    static Map<Long,ProcessLaunchRequest> requestMap = new ConcurrentHashMap<>();

    static ProcessLaunchRequest create(Long parentProcessInstanceId) {
        ProcessLaunchRequest request = new ProcessLaunchRequest();
        requestMap.put(parentProcessInstanceId, request);
        return request;
    }

    private Map<String,String> headers;

    public String getHeader(String name) {
        return headers == null ? null : headers.get(name);
    }

    public void setHeader(String name, String value) {
        if (headers == null)
            headers = new HashMap<>();
        headers.put(name, value);
    }

    Tracer.SpanInScope spanInScope;
}
