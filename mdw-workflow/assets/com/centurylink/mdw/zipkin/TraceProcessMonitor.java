package com.centurylink.mdw.zipkin;

import brave.Span;
import brave.Tracer;
import brave.Tracing;
import brave.propagation.TraceContext;
import brave.propagation.TraceContextOrSamplingFlags;
import com.centurylink.mdw.annotations.Monitor;
import com.centurylink.mdw.constant.OwnerType;
import com.centurylink.mdw.model.workflow.ProcessRuntimeContext;
import com.centurylink.mdw.monitor.ProcessMonitor;

import java.util.Map;

/**
 * For async, non-service processes (otherwise see TraceServiceMonitor).
 * This monitor provides a mechanism for creating/propagating Zipkin trace/span ids.
 */
@Monitor(value="Zipkin Process Monitor", category=ProcessMonitor.class, defaultEnabled=true)
public class TraceProcessMonitor implements ProcessMonitor {

    @Override
    public Map<String,Object> onStart(ProcessRuntimeContext context) {
        if (!context.isInService()) {
            Tracing tracing = TraceHelper.getTracing("mdw-process");
            Tracer tracer = tracing.tracer();
            ProcessLaunchRequest processLaunchRequest = getProcessLaunchRequest(context);
            if (processLaunchRequest == null) {
                // TODO master process launch or launch request not found
                // master process launch
//                Span childSpan = null;
//                Object requestHeaders = context.getVariables().get("requestHeaders");
//                if (requestHeaders instanceof Map) {
//                    Map<?,?> headers = (Map<?,?>)requestHeaders;
//                    if (headers.containsKey("x-b3-traceid")) {
//                        TraceContext.Extractor<Map<?,?>> extractor =
//                                tracing.propagation().extractor((map, key) -> {
//                                    Object val = map.get(key);
//                                    return val == null ? null : val.toString();
//                                });
//                        TraceContextOrSamplingFlags extracted = extractor.extract(headers);
//                        Span span = extracted.context() != null
//                                ? tracer.joinSpan(extracted.context())
//                                : tracer.nextSpan(extracted);
//                        span.name(context.getProcess().oneLineName()).kind(Span.Kind.SERVER);
//                        span.start().flush();
//                        childSpan = tracer.newChild(span.context()).name(context.getProcess().oneLineName());
//                    }
//                }
//                if (childSpan == null) {
//                    // new root span
//                    Span span = tracer.nextSpan().name(context.getProcess().oneLineName());
//                    span.start().flush();
//                    childSpan = tracer.newChild(span.context()).name(context.getProcess().oneLineName());
//                }
//                childSpan.start();
            }
            else {
                // subprocess launch
                TraceContext.Extractor<ProcessLaunchRequest> extractor =
                        tracing.propagation().extractor(ProcessLaunchRequest::getHeader);
                TraceContextOrSamplingFlags extracted = extractor.extract(processLaunchRequest);
                Span span = extracted.context() != null
                        ? tracer.joinSpan(extracted.context())
                        : tracer.nextSpan(extracted);

                span.name(context.getProcess().oneLineName()).kind(Span.Kind.SERVER);
                span.start().flush();
                Span childSpan = tracer.newChild(span.context()).name(context.getProcess().oneLineName());
                childSpan.start();
                Tracer.SpanInScope spanInScope = tracer.withSpanInScope(childSpan);
                processLaunchRequest.spanInScope = spanInScope;
            }
        }
        return null;
    }

    @Override
    public Map<String,Object> onFinish(ProcessRuntimeContext context) {
        if (!context.isInService()) {
            ProcessLaunchRequest processLaunchRequest = getProcessLaunchRequest(context);
            if (processLaunchRequest != null) {
                if (processLaunchRequest.spanInScope != null) {
                    processLaunchRequest.spanInScope.close();
                }
                ProcessLaunchRequest.requestMap.remove(context.getProcessInstanceId());
            }
        }
        return null;
    }

    @Override
    public void onError(ProcessRuntimeContext context) {
        if (!context.isInService()) {
            ProcessLaunchRequest processLaunchRequest = getProcessLaunchRequest(context);
            if (processLaunchRequest != null) {
                if (processLaunchRequest.spanInScope != null) {
                    processLaunchRequest.spanInScope.close();
                }
                ProcessLaunchRequest.requestMap.remove(context.getProcessInstanceId());
            }
        }
    }

    /**
     * Returns non-null from async subprocess launch requests.
     */
    private ProcessLaunchRequest getProcessLaunchRequest(ProcessRuntimeContext context) {
        if (OwnerType.PROCESS_INSTANCE.equals(context.getProcessInstance().getOwner())) {
            return ProcessLaunchRequest.requestMap.get(context.getProcessInstance().getOwnerId());
        }
        return null;
    }

}
