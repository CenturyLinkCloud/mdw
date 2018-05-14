package com.centurylink.mdw.microservice;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.constant.OwnerType;
import com.centurylink.mdw.model.variable.DocumentReference;
import com.centurylink.mdw.workflow.activity.event.PublishEventMessage;

public class ServiceEventPublish extends PublishEventMessage {

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
        DocumentReference docref = createDocument(String.class.getName(),
                eventMessage, OwnerType.INTERNAL_EVENT, this.getActivityInstanceId());
        loginfo("Publish message, event=" + eventName +
                ", id=" + docref.getDocumentId() + ", message=" + eventMessage);

        getEngine().notifyProcess(eventName, docref.getDocumentId(), eventMessage, delay);
    }
}
