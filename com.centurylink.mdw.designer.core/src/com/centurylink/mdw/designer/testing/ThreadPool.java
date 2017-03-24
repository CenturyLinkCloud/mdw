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
package com.centurylink.mdw.designer.testing;

import java.util.Collection;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ThreadPool implements ExecutorService {
    
    private Vector<PooledThread> activeThreads;
    private Vector<Runnable> waitingRunnables;
    private boolean isTerminating;
    private int max_threads;
    private long keepAliveTime = 0;

    public ThreadPool(int max_threads) {
        this.max_threads = max_threads;
        activeThreads = new Vector<PooledThread>(max_threads);
        waitingRunnables = new Vector<Runnable>();
        isTerminating = false;
    }
    
    private class PooledThread extends Thread {
        private Runnable obj;
        private PooledThread(Runnable obj) {
            this.obj = obj;
            this.setName("pool-thread-" + this.getId());
        }
        public void run() {
            while (obj!=null) {
                obj.run();
                synchronized (waitingRunnables) {
                    if (waitingRunnables.size()>0) {
                        obj = waitingRunnables.remove(0);
                    } else {
                        obj = null;
                        if (keepAliveTime>0 && !isTerminating) {
                        	try {
								waitingRunnables.wait(keepAliveTime);
								if (waitingRunnables.size()>0) {
			                        obj = waitingRunnables.remove(0);
								}
							} catch (InterruptedException e) {
								// exit?
							}
                        }
                    }
                }
            }
            synchronized (activeThreads) {
            	activeThreads.remove(PooledThread.this);
                activeThreads.notifyAll();
            }
        }
    }
    
    public void execute(Runnable obj) {
    	if (isTerminating) return;
    	PooledThread oneThread ;
        synchronized (activeThreads) {
            if (activeThreads.size()<max_threads) {
            	oneThread = new PooledThread(obj);
            	activeThreads.add(oneThread);
            } else oneThread = null;
        }
        if (oneThread==null) {
        	synchronized (waitingRunnables) {
                waitingRunnables.add(obj);
                waitingRunnables.notify();
        	}
        } else {
            oneThread.start();
        }
    }
    
    public void awaitTermination() {
        synchronized (activeThreads) {
            while (activeThreads.size()>0) {
                try {
                    activeThreads.wait();
                } catch (InterruptedException e) {
                }
            }
        }
    }
    
    public void shutdown() {
    	isTerminating = true;
    }
    
    public boolean isTerminated() {
    	if (!isTerminating) return false;
        synchronized (activeThreads) {
            return activeThreads.size()==0;
        }
    }
    
	public boolean isShutdown() {
		return isTerminating;
	}

	public boolean awaitTermination(long timeout, TimeUnit unit)
			throws InterruptedException {
		if (!isTerminating) return false;
		long elapse = 0;
		long toWait = unit.convert(timeout, TimeUnit.MILLISECONDS);
		while (!isTerminated()) {
			if (elapse>=toWait) return false;
			elapse += 2000;
			Thread.sleep(2000);
     	}
		return true;
	}
    
    public void stop() {
    	isTerminating = true;
    	synchronized (waitingRunnables) {
            waitingRunnables.removeAllElements();    		
    	}
        synchronized (activeThreads) {
            for (PooledThread thread : activeThreads) {
                thread.interrupt();
                if (thread.obj instanceof Threadable) {
                	((Threadable)thread.obj).stop();
                }
            }
            activeThreads.clear();
        }
    }
    
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
		throws InterruptedException {
    	return null;		// not used
	}
	
	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks,
		long timeout, TimeUnit unit) throws InterruptedException {
		return null;		// not used
	}
	
	public <T> T invokeAny(Collection<? extends Callable<T>> tasks)
		throws InterruptedException, ExecutionException {
		return null;		// not used
	}
	
	public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout,
		TimeUnit unit) throws InterruptedException, ExecutionException,
		TimeoutException {
		return null;		// not used
	}
	
	public List<Runnable> shutdownNow() {
	return null;		// not used
	}
	
	public <T> Future<T> submit(Callable<T> task) {
	return null;		// not used
	}
	
	public Future<?> submit(Runnable task) {
	return null;		// not used
	}
	
	public <T> Future<T> submit(Runnable task, T result) {
	return null;		// not used
	}


}
