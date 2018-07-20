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
package com.centurylink.mdw.soccom;

import java.net.Socket;
import java.net.SocketException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

class ServerThread extends Thread {
    Socket socket;
    InputStream in;
    String threadId;
    private OutputStream out;
    private SoccomServer server;
    private boolean excessThread;

    ServerThread(Socket socket, SoccomServer server, boolean excessThread) {
    this.socket = socket;
    this.server = server;
    this.excessThread = excessThread;;
    this.threadId = getName();
    int numThreads = this.server.addThread(threadId, this);
    this.server.logline(threadId, "Accept a client from "
            + this.socket.getInetAddress() + "; active threads " + numThreads);
    }

    public void run() {
        try {
            in = socket.getInputStream();
            out = socket.getOutputStream();
            boolean done = false;
            while (!done) {
                done = server.processMessage(in, out, threadId, excessThread);
            }
        }
        catch (SocketException e) {
            server.logline(threadId, "Socket exception - client may close the connection");
        }
        catch (InterruptedException e) {
            server.logline(threadId, "received InterruptedException");
        }
        catch (Exception e) {
            server.logline(threadId, "Exception: " + e);
            e.printStackTrace(System.err);
        }
        finally {
            try {
                if (!excessThread)
                    server.disconnectProc(socket);
                socket.close();
            }
            catch (IOException e) {
                // ignore, only happens when socket is already closed
            }
            server.removeThread(threadId, this);
            server.logline(threadId, "terminates");
        }
    }
}

