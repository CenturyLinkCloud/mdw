package com.centurylink.mdw.util;

import org.apache.commons.lang.StringUtils;

import java.util.Date;

/**
 * Use {@link org.apache.commons.lang.StringUtils}, {@link DateHelper},
 *
 */
@Deprecated
public class StringHelper {

    @Deprecated
    public static boolean isEmpty(String str) {
        return StringUtils.isBlank(str);
    }

    @Deprecated
    public static String dateToString(Date d) {
        return DateHelper.dateToString(d);
    }

    @Deprecated
    public static Date stringToDate(String s) {
        return DateHelper.stringToDate(s);
    }

    @Deprecated
    public static String serviceDateToString(Date d) {
        return DateHelper.serviceDateToString(d);
    }

    @Deprecated
    public static Date serviceStringToDate(String s) {
        return DateHelper.serviceStringToDate(s);
    }

    @Deprecated
    public static boolean isEqual(String s1, String s2) {
        return StringUtils.equals(s1, s2);
    }
}