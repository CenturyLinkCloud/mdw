package com.centurylink.mdw.model.workflow;

import java.util.concurrent.TimeUnit;

public enum CompletionTimeUnit {

    Milliseconds(TimeUnit.MILLISECONDS),
    Seconds(TimeUnit.SECONDS),
    Minutes(TimeUnit.MINUTES),
    Hours(TimeUnit.HOURS),
    Days(TimeUnit.DAYS);

    public final TimeUnit timeUnit;

    CompletionTimeUnit(TimeUnit timeUnit) {
        this.timeUnit = timeUnit;
    }

    public long convert(long value, CompletionTimeUnit sourceUnit) {
        return timeUnit.convert(value, sourceUnit.timeUnit);
    }
}
