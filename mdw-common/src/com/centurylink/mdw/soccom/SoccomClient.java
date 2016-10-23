/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.soccom;

// need to log time
// need to set timeout
// need to check msg ID in client_getresp

import java.net.*;
import java.io.*;
import java.util.StringTokenizer;

/**
 * This class defines the method for soccom client.
 *
 * Sample usage:
 * <pre>
 *    try {
 *        SoccomClient client = new SoccomClient(host, port);
 *        client.putreq("test message");
 *        String resp = client.getresp();
 *        System.out.println("response: " + resp);
 *        client.close();
 *    } catch (SoccomException e) {
 *        System.out.println("Exception: " + e);
 *    }
 * </pre>
 *
 * For large messages, if we'd like to get the messages line-by-line
 * in order to display first part of messages before the entire message is
 * received, instead of <code>getresp()</code>, we can use the following code:
 * <pre>
 *    String resp = client.getresp_first();
 *    System.out.println("response first: " + resp);
 *    while (client.getresp_hasmore()) {
 *        resp = client.getresp_next();
 *        System.out.println("response next: " + resp);
 *    }
 * </pre>
 *
 * Similarly, we can also send message line-by-line, as shown in the following
 * example:
 * <pre>
 *    String endmark = "////";
 *    putreq_vheader(endmark);
 *    for (int i=0; i&lt;number_of_lines; i++) {
 *        putreq_vline(lines[i]);
 *    }
 *    putreq_vfooter(endmark);
 * </pre>
 *
 * It is not necessary to open/close the connection for each request,
 * although a server or a pool has a limited number of open connection
 * allowed.
 *
 * @auther Jiyang Xu
 * @version 4.5
 */

public class SoccomClient
{

    /**
     * Basic constructor.
     * @param host Either a domain name or an IP address.
     * @param port The port number.
     * @param log A PrintStream for printint log lines
     * @exception SoccomException The exception is thrown possibly because
     *         of the following reasons:
     *               <ul>
     *          <li>Host name is unknown</li>
     *          <li>Cannot connect to the service</li>
     *          <li>Failed to create the socket</li>
     *               </ul>
     */
    public SoccomClient(String host, String port, PrintStream log)
    throws SoccomException
    {
    _log = log;
    try {
        _socket = new Socket(host, Integer.parseInt(port));

        logline("Client connect on host " + host +
                   ", port " + port);
        logline("Local address: " + _socket.getLocalAddress());
        _in = _socket.getInputStream();
        _out = _socket.getOutputStream();
        _msgid = new byte[SoccomMessage.MSGID_SIZE];
    } catch (UnknownHostException e) {
        throw new SoccomException(SoccomException.HOSTNAME, e);
    } catch (ConnectException e) {
        throw new SoccomException(SoccomException.CONNECT, e);
    } catch (IOException e) {
        throw new SoccomException(SoccomException.CREATE_SOCKET, e);
    }
    }

    /**
     * Alternative constructor. It has no parameter for log PrintStream
     * and it logs all messages to the terminal screen.
     *
     * @param host Either a domain name or an IP address.
     * @param port The port number.
     * @exception SoccomException The exception is thrown possibly because
     *         of the following reasons:
     *               <ul>
     *          <li>Host name is unknown</li>
     *          <li>Cannot connect to the service</li>
     *          <li>Failed to create the socket</li>
     *               </ul>
     */
    public SoccomClient(String host, String port)
    throws SoccomException
    {
    this(host, port, System.out);
    }

    private void copy_msgid(byte[] target, byte[] source) {
    for (int i=0; i<SoccomMessage.MSGID_SIZE; i++) {
        target[i] = source[i];
    }
    }

    private void check_msgid(byte[] expect, byte[] get)
    throws SoccomException
    {
    for (int i=0; i<SoccomMessage.MSGID_SIZE; i++) {
        if (expect[i]!=get[i])
        throw new SoccomException(SoccomException.MSGID_MISMATCH);
    }
    }

    /**
     * Send a request message.
     * @param msg The message to be sent.
     * @exception SoccomException Thrown when an IOException is encountered.
     */
    public void putreq(String msg)
    throws SoccomException
    {
    byte msgbytes[] = SoccomMessage.makeMessage(msg, null);
    logline("SEND: " + new String(msgbytes));
    copy_msgid(_msgid, msgbytes);
    try {
        // _out.print(msg);
        _out.write(msgbytes);
    } catch (IOException e) {
        throw new SoccomException(SoccomException.REQUEST);
    }
    }

    /**
     * Send the header part of a variable-size request message.
     * @param endmark Must be a four character string that will
     *           not appear at the beginning of a line in message body.
     * @exception SoccomException Thrown when an IOException is encountered.
     */
    public void putreq_vheader(String endmark)
    throws SoccomException
    {
    if (endmark.length()!=4)
        throw new SoccomException(SoccomException.ENDM_LENGTH);
    byte msgbytes[] = SoccomMessage.
        makeMessageSpecial("ENDM" + endmark, null);
    logline("SEND: " + new String(msgbytes));
    copy_msgid(_msgid, msgbytes);
    try {
        _out.write(msgbytes);
    } catch (IOException e) {
        throw new SoccomException(SoccomException.REQUEST);
    }
    }

    /**
     * Send a line as a part of a variable-size request message.
     * @param msg A line of message. If it is not ended with a new
     *        line character, add it.
     * @exception SoccomException Thrown when an IOException is encountered.
     */
    public void putreq_vline(String msg)
    throws SoccomException
    {
    int length = msg.length();
    if (msg.charAt(length-1) == '\n') {
        logline("SEND: " + msg.substring(0,length-1));
    } else {
        logline("SEND: " + msg);
        msg += "\n";
    }
    byte msgbytes[] = msg.getBytes();
    try {
        _out.write(msgbytes);
    } catch (IOException e) {
        throw new SoccomException(SoccomException.REQUEST);
    }
    }

    /**
     * Mark the end of a variable-size message.
     * @param endmark This must be the same one as used in the putreq_vheader.
     * @exception SoccomException Thrown when an IOException is encountered.
     */
    public void putreq_vfooter(String endmark)
    throws SoccomException
    {
    if (endmark.length()!=4)
        throw new SoccomException(SoccomException.ENDM_LENGTH);
    String msg = endmark + "\n";
    byte msgbytes[] = msg.getBytes();
    logline("SEND: " + endmark);
    try {
        _out.write(msgbytes);
    } catch (IOException e) {
        throw new SoccomException(SoccomException.REQUEST);
    }
    }

    /**
     * Get the response from the server, after sending a request.
     * @param timeout timeout value in seconds
     * @return The response message.
     * @exception SoccomException Thrown when an IOException is encountered.
     */
    public String getresp(int timeout)
    throws SoccomException
    {
    int size, n;
    String sizestr;
    try {
        byte[] _header = new byte[SoccomMessage.HEADER_SIZE];
        _socket.setSoTimeout(timeout*1000);
        n = _in.read(_header, 0, SoccomMessage.HEADER_SIZE);
        if (n!=SoccomMessage.HEADER_SIZE)
        throw new SoccomException(SoccomException.RECV_HEADER);
        logline("RECV HDR: " + new String(_header));
        check_msgid(_msgid, _header);
        sizestr = new String(_header, SoccomMessage.MSGSIZE_OFFSET, 8);
        if (sizestr.startsWith("ENDM")) size = -1;
        else size = Integer.parseInt(sizestr);
    } catch (InterruptedIOException e) {
        throw new SoccomException(SoccomException.POLL_TIMEOUT);
    } catch (IOException e) {
        throw new SoccomException(SoccomException.RECV_HEADER);
    }
    try {
        String msg;
        if (size == -1) {
        String endm = sizestr.substring(4,8);
        String line;
        StringBuffer sb = new StringBuffer();
        byte[] buffer = new byte[1024];
        int k;
        boolean done = false;
        while (!done) {
            k = readLine(_in, buffer, 0, 1024);
            line = new String(buffer,0,k);
            if (k==5 && line.startsWith(endm)) done = true;
            else sb.append(line);
        }
        msg = sb.toString();
        } else {
        byte[] buffer = new byte[size+SoccomMessage.HEADER_SIZE];
        int got = 0;
        while (got<size) {
            n = _in.read(buffer, got, size-got);
            if (n>0) got += n;
            else if (n==-1)
            throw new SoccomException(SoccomException.SOCKET_CLOSED);
            else throw new SoccomException(SoccomException.RECV_ERROR);
        }
        msg = new String(buffer, 0, size);
        }
        logline("RECV MSG: " + msg);
        return msg;
    } catch (InterruptedIOException e) {
        throw new SoccomException(SoccomException.RECV_ERROR);
    } catch (IOException e) {
        throw new SoccomException(SoccomException.RECV_ERROR);
    }
    }

    /**
     * This is the same as getresp(int timeout)
     * except the timeout value is set to the default 120 seconds.
     * @return The response message.
     * @exception SoccomException Thrown when an IOException is encountered.
     */
    public String getresp()
    throws SoccomException
    {
    return getresp(120);
    }

    /**
     * The method receives the first part of response message from the
     * server, up to maxbytes bytes.
     * Use getresp_hasmore and getresp_next to get
     * getresp_rest to get the remaining part of messages.
     * When the server sends the message using ENDM, the string returned
     * may be longer than maxbytes, but only till the first line after that.
     * When maxbytes is 0, the procedure reads the first line.
     * @param    maxbytes The maximum number of bytes to be returned.
     * @param   timeout timeout in seconds
     * @return    The string converted from the bytes read.
     * @exception SoccomException
     *        Any transmission error such as reading socket error.
     */
    public String getresp_first(int maxbytes, int timeout)
    throws SoccomException
    {
    int n;
    String sizestr, msg;
    _resp_read = -1;
    try {
        byte[] _header = new byte[SoccomMessage.HEADER_SIZE];
        _socket.setSoTimeout(timeout*1000);
        n = _in.read(_header, 0, SoccomMessage.HEADER_SIZE);
        if (n!=SoccomMessage.HEADER_SIZE)
        throw new SoccomException(SoccomException.RECV_HEADER);
        logline("RECV HDR: " + new String(_header));
        check_msgid(_msgid, _header);
        sizestr = new String(_header, SoccomMessage.MSGSIZE_OFFSET, 8);
        if (sizestr.startsWith("ENDM")) _resp_size = -1;
        else _resp_size = Integer.parseInt(sizestr);
    } catch (InterruptedIOException e) {
        throw new SoccomException(SoccomException.RECV_HEADER);
    } catch (IOException e) {
        throw new SoccomException(SoccomException.RECV_HEADER);
    }
    try {
        if (maxbytes==0) {
        if (_resp_size == -1) {
            _endm = sizestr.substring(4,8);
        }
        _resp_read = 0;
        if (getresp_hasmore()) msg = getresp_next(maxbytes);
        else msg = "";
        } else if (_resp_size == -1) {
        _endm = sizestr.substring(4,8);
        byte[] buffer = new byte[maxbytes];
        int k=0;
        boolean done = false;
        while (!done && k<maxbytes) {
            n = readLine(_in, buffer, k, maxbytes);
            if (n==5 && _endm.equals(new String(buffer,k,4))) {
            done = true;
            } else k += n;
        }
        if (done) _resp_read = -1;
        else _resp_read = k;
        msg = new String(buffer,0,k);
        logline("RECV MSG: " + msg);
        } else {
        byte[] buffer = new byte[maxbytes];
        if (_resp_size<=maxbytes) {
            n = _in.read(buffer, 0, _resp_size);
        } else {
            n = _in.read(buffer, 0, maxbytes);
        }
        if (n>=0) {
            _resp_read = n;
            msg = new String(buffer, 0, n);
        } else if (n==-1)
            throw new SoccomException(SoccomException.SOCKET_CLOSED);
        else throw new SoccomException(SoccomException.RECV_ERROR);
        logline("RECV MSG: " + msg);
        }
        return msg;
    } catch (InterruptedIOException e) {
        throw new SoccomException(SoccomException.RECV_ERROR);
    } catch (IOException e) {
        throw new SoccomException(SoccomException.RECV_ERROR);
    }

    }

    /**
     * same as getresp_first(int maxbytes, int timeout)
     * except the timeout is default to 120 seconds
     */
    public String getresp_first(int maxbytes)
    throws SoccomException
    {
    return getresp_first(maxbytes, 120);
    }

    /**
     * same as getresp_first(int maxbytes, int timeout)
     * except the timeout is default to 120 seconds and maxbytes is 0
     */
    public String getresp_first()
    throws SoccomException
    {
    return getresp_first(0, 120);
    }

    /**
     * The method checks if the message is completed read.
     * @return <code>true</code> if the message has not been completed
     *        read by <code>getresp_first()</code> or
     *         <code>getresp_next()</code>.
     */
    public boolean getresp_hasmore() {
    if (_resp_size>0) {
        return _resp_read>=0 && _resp_size > _resp_read;
    } else {
        return (_resp_read>=0);
    }
    }

    private int readLine(InputStream in, byte buffer[], int offset,
             int maxbytes)
    throws IOException
    {
    int n = 0;
    boolean line_read = false;
    while (!line_read && n<maxbytes) {
        in.read(buffer, offset+n, 1);
        if (buffer[offset+n] == '\n') line_read = true;
        n++;
    }
    return n;
    }

    private String readLine(InputStream in, int maxbytes)
    throws IOException
    {
    byte[] buffer = new byte[maxbytes];
    int n = readLine(in, buffer, 0, maxbytes);
    return new String(buffer, 0, n);
    }

    /**
     * The method receives the next part of response message from the
     * server, up to maxbytes bytes. The method must only be called
     * following a call to getresp_first or another getresp_next
     * when getresp_hasmore is true.
     * When the server sends the message using ENDM, the string returned
     * may be longer than maxbytes, but only till the first line after that.
     * The timeout value is inherited from getresp_first.
     * When maxbytes is 0, read till the end of a line ('\n' is included)
     * @param    maxbytes The maximum number of bytes to be returned.
     * @return    The string converted from the bytes read.
     * @exception SoccomException
     *        Any transmission error such as reading socket error.
     */
    public String getresp_next(int maxbytes)
    throws SoccomException
    {
    int n;
    String msg;
    try {
        if (_resp_size == -1) {
        if (maxbytes==0) {
            msg = readLine(_in, 1024);
            if (msg.startsWith(_endm)) {
            _resp_read = -1;
            msg = "";
            } else _resp_read += msg.length();
        } else {
            byte[] buffer = new byte[maxbytes];
            int k=0;
            boolean done = false;
            while (!done && k<maxbytes) {
            n = readLine(_in, buffer, k, maxbytes);
            if (n==5 && _endm.equals(new String(buffer,k,4))) {
                done = true;
            } else k += n;
            }
            if (done) _resp_read = -1;
            else _resp_read += k;
            msg = new String(buffer,0,k);
        }
        } else {
        if (maxbytes==0) {
            msg = readLine(_in, _resp_size-_resp_read);
            _resp_read += msg.length();
        } else {
            byte[] buffer = new byte[maxbytes];
            if (_resp_size - _resp_read <= maxbytes) {
            n = _resp_size - _resp_read;
            } else n = maxbytes;
            n = _in.read(buffer, 0, n);
            if (n>=0) _resp_read += n;
            else if (n==-1)
            throw new SoccomException(SoccomException.SOCKET_CLOSED);
            else throw new SoccomException(SoccomException.RECV_ERROR);
            msg = new String(buffer, 0, n);
        }
        }
        logline("RECV MSG: " + msg);
        return msg;
    } catch (InterruptedIOException e) {
        throw new SoccomException(SoccomException.RECV_ERROR);
    } catch (IOException e) {
        throw new SoccomException(SoccomException.RECV_ERROR);
    }
    }

    private void logline(String msg) {
        if (_log!=null) _log.println(msg);
    }

    /**
     * Same as getresp_next(0), i.e. read in the next line
     */
    public String getresp_next()
    throws SoccomException
    {
    return getresp_next(0);
    }

    /**
     * This method is a simple wrapper for synchronous invocation
     * of server's service. It is roughly implemented  as:
     * <pre>
     *    SoccomClient soccom = new SoccomClient(host,port,log);
     *    putreq(msg);
     *    return getresp();
     *    soccom.close();
     * </pre>
     * @param serverspec In the form of host:port. If ':' is missing, assume
     *        port is 4001
     * @param msg The message to be sent.
     * @param timeout Time out in seconds
     * @param log For logging information
     * @return The respose message.
     * @exception SoccomException It passes up any exception encountered
     *        along the way.
     */
    public static String call(String serverspec, String msg,
                  int timeout, PrintStream log)
    throws SoccomException
    {
    String host, port;
    int k = serverspec.indexOf(':');
    if (k>=0) {
        host = serverspec.substring(0,k);
        port = serverspec.substring(k+1);
    } else {
        host = serverspec;
        port = "4001";
    }
    SoccomClient soccom = new SoccomClient(host, port, log);
    String reply;
    try {
        soccom.putreq(msg);
        reply = soccom.getresp(timeout);
        soccom.close();
    } catch (SoccomException e) {
        soccom.close();
        throw e;
    }
    return reply;
    }

    /**
     * Close the connection.
     * This is automatically called at garbage collection but
     * it is a good idea to voluntarily call it as soon as the connection
     * is not needed any more.
     */
    public void close()
    {
    if (_socket!=null) {
        try {
        _socket.close();
        _socket = null;
        } catch (IOException e) {
        System.err.println("Exception: " + e);
        // throw new SoccomException(SoccomException.RECV_ERROR);
        }
        _in = null;
        _out = null;
    }
    }

    /**
     * The method calls <code>close()</code> if the socket has not been
     * closed yet.
     */
    protected void finalize() {
    close();
    }

    /**
     * Tests for client
     */
    private String tests(String msg)
    {
    String testcase = msg.substring(6,12);
    String resp;
    try {
        if (testcase.equals("TIMEOU")) {
        int timeout = Integer.parseInt(msg.substring(13));
        putreq("SLEEP " + (timeout+5));
        resp = getresp(timeout);
        } else if (testcase.equals("COMBIN")) {
        /* test combination of sending/receiving line-by-line
         * or as a whole. Spec has 4 characters as follows:
         * 1. for client send: W - as a whole, L - line-by-line
         * 2. for server recv: W - as a whole, L - not implemented
         * 3. for server send: W - as a whole, L - line-by-line
         * 4. for client recv: W - as a whole, L - line-by-line
         */
        StringTokenizer st = new StringTokenizer(msg.substring(12));
        String testspec = msg.substring(13,17);
        if (testspec.charAt(0)=='L') {
            String endmark = "////";
            putreq_vheader(endmark);
            st = new StringTokenizer(msg, "\n");
            while (st.hasMoreTokens()) {
            putreq_vline(st.nextToken());
            }
            putreq_vfooter(endmark);
        } else putreq(msg);
        if (testspec.charAt(3)=='L') {
            resp = getresp_first();
            while (getresp_hasmore()) {
            resp = resp + getresp_next();
            }
        } else if (testspec.charAt(3)=='P') {
            String one;
            resp = getresp_first(80);
            while (getresp_hasmore()) {
            one = getresp_next(80);
            resp = resp + one;
            }
        } else resp = getresp();
        } else {    // default: REQRES, testing reqresp
        /* SV_LBL falls here, too */
        putreq(msg);
        resp = getresp();
        }
    } catch (SoccomException e) {
        resp = "TEST ERROR " + e.errdesc() + "\n";
    }
    return resp;
    }

    private static void printUsage()
    {
    System.err.println("Usage: java soccom.SoccomClient host port msg");
    }

    /**
     * This method allows to send a message from the command line.
     * As an example:
     * <pre>
     *    java com.qwest.magic.soccom.SoccomClient magicdevl 4001 "test msg"
     * </pre>
     */
    public static void main(String argv[])
    throws Exception
    {

    if (argv.length!=3) {
        printUsage();
        System.exit(1);
    }
    String host = argv[0];
    String port = argv[1];
    String msg = argv[2];
    try {
        SoccomClient client = new SoccomClient(host, port);
        String resp;
        if (msg.length()>=6 && msg.substring(0,6).equals("_TEST_")) {
        resp = client.tests(msg);
        } else {
        client.putreq(msg);
        resp = client.getresp();
        }
        System.out.println("response: " + resp);
        client.close();
    } catch (SoccomException e) {
        System.out.println("ERROR: " + e);
    }
    }

    private InputStream _in;
    private OutputStream _out;
    private byte _msgid[];
    private Socket _socket;
    private PrintStream _log;
    // the followings are used only by getresp_first() and getresp_next();
    private int _resp_size;
    private int _resp_read;
    private String _endm;
}
