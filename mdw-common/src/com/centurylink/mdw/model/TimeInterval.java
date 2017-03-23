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
package com.centurylink.mdw.model;

public class TimeInterval {
    private Integer secs;
    private Integer mins;
    private Integer hrs;
    private Integer days;
    private int totalSeconds;
    boolean normalize = false;
    boolean unset = false;

    public TimeInterval(int totalSeconds) {
        this.totalSeconds = totalSeconds;
    }
    
    private void initialize() {
      totalSeconds = 0;
      days = 0;
      hrs = 0;
      mins = 0;
      secs = 0;
    }
    
    /**
     * This method will always resolve the time into days, hrs, mins and seconds irrespective
     * of whether the user wants to see only the mins and seconds portion.
     */
    private void normalize() {
        if (totalSeconds < 0)
            return;
        days = totalSeconds / (3600 * 24) ;
        hrs = (totalSeconds - (days * 3600 * 24)) / 3600;
        mins = (totalSeconds - (days * 3600 * 24) - (hrs * 3600)) / 60;
        secs = totalSeconds - (days * 3600 * 24) - (hrs * 3600) - (mins * 60);
        normalize = true;
    }

    public Integer getTotalSeconds() {
        return totalSeconds;
    }

    public Integer getSecs() {
        if (!normalize)
            normalize();
        return secs;
    }

    public void setSecs(Integer secs) {
        if (!unset) {
            unset = true;
            initialize();
        }
        this.secs = secs;
        this.totalSeconds += (null != secs ? secs : 0);
    }

    public Integer getMins() {
        if (!normalize)
            normalize();
        return mins;
    }

    public void setMins(Integer mins) {
        if (!unset) {
            unset = true;
            initialize();
        }
        this.mins = mins;
        this.totalSeconds += (null != mins ? mins : 0) * 60;
    }

    public Integer getHrs() {
        if (!normalize)
            normalize();
        return hrs;
    }

    public void setHrs(Integer hrs) {
        if (!unset) {
            unset = true;
            initialize();
        }
        this.hrs = hrs;
        this.totalSeconds += (null != hrs ? hrs : 0) * 3600;
    }
    

    public Integer getDays() {
        if (!normalize)
            normalize();
        return days;
    }

    public void setDays(Integer days) {
        if (!unset) {
            unset = true;
            initialize();
        }
        this.days = days;
        this.totalSeconds += (null != days ? days : 0) * 24 * 3600;
    }
}
