package com.centurylink.mdw.zipkin;

import brave.Span;
import brave.Tracer;
import brave.Tracing;
import brave.internal.HexCodec;
import com.centurylink.mdw.annotations.RegisteredService;
import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.util.log.LogLineInjector;

@RegisteredService(LogLineInjector.class)
public class TracingLogLine implements LogLineInjector {

    private Boolean hasSpringSleuth;
    private boolean isHasSpringSleuth() {
        if (hasSpringSleuth == null) {
            try {
                Class.forName("org.springframework.cloud.sleuth.SpanName");
                hasSpringSleuth = true;
            } catch (ClassNotFoundException | NoClassDefFoundError ex) {
                hasSpringSleuth = false;
            }
        }
        return hasSpringSleuth;
    }

    public String prefix() {
        if (!isHasSpringSleuth()) {
            Tracer tracer = Tracing.currentTracer();
            if (tracer != null) {
                Span span = tracer.currentSpan();
                if (span != null) {
                    return "[" + ApplicationContext.getAppId() + "," + span.context().traceIdString() + "," +
                            spanIdHex(span.context().spanId()) + "]";
                }
            }
        }
        return null;
    }

    public static String spanIdHex(long spanId) {
        if (spanId == 0)
            return null;
        char[] spanRes = new char[16];
        HexCodec.writeHexLong(spanRes, 0, spanId);
        return new String(spanRes);
    }
}
