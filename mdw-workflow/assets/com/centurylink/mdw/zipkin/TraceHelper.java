package com.centurylink.mdw.zipkin;

import brave.Tracing;
import brave.context.slf4j.MDCScopeDecorator;
import brave.propagation.CurrentTraceContext;
import brave.propagation.ThreadLocalCurrentTraceContext;

public class TraceHelper {

    public static Tracing getTracing(String serviceName) {
        return getTracing(serviceName, getDefaultCurrentTraceContext());
    }

    /**
     * Get or build tracing.
     */
    public static Tracing getTracing(String serviceName, CurrentTraceContext context) {
        Tracing tracing = Tracing.current();
        if (tracing == null) {
            // TODO reporter based on prop/config
            tracing = getBuilder(serviceName, context).build();
        }
        return tracing;
    }

    public static Tracing.Builder getBuilder(String serviceName, CurrentTraceContext context) {
        return Tracing.newBuilder()
                .localServiceName(serviceName)
                .currentTraceContext(context);
    }

    public static CurrentTraceContext getDefaultCurrentTraceContext() {
        return ThreadLocalCurrentTraceContext.newBuilder()
                .addScopeDecorator(MDCScopeDecorator.create())
                .build();
    }
}
