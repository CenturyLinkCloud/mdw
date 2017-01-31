/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.base;

import java.net.InetSocketAddress;

import org.java_websocket.WebSocketImpl;

import com.centurylink.mdw.annotations.RegisteredService;
import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.common.service.AbstractWebSocketServer;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.provider.StartupException;
import com.centurylink.mdw.provider.StartupService;

/**
 * WebSocket server for RuntimeUI LiveView status updates
 */
@RegisteredService(StartupService.class)
public class WebSocketServerLiveView extends AbstractWebSocketServer implements StartupService {

    private static final int SLEEP = 5000;

    private static WebSocketServerLiveView instance = null;

    public WebSocketServerLiveView() {
        super(new InetSocketAddress(ApplicationContext.getLiveViewWebSocketPort()));

        if (instance == null)
            instance = this;
    }

    public WebSocketServerLiveView(InetSocketAddress address) {
        super(address);

        if (instance == null)
            instance = this;
    }

    /**
     * Use this method to access the singleton of WebSocketServer
     */
    public static synchronized WebSocketServerLiveView getInstance() {
        if (instance == null)  // This should never happen since it's instantiated by StartupListener (dynamicStartupService)
            instance = new WebSocketServerLiveView();

        return instance;
    }

    @Override
    public void onStartup() throws StartupException {
        WebSocketImpl.DEBUG = logger.isMdwDebugEnabled();
        try {
            getInstance().start();
            logger.info("WebSocketServerLiveView started on port: " + getPort());
            new Thread(new Runnable() {
                public void run() {
                    while (true) {
                        try {
                            Thread.sleep(SLEEP);
             // TODO: Implement listener for runtimeUI LiveView to send updates back to browser

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
        return PropertyManager.getProperty(PropertyNames.MDW_LIVEVIEW_WEBSOCKET_URL) != null;
    }
}
