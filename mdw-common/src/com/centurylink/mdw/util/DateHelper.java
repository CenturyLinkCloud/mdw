package com.centurylink.mdw.util;

import java.text.SimpleDateFormat;
import java.time.temporal.ChronoField;
import java.util.Date;

/**
 * Legacy date utilities.
 */
public class DateHelper {
    private static final String _dateFormat = "yyyy-MM-dd HH:mm:ss";
    private static final String _dateFormatMs = "yyyy-MM-dd HH:mm:ss.SSS";
    private static final String _serviceDateFormat = "MM-dd-yyyy HH:mm:ss";
    private static final String _filenameDateFormat = "yyyy-MM-dd'T'HH-mm-ss";


    public static String dateToString(Date d) {
        if (d == null)
            return null;
        if (d.toInstant().get(ChronoField.MICRO_OF_SECOND) > 0)
            return new SimpleDateFormat(_dateFormatMs).format(d);
        else
            return new SimpleDateFormat(_dateFormat).format(d);
    }

    public static Date stringToDate(String s) {
        if (s == null)
            return null;
        try {
            return new SimpleDateFormat(_dateFormat).parse(s);
        }
        catch (Exception e) {
            return null;
        }
    }

    public static String serviceDateToString(Date d) {
        return d == null ? null : new SimpleDateFormat(_serviceDateFormat).format(d);
    }

    public static Date serviceStringToDate(String s) {
        if (s == null)
            return null;

        try {
            return new SimpleDateFormat(_serviceDateFormat).parse(s);
        }
        catch (Exception e) {
            return null;
        }
    }

    public static String filenameDateToString(Date d) {
        return d == null ? null : new SimpleDateFormat(_filenameDateFormat).format(d);
    }
}
