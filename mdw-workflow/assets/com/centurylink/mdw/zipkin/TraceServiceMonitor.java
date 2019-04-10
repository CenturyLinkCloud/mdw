package com.centurylink.mdw.zipkin;

import brave.Span;
import brave.Tracer;
import brave.Tracing;
import brave.context.slf4j.MDCScopeDecorator;
import brave.http.HttpServerHandler;
import brave.http.HttpTracing;
import brave.propagation.ThreadLocalCurrentTraceContext;
import brave.propagation.TraceContext;
import com.centurylink.mdw.annotations.Monitor;
import com.centurylink.mdw.common.MdwException;
import com.centurylink.mdw.model.Response;
import com.centurylink.mdw.model.listener.Listener;
import com.centurylink.mdw.monitor.ServiceMonitor;

import java.util.Map;

@Monitor(value="Zipkin Service Monitor", category=ServiceMonitor.class, defaultEnabled=true)
public class TraceServiceMonitor implements ServiceMonitor {

    private HttpServerHandler handler;
    private TraceContext.Extractor<ServerAdapter.ServerRequest> extractor;
    private Span span;
    private Tracer.SpanInScope scope;

    /**
     * Only for HTTP services.
     */
    @Override
    public Object onRequest(Object request, Map<String,String> headers) {
        if (headers.containsKey(Listener.METAINFO_HTTP_METHOD)) {
            Tracing tracing = Tracing.current();
            if (tracing == null) {
                // TODO reporter based on prop/config
                tracing = Tracing.newBuilder()
                        .localServiceName(headers.get(Listener.METAINFO_REQUEST_PATH))
                        .currentTraceContext(ThreadLocalCurrentTraceContext.newBuilder()
                                .addScopeDecorator(MDCScopeDecorator.create())
                                .build()
                        )
                        .build();
            }
            HttpTracing httpTracing = HttpTracing.create(tracing);
            handler = HttpServerHandler.create(httpTracing, new ServerAdapter());
            extractor = httpTracing.tracing().propagation().extractor(ServerAdapter.ServerRequest::getHeader);
            span = handler.handleReceive(extractor, new ServerAdapter.ServerRequest(headers));
            scope = httpTracing.tracing().tracer().withSpanInScope(span);
        }
        return null;
    }

    @Override
    public Object onResponse(Object response, Map<String,String> headers) {
        if (scope != null)
            scope.close();
        if (span != null) {
            int code = 0;
            if (response instanceof Response) {
                Integer responseStatus = ((Response)response).getStatusCode();
                if (responseStatus != null)
                    code = responseStatus;
            }
            ServerAdapter.ServerResponse serverResponse = new ServerAdapter.ServerResponse(code);
            handler.handleSend(serverResponse, null, span);
        }
        return null;
    }

    @Override
    public Object onError(Throwable t, Map<String,String> headers) {
        if (scope != null)
            scope.close();
        if (span != null) {
            int code = t instanceof MdwException ? ((MdwException)t).getCode() : 0;
            ServerAdapter.ServerResponse serverResponse = new ServerAdapter.ServerResponse(code);
            handler.handleSend(serverResponse, t, span);
        }
        return null;
    }
}
