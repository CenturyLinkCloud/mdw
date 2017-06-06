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
package com.centurylink.mdw.microservice;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.constant.OwnerType;
import com.centurylink.mdw.event.BroadcastEventLockCache;
import com.centurylink.mdw.model.variable.DocumentReference;
import com.centurylink.mdw.workflow.activity.event.PublishEventMessage;

public class ServiceSummaryEventPublish extends PublishEventMessage {

    @Override
    public void execute() throws ActivityException {
        try {
            publish(getEventName(), getEventMessage(), getEventDelay());
        }
        catch (Exception ex) {
            logexception(ex.getMessage(), ex);
            throw new ActivityException(-1, "Failed to publish event message", ex);
        }
    }

    protected void publish(String eventName, String eventMessage, int delay) throws Exception {
        DocumentReference docref = this.createDocument(String.class.getName(),
                eventMessage, OwnerType.INTERNAL_EVENT, this.getActivityInstanceId());
        loginfo("Publish message, event=" + eventName +
                ", id=" + docref.getDocumentId() + ", message=" + eventMessage);

        BroadcastEventLockCache.lock(eventName);
        getEngine().notifyProcess(eventName, docref.getDocumentId(), eventMessage, delay);
        BroadcastEventLockCache.unlock(eventName);
    }
}
