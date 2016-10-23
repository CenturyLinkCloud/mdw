/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.soccom;

import java.net.Socket;
import java.net.SocketException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

class ServerThread extends Thread
{
    Socket _socket;
    InputStream in;
    OutputStream out;
    SoccomServer _server;
    String thread_id;
    boolean _excess_thread;

    ServerThread(Socket socket, SoccomServer server, boolean excess_thread) {
    _socket = socket;
    _server = server;
    _excess_thread = excess_thread;;
    thread_id = getName();
    int nthreads = _server.add_thread(thread_id, this);
    _server.logline(thread_id, "Accept a client from "
            +_socket.getInetAddress() + "; active threads "
            + nthreads);
    }

    public void run() {
    try {
        in = _socket.getInputStream();
        out = _socket.getOutputStream();
        boolean done = false;
        while (!done) {
        done = _server.process_message(in, out, thread_id,
                           _excess_thread);
        }
    } catch (SocketException e) {
        _server.logline(thread_id,
            "Socket exception - client may close the connection");
    } catch (InterruptedException e) {
        _server.logline(thread_id, "received InterruptedException");
    } catch (Exception e) {
        _server.logline(thread_id, "Exception: " + e);
        e.printStackTrace(System.err);
    } finally {
        try {
        if (!_excess_thread) _server.disconnect_proc(_socket);
        _socket.close();
        } catch (IOException e) {
        // ignore, only happens when socket is already closed
        }
        _server.remove_thread(thread_id, this);
        _server.logline(thread_id, "terminates");
    }
    }
}

