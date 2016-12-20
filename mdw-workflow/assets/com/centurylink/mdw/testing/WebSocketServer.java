/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.testing;

import java.io.File;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.util.Collection;

import org.java_websocket.WebSocket;
import org.java_websocket.WebSocketImpl;
import org.java_websocket.handshake.ClientHandshake;

import com.centurylink.mdw.annotations.RegisteredService;
import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.provider.StartupException;
import com.centurylink.mdw.provider.StartupService;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.TestingServices;
import com.centurylink.mdw.test.TestCaseList;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

/**
 * WebSocket server for autotest status updates
 */
@RegisteredService(StartupService.class)
public class WebSocketServer extends org.java_websocket.server.WebSocketServer implements StartupService {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    private static final int SLEEP = 5000;

    private TestingServices testServices;
    private TestCaseList testCaseList;
    private File resultsFile;

    public WebSocketServer() {
        super(new InetSocketAddress(ApplicationContext.getAutoTestWebSocketPort()));
    }

    public WebSocketServer(InetSocketAddress address) {
        super(address);
    }

    @Override
    public void onOpen(WebSocket websocket, ClientHandshake handshake) {
    }

    @Override
    public void onClose(WebSocket websocket, int code, String reason, boolean remote) {
    }

    @Override
    public void onError(WebSocket websocket, Exception ex) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onMessage(WebSocket websocket, String message) {
        // TODO Auto-generated method stub
    }


    @Override
    protected boolean onConnect(SelectionKey key) {
        // TODO Auto-generated method stub
        return super.onConnect(key);
    }

    public void send(String text) {
        Collection<WebSocket> conns = connections();
        synchronized (conns) {
            for (WebSocket conn : conns) {
                conn.send(text);
            }
        }
    }

    @Override
    public void onStartup() throws StartupException {
        WebSocketImpl.DEBUG = logger.isMdwDebugEnabled();
        try {
            testServices = ServiceLocator.getTestingServices();
            start();
            logger.info("WebSocketServer started on port: " + getPort());
            new Thread(new Runnable() {
                public void run() {
                    while (true) {
                        try {
                            Thread.sleep(SLEEP);
                            resultsFile = testServices.getTestResultsFile(null);
                            if (resultsFile != null) {
                                if (testCaseList == null || testCaseList.getRetrieveDate() == null
                                        || resultsFile.lastModified() > testCaseList.getRetrieveDate().getTime()) {
                                    testCaseList = testServices.getTestCases();
                                    send(testCaseList.getJson().toString(2));
                                }
                            }
                        }
                        catch (Exception ex) {
                            throw new RuntimeException(ex);
                        }
                    }
                }
            }).start();
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            throw new StartupException(ex.getMessage(), ex);
        }

    }

    @Override
    public void onShutdown() {
        try {
            stop();
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
        }
    }

    @Override
    public boolean isEnabled() {
        return PropertyManager.getProperty(PropertyNames.MDW_AUTOTEST_WEBSOCKET_URL) != null;
    }
}
