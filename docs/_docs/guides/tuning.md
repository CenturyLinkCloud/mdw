---
permalink: /docs/guides/tuning/
title: Performance Tuning
---

## DB Cleanup Scheduled Job
  - TODO

## Database Connection Pool Size
  Some contributors to MDW database connection demand:
   1. The setting `threadpool.max_threads` is important, since each one of those threads will generally need an active 
      database connection (except threads running a processes at Performance Level 9, see "Performance Levels" below).
   2. Service Processes invoked through a REST or SOAP service call are executed in Tomcat's thread pool, and these can each spawn 
      process instances that also require database connections.
   3. MDWHub UI sessions can also briefly require a connection in order to at least persist the incoming 
      request in the DB, among other things depending on the nature of the interaction.

   While threads do share connections through the pool, not having a readily available connection can slow the system down considerably 
   (flows cannot make progress while waiting for a DB connection).  Theoretically, you'd size the connection pool by adding the maximum Tomcat threads
   to the maximum MDW threads (e.g. Tomcat(200) + MDW(400) = 600 DB pool size).  Realistically, each connection requires server resources, so this might 
   not be feasible considering the number of MDW instances your application has.
   
   Just keep in mind that having a max_threads setting of 500 won't generally do you much good if you set your database.poolsize to 10, since your 500 threads 
   will have to take turns using the 10 DB connections.

    - An additional setting you want to look at is mdw.database.poolMaxIdle. You don't want this set too low based on your load because that will make the 
      system waste a lot of time constantly closing and re-opening DB connections, which takes a lot more time to do than re-using an open connection.
      A connection is considered "idle" if it is returned to the connection pool and there isn't a thread already waiting in queue for a connection 
      (i.e. there is NO grace period before the connection is considered to be idle).   A value less than 5 would be too low in almost all production scenarios.    

## ActiveMQ Broker Configuration
  - TODO

  Possibilities to consider:
  - Prevent Creating a Separate Transport Thread
    If you don't require Async delivery of your JMS messages you can configure the VM transport to suppress 
    creation of a separate worker thread by adding "async=false" to your connector URL (http://activemq.apache.org/uri-protocols.html).

  - Employ ActiveMQ Thread Pooling
    You can tell ActiveMQ to use an internal thread pool to control dispatching of messages 
    by adding "-Dorg.apache.activemq.UseDedicatedTaskRunner" to ACTIVEMQ_OPTS.  Also there is an option to set a destination 
    policy on the queue with "optimizedDispatch=true" to avoid spawning a separate session thread for delivering the message to the listener.

  - Enable Async Index Writing in KahaDB - To minimize file I/O bottlenecs with the ActiveMQ persistent store you can set "enableIndexWriteAsync=true" in your 
    broker configuration (http://activemq.apache.org/kahadb.html).

## Non-Service Processes
  When a process is launched, MDW puts a message on its internal JMS queue, which uses ActiveMQ with the VM transport.  According to the ActiveMQ documentation, 
  "Using the VM transport to connect to an in-JVM broker is the fastest and most efficient transport you can use."  To execute the process flow, the MDW JMS 
  Listener receives the launch message from the queue and processes it using the MDW CommonThreadPool, which is built on the 
  [JavaEE Concurrency API](https://javaee.github.io/tutorial/concurrency-utilities.html).

  To avoid exhausting the MDW thread pool, part of your performance tuning exercise should include drastically increasing the pool size from its default value 
  of 10 (see [Configuration](../configuration/).  For high-volume systems a reasonable starting number is 100.  The tradeoff with this is application 
  memory footprint.  So for maximum throughput the optimal setting for thread pool size is the highest value that avoids excessive memory consumption.

## Service Processes
  If a service process is launched through HTTP, the initial thread pool for the master process is configured through the Tomcat servlet container.
  However, even within a service process, if an InvokeHeterogeneousProcessActivity is executed with Force Parallel Execution configured, this spawns a separate thread 
  for each subprocess instance using the MDW CommonThreadPool, which uses Java thread pooling:
  <http://docs.oracle.com/javase/6/docs/api/java/util/concurrent/ThreadPoolExecutor.html>.  The Tomcat thread that spawned the Heterogeneous process launch will 
  block until all the Subprocesses are completed.

  Here's a good article on Thread Pooling in general (see the section on "Tuning the pool size"):
  <http://www.ibm.com/developerworks/library/j-jtp0730/index.html>

## Performance Levels
  - **1**: All process, activity, transition, and variable instances as well as documents are persisted in database.  No in-memory cache is used. 
  - **3**: All process, activity, transition, and variable instances as well as documents are persisted in database, but variable instances and documents 
    are cached for each engine processing session to speed up read accesses. 
  - **5**: This is for service processes only. All process, activity, and transition instances are persisted in database; variable instances and documents 
    are created in memory cache only, not persisted in database. 
  - **9**: All process, activity, transition and variable instances as well as documents are stored in memory cache only. No database persistence is used. 
    For service processes, one memory cache is used for each top level invocation to a service process; for regular processes, a shared memory cache is used 
    for all executions at this performance level. 

## Timer Delay Threshold
  A very important factor in MDW processing is that mdw.timer.ThresholdForDelay MUST NOT be set to zero.  
  This forces all delays to be scheduled in the database, including the 1-2 second delays that are scheduled internally.  
  If these rely on database polling to trigger the events then this can be a serious detriment to performance and can lead to backlogs under load.  
  The minimum value for this threshold should be on the order of 5 minutes.  Some teams have mistakenly set this to zero to enable timer wait functionality,
  but instead scheduler support should be enabled in ActiveMQ (<http://activemq.apache.org/delay-and-schedule-message-delivery.html>).

## MDW Internal Events Recovery
  The UnscheduledEventsMonitor periodically checks for internal events that have not been processed, and will process those that are older than a designated age.
  The maximum number of events that will be processed per cycle are specified in the batch size.  The settings for controlling this are under unscheduled.events
  in the [Config Guide](../config).

  **NOTE**: For high volume/load applications, it could be beneficial in certain cases for the unscheduled.events/max.batch.size value to be set to a
   number that is lower than the mdw.threadpool.max_threads value so as to NOT exceed the number of available threads.
