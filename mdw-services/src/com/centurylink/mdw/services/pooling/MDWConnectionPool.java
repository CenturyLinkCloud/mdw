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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;

public abstract class MDWConnectionPool {

    private GenericObjectPool pool;
    private String name;
    private List<PooledConnection> connections;
    private int next_id;
    private boolean started;
    private int pool_size;
    private Integer maxConnectionRequests;
    private Integer totalConnectionRequests;
    private int borrow_timeout;
    
    public MDWConnectionPool(String name) {
        this.name = name;
        connections = new ArrayList<PooledConnection>();
        started = false;
        maxConnectionRequests = 0;
        totalConnectionRequests = 0;
        next_id = 1;
        pool = null;
        pool_size = 1;
        borrow_timeout = 0;
    }
    
    /**
     * This can be overriden to set pool size and borrow timeout
     * for each start/restart.
     * The super method must be called at the end of the overriding method.
     * @throws Exception
     */
    public synchronized void start() throws Exception {
        if (pool==null) {
            pool = new GenericObjectPool(new MDWPoolFactory());
        }
        pool.setMaxActive(pool_size);
        if (borrow_timeout<0) {
            pool.setWhenExhaustedAction(GenericObjectPool.WHEN_EXHAUSTED_BLOCK);
            pool.setMaxWait(-1);
        } else if (borrow_timeout==0) {
            pool.setWhenExhaustedAction(GenericObjectPool.WHEN_EXHAUSTED_FAIL);
            pool.setMaxWait(0);
        } else {
            pool.setWhenExhaustedAction(GenericObjectPool.WHEN_EXHAUSTED_BLOCK);
            pool.setMaxWait(borrow_timeout*1000);
        }
        setStarted(true);
    }

    public String getName() {
        return name;
    }
    
    public List<PooledConnection> getConnectionList() {
        return connections;
    }
    
    public boolean isStarted() {
        return started;
    }
    
    public void setStarted(boolean v) {
        started = v;
    }
    
    public int getPoolSize() {
        return pool_size;
    }

    protected void setPoolSize(int pool_size) {
        this.pool_size = pool_size;
    }
    
    protected void setBorrowTimeout(int v) {
        this.borrow_timeout = v;
    }

    public synchronized void destroyIdleConnections() throws Exception {
        pool.clear();
    }
    
    public int getMaxConnectionRequests() {
        return this.maxConnectionRequests;
    }
    
    protected void recordMaxConnectionRequests(int v) {
        synchronized (maxConnectionRequests) {
            if (v>maxConnectionRequests) maxConnectionRequests = v;
        }
    }
    
    protected void recordTotalConnectionRequest() {
        synchronized (totalConnectionRequests) {
            totalConnectionRequests ++;
        }
    }
    
    public int getTotalConnectionRequests() {
        return this.totalConnectionRequests;
    }
    
    protected int getNumActive() {
        return pool.getNumActive();
    }
        
    synchronized public void shutdown(boolean finalShutDown) {
        setStarted(false);
        if (finalShutDown) {    // cannot be restarted after this
            try {
                pool.close();
            } catch (Exception e) {
            }
        }
    }
    
    public PooledConnection borrowObject(String assignee) throws Exception {
        PooledConnection conn = (PooledConnection)pool.borrowObject();
        conn.setAssignee(assignee);
        conn.setAssignTime(new Date());
        return conn;
    }
    
    public void returnObject(PooledConnection conn) throws Exception {
        pool.returnObject(conn);
        conn.setAssignee(null);
        conn.setAssignTime(null);
    }

    public void invalidateObject(PooledConnection conn) throws Exception {
        pool.invalidateObject(conn);
        conn.setAssignee(null);
        conn.setAssignTime(null);
    }
    
    abstract public PooledConnection makeConnection() throws Exception;
    
    private class MDWPoolFactory implements PoolableObjectFactory {
        public void destroyObject(Object arg0) throws Exception {
            synchronized (connections) {
                connections.remove(arg0);
            }
            ((PooledConnection)arg0).destroy();
        }
        public Object makeObject() throws Exception {
            PooledConnection conn = makeConnection();
            synchronized (connections) {
                conn.setId(next_id++);
                connections.add(conn);
            }
            return conn;
        }
        public void activateObject(Object arg0) throws Exception {}
        public void passivateObject(Object arg0) throws Exception {}
        public boolean validateObject(Object arg0) { return true; }
    }
    
}
