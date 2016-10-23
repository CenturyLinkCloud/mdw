/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.util.log;

import java.text.SimpleDateFormat;
import java.util.Date;

public abstract class AbstractStandardLoggerBase implements StandardLogger {

	public static final String DEFAULT_HOST = "localhost";
	public static final String DEFAULT_PORT = "7181";

	private static final String SENTRY_MARK = "[SENTRY-MARK] ";

	public String getDefaultHost() {
		return DEFAULT_HOST;
	}

	public String getDefaultPort() {
		return DEFAULT_PORT;
	}

	public String getSentryMark() {
		return SENTRY_MARK;
	}

	protected String generate_log_line(char type, String tag, String message) {
    	StringBuffer sb = new StringBuffer();
    	sb.append("[(");
    	sb.append(type);
    	sb.append(")");
    	SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd.HH:mm:ss.SSS");
    	sb.append(df.format(new Date()));
    	if (tag!=null) {
    		sb.append(" ");
    		sb.append(tag);
    	} else {
    		sb.append(" ~");
        	sb.append(Thread.currentThread().getId());
    	}
		sb.append("] ");
		sb.append(message);
		return sb.toString();
    }

}
