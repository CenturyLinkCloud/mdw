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
