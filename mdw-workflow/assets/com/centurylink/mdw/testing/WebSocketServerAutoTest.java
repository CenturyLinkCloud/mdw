/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.testing;

import java.io.File;
import java.net.InetSocketAddress;

import org.java_websocket.WebSocketImpl;

import com.centurylink.mdw.annotations.RegisteredService;
import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.common.service.AbstractWebSocketServer;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.provider.StartupException;
import com.centurylink.mdw.provider.StartupService;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.TestingServices;
import com.centurylink.mdw.test.TestCaseList;

/**
 * WebSocket server for autotest status updates
 */
@RegisteredService(StartupService.class)
public class WebSocketServerAutoTest extends AbstractWebSocketServer implements StartupService {

    private static final int SLEEP = 5000;

    private static WebSocketServerAutoTest instance = null;

    private TestingServices testServices;
    private TestCaseList testCaseList;
    private File resultsFile;

    public WebSocketServerAutoTest() {
        super(new InetSocketAddress(ApplicationContext.getAutoTestWebSocketPort()));

        if (instance == null)
            instance = this;
    }

    public WebSocketServerAutoTest(InetSocketAddress address) {
        super(address);

        if (instance == null)
            instance = this;
    }

    /**
     * Use this method to access the singleton of WebSocketServer
     */
    public static synchronized WebSocketServerAutoTest getInstance() {
        if (instance == null)  // This should never happen since it's instantiated by StartupListener (dynamicStartupService)
            instance = new WebSocketServerAutoTest();

        return instance;
    }

    @Override
    public void onStartup() throws StartupException {
        WebSocketImpl.DEBUG = logger.isMdwDebugEnabled();
        try {
            testServices = ServiceLocator.getTestingServices();
            getInstance().start();
            logger.info("WebSocketServerAutoTest started on port: " + getPort());
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
                                    getInstance().send(testCaseList.getJson().toString(2));
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
            getInstance().stop();
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
