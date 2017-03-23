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
