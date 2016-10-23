/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services.pooling;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Properties;

import org.json.JSONObject;

import com.centurylink.mdw.activity.types.AdapterActivity;
import com.centurylink.mdw.adapter.PoolableAdapter;
import com.centurylink.mdw.cache.CacheEnabled;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.config.PropertyUtil;
import com.centurylink.mdw.connector.adapter.ConnectionException;
import com.centurylink.mdw.constant.OwnerType;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.model.monitor.ScheduledEvent;
import com.centurylink.mdw.provider.CacheService;
import com.centurylink.mdw.services.EventManager;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.cache.CacheRegistration;
import com.centurylink.mdw.services.event.CertifiedMessageManager;
import com.centurylink.mdw.services.event.ScheduledEventQueue;
import com.centurylink.mdw.services.messenger.InternalMessenger;
import com.centurylink.mdw.services.messenger.MessengerFactory;
import com.centurylink.mdw.util.StringHelper;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

public class AdapterConnectionPool
        extends MDWConnectionPool implements CacheEnabled, CacheService {

    public static final String PROP_ADAPTER = "Adapter";
    public static final String PROP_CONNECTOR_CLASS = "ConnectorClass";
    public static final String PROP_ONE_CONNECTION_FOR_ALL = "OneConnectionForAll";
    public static final String PROP_POOL_SIZE = "PoolSize";
    public static final String PROP_BORROW_TIMEOUT = "BorrowTimeout";
    public static final String PROP_DISABLED = "Disabled";
    public static final String PROP_RETRY_INTERVAL = "retry_interval";        // in seconds
    public static final String PROP_TIMEOUT = AdapterActivity.PROP_TIMEOUT;        // in seconds
    public static final String PROP_MAX_TRIES = AdapterActivity.PROP_MAX_TRIES;    // max (automated) tries; 0 is treated as 1
    public static final String PROP_AUTO_SHUTDOWN_THRESHOLD = "auto_shutdown_threshold";
    public static final String PROP_PING_INTERVAL = "ping_interval";
    public static final String PROP_PING_TIMEOUT = "PingTimeout";

    private static final String STATUS_UP = "up";
    private static final String STATUS_DOWN = "down";
    public static final String STATUS_MANUAL_UP = "manual_up";
    public static final String STATUS_MANUAL_DOWN = "manual_down";

    private static int reservation_timeout = 120;    // seconds

    private String adapterClassName;
    private Class<?> adapterClass;
    private Properties properties;
    private List<WaitingConnectionRequest> waitingQueue;
    private List<WaitingConnectionRequest> readyQueue;
    private int consecutiveConnectionFailure;
    private int pingTimeout;

    public AdapterConnectionPool(String name, Properties props) {
        super(name);
        this.adapterClassName = props.getProperty(AdapterConnectionPool.PROP_ADAPTER);
        if (adapterClassName==null)    // for backward compatibility
            adapterClassName = props.getProperty(AdapterConnectionPool.PROP_CONNECTOR_CLASS);
        properties = props;
        waitingQueue = new ArrayList<WaitingConnectionRequest>();
        readyQueue = new ArrayList<WaitingConnectionRequest>();
    }

    @Override
    synchronized public void start() throws Exception {
        adapterClass = Class.forName(adapterClassName);
        this.setPoolSize(StringHelper.getInteger(properties.getProperty(PROP_POOL_SIZE), 1));
        this.setBorrowTimeout(StringHelper.getInteger(properties.getProperty(PROP_BORROW_TIMEOUT), 0));
        pingTimeout = StringHelper.getInteger(properties.getProperty(PROP_PING_TIMEOUT), -1);
        if (pingTimeout<0) {
            pingTimeout = StringHelper.getInteger(properties.getProperty(PROP_TIMEOUT), -1);
        }
        consecutiveConnectionFailure = 0;
        super.start();
    }

    private boolean processWaitingRequest(WaitingConnectionRequest req,
            PooledAdapterConnection conn) {
        req.connection = conn;
        conn.setAssignee("reserved-for-" + req.activityInstId);
        conn.setAssignTime(new Date());
        readyQueue.add(req);
        String eventName = ScheduledEvent.INTERNAL_EVENT_PREFIX+req.activityInstId.toString();
        EventManager eventManager = ServiceLocator.getEventManager();
        boolean processed = eventManager.processUnscheduledEvent(eventName);
        if (!processed) readyQueue.remove(req);
        return processed;
    }

    /**
     * Process all waiting requests. Optionally schedule all blocked certified
     * messages first.
     * @param processCertifiedMessagesAsWell If true, schedule all blocked certified
     * messages first
     */
    synchronized public void processWaitingRequests(boolean processCertifiedMessagesAsWell) {
        if (processCertifiedMessagesAsWell) {
            CertifiedMessageManager cmmgr = CertifiedMessageManager.getSingleton();
            cmmgr.resumeConnectionPoolMessages(getName());
        }
        while (isStarted() && waitingQueue.size()>0 && super.getNumActive()<getPoolSize()) {
            WaitingConnectionRequest req = waitingQueue.remove(0);
            try {
                PooledAdapterConnection conn = this.getConnection("reserved-for-" + req.activityInstId, req.activityInstId);
                boolean processed = processWaitingRequest(req, conn);
                if (!processed) {
                    super.returnObject(req.connection);
                }
            } catch (Exception e) {
                StandardLogger logger = LoggerUtil.getStandardLogger();
                logger.severeException("Failed to process waiting request " + req.assignee, e);
            }
        }
    }

    synchronized public void addWaitingRequest(ScheduledEvent one) {
        WaitingConnectionRequest req = new WaitingConnectionRequest();
        String actInstId = one.getName().substring(ScheduledEvent.INTERNAL_EVENT_PREFIX.length());
        req.activityInstId = new Long(actInstId);
        req.connection = null;
        waitingQueue.add(req);
    }

    //
    // methods used by adapter activities
    //

    class WaitingConnectionRequest {
        String assignee;
        Long activityInstId;    // activity instance ID
        PooledAdapterConnection connection;
    }

    public synchronized PooledConnection getReservedConnection(String assignee, Long reservationId) throws ConnectionException {
//        if (!started) throw new ConnectionException(ConnectionException.POOL_DISABLED, "Pool is disabled");
        for (int i=0; i<readyQueue.size(); i++) {
            WaitingConnectionRequest req = readyQueue.get(i);
            if (req.activityInstId.equals(reservationId)) {
                readyQueue.remove(i);
                req.connection.setAssignee(assignee);
                req.connection.setAssignTime(new Date());
                return req.connection;
            }
        }
        // should only be possible when the connection is reserved for too long (exceeding 5 minutes)
        return getConnection(assignee, reservationId);
    }

    public synchronized PooledAdapterConnection getConnection(String assignee, Long reservationId) throws ConnectionException {
        super.recordTotalConnectionRequest();
        if (!isStarted()) {
            if (reservationId!=null) {
                WaitingConnectionRequest req = new WaitingConnectionRequest();
                req.activityInstId = reservationId;
                req.assignee = assignee;
                req.connection = null;
                waitingQueue.add(req);
                super.recordMaxConnectionRequests(waitingQueue.size());
            }
            throw new ConnectionException(ConnectionException.POOL_DISABLED, "Pool is disabled");
        }
        Date now = new Date();
        try {
            PooledAdapterConnection conn = (PooledAdapterConnection)super.borrowObject(assignee);
            super.recordMaxConnectionRequests(super.getNumActive());
            return conn;
        } catch (NoSuchElementException e) {
            // all pool connections are used
            if (readyQueue.size()>0 && readyQueue.get(0).connection.getAssignTime().getTime()
                    > now.getTime()+reservation_timeout*1000L) {
                WaitingConnectionRequest req = readyQueue.remove(0);
                req.connection.setAssignee(assignee);
                req.connection.setAssignTime(now);
                return req.connection;
            }
            if (reservationId!=null) {
                WaitingConnectionRequest req = new WaitingConnectionRequest();
                req.activityInstId = reservationId;
                req.assignee = assignee;
                req.connection = null;
                waitingQueue.add(req);
                super.recordMaxConnectionRequests(getPoolSize()+waitingQueue.size());
            }
            throw new ConnectionException(ConnectionException.POOL_EXHAUSTED, "Pool connections are exhausted", e);
        } catch (Exception e) {
            throw new ConnectionException(ConnectionException.POOL_BORROW, e.getMessage(), e);
        }
    }

    private void schedulePingEvent() {
        String av = properties.getProperty(PROP_PING_INTERVAL);
        if (av!=null) {
            int pingInterval = StringHelper.getInteger(av, -1);
            if (pingInterval>0) {
                ScheduledEventQueue queue = ScheduledEventQueue.getSingleton();
                String eventName = ScheduledEvent.SPECIAL_EVENT_PREFIX + "Ping." + getName();
                String message = "<_mdw_pool_ping>" + getName() + "</_mdw_pool_ping>";
                queue.scheduleExternalEvent(eventName,
                        new Date(DatabaseAccess.getCurrentTime()+pingInterval*1000),
                        message, null);
            }
        }
    }

    public synchronized void ping_and_start() {
//        if (started) return;    Check after receiving broadcast
        PooledAdapterConnection conn = null;
        boolean up;
        try {
            conn = (PooledAdapterConnection)super.borrowObject("Ping");
            up = conn.ping();
        } catch (Exception e) {
            up = false;
        } finally {
            if (conn!=null) {
                try {
                    super.returnObject(conn);
                } catch (Exception e) {
                }
            }
        }
        StandardLogger logger = LoggerUtil.getStandardLogger();
        logger.info("Connection pool " + getName() + " ping returns " + (up?STATUS_UP:STATUS_DOWN));
        if (up) {
            broadcastPoolStatus(STATUS_UP);
        } else {
            schedulePingEvent();
        }
    }

    synchronized void returnToPool(PooledAdapterConnection conn, int exceptionCode) {
        int threshold = StringHelper.getInteger(properties.getProperty(PROP_AUTO_SHUTDOWN_THRESHOLD), -1);
        if (threshold>0) {
            if (exceptionCode==ConnectionException.CONNECTION_DOWN) {
                consecutiveConnectionFailure++;
                if (consecutiveConnectionFailure>=threshold) {
                    // shutdown servers in memory only - attribute is not updated
                    StandardLogger logger = LoggerUtil.getStandardLogger();
                    logger.info("Connection pool auto shut down after consecutive failures: " + getName());
                    this.shutdown(false);
                    schedulePingEvent();
                }
                this.broadcastPoolStatus(STATUS_DOWN);
            } else {
                if (consecutiveConnectionFailure>0) {
                    consecutiveConnectionFailure = 0;
                    this.broadcastPoolStatus(STATUS_UP);
                }
            }
        }
        try {
            while (isStarted() && waitingQueue.size()>0) {
                WaitingConnectionRequest req = waitingQueue.remove(0);
                boolean processed = processWaitingRequest(req, conn);
                if (processed) return;
                // else try next one
            }
            // now release the connection
            super.returnObject(conn);
        } catch (Exception e) {
            StandardLogger logger = LoggerUtil.getStandardLogger();
            logger.severeException("Failed to return connection to pool", e);
        }
    }

    public String getProperty(String name) {
        return properties.getProperty(name);
    }

    //
    // adminstration methods
    //

    public Properties getProperties() {
        return properties;
    }

    boolean isDisabled() {
        boolean disabled = "true".equalsIgnoreCase(properties.getProperty(PROP_DISABLED));
        if (!disabled && adapterClassName==null) disabled = true;
        return disabled;
    }

    public PooledConnection makeConnection() throws Exception {
        PoolableAdapter adapter = (PoolableAdapter)adapterClass.newInstance();
        PooledAdapterConnection connection =
            new PooledAdapterConnection(AdapterConnectionPool.this, adapter, properties);
        return connection;
    }

    private void reloadProperties() throws Exception {
        PropertyManager propmgr = PropertyUtil.getInstance().getPropertyManager();
        // propmgr.refresh();
        String propgrp = PropertyNames.MDW_CONNECTION_POOL + "." + getName();
        Properties propsNew = propmgr.getProperties(propgrp);
        properties.clear();
        for (Object key : propsNew.keySet()) {
            String pn = (String)key;
            String[] pnParsed = pn.split("\\.");
            if (pnParsed.length==5) {
                String attrname = pnParsed[4];
                properties.put(attrname, propsNew.get(key));
            }
        }
    }

    public void clearCache() {
    }

    public synchronized void refreshCache() throws Exception {
        // this method is invoked when a connection pool is started or stopped
        reloadProperties();
        boolean disabled = "true".equalsIgnoreCase(properties.getProperty(PROP_DISABLED));
        StandardLogger logger = LoggerUtil.getStandardLogger();
        logger.info("Connection pool " + getName() + " is changing status from "
                + (isStarted()?"enabled":"disabled")
                + " to " + (disabled?"disabled":"enabled"));
        if (isStarted()) {
            if (disabled) shutdown(false);
        } else {
            if (!disabled) {
                start();
                this.processWaitingRequests(true);
            }
        }
    }

    private void updatePoolProperty(String pn, String pv) throws Exception {
        String attrname = PropertyNames.MDW_CONNECTION_POOL + "." + getName() + "." + pn;
        if (pv!=null && pv.length()==0) pv = null;
        EventManager eventMgr = ServiceLocator.getEventManager();
        eventMgr.setAttribute(OwnerType.SYSTEM, 0L, attrname, pv);
        if (pv!=null) properties.put(pn, pv);
        else properties.remove(pn);
    }

    /**
     * Enable and disable the connection pool globally. Used by admin GUI
     * The method informs all managed servers in the cluster through JMS broadcast.
     *
     * @param yes
     * @throws Exception when there is JMS broadcast error. Later exceptions will be in the log only
     */
    public synchronized void setEnabled(boolean yes) throws Exception {
        this.updatePoolProperty(AdapterConnectionPool.PROP_DISABLED, yes?"false":"true");
        Thread.sleep(2000);        // to ensure database update is committed
        CacheRegistration.broadcastRefresh(
                PropertyManager.class.getName() + "," +
                AdapterConnectionPool.class.getName() + ":" + getName(),
                MessengerFactory.newInternalMessenger());
    }

    public List<String> getWaitingRequests() {
        List<String> ret = new ArrayList<String>();
        for (WaitingConnectionRequest one : waitingQueue) {
            ret.add(one.assignee);
        }
        return ret;
    }

    public int getPingTimeout() {
        return pingTimeout;
    }

    public void broadcastPoolStatus(String status) {
        try {
            JSONObject json = new JSONObject();
            json.put("ACTION", "ADAPTER_POOL_STATUS");
            json.put("POOL_NAME", getName());
            json.put("STATUS", status);
            InternalMessenger messenger = MessengerFactory.newInternalMessenger();
            messenger.broadcastMessage(json.toString());
        } catch (Exception e) {
            StandardLogger logger = LoggerUtil.getStandardLogger();
            logger.severeException("Failed to publish event invalidation message", e);
        }
    }

    public synchronized void processPoolStatusBroadcast(String status) {
        if (status.equalsIgnoreCase(STATUS_UP)) {
            consecutiveConnectionFailure = 0;
            if (isStarted()) return;
            StandardLogger logger = LoggerUtil.getStandardLogger();
            try {
                this.start();
                logger.info("Connection pool auto re-started after successful ping: " + getName());
                this.processWaitingRequests(true);
            } catch (Exception e) {    // only possible when adapter class cannot be initialized
                logger.severeException("Failed to restart connection pool " + getName(), e);
                setStarted(false);
            }
        } else if (status.equals(STATUS_DOWN)) {
            if (!isStarted()) return;
            consecutiveConnectionFailure++;
            int threshold = StringHelper.getInteger(properties.getProperty(PROP_AUTO_SHUTDOWN_THRESHOLD), -1);
            if (consecutiveConnectionFailure>=threshold) {
                StandardLogger logger = LoggerUtil.getStandardLogger();
                logger.info("Connection pool auto shut down after consecutive failures (detected by other servers): "
                        + getName());
                this.shutdown(false);
            }
        } else if (status.equals(STATUS_MANUAL_UP)) {
            if (isStarted()) return;
            consecutiveConnectionFailure = 0;
            StandardLogger logger = LoggerUtil.getStandardLogger();
            try {
                this.start();
                logger.info("Connection pool restarted manually: " + getName());
                this.processWaitingRequests(true);
            } catch (Exception e) {    // only possible when adapter class cannot be initialized
                logger.severeException("Failed to restart connection pool " + getName(), e);
            }
        } else if (status.equals(STATUS_MANUAL_DOWN)) {
            if (!isStarted()) return;
            StandardLogger logger = LoggerUtil.getStandardLogger();
            logger.info("Connection pool shutdown manually: " + getName());
            this.shutdown(false);
        }
    }

}
