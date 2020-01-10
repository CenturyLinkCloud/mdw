package com.centurylink.mdw.servicenow;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Date format used by ServiceNow.
 */
public class GlideDateTime {
    public static DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private LocalDateTime localDateTime;
    public LocalDateTime getLocalDateTime() { return localDateTime; }

    GlideDateTime(LocalDateTime localDateTime) {
        this.localDateTime = localDateTime;
    }

    GlideDateTime(long epochMillis) {
        this.localDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault());
    }

    GlideDateTime(String formattedDateTime) {
        this.localDateTime = LocalDateTime.parse(formattedDateTime, FORMAT);
    }

    @Override
    public String toString() {
        return FORMAT.format(this.localDateTime);
    }
}
