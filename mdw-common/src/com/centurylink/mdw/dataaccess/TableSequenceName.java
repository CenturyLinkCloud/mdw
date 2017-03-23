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
package com.centurylink.mdw.dataaccess;

public class TableSequenceName {
    
    private String tableName;
    
    public TableSequenceName(String tableName) {
        this.tableName = tableName;
    }
    
    public String getSequenceName() {
        if (tableName.equalsIgnoreCase("EVENT_WAIT_INSTANCE"))
            return "EVENT_WAIT_INSTANCE_ID_SEQ";
        return "UKNOWN_SEQUENCE";
    }

}
