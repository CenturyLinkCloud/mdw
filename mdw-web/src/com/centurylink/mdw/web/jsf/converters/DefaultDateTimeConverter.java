/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.jsf.converters;

import java.util.TimeZone;

/**
 * Override the JSF default date-time converter to use local time instead of GMT
 * (http://wiki.apache.org/myfaces/FAQ#Date)
 */
public class DefaultDateTimeConverter extends javax.faces.convert.DateTimeConverter
{

  public DefaultDateTimeConverter()
  {
    // set the timezone to the default instead of GMT
    setTimeZone(TimeZone.getDefault());
    // could also override the default format as follows
    // setPattern("M/d/yy");
  }
}