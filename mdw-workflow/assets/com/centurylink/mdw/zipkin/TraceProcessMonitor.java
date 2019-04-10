package com.centurylink.mdw.zipkin;

import brave.ScopedSpan;
import brave.Tracer;
import brave.Tracing;
import brave.context.slf4j.MDCScopeDecorator;
import brave.propagation.ThreadLocalCurrentTraceContext;
import com.centurylink.mdw.annotations.Monitor;
import com.centurylink.mdw.model.workflow.ProcessRuntimeContext;
import com.centurylink.mdw.monitor.ProcessMonitor;

import java.util.HashMap;
import java.util.Map;

@Monitor(value="Zipkin Process Monitor", category=ProcessMonitor.class, defaultEnabled=false)
public class TraceProcessMonitor implements ProcessMonitor {

    private ScopedSpan scopedSpan;

    @Override
    public Map<String,Object> onStart(ProcessRuntimeContext context) {
        if (!context.isInService()) {
            Tracing tracing = Tracing.current();
            if (tracing == null) {
                // TODO reporter based on prop/config
                tracing = Tracing.newBuilder()
                        .localServiceName("mdw-process")
                        .currentTraceContext(ThreadLocalCurrentTraceContext.newBuilder()
                                .addScopeDecorator(MDCScopeDecorator.create())
                                .build()
                        )
                        .build();
            }

            Tracer tracer = tracing.tracer();
            scopedSpan = tracer.startScopedSpan(context.getProcessInstanceId().toString());

            if (context.getProcess().getVariable("requestHeaders") != null) {
                String traceId = scopedSpan.context().traceIdString();
                String spanId = TracingLogLine.spanIdHex(scopedSpan.context().spanId());
                String parentSpanId = scopedSpan.context().parentIdString();
                Boolean sampled = scopedSpan.context().sampled();
                Boolean debug = scopedSpan.context().debug();
                Map<String,String> requestHeaders = (Map<String,String>)context.getVariables().get("requestHeaders");
                if (requestHeaders == null)
                    requestHeaders = new HashMap<>();

            }


        }
        return null;
    }

    @Override
    public Map<String,Object> onFinish(ProcessRuntimeContext context) {
        if (scopedSpan != null) {
            scopedSpan.finish();
        }
        return null;
    }
}
