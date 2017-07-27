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
package com.centurylink.mdw.services.event;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import com.centurylink.mdw.cache.CacheService;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.model.monitor.CertifiedMessage;
import com.centurylink.mdw.services.EventManager;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.cache.CacheRegistration;
import com.centurylink.mdw.util.StringHelper;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

/**
 * The start up class that monitors the process state and raises events
 *
 */
public class CertifiedMessageManager implements CacheService {

    private static CertifiedMessageManager singleton = null;

    /**
     * The following 3 properties can be specified in 3 or 4 ways. The following
     * is the precedence of the specification used:
     * a) The property values specified in individual sendTextMessage request:
     *             CertifiedMessage.PROP_*
     * b) Connection pool only - the property values specified for poolable adapter
     *             AdapterActivity.PROP_*
     * c) Global properties PropertyNames.MDW_CERTIFIED_MESSAGE_*
     * d) Hard coded values (timeout = 15, retry interval = 600, max retries = 12
     */
    private int DEFAULT_RETRY_INTERVAL;        // default retry interval in seconds
    private int DEFAULT_ACK_TIMEOUT;        // default acknowledgment timeout in seconds
    private int DEFAULT_MAX_TRIES;            // maximum tries

    private PriorityQueue<CertifiedMessage> queue;

    private CertifiedMessageManager() {
        queue = new PriorityQueue<CertifiedMessage>();
        DEFAULT_ACK_TIMEOUT = PropertyManager.getIntegerProperty(PropertyNames.MDW_CERTIFIED_MESSAGE_ACK_TIMEOUT, 15);
        DEFAULT_RETRY_INTERVAL = PropertyManager.getIntegerProperty(PropertyNames.MDW_CERTIFIED_MESSAGE_RETRY_INTERVAL, 600);
        DEFAULT_MAX_TRIES = PropertyManager.getIntegerProperty(PropertyNames.MDW_CERTIFIED_MESSAGE_MAX_TRIES, 12);
    }

    private synchronized void load() {
        try {
            EventManager eventManager = ServiceLocator.getEventManager();
            List<CertifiedMessage> msglist = eventManager.getCertifiedMessageList();
            queue.addAll(msglist);
        }
        catch (Exception ex) {
            StandardLogger logger = LoggerUtil.getStandardLogger();
            logger.severeException(ex.getMessage(), ex);
        }
    }

    public synchronized void retryAll() {
        Date now = new Date(DatabaseAccess.getCurrentTime());
        CertifiedMessage message = getNextReadyMessage(now);
        while (message!=null) {
            deliverMessage(message);
            message = getNextReadyMessage(now);
        }
    }

    private synchronized CertifiedMessage getNextReadyMessage(Date now) {
        CertifiedMessage message = queue.peek();
        if (message!=null && message.getNextTryTime().compareTo(now)<0) {
            return queue.poll();
        } else return null;
    }

    private synchronized void addToQueue(CertifiedMessage message) {
        queue.offer(message);
    }

    public void sendTextMessage(Map<String,String> properties, String content, Long documentId, String reference)
            throws DataAccessException {
        CertifiedMessage message = new CertifiedMessage();
        Date now = new Date(DatabaseAccess.getCurrentTime());
        message.setInitiateTime(now);
        message.setProperties(properties);
        message.setDocumentId(documentId);
        message.setContent(content);
        message.setTryCount(0);
        message.setReference(reference);
        recordMessage(message);        // let it throw back exception when fails to record the message
        boolean noInitialSend = "true".equalsIgnoreCase(message.getProperty(CertifiedMessage.PROP_NO_INITIAL_SEND));
        if (noInitialSend) {
            message.setNextTryTime(now);
            this.addToQueue(message);
        } else {
            // first try is synchronous, and logs detailed exception if fails
            deliverMessage(message);
        }
    }

    private void deliverMessage(CertifiedMessage message) {
        try {
            EventManager eventManager = ServiceLocator.getEventManager();
            int timeout = StringHelper.getInteger(message.getProperty(CertifiedMessage.PROP_TIMEOUT), DEFAULT_ACK_TIMEOUT);
            int maxTries = StringHelper.getInteger(message.getProperty(CertifiedMessage.PROP_MAX_TRIES), DEFAULT_MAX_TRIES);
            int retryInterval = StringHelper.getInteger(message.getProperty(CertifiedMessage.PROP_RETRY_INTERVAL), DEFAULT_RETRY_INTERVAL);
            boolean needRetry = eventManager.deliverCertifiedMessage(message, timeout, maxTries, retryInterval);
             if (needRetry) {
                this.addToQueue(message);
            }
        } catch (Exception e) {
            // only when EJB connection fails - should never happen
            int retryInterval = StringHelper.getInteger(message.getProperty(CertifiedMessage.PROP_RETRY_INTERVAL), DEFAULT_RETRY_INTERVAL);
             message.setNextTryTime(new Date(DatabaseAccess.getCurrentTime()+retryInterval*1000));
             this.addToQueue(message);
        }
    }

    private void recordMessage(CertifiedMessage message)
            throws DataAccessException {
        try {
            EventManager eventManager = ServiceLocator.getEventManager();
            eventManager.recordCertifiedMessage(message);
        } catch (Exception e) {
            String errmsg = "Failed to record certified message " + message.getId();
            throw new DataAccessException(-1, errmsg, e);
        }
    }

    public synchronized void resumeConnectionPoolMessages(String poolname) {
        try {
            EventManager eventManager = ServiceLocator.getEventManager();
            List<CertifiedMessage> msglist = eventManager.getCertifiedMessageList();
            Date now = new Date(DatabaseAccess.getCurrentTime()+5000);    // wait for pool to start
            for (CertifiedMessage one : msglist) {
                Map<String,String> props = one.getProperties();
                if (CertifiedMessage.PROTOCOL_POOL.equals(props.get(CertifiedMessage.PROP_PROTOCOL))
                        && poolname.equals(props.get(CertifiedMessage.PROP_POOL_NAME))) {
                    if (!queue.contains(one)) {
                        one.setNextTryTime(now);
                        queue.offer(one);
                    }
                }
            }
        }
        catch (Exception ex) {
            StandardLogger logger = LoggerUtil.getStandardLogger();
            logger.severeException(ex.getMessage(), ex);
        }
    }

    public void clearCache() {
        queue.clear();
    }

    public synchronized void refreshCache() {
        queue.clear();
        load();
    }

    public static CertifiedMessageManager getSingleton() {
        if (singleton==null) {
            singleton = new CertifiedMessageManager();
            singleton.load();
            (new CacheRegistration()).registerCache(CertifiedMessageManager.class.getName(), singleton);
        }
        return singleton;
    }

}
