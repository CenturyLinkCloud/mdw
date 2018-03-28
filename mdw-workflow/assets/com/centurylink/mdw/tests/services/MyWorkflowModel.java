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
package com.centurylink.mdw.tests.services;

public class MyWorkflowModel implements java.io.Serializable {

    private String flowmaster;
    public String getFlowmaster() { return flowmaster; }
    public void setFlowmaster(String fm) { this.flowmaster = fm; }

    @Override
    public boolean equals(Object other) {
        return other instanceof MyWorkflowModel &&
                ((MyWorkflowModel)other).flowmaster.equals(flowmaster);
    }

    @Override
    public String toString() {
        return "flowmaster: " + flowmaster;
    }
}
