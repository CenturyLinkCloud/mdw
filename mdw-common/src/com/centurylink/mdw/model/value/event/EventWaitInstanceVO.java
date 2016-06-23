/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model.value.event;

import java.io.Serializable;

/**
 */


public class EventWaitInstanceVO implements Serializable{

    public static final long serialVersionUID = 1L;
    
    private Long activityInstanceId;
    private String completionCode;
    private Long messageDocumentId;

    public EventWaitInstanceVO(){
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
