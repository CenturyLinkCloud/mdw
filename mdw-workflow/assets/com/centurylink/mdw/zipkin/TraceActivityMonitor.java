package com.centurylink.mdw.zipkin;

import brave.Span;
import brave.Tracer;
import brave.Tracing;
import brave.propagation.TraceContext;
import brave.propagation.TraceContextOrSamplingFlags;
import com.centurylink.mdw.activity.types.FinishActivity;
import com.centurylink.mdw.activity.types.StartActivity;
import com.centurylink.mdw.annotations.Monitor;
import com.centurylink.mdw.model.workflow.ActivityImplementor;
import com.centurylink.mdw.model.workflow.ActivityRuntimeContext;
import com.centurylink.mdw.model.workflow.RuntimeContext;
import com.centurylink.mdw.monitor.ActivityMonitor;
import com.centurylink.mdw.monitor.MonitorException;
import com.centurylink.mdw.service.data.activity.ImplementorCache;

import java.util.Map;

/**
 * This monitor takes care of tracing for async/non-service flows.
 * Service flows tracing is propogated via standard ThreadLocal spans (see TraceServiceMonitor).
 * Note: not supported for processes with delay transitions.
 */
@Monitor(value="Zipkin Activity", category=ActivityMonitor.class, defaultEnabled=true)
public class TraceActivityMonitor implements ActivityMonitor {

    /**
     * Resumes or forks
     */
    private void startSpan(ActivityRuntimeContext context) {
        Tracing tracing = TraceHelper.getTracing("mdw-activity");
        Tracer tracer = tracing.tracer();
        Span span = tracer.currentSpan();
        if (span == null) {
            // async brave server if b3 requestHeaders populated (subspan)
            Object requestHeaders = context.getValue("requestHeaders");
            if (requestHeaders instanceof Map) {
                Map<?, ?> headers = (Map<?, ?>) requestHeaders;
                if (headers.containsKey("x-b3-traceid")) {
                    TraceContext.Extractor<Map<?, ?>> extractor =
                            tracing.propagation().extractor((map, key) -> {
                                Object val = map.get(key.toLowerCase());
                                return val == null ? null : val.toString();
                            });
                    TraceContextOrSamplingFlags extracted = extractor.extract(headers);
                    span = extracted.context() != null
                            ? tracer.joinSpan(extracted.context())
                            : tracer.nextSpan(extracted);
                    span.name("m" + context.getMasterRequestId()).kind(Span.Kind.SERVER);
                    span = tracer.newChild(span.context()).name("p" + context.getProcessInstanceId());
                }
            }
        }
        if (span == null) {
            // create a new root span for async start
            span = tracer.nextSpan().name("a" + context.getActivityInstanceId());
        }
        span.start().flush(); // async
        Tracer.SpanInScope spanInScope = tracer.withSpanInScope(span);
        context.getRuntimeAttributes().put(Tracer.SpanInScope.class, spanInScope);
    }

    @Override
    public Map<String,Object> onStart(ActivityRuntimeContext context) {
        if (context.getProcess().hasDelayTransition()) {
            throw new MonitorException("Process " + context.getProcess() + " has delay transition(s), which are " +
                    "unsupported for Zipkin trace monitoring. Explicitly disable monitoring to avoid this message.");
        }
        if (isProcessStart(context)) {
            startSpan(context);
        }
        return null;
    }

    @Override
    public Map<String,Object> onFinish(ActivityRuntimeContext context) {
        if (isProcessFinish(context)) {
            closeSpanInScope(context);
        }
        else if (isSuspendable(context)) {
            // resume tracing
            startSpan(context);
        }
        return null;
    }

    @Override
    public void onError(ActivityRuntimeContext context) {
        closeSpanInScope(context);
    }

    @Override
    public void onSuspend(ActivityRuntimeContext context) {
        closeSpanInScope(context);
    }

    private void closeSpanInScope(ActivityRuntimeContext context) {
        Tracer.SpanInScope spanInScope = (Tracer.SpanInScope)
                context.getRuntimeAttributes().remove(Tracer.SpanInScope.class);
        if (spanInScope != null) {
            spanInScope.close();
        }
    }

    @Override
    public boolean isEnabled(RuntimeContext context) {
        boolean superEnabled = ActivityMonitor.super.isEnabled(context);
        if (superEnabled) {
            ActivityRuntimeContext activityRuntimeContext = (ActivityRuntimeContext) context;
            return !activityRuntimeContext.isInService() && activityRuntimeContext.getPerformanceLevel() >= 3
                    && (isProcessStart(activityRuntimeContext) || isProcessFinish(activityRuntimeContext)
                      || isSuspendable(activityRuntimeContext));
        }
        else {
            return false;
        }
    }

    private boolean isProcessStart(ActivityRuntimeContext context) {
        return StartActivity.class.getName().equals(getImplCategory(context));
    }
    private boolean isProcessFinish(ActivityRuntimeContext context) {
        return FinishActivity.class.getName().equals(getImplCategory(context));
    }
    private boolean isSuspendable(ActivityRuntimeContext context) {
        return context.isSuspendable();
    }

    private String getImplCategory(ActivityRuntimeContext context) {
        ActivityImplementor implementor = ImplementorCache.get(context.getActivity().getImplementor());
        return implementor == null ? null : implementor.getCategory();
    }
}
