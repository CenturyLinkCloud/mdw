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
package com.centurylink.mdw.listener.jms;

import javax.jms.JMSException;
import javax.jms.TextMessage;

import com.centurylink.mdw.constant.JMSDestinationNames;
import com.centurylink.mdw.container.ThreadPoolProvider;
import com.centurylink.mdw.services.process.InternalEventDriver;

public class InternalEventListener extends JmsListener {

    public InternalEventListener(ThreadPoolProvider thread_pool) {
        super(ThreadPoolProvider.WORKER_ENGINE,
                JMSDestinationNames.PROCESS_HANDLER_QUEUE, thread_pool);
    }

    protected Runnable getProcesser(TextMessage message) throws JMSException {
        String msgid = message.getJMSCorrelationID();
        return new InternalEventDriver(msgid, message.getText());
    }
}