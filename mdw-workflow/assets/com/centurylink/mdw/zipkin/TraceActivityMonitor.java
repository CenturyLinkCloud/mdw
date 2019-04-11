package com.centurylink.mdw.zipkin;

import brave.Span;
import brave.Tracer;
import brave.Tracing;
import brave.propagation.TraceContext;
import com.centurylink.mdw.annotations.Monitor;
import com.centurylink.mdw.model.workflow.ActivityRuntimeContext;
import com.centurylink.mdw.monitor.ActivityMonitor;
import com.centurylink.mdw.workflow.activity.process.InvokeHeterogeneousProcessActivity;

import java.util.Map;

import static brave.Span.Kind.CLIENT;

@Monitor(value="Zipkin Subflow Activity", category=ActivityMonitor.class,
        enablementCategory="com.centurylink.mdw.activity.types.InvokeProcessActivity")
public class TraceActivityMonitor implements ActivityMonitor {

    @Override
    public Map<String,Object> onStart(ActivityRuntimeContext context) {
        Tracing tracing = TraceHelper.getTracing("mdw-subprocess");

        if (!isSynchronousLaunch(context)) {
            Tracer tracer = tracing.tracer();
            Span oneWaySend = tracer.nextSpan().name(context.getProcess().oneLineName()).kind(CLIENT);
            TraceContext.Injector<ProcessLaunchRequest> injector =
                    tracing.propagation().injector(ProcessLaunchRequest::setHeader);
            injector.inject(oneWaySend.context(), ProcessLaunchRequest.create(context.getProcessInstanceId()));
            oneWaySend.start().flush();
        }
        return null;
    }

    private boolean isSynchronousLaunch(ActivityRuntimeContext context) {
        String implementor = context.getActivity().getImplementor();
        boolean synchronousLaunch = !implementor.equals(InvokeHeterogeneousProcessActivity.class.getName());
        String syncAttr = context.getAttribute("synchronous");
        if (syncAttr != null) {
            synchronousLaunch = Boolean.parseBoolean(syncAttr);
        }
        return synchronousLaunch;
    }
}
