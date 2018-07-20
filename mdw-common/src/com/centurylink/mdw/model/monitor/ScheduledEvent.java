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
package com.centurylink.mdw.model.monitor;

import java.util.Date;


public class ScheduledEvent implements Comparable<ScheduledEvent> {
    
    public static final String SCHEDULED_JOB_PREFIX = "ScheduledJob.";
    public static final String INTERNAL_EVENT_PREFIX = "InternalEvent.";
    public static final String EXTERNAL_EVENT_PREFIX = "ExternalEvent.";    // replaced by special event. Keep for backward compatibility
    public static final String SPECIAL_EVENT_PREFIX = "SpecialEvent.";        // handled by special event handler

    private Date createTime;
    private Date scheduledTime;
    private String name;
    private String message;
    private String reference;

    public Date getScheduledTime() {
        return scheduledTime;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setScheduledTime(Date scheduledTime) {
        this.scheduledTime = scheduledTime;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    @Override
    public int compareTo(ScheduledEvent o) {
        return scheduledTime.compareTo(o.scheduledTime);
    }
    
    @Override
    public boolean equals(Object o) {
        if (o instanceof ScheduledEvent) return ((ScheduledEvent)o).name.equals(name);
        else return false;
    }
    
    public boolean isInternalEvent() {
        return name.startsWith(INTERNAL_EVENT_PREFIX);
    }
    
    public boolean isScheduledJob() {
        return name.startsWith(SCHEDULED_JOB_PREFIX);
    }
    
    public boolean isSpecialEvent() {
        return name.startsWith(SPECIAL_EVENT_PREFIX) || name.startsWith(EXTERNAL_EVENT_PREFIX);
    }
    
}
