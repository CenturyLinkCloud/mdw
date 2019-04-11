package com.centurylink.mdw.zipkin;

import brave.Tracing;
import brave.context.slf4j.MDCScopeDecorator;
import brave.propagation.ThreadLocalCurrentTraceContext;

public class TraceHelper {

    /**
     * Get or build tracing.
     */
    public static Tracing getTracing(String serviceName) {
        Tracing tracing = Tracing.current();
        if (tracing == null) {
            // TODO reporter based on prop/config
            tracing = Tracing.newBuilder()
                    .localServiceName(serviceName)
                    .currentTraceContext(ThreadLocalCurrentTraceContext.newBuilder()
                            .addScopeDecorator(MDCScopeDecorator.create())
                            .build()
                    )
                    .build();
        }
        return tracing;
    }
}
