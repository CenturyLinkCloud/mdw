package com.centurylink.mdw.zipkin;

import brave.Span;
import brave.SpanCustomizer;
import brave.Tracer;
import brave.Tracing;
import brave.http.HttpAdapter;
import brave.http.HttpClientHandler;
import brave.http.HttpClientParser;
import brave.http.HttpTracing;
import brave.propagation.TraceContext;
import com.centurylink.mdw.annotations.Monitor;
import com.centurylink.mdw.common.MdwException;
import com.centurylink.mdw.model.workflow.ActivityRuntimeContext;
import com.centurylink.mdw.monitor.AdapterMonitor;
import com.centurylink.mdw.util.HttpConnection;
import com.centurylink.mdw.util.HttpRequest;
import com.centurylink.mdw.util.HttpResponse;

import java.util.Map;

@Monitor(value="Zipkin Adapter Monitor", category=AdapterMonitor.class, defaultEnabled=true)
public class TraceAdapterMonitor implements AdapterMonitor {

    private HttpClientHandler handler;
    private TraceContext.Injector<HttpRequest> injector;
    private Span span;
    private Tracer.SpanInScope scope;

    /**
     * Only for HTTP.
     */
    @Override
    public Object onRequest(ActivityRuntimeContext context, Object content, Map<String,String> headers, Object connection) {
        if (connection instanceof HttpConnection) {
            HttpConnection httpConnection = (HttpConnection)connection;
            Tracing tracing = TraceHelper.getTracing("mdw-adapter");
            HttpTracing httpTracing = HttpTracing.create(tracing).toBuilder()
                    .clientParser(new HttpClientParser() {
                        public <Req> void request(HttpAdapter<Req, ?> adapter, Req req, SpanCustomizer customizer) {
                            // customize span name
                            customizer.name(context.getActivity().oneLineName());
                        }
                    })
                    .build();
            Tracer tracer = httpTracing.tracing().tracer();
            handler = HttpClientHandler.create(httpTracing, new ClientAdapter());
            injector = httpTracing.tracing().propagation().injector((httpRequest, key, value) -> headers.put(key, value));
            span = handler.handleSend(injector, new HttpRequest(httpConnection));
            scope = tracer.withSpanInScope(span);
        }
        return null;
    }

    @Override
    public Object onResponse(ActivityRuntimeContext context, Object content, Map<String,String> headers, Object connection) {
        if (scope != null)
            scope.close();
        if (span != null) {
            int statusCode = 0;
            if (connection instanceof HttpConnection)
                statusCode = ((HttpConnection)connection).getResponse().getCode();
            handler.handleReceive(new HttpResponse(statusCode), null, span);
        }
        return null;
    }

    @Override
    public String onError(ActivityRuntimeContext context, Throwable t) {
        if (scope != null)
            scope.close();
        if (span != null) {
            int statusCode = 500;
            if (t instanceof MdwException)
                statusCode = ((MdwException)t).getCode();
            handler.handleReceive(new HttpResponse(statusCode), t, span);
        }
        return null;
    }
}
