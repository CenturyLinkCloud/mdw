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
package com.centurylink.mdw.services.pooling;

import java.util.Date;

/**
 * An instance of this class represents a connection in a connection pool.
 * 
 * 
 *
 */
public abstract class PooledConnection {
    
    private int id;
    private String assignee;
    private Date assignTime;
    
    /**
     * @throws Exception
     */
    public PooledConnection() {
        this.assignee = null;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public int getId() {
        return id;
    }
    
    public Date getAssignTime() {
        return assignTime;
    }
    
    public void setAssignTime(Date v) {
        assignTime = v;
    }
    
    public String getAssignee() {
        return assignee;
    }
    
    public void setAssignee(String v) {
        assignee = v;
    }
    
    abstract public void destroy();

}
