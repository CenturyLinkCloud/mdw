/*
 * Copyright (C) 2017 CenturyLink, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.centurylink.mdw.util;

import org.apache.commons.lang.StringUtils;

import java.util.Date;

/**
 * Use {@link org.apache.commons.lang.StringUtils}, {@link DateHelper},
 * {@link com.centurylink.mdw.model.attribute.Attribute#parseList(String), etc}
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