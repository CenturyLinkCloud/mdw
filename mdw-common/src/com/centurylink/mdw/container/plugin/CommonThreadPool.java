package com.centurylink.mdw.container.plugin;

import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.container.ThreadPoolProvider;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class CommonThreadPool implements ThreadPoolProvider, CommonThreadPoolMXBean {

    private int maxThreads;
    private int workQueueSize;
    private int terminationTimeout;
    private int coreThreads;
    private long keepAliveTime;

    private MyThreadPoolExecutor threadPool;
    private Map<String,Worker> workers;
    private Worker defaultWorker;
    private List<ManagedThread> threadList;

    private StandardLogger logger;

    public CommonThreadPool() {
        this.maxThreads = PropertyManager.getIntegerProperty(PropertyNames.MDW_THREADPOOL_MAX_THREADS, 10);
        this.coreThreads = PropertyManager.getIntegerProperty(PropertyNames.MDW_THREADPOOL_CORE_THREADS, ((maxThreads / 2) > 50 ? 50 : (maxThreads / 2)));
        this.workQueueSize = PropertyManager.getIntegerProperty(PropertyNames.MDW_THREADPOOL_QUEUE_SIZE, (maxThreads > 100 ? 100 : 20));
        this.terminationTimeout = PropertyManager.getIntegerProperty(PropertyNames.MDW_THREADPOOL_TERMINATION_TIMEOUT, 60);
        this.keepAliveTime = PropertyManager.getIntegerProperty(PropertyNames.MDW_THREADPOOL_KEEP_ALIVE, 300);
    }

    public void start() {
        logger = LoggerUtil.getStandardLogger();
        workers = new HashMap<>();
        threadList = new ArrayList<>();
        loadWorker(WORKER_ENGINE);
        loadWorker(WORKER_LISTENER);
        loadWorker(WORKER_SCHEDULER);
        loadWorker(WORKER_MONITOR);
        adjustThreads();
        BlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<>(workQueueSize);
        ThreadFactory threadFactory = new MyThreadFactory();
        RejectedExecutionHandler rejectHandler = new MyRejectedExecutionHandler();
        threadPool = new MyThreadPoolExecutor(coreThreads, maxThreads,
                keepAliveTime, TimeUnit.SECONDS, workQueue, threadFactory, rejectHandler);
        threadPool.allowCoreThreadTimeOut(true);
    }

    public void stop() {
        logger.info("Waiting for Common threads to finish processing...");
        synchronized (this) {
            if (threadPool.isTerminating() || threadPool.isTerminated())
                return;
            threadPool.shutdown();
        }
        try {
            if (!threadPool.awaitTermination(terminationTimeout, TimeUnit.SECONDS)) {
                logger.error("Thread pool fails to terminate after " + terminationTimeout + " seconds");
            }
        } catch (InterruptedException ex) {
            logger.error("Thread pool interrupted awaiting termination", ex);
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
        one.minThreads = PropertyManager.getIntegerProperty(PropertyNames.MDW_THREADPOOL_WORKER + "." + one.name + ".min_threads", 0);
        one.maxThreads = PropertyManager.getIntegerProperty(PropertyNames.MDW_THREADPOOL_WORKER + "." + one.name + ".max_threads", (int)Math.floor((maxThreads + workQueueSize) * 0.9));
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
        if (coreThreads < totalMinThreads) {
            coreThreads = totalMinThreads;
            logger.info("Adjust core threads to " + coreThreads);
        }
        if (maxThreads < coreThreads) {
            maxThreads = coreThreads;
            logger.info("Adjust max threads to " + maxThreads);
        }
        defaultWorker = new Worker("DefaultWorker");
        defaultWorker.minThreads = 0;
        defaultWorker.maxThreads = maxThreads - coreThreads;
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
                (threadPool.getPoolSize() < threadPool.getMaximumPoolSize() ||
                 threadPool.getQueue().remainingCapacity() > 0));
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
        logger.error("+++Reject work " + work.workerName + ": " + worker.curWorks);
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

    public synchronized boolean execute(String workerName, String assignee, Runnable command) {
        if (!hasAvailableThread(workerName))
            return false;

        Work work = new Work(workerName, assignee, command);
        recordSubmit(work);
        threadPool.execute(work);
        return true;
    }

    public void pause() {
        threadPool.pause();
    }

    public void resume() {
        threadPool.resume();
    }

    @Override
    public boolean isPaused() {
        return threadPool.isPaused;
    }

    public class ManagedThread extends Thread {
        String assignee;
        Date assignTime;
        ManagedThread(Runnable runnable) {
            super(runnable);
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

            super.beforeExecute(t, r);
            pauseLock.lock();
            try {
                while (isPaused)
                    unpaused.await();
            } catch (InterruptedException ie) {
                t.interrupt();
            } finally {
                pauseLock.unlock();
            }
            recordStart((Work)r, (ManagedThread)t);
        }

        @Override
        protected void afterExecute(Runnable r, Throwable t) {
            super.afterExecute(r, t);
            if (t != null) {
                if (logger == null)
                    t.printStackTrace();
                else
                    logger.error(t.getMessage(), t);
            }
            recordEnd((Work)r);
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
    // methods below are for management and status display
    //

    public ThreadPoolExecutor getThreadPoolExecutor() {
        return threadPool;
    }

    public List<ManagedThread> getThreadList() {
        return threadList;
    }

    public String currentStatus() {
        StringBuffer sb = new StringBuffer();
        sb.append("===== CommonThreadPool Status at ").append(new Date()).append(" =====\n");
        sb.append("Threads: current=").append(threadPool.getPoolSize());
        sb.append(", core=").append(threadPool.getCorePoolSize());
        sb.append(", max=").append(threadPool.getMaximumPoolSize());
        sb.append(", active=").append(threadPool.getActiveCount()).append("\n");
        BlockingQueue<Runnable> workQueue = threadPool.getQueue();
        sb.append("Queue: current=").append(workQueue.size()).append("\n");
        sb.append("Works: total=").append(threadPool.getTaskCount());
        sb.append(", completed=").append(threadPool.getCompletedTaskCount()).append("\n");
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
        return threadPool.getPoolSize();
    }

    public int getCoreThreadPoolSize() {
        return threadPool.getCorePoolSize();
    }

    public int getMaxThreadPoolSize() {
        return threadPool.getMaximumPoolSize();
    }

    public int getActiveThreadCount() {
        return threadPool.getActiveCount();
    }

    public int getCurrentQueueSize() {
        return threadPool.getQueue().size();
    }

    public long getTaskCount() {
        return threadPool.getTaskCount();
    }

    public long getCompletedTaskCount() {
        return threadPool.getCompletedTaskCount();
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