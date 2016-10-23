/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model.event;

import java.io.Serializable;

public class EventWaitInstance implements Serializable {

    private Long activityInstanceId;
    private String completionCode;
    private Long messageDocumentId;

    public EventWaitInstance(){
        messageDocumentId = null;
    }

    public Long getActivityInstanceId() {
        return activityInstanceId;
    }

    public void setActivityInstanceId(Long activityInstanceId) {
        this.activityInstanceId = activityInstanceId;
    }

    public String getCompletionCode() {
        return completionCode;
    }

    public void setCompletionCode(String completionCode) {
        this.completionCode = completionCode;
    }

    public Long getMessageDocumentId() {
        return messageDocumentId;
    }

    public void setMessageDocumentId(Long messageDocumentId) {
        this.messageDocumentId = messageDocumentId;
    }



}
