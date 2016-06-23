/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.utilities.logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.common.cache.CacheEnabled;
import com.centurylink.mdw.common.constant.PropertyNames;
import com.centurylink.mdw.common.utilities.property.PropertyManager;
import com.centurylink.mdw.soccom.SoccomClient;

public class SimpleLogger extends AbstractStandardLoggerBase implements CacheEnabled {

	private static final long serialVersionUID = 1L;
	private static final int WARN_LEVEL = 1;
	private static final int INFO_LEVEL = 2;
	private static final int DEBUG_LEVEL = 3;
	private static final int MDW_DEBUG_LEVEL = 4;
	private static final int TRACE_LEVEL = 5;

	private static SimpleLogger singleton = null;
	private static HashMap<String,SimpleLogger> loggers = null;

	private int loglevel = INFO_LEVEL;
	private String watcher = null;
	private PrintStream logfile = null;
	private boolean toConsole;

	private SimpleLogger() {
		// get some basic setting first and call refreshCache() later to load options based on property setting
		toConsole = true;
	}

	private SimpleLogger(String loggerName) {
		toConsole = false;
		String dir = PropertyManager.getProperty(PropertyNames.MDW_LOGGING_DIR);
		if (dir==null) dir = ".";
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		String loggerFileName = dir + "/" + loggerName + "_" + sdf.format(new Date()) + ".log";
		loggerFileName = loggerFileName.replaceAll("%", ApplicationContext.getServerHostPort());
		try {
			if (logfile!=null) logfile.close();
			logfile = new PrintStream(new File(loggerFileName));
		} catch (FileNotFoundException e) {
			System.out.println("+++MDW ERROR+++ Cannot open log file " + loggerFileName);
			logfile = null;
		}
	}

	public static SimpleLogger getSingleton() {
		if (singleton==null) singleton = new SimpleLogger();
		return singleton;
	}

	public static SimpleLogger getLogger(String loggerName) {
		if (loggers==null) loggers = new HashMap<String,SimpleLogger>();
		SimpleLogger logger = loggers.get(loggerName);
		if (logger==null) {
			logger = new SimpleLogger(loggerName);
			loggers.put(loggerName, logger);
		}
		return logger;
	}

	public void clearCache() {
	}

	// set loglevel, logfile and watcher
    public void refreshCache() {
		String v = PropertyManager.getProperty(PropertyNames.MDW_LOGGING_LEVEL);
		if (v==null) loglevel = INFO_LEVEL;
		else if (v.equalsIgnoreCase("DEBUG")||v.equals("3")) loglevel = DEBUG_LEVEL;
		else if (v.equalsIgnoreCase("INFO")||v.equals("2")) loglevel = INFO_LEVEL;
		else if (v.equalsIgnoreCase("MDW_DEBUG")||v.equals("4")) loglevel = MDW_DEBUG_LEVEL;
		else if (v.equalsIgnoreCase("TRACE")||v.equals("5")) loglevel = TRACE_LEVEL;
		else if (v.equalsIgnoreCase("WARN")||v.equals("1")) loglevel = WARN_LEVEL;
		else loglevel = INFO_LEVEL;
		watcher = PropertyManager.getProperty(PropertyNames.MDW_LOGGING_WATCHER);
		v = PropertyManager.getProperty(PropertyNames.MDW_LOGGING_FILE);
		if (v!=null && v.length()>0) {
			try {
				if (logfile!=null) logfile.close();
				logfile = new PrintStream(new File(v));  // TODO lose existing log file
			} catch (FileNotFoundException e) {
				System.out.println("+++MDW ERROR+++ Cannot open log file " + v);
				logfile = null;
			}
		}

	}

	private void logline(char type, String tag, String message) {
    	String line = generate_log_line(type, tag, message);
    	if (toConsole) System.out.println(line);
    	if (logfile!=null) {
    		logfile.println(line);
    	}
		if (watcher!=null) sendToWatcher(line);
    }

	private void logexception(char type, String tag, String message, Throwable throwable) {
		String line = generate_log_line(type, tag, message);
		if (toConsole) {
			System.out.println(line);
			throwable.printStackTrace(System.out);
		}
    	if (logfile!=null) {
    		logfile.print(line);
    		throwable.printStackTrace(logfile);
    	}
    	if (watcher!=null) sendToWatcher(line);
	}

	public void debug(String logtodisplay) {
    	if (!isDebugEnabled()) return;
    	logline('d', null, logtodisplay);
	}

	public void debugException(String msg, Throwable throwable) {
		if (!isDebugEnabled()) return;
    	logexception('d', null, msg, throwable);

	}

	public void info(String logtodisplay) {
    	if (!isInfoEnabled()) return;
    	logline('i', null, logtodisplay);
	}

	public void infoException(String logtodisplay, Throwable throwable) {
    	if (!isInfoEnabled()) return;
    	logexception('i', null, logtodisplay, throwable);
	}

	public boolean isDebugEnabled() {
		return loglevel>=DEBUG_LEVEL;
	}

	public boolean isInfoEnabled() {
		return loglevel>=INFO_LEVEL;
	}

	public boolean isTraceEnabled() {
	    return loglevel>=TRACE_LEVEL;
	}

	public void severe(String logtodisplay) {
    	logline('s', null, logtodisplay);
	}

	public void severeException(String message, Throwable throwable) {
    	logexception('s', null, message, throwable);
	}

	public void warn(String logtodisplay) {
    	logline('w', null, logtodisplay);
	}

	public void warnException(String logtodisplay, Throwable throwable) {
    	logexception('w', null, logtodisplay, throwable);
	}

    public void info(String tag, String message) {
    	if (!isInfoEnabled()) return;
    	logline('i', tag, message);
    }

    public void debug(String tag, String message) {
    	if (!isDebugEnabled()) return;
    	logline('d', tag, message);
    }

    public void warn(String tag, String message) {
    	logline('w', tag, message);
    }

    public void severe(String tag, String message) {
    	logline('s', tag, message);
    }

    public void exception(String tag, String message, Throwable e) {
    	logexception('e', tag, message, e);
    }

    public void trace(String tag, String message) {
        if (isTraceEnabled())
            logline('t', tag, message);
    }

    public void trace(String message) {
        if (isTraceEnabled())
            logline('t', null, message);
    }

    public void traceException(String msg, Throwable t) {
        if (isTraceEnabled())
          logexception('t', null, msg, t);
    }

	public boolean isMdwDebugEnabled() {
		return loglevel>=MDW_DEBUG_LEVEL;
	}

	public void mdwDebug(String message) {
		if (!isMdwDebugEnabled()) return;
    	logline('m', null, message);
	}

	private void sendToWatcher(String message) {
    	SoccomClient client = null;
		try {
			String[] spec = watcher.split(":");
			String host = spec.length>0?spec[0]:getDefaultHost();
			String port = spec.length>1?spec[1]:getDefaultPort();
			client = new SoccomClient(host, port, null);
    		client.putreq(message);
		} catch (Exception e) {
			watcher = null;
	    	System.out.println("Exception when sending log messages to watcher - turn it off");
	    	e.printStackTrace();
		} finally {
    		if (client!=null) client.close();
		}
	}

    public boolean isEnabledFor(LogLevel level) {
        switch (level) {
          case TRACE:
              return isTraceEnabled();
          case DEBUG:
              return isDebugEnabled();
          case INFO:
              return isInfoEnabled();
          case WARN:
              return isInfoEnabled();
          case ERROR:
              return true;
          default:
              return isMdwDebugEnabled();
        }
    }

    public void log(LogLevel level, String message) {
        switch (level) {
          case TRACE:
            trace(message);
            break;
          case DEBUG:
            debug(message);
            break;
          case INFO:
            info(message);
            break;
          case WARN:
            warn(message);
            break;
          case ERROR:
            severe(message);
            break;
      }
    }

    public PrintStream getPrintStream() {
        return System.out;
    }

}
