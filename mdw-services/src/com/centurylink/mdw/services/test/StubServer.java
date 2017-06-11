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
package com.centurylink.mdw.services.test;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.model.JsonObject;
import com.centurylink.mdw.model.event.AdapterStubRequest;
import com.centurylink.mdw.model.event.AdapterStubResponse;
import com.centurylink.mdw.model.workflow.ActivityStubRequest;
import com.centurylink.mdw.model.workflow.ActivityStubResponse;
import com.centurylink.mdw.soccom.SoccomException;
import com.centurylink.mdw.soccom.SoccomServer;
import com.centurylink.mdw.test.TestException;

public class StubServer extends SoccomServer {
    public static final int DEFAULT_PORT = 7182;
    private static StubServer instance = null;

    private Stubber stubber;
    private boolean running;

    public static boolean isRunning() {
        return instance != null;
    }

    public static void start(Stubber stubber) throws IOException {
        start(DEFAULT_PORT, stubber);
    }

    public static void start(int port, Stubber stubber) throws IOException {
        if (instance == null) {
            instance = new StubServer(port, stubber);
            instance.start(true);
        }
        else {
            throw new IOException("Stub server already running");
        }
    }

    public static void stop() {
        if (instance != null) {
            instance.shutdown();
            instance = null;
        }
    }

    private StubServer(int port, Stubber stubber) throws IOException {
        super(String.valueOf(port), (PrintStream)null);
        this.stubber = stubber;
        super.maxThreads = 1;
    }

    public void start() {
        start(true);
    }

    @Override
    public void start(boolean useNewThread) {
        if (!running) {
            try {
                running = true;
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            super.start(useNewThread);
        }
    }

    @Override
    public void shutdown() {
        if (running) {
            try {
                running = false;
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            super.shutdown();
        }
    }

    @Override
    protected void requestProc(String threadId, String msgId, byte[] msg, int msgSize,
            OutputStream out) throws IOException, SoccomException {
        String request = new String(msg, 0, msgSize);
        try {
            JSONObject json = new JsonObject(request);
            if (json.has(ActivityStubRequest.JSON_NAME)) {
                ActivityStubRequest activityStubRequest = new ActivityStubRequest(json);
                ActivityStubResponse activityStubResponse = stubber.processRequest(activityStubRequest);
                putResp("main", out, msgId, activityStubResponse.getJson().toString(2));
            }
            else if (json.has(AdapterStubRequest.JSON_NAME)) {
                AdapterStubRequest adapterStubRequest = new AdapterStubRequest(json);
                AdapterStubResponse adapterStubResponse = stubber.processRequest(adapterStubRequest);
                putResp("main", out, msgId, adapterStubResponse.getJson().toString(2));
            }
            else {
                throw new IOException("Unexpected stub request content");
            }
        }
        catch (JSONException ex) {
            throw new IOException(ex.getMessage(), ex);
        }
        catch (TestException ex) {
            throw new IOException(ex.getMessage(), ex);
        }
    }

    public interface Stubber {
        ActivityStubResponse processRequest(ActivityStubRequest request) throws JSONException, TestException;
        AdapterStubResponse processRequest(AdapterStubRequest request) throws JSONException, TestException;
    }
}
