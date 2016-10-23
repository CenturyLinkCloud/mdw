/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.util.log.log4j;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class LoggingOutputStream extends OutputStream {

    private static final int DEFAULT_BUFFER_LENGTH = 2048;
    private boolean hasBeenClosed = false;
    private byte[] buf;
    private int count;
    private int curBufLength;
    private Logger log;
    private Level level;

    public LoggingOutputStream(final Logger log) {
        this(log, null);
    }

    public LoggingOutputStream(final Logger log, final Level level) {
        this.log = log;
        if (this.log == null)
            this.log = Logger.getRootLogger();
        this.level = level;
        if (this.level == null)
            this.level = Level.INFO;
        curBufLength = DEFAULT_BUFFER_LENGTH;
        buf = new byte[curBufLength];
        count = 0;
    }

    public void write(final int b) throws IOException {
        if (hasBeenClosed)
            throw new IOException("The stream has been closed.");
        if (b == 0)
            return;
        if (count == curBufLength) {
            final int newBufLength = curBufLength + DEFAULT_BUFFER_LENGTH;
            final byte[] newBuf = new byte[newBufLength];
            System.arraycopy(buf, 0, newBuf, 0, curBufLength);
            buf = newBuf;
            curBufLength = newBufLength;
        }

        buf[count] = (byte) b;
        count++;
    }

    public void flush() {
        if (count == 0)
            return;
        final byte[] bytes = new byte[count];
        System.arraycopy(buf, 0, bytes, 0, count);
        String str = new String(bytes);
        log.log(level, str);
        count = 0;
    }

    public void close() {
        flush();
        hasBeenClosed = true;
    }
}
