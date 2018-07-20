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
package com.centurylink.mdw.model.workflow;

public interface TransitionStatus {

    public static final Integer STATUS_INITIATED = new Integer(1);
    public static final Integer STATUS_COMPLETED = new Integer(6);
    
    public static final Integer[] allStatusCodes = {STATUS_INITIATED, STATUS_COMPLETED};
    public static final String[] allStatusNames = {"Initiated", "Completed" };
    
}