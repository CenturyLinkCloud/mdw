/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.soccom;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;

public class SoccomServer extends ServerSocket {

    private boolean shutdown;
    private Thread mainThread;
    private SimpleDateFormat df;
    private PrintStream log;
    private int threadCount = 0;
    private Hashtable<String,ServerThread> threads;
    protected int maxThreads = 0;

    // when max_threads is 0, there is no thread control
    // when max_threads is 1, use single thread model

    public SoccomServer(String port, PrintStream log) throws IOException {
        super(Integer.parseInt(port));
        this.log = log;
        df = new SimpleDateFormat("HH:mm:ss");
        logline(null, "Server started on port " + port);
    }

    public SoccomServer(String port, String logname) throws IOException {
        super(Integer.parseInt(port));
        log = new PrintStream(new FileOutputStream(logname, true), true);
        df = new SimpleDateFormat("HH:mm:ss");
        logline(null, "Server started on port " + port);
    }

    public synchronized void shutdown() {
        shutdown = true;
        if (mainThread != null)
            mainThread.interrupt();
        try {
            this.close();
        } catch (IOException e) {
        }
    }

    synchronized void logline(String tid, String msg) {
        if (log != null) {
            if (tid == null)
                log.println(df.format(new Date()) + ": " + msg);
            else
                log.println(df.format(new Date()) + " " + tid + ": " + msg);
        }
    }

    private synchronized int changeThreadCount(boolean add) {
        if (add)
            threadCount++;
        else
            threadCount--;
        return threadCount;
    }

    /**
     * @param threadId
     * @param thread
     * @return
     */
    int addThread(String threadId, ServerThread thread) {
        threads.put(threadId, thread);
        return changeThreadCount(true);
    }

    void removeThread(String threadId, ServerThread thread) {
        threads.remove(threadId);
        changeThreadCount(false);
    }

    private void shutdownThreads() {
        Enumeration<ServerThread> en = threads.elements();
        ServerThread thread;
        while (en.hasMoreElements()) {
            thread = en.nextElement();
            logline(null, "Interrupt thread " + thread.threadId);
            thread.interrupt();
        }
    }

    public void start(boolean useNewThread) {
        if (useNewThread) {
            mainThread = new Thread() {
                public void run() {
                    startSub();
                }
            };
            mainThread.start();
        } else {
            mainThread = Thread.currentThread();
            startSub();
        }
    }

    private void startSub() {
        ServerThread thread;
        shutdown = false;
        threads = new Hashtable<String, ServerThread>();
        while (!shutdown) {
            try {
                setSoTimeout(0);
                Socket socket = accept();
                if (shutdown) {
                    // accept returns a default socket 0.0.0.0 instead of
                    // raising an InterruptedIOException, when shutdown()
                    // is called. Seems to be a bug in Java 1.3.
                    logline(null, "Server shuts down here");
                }
                else if (connectProc(socket)) {
                    socket.setSoTimeout(120 * 1000);
                    if (maxThreads == 1) {
                        try {
                            processMessage(socket.getInputStream(), socket
                                    .getOutputStream(), "main", false);
                            socket.close();
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                            logline("main", "Exception: " + e);
                        }
                    }
                    else {
                        if (maxThreads > 1 && threadCount >= maxThreads) {
                            logline(null, "MAXIMUM NUMBER OF THREADS REACHED");
                            thread = new ServerThread(socket, this, true);
                        }
                        else {
                            thread = new ServerThread(socket, this, false);
                        }
                        thread.start();
                    }
                }
                else {
                    logline(null, "Reject connection request from "
                            + socket.getInetAddress());
                    socket.close();
                }
            }
            catch (InterruptedIOException e) {
                if (shutdown)
                    logline(null, "Server shuts down");
                else
                    logline(null, "Exception: " + e);
            }
            catch (IOException e) {
                if (shutdown) {
                    logline(null, "Server shuts down");
                }
                else {
                    e.printStackTrace();
                    logline(null, "Exception: " + e);
                }
            }
        }
        // call user shutdown processor
        shutdownProc();
        // need to kill all active thread
        shutdownThreads();
    }

    protected boolean connectProc(Socket socket) {
        return true;
    }

    protected void disconnectProc(Socket socket) {
    }

    protected void shutdownProc() {
    }

    protected void putResp(String thread_id, OutputStream out, String msgid,
            String msg) throws IOException {
        byte msgbytes[] = SoccomMessage.makeMessage(msg, msgid);
        logline(thread_id, "SEND: " + new String(msgbytes));
        out.write(msgbytes);
    }

    protected void putRespVheader(String thread_id, OutputStream out,
            String msgid, String endIndicator) throws IOException,
            SoccomException {
        if (endIndicator.length() != 4)
            throw new SoccomException(SoccomException.ENDM_LENGTH);
        byte msgbytes[] = SoccomMessage.makeMessageSpecial("ENDM"
                + endIndicator, msgid);
        logline(thread_id, "SEND: " + new String(msgbytes));
        out.write(msgbytes);
    }

    protected void putRespVline(String thread_id, OutputStream out, String msg)
            throws IOException, SoccomException {
        int length = msg.length();
        if (msg.charAt(length - 1) == '\n') {
            logline(thread_id, "SEND: " + msg.substring(0, length - 1));
        } else {
            logline(thread_id, "SEND: " + msg);
            msg += "\n";
        }
        byte msgbytes[] = msg.getBytes();
        out.write(msgbytes);
    }

    protected void putRespVfooter(String thread_id, OutputStream out,
            String endIndicator) throws IOException, SoccomException {
        if (endIndicator.length() != 4)
            throw new SoccomException(SoccomException.ENDM_LENGTH);
        String msg = endIndicator + "\n";
        byte msgbytes[] = msg.getBytes();
        logline(thread_id, "SEND: " + endIndicator);
        out.write(msgbytes);
    }

    protected void requestProc(String threadId, String msgId, byte[] msg, int msgSize,
            OutputStream out) throws IOException, SoccomException {
        String msgstr = new String(msg, 0, msgSize);
        if (msgstr.length() >= 5 && msgstr.substring(0, 5).equals("SLEEP")) {
            int seconds = Integer.parseInt(msgstr.substring(6));
            try {
                if (seconds >= 0) {
                    Thread.sleep(seconds * 1000);
                }
                else {
                    boolean hangup = false;
                    while (!hangup) {
                        Thread.sleep(-seconds * 1000);
                        hangup = clientHangup();
                        if (hangup)
                            logline(threadId, "client hang-up");
                    }
                }
            }
            catch (InterruptedException e) {
            }
            putResp(threadId, out, msgId, msgstr);
        }
        else if (msgstr.length() >= 8 && msgstr.substring(0, 8).equals("BUSYLOOP")) {
            int count = Integer.parseInt(msgstr.substring(9));
            for (int i = 0; i < count; i++) {
                System.out.println(" loop " + i + " of " + count);
                if (Thread.interrupted()) {
                    logline(threadId, "Thread interrupted");
                    break;
                }
            }
            putResp(threadId, out, msgId, msgstr);
        }
        else if (msgstr.length() >= 12 && msgstr.substring(0, 12).equals("_TEST_COMBIN")) {
            /*
             * test combination of sending/receiving line-by-line or as a whole.
             * Spec has 4 characters as follows: 1. for client send: W - as a
             * whole, L - line-by-line 2. for server recv: W - as a whole, L -
             * not implemented 3. for server send: W - as a whole, L -
             * line-by-line 4. for client recv: W - as a whole, L - line-by-line
             */
            msgstr = "_RESP_" + msgstr.substring(6);
            String testspec = msgstr.substring(13, 17);
            if (testspec.charAt(2) == 'L') {
                StringTokenizer st = new StringTokenizer(msgstr, "\n");
                putRespVheader(threadId, out, msgId, "****");
                while (st.hasMoreTokens()) {
                    putRespVline(threadId, out, st.nextToken());
                }
                putRespVfooter(threadId, out, "****");
            }
            else {
                putResp(threadId, out, msgId, msgstr);
            }
        }
        else if (msgstr.length() >= 6 && msgstr.substring(0, 6).equals("_TEST_")) {
            msgstr = "_RESP_" + msgstr.substring(6);
            putResp(threadId, out, msgId, msgstr);
        }
        else {
            putResp(threadId, out, msgId, msgstr);
        }
    }

    private int readLine(InputStream in, byte buffer[], int offset, int maxbytes)
            throws IOException {
        int n = 0;
        boolean line_read = false;
        while (!line_read && n < maxbytes) {
            in.read(buffer, offset + n, 1);
            if (buffer[offset + n] == '\n')
                line_read = true;
            n++;
        }
        return n;
    }

    boolean processMessage(InputStream in, OutputStream out, String threadId,
            boolean excessThread) throws SoccomException, Exception {
        byte[] header = new byte[SoccomMessage.HEADER_SIZE];
        int n;
        n = in.read(header, 0, SoccomMessage.HEADER_SIZE);
        if (n != SoccomMessage.HEADER_SIZE) {
            if (n == -1)
                return true;
            else
                throw new Exception("Header imcomplete: " + n);
        }
        String sizestr = new String(header, SoccomMessage.MSGSIZE_OFFSET, 8);
        if (sizestr.equals("SHUTDOWN")) {
            logline(threadId, "request shut down");
            shutdown();
            return true;
        }
        byte[] buffer;
        int size;
        if (sizestr.startsWith("ENDM")) {
            String endm = sizestr.substring(4, 8);
            boolean line_read = false;
            buffer = new byte[1024];
            size = 0;
            do {
                n = readLine(in, buffer, size, 1024);
                if (n == 5 && endm.equals(new String(buffer, size, 4))) {
                    line_read = true;
                } else if (n > 0) {
                    size += n;
                } else
                    break;
            } while (!line_read);
        }
        else {
            size = Integer.parseInt(sizestr);
            buffer = new byte[size];
            int got = 0;
            while (got < size) {
                n = in.read(buffer, got, size - got);
                if (n > 0)
                    got += n;
                else if (n == -1)
                    throw new SoccomException(SoccomException.SOCKET_CLOSED);
                else
                    throw new SoccomException(SoccomException.RECV_ERROR);
            }
        }
        String msgid = new String(header, 0, SoccomMessage.MSGID_SIZE);
        logline(threadId, "RECV: " + new String(header));
        logline(threadId, " msg: " + new String(buffer, 0, size));
        if (excessThread) {
            putResp(threadId, out, msgid, "ERROR: NO MORE CONNECTION ALLOWED");
            return true;
        }
        else {
            requestProc(threadId, msgid, buffer, size, out);
            return false;
        }
    }

    protected boolean clientHangup() {
        ServerThread thread = (ServerThread) Thread.currentThread();
        boolean hangup;
        try {
            thread.socket.setSoTimeout(1);
            int n = thread.in.read();
            hangup = (n == -1);
        } catch (SocketTimeoutException e) {
            hangup = false;
        } catch (IOException e) {
            hangup = true;
        }
        return hangup;
    }
}
