/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.util;

import java.io.Serializable;
import java.util.Map;

/**
 * POJO that is agnostic towards Rabbit or ActiveMQ
 * @author aa70413
 *
 */
public class InternalEventMessageHandler implements MessageHandler {

    /* (non-Javadoc)
     * @see com.centurylink.mdw.common.utilities.MessageHandler#handleMessage(java.lang.String)
     */
    @Override
    public void handleMessage(String message) {

    }

    /* (non-Javadoc)
     * @see com.centurylink.mdw.common.utilities.MessageHandler#handleMessage(java.util.Map)
     */
    @Override
    public void handleMessage(Map<?, ?> message) {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see com.centurylink.mdw.common.utilities.MessageHandler#handleMessage(byte[])
     */
    @Override
    public void handleMessage(byte[] message) {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see com.centurylink.mdw.common.utilities.MessageHandler#handleMessage(java.io.Serializable)
     */
    @Override
    public void handleMessage(Serializable message) {
        // TODO Auto-generated method stub

    }

}
