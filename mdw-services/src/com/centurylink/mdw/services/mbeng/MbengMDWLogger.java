/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services.mbeng;

import java.io.PrintStream;

import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.qwest.mbeng.Logger;

/**
 * Convert MDW standarad logger for the use of Magic Box rule engine.
 * 
 *
 */
public class MbengMDWLogger implements Logger {
	StandardLogger _logger;
	public MbengMDWLogger(StandardLogger logger) {
		_logger = logger;
	}

	public PrintStream getPrintStream() {
		return null;
	}

	public void logline(String message) {
		_logger.info(message);
	}
}