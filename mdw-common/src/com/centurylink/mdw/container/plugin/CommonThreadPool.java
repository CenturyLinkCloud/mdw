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
package com.centurylink.mdw.container.plugin;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.container.ThreadPoolProvider;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

public class CommonThreadPool implements ThreadPoolProvider, CommonThreadPoolMXBean {

    private int max_threads;
    private int work_queue_size;
    private int termination_timeout;
    private int core_threads;
    private long keep_alive_time;

    private MyThreadPoolExecutor thread_pool;
    private Map<String,Worker> workers;
    private Worker defaultWorker;
    private List<ManagedThread> threadList;

    private StandardLogger logger;

    public CommonThreadPool() {
        this.max_threads = PropertyManager.getIntegerProperty(PropertyNames.MDW_THREADPOOL_MAX_THREADS, 10);
        this.core_threads = PropertyManager.getIntegerProperty(PropertyNames.MDW_THREADPOOL_CORE_THREADS, ((max_threads/2)>50?50:(max_threads/2)));
        this.work_queue_size = PropertyManager.getIntegerProperty(PropertyNames.MDW_THREADPOOL_QUEUE_SIZE, (max_threads>100?100:20));
        this.termination_timeout = PropertyManager.getIntegerProperty(PropertyNames.MDW_THREADPOOL_TERMINATION_TIMEOUT, 120);
        this.keep_alive_time = PropertyManager.getIntegerProperty(PropertyNames.MDW_THREADPOOL_KEEP_ALIVE, 300);
    }

    public void start() {
        logger = LoggerUtil.getStandardLogger();
        workers = new HashMap<String,Worker>();
        threadList = new ArrayList<ManagedThread>();
        loadWorker(WORKER_ENGINE);
        loadWorker(WORKER_LISTENER);
        loadWorker(WORKER_SCHEDULER);
        loadWorker(WORKER_MONITOR);
        adjustThreads();
        BlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<Runnable>(work_queue_size);
        ThreadFactory thread_factory = new MyThreadFactory();
        RejectedExecutionHandler rejectHandler = new MyRejectedExecutionHandler();
        thread_pool = new MyThreadPoolExecutor(core_threads, max_threads,
                keep_alive_time, TimeUnit.SECONDS, workQueue, thread_factory, rejectHandler);
        thread_pool.allowCoreThreadTimeOut(true);
    }

    public synchronized void stop() {
        if (thread_pool.isTerminating() || thread_pool.isTerminated()) return;
        thread_pool.shutdown();
        try {
            boolean good = thread_pool.awaitTermination(termination_timeout, TimeUnit.SECONDS);
            if (!good) logger.severe("JmsInternalMessageListener: thread pool fail to terminate after "
                    + termination_timeout + " seconds");
        } catch (InterruptedException e1) {
            logger.severeException("JmsInternalMessageListener: thread pool termination is interrupted", e1);
        }
    }

    private class Worker {
        String name;
        int minThreads;
        int maxThreads;
        int curThreads;
        int curWorks;   // curThreads + queued
        int totalSubmits;
        int totalRejects;

        Worker(String name) {
            this.name = name;
        }
    }

    private void loadWorker(String workerName) {
        Worker one = new Worker(workerName);
        one.minThreads = PropertyManager.getIntegerProperty(PropertyNames.MDW_THREADPOOL_WORKER+"."+one.name+".min_threads", 0);
        one.maxThreads = PropertyManager.getIntegerProperty(PropertyNames.MDW_THREADPOOL_WORKER+"."+one.name+".max_threads", (int)Math.floor((max_threads+work_queue_size)*0.9));
        one.curThreads = 0;
        one.curWorks = 0;
        one.totalSubmits = 0;
        one.totalRejects = 0;
        workers.put(one.name, one);
    }

    private void adjustThreads() {
        int totalMinThreads = 0;
        for (String wmname : workers.keySet()) {
            Worker one = workers.get(wmname);
            totalMinThreads += one.minThreads;
        }
        if (this.core_threads<totalMinThreads) {
            core_threads = totalMinThreads;
            logger.info("Adjust core threads to " + core_threads);
        }
        if (max_threads<core_threads) {
            max_threads = core_threads;
            logger.info("Adjust max threads to " + max_threads);
        }
        defaultWorker = new Worker("DefaultWorker");
        defaultWorker.minThreads = 0;
        defaultWorker.maxThreads = max_threads-core_threads;
        defaultWorker.curThreads = 0;
        defaultWorker.curWorks = 0;
    }

    private Worker getWorker(String workerName) {
        Worker worker = workers.get(workerName);
        if (worker == null)
            worker = defaultWorker;
        return worker;
    }

    public boolean hasAvailableThread(String workerName) {
        Worker worker = getWorker(workerName);
        return (worker.curWorks < worker.maxThreads &&
                (thread_pool.getPoolSize() < thread_pool.getMaximumPoolSize() ||
                 thread_pool.getQueue().remainingCapacity() > 0));
    }

    private synchronized void recordSubmit(Work work) {
        Worker worker = getWorker(work.workerName);
        worker.curWorks++;
        worker.totalSubmits++;
        if (logger.isTraceEnabled())
            logger.trace("+++Add work " + work.workerName + ": " + worker.curWorks);
    }

    private synchronized void recordReject(Work work) {
        Worker worker = getWorker(work.workerName);
        worker.curWorks--;
        worker.totalRejects++;
        logger.severe("+++Reject work " + work.workerName + ": " + worker.curWorks);
    }

    private synchronized void recordStart(Work work, ManagedThread thread) {
        Worker worker = getWorker(work.workerName);
        worker.curThreads++;
        work.thread = thread;
        thread.assignee = work.assignee;
        thread.assignTime = new Date();
        if (logger.isTraceEnabled())
            logger.trace("+++Start work " + work.workerName + ": " + worker.curThreads);
    }

    private synchronized void recordEnd(Work work) {
        Worker worker = getWorker(work.workerName);
        worker.curThreads--;
        worker.curWorks--;
        work.thread.assignee = null;
        work.thread.assignTime = null;
        work.thread = null;
        if (logger.isTraceEnabled())
            logger.trace("+++End work " + work.workerName + ": " + worker.curWorks + ", in processing " + worker.curThreads);
    }

    /* (non-Javadoc)
     * @see com.centurylink.mdw.container.plugins.TestInterface#execute(java.lang.String, java.lang.String, java.lang.Runnable)
     */
    public synchronized boolean execute(String workerName, String assignee, Runnable command) {
        if (!hasAvailableThread(workerName))
            return false;

        Work work = new Work(workerName, assignee, command);
        recordSubmit(work);
        thread_pool.execute(work);
        return true;
    }

    /* (non-Javadoc)
     * @see com.centurylink.mdw.container.plugins.TestInterface#pause()
     */
    public void pause() {
        thread_pool.pause();
    }

    /* (non-Javadoc)
     * @see com.centurylink.mdw.container.plugins.TestInterface#resume()
     */
    public void resume() {
        thread_pool.resume();
    }

    /* (non-Javadoc)
     * @see com.centurylink.mdw.container.plugins.TestInterface#isPaused()
     */
    public boolean isPaused() {
        return thread_pool.isPaused;
    }

    public class ManagedThread extends Thread {
        String assignee;
        Date assignTime;
        ManagedThread(Runnable runnable) {
            super(runnable);
//          this.setDaemon(true);
        }
        @Override
        public void run() {
            assignee = null;
            assignTime = null;
            threadList.add(this);
            super.run();
            threadList.remove(this);
        }
        public String getAssignee() {
            return assignee;
        }
        public Date getAssignTime() {
            return assignTime;
        }
    }

    private class MyThreadFactory implements ThreadFactory {
        public Thread newThread(Runnable r) {
            ManagedThread thread = new ManagedThread(r);
            thread.setName("CommonThread" + thread.getId());
            return thread;
        }
    }

    private class Work implements Runnable {
        Runnable command;
        String workerName;
        String assignee;
        ManagedThread thread;
        Work(String workerName, String assignee, Runnable command) {
            this.command = command;
            this.workerName = workerName;
            this.assignee = assignee;
            this.thread = null;
        }
        public void run() {
            command.run();
        }

    }

    private class MyRejectedExecutionHandler implements RejectedExecutionHandler {
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            recordReject((Work)r);
        }
    }

    private class MyThreadPoolExecutor extends ThreadPoolExecutor {

        private boolean isPaused;
        private ReentrantLock pauseLock = new ReentrantLock();
        private Condition unpaused = pauseLock.newCondition();

        MyThreadPoolExecutor(int core_threads, int max_threads, long keep_alive_time,
                TimeUnit time_unit, BlockingQueue<Runnable> workQueue,
                ThreadFactory threadFactory, RejectedExecutionHandler rejectHandler) {
            super(core_threads, max_threads, keep_alive_time, time_unit,
                    workQueue, threadFactory, rejectHandler);
        }

        @Override
        protected void beforeExecute(Thread t, Runnable r) {

            // TransactionUtil.clearCurrentConnection();
            // MdwTransactionManager.clearTransactionManager();

            super.beforeExecute(t, r);
            pauseLock.lock();
            try {
                while (isPaused) unpaused.await();
            } catch (InterruptedException ie) {
                t.interrupt();
            } finally {
                pauseLock.unlock();
            }
            recordStart((Work)r, (ManagedThread)t);
        }

        @Override
        protected void afterExecute(Runnable r, Throwable t) {
            if (t != null) {
                if (logger == null)
                    t.printStackTrace();
                else
                    logger.severeException(t.getMessage(), t);
            }
            recordEnd((Work)r);
            super.afterExecute(r, t);
        }

        void pause() {
            pauseLock.lock();
            try {
                isPaused = true;
            } finally {
                pauseLock.unlock();
            }
        }

        void resume() {
            pauseLock.lock();
            try {
                isPaused = false;
                unpaused.signalAll();
            } finally {
                pauseLock.unlock();
            }
        }

    }

    //
    // all the methods below are for management GUI and status display
    //

    /* (non-Javadoc)
     * @see com.centurylink.mdw.container.plugins.TestInterface#getThreadPoolExecutor()
     */
    public ThreadPoolExecutor getThreadPoolExecutor() {
        return thread_pool;
    }

    /* (non-Javadoc)
     * @see com.centurylink.mdw.container.plugins.TestInterface#getThreadList()
     */
    public List<ManagedThread> getThreadList() {
        return threadList;
    }

    /* (non-Javadoc)
     * @see com.centurylink.mdw.container.plugins.TestInterface#currentStatus()
     */
    public String currentStatus() {
        StringBuffer sb = new StringBuffer();
        sb.append("===== CommonThreadPool Status at ").append(new Date()).append(" =====\n");
        sb.append("Threads: current=").append(thread_pool.getPoolSize());
        sb.append(", core=").append(thread_pool.getCorePoolSize());
        sb.append(", max=").append(thread_pool.getMaximumPoolSize());
        sb.append(", active=").append(thread_pool.getActiveCount()).append("\n");
        BlockingQueue<Runnable> workQueue = thread_pool.getQueue();
        sb.append("Queue: current=").append(workQueue.size()).append("\n");
        sb.append("Works: total=").append(thread_pool.getTaskCount());
        sb.append(", completed=").append(thread_pool.getCompletedTaskCount()).append("\n");
        for (String name : workers.keySet()) {
            Worker worker = workers.get(name);
            sb.append(" - Worker ").append(name);
            sb.append(": current works=").append(worker.curWorks);
            sb.append(", current thread=").append(worker.curThreads);
            sb.append(", total submit=").append(worker.totalSubmits);
            sb.append(", total reject=").append(worker.totalRejects).append("\n");
        }
        Worker worker = defaultWorker;
        sb.append(" - Default Worker: ");
        sb.append(": current works=").append(worker.curWorks);
        sb.append(", current thread=").append(worker.curThreads);
        sb.append(", total submit=").append(worker.totalSubmits);
        sb.append(", total reject=").append(worker.totalRejects).append("\n");
        return sb.toString();
    }

    public int getCurrentThreadPoolSize() {
        return thread_pool.getPoolSize();
    }

    public int getCoreThreadPoolSize() {
        return thread_pool.getCorePoolSize();
    }

    public int getMaxThreadPoolSize() {
        return thread_pool.getMaximumPoolSize();
    }

    public int getActiveThreadCount() {
        return thread_pool.getActiveCount();
    }

    public int getCurrentQueueSize() {
        return thread_pool.getQueue().size();
    }

    public long getTaskCount() {
        return thread_pool.getTaskCount();
    }

    public long getCompletedTaskCount() {
        return thread_pool.getCompletedTaskCount();
    }

    public String workerInfo() {
        StringBuffer sb = new StringBuffer();
        sb.append("===== Workers ").append(new Date()).append(" =====\n");
        for (String name : workers.keySet()) {
            Worker worker = workers.get(name);
            sb.append(" - Worker ").append(name);
            sb.append(": current works=").append(worker.curWorks);
            sb.append(", current thread=").append(worker.curThreads);
            sb.append(", total submit=").append(worker.totalSubmits);
            sb.append(", total reject=").append(worker.totalRejects).append("\n");
        }
        return sb.toString();
    }

    public String defaultWorkerInfo() {
        StringBuffer sb = new StringBuffer();
        Worker worker = defaultWorker;
        sb.append("===== Default Worker").append(new Date()).append(" =====\n");
        sb.append("current works=").append(worker.curWorks);
        sb.append(", current thread=").append(worker.curThreads);
        sb.append(", total submit=").append(worker.totalSubmits);
        sb.append(", total reject=").append(worker.totalRejects).append("\n");
        return sb.toString();
    }

}