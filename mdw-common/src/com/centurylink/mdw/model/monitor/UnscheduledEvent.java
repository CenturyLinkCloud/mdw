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

public class UnscheduledEvent extends ScheduledEvent {

    @Override
    public Date getScheduledTime() {
        return null;
    }

    public void setScheduledTime(Date scheduledTime) {
        throw new UnsupportedOperationException("Can't set scheduledTime for an unscheduled event");
    }

    @Override
    public int compareTo(ScheduledEvent o) {
        return getCreateTime().compareTo(o.getCreateTime());
    }
}
