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
package com.centurylink.mdw.tests.workflow;

/**
 * Dynamic Java workflow asset.
 */
public class TimerBean implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    private int timerDelaySeconds;
    /**
     * Does a calculation simulating dueDate calc.
     */
    public int getTimerDelaySeconds() {
        timerDelaySeconds -= 100;
        return timerDelaySeconds;
    }
    public void setTimerDelaySeconds(int delaySeconds) {
        this.timerDelaySeconds = delaySeconds;
    }

    public String toString() {
        return "{ timerDelaySeconds: " + timerDelaySeconds + " }";
    }

    public boolean equals(Object other) {
        return toString().equals(other.toString());
    }
}
