/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.filepanel;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.List;
import java.util.Map;

public class Tools
{
  private String _results;
  public String getResults() { return _results; }

  private int _lineCount;
  public int getLineCount() { return _lineCount; }

  public void performAction(String action)
  {
    _results = "";

    if (action == null)
      return;
    else if (action.equals("stackDump"))
      _results = stackDump();
    else if (action.equals("memoryInfo"))
      _results = memoryInfo();
  }

  private String stackDump()
  {
    String dump = "";
    Map<Thread, StackTraceElement[]> threads = Thread.getAllStackTraces();
    StringBuffer output = new StringBuffer();
    _lineCount = 3;
    output.append("Total Threads: ").append(threads.size()).append("\n------------------\n\n");
    for (Thread thread : threads.keySet())
    {
      output.append(thread.getName()).append(" (");
      output.append("priority=").append(thread.getPriority()).append(" ");
      output.append("group=").append(thread.getThreadGroup()).append(" ");
      output.append("state=").append(thread.getState()).append(" ");
      output.append("id=").append(thread.getId());
      output.append("):\n");
      _lineCount++;
      StackTraceElement[] elements = threads.get(thread);
      if (elements != null)
      {
        for (StackTraceElement element : elements)
        {
          output.append("\tat ").append(element).append("\n");
          _lineCount++;
        }
      }
      output.append("\n");
      _lineCount++;

      try
      {
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        if (threadBean.isThreadCpuTimeSupported() && threadBean.isThreadCpuTimeEnabled())
        {
          output.append("Thread CPU Time (ms): " + threadBean.getThreadCpuTime(thread.getId()) + "\n");
          output.append("Thread User Time (ms): " + threadBean.getThreadUserTime(thread.getId()) + "\n");
          _lineCount += 2;
        }
        ThreadInfo threadInfo = threadBean.getThreadInfo(thread.getId());
        if (threadInfo != null)
        {
          if (threadBean.isThreadContentionMonitoringSupported() && threadBean.isThreadContentionMonitoringEnabled())
          {
            output.append("Blocked Count: " + threadInfo.getBlockedCount() + "\n");
            output.append("Blocked Time (ms): " + threadInfo.getBlockedTime() + "\n");
            _lineCount += 2;
          }
          if (threadInfo.getLockName() != null)
          {
            output.append("Lock Name: " + threadInfo.getLockName() + "\n");
            output.append("Lock Owner: " + threadInfo.getLockOwnerName() + "\n");
            output.append("Lock Owner Thread ID: " + threadInfo.getLockOwnerId() + "\n");
            _lineCount += 3;
          }
          output.append("Waited Count: " + threadInfo.getWaitedCount() + "\n");
          output.append("Waited Time (ms): " + threadInfo.getWaitedTime() + "\n");
          _lineCount += 2;
          output.append("Is In Native: " + threadInfo.isInNative() + "\n");
          output.append("Is Suspended: " + threadInfo.isSuspended() + "\n");
          output.append("\n");
          _lineCount += 3;
        }
      }
      catch (Exception ex)
      {
        // don't let an exception here interfere with display of stack info
      }
    }

    try
    {
      ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
      long[] blockedThreadIds = threadBean.findMonitorDeadlockedThreads();

      if (blockedThreadIds != null)
      {
        String blocked = "Blocked Thread IDs : ";
        for (long id : blockedThreadIds)
          blocked += id + " ";
        output.append(blocked + "\n");
        _lineCount++;
      }

      output.append("Thread Count: " + threadBean.getThreadCount() + "\n");
      output.append("Peak Thread Count: " + threadBean.getPeakThreadCount() + "\n");
      output.append("\n");
      _lineCount += 3;
      dump = output.toString();
      System.out.println(dump);
    }
    catch (Exception ex)
    {
      // don't let an exception here interfere with display of stack info
    }

    return dump;
  }

  private String memoryInfo()
  {
    String info = "";
    StringBuffer output = new StringBuffer();
    _lineCount = 0;

    MemoryMXBean memBean = ManagementFactory.getMemoryMXBean();

    MemoryUsage heapMemUsage = memBean.getHeapMemoryUsage();
    output.append("Heap Memory:\n------------\n");
    _lineCount += 2;
    output.append(memoryUsage(heapMemUsage, 0));
    _lineCount += 4;

    output.append("\n");
    _lineCount++;

    MemoryUsage nonHeapMemUsage = memBean.getNonHeapMemoryUsage();
    output.append("Non-Heap Memory:\n----------------\n");
    _lineCount += 2;
    output.append(memoryUsage(nonHeapMemUsage, 0));
    _lineCount += 4;

    output.append("\n");
    _lineCount++;

    output.append("Objects Pending Finalization: " + memBean.getObjectPendingFinalizationCount() + "\n\n");
    _lineCount += 2;

    output.append("Memory Pools:\n-------------\n");
    _lineCount += 2;
    List<MemoryPoolMXBean> memoryPoolBeans = ManagementFactory.getMemoryPoolMXBeans();
    for (MemoryPoolMXBean memoryPoolBean : memoryPoolBeans)
    {
      output.append(memoryPoolBean.getName() + " ");
      output.append("(type=" + memoryPoolBean.getType() + "):\n");
      _lineCount++;

      if (memoryPoolBean.isUsageThresholdSupported())
      {
        output.append("\tUsage Threshold:" + memoryPoolBean.getUsageThreshold() + " (" + (memoryPoolBean.getUsageThreshold() >> 10) + "K)\n");
        output.append("\tUsage Threshold Count:" + memoryPoolBean.getUsageThresholdCount() + " (" + (memoryPoolBean.getUsageThresholdCount() >> 10) + "K)\n");
        output.append("\tUsage Threshold Exceeded: " + memoryPoolBean.isUsageThresholdExceeded() + "\n");
        _lineCount += 3;
      }

      if (memoryPoolBean.isCollectionUsageThresholdSupported())
      {
        output.append("\tCollection Usage Threshold: " + memoryPoolBean.getCollectionUsageThreshold() + " (" + (memoryPoolBean.getCollectionUsageThreshold() >> 10) + "K)\n");
        output.append("\tCollection Usage Threshold Count: " + memoryPoolBean.getCollectionUsageThresholdCount() + " (" + (memoryPoolBean.getCollectionUsageThresholdCount() >> 10) + "K)\n");
        output.append("\tCollection Usage Threshold Exceeded: " + memoryPoolBean.isCollectionUsageThresholdExceeded() + "\n");
        _lineCount += 3;
      }

      if (memoryPoolBean.isUsageThresholdSupported() && memoryPoolBean.getUsage() != null)
      {
        output.append("\n\tUsage:\n\t------\n").append(memoryUsage(memoryPoolBean.getUsage(), 1));
        _lineCount += 7;
      }
      if (memoryPoolBean.isCollectionUsageThresholdSupported() && memoryPoolBean.getCollectionUsage() != null)
      {
        output.append("\n\tCollection Usage:\n\t-----------------\n").append(memoryUsage(memoryPoolBean.getCollectionUsage(), 1));
        _lineCount += 7;
      }
      if (memoryPoolBean.getPeakUsage() != null)
      {
        output.append("\n\tPeak Usage:\n\t-----------\n").append(memoryUsage(memoryPoolBean.getPeakUsage(), 1));
        _lineCount += 7;
      }

      String[] memoryManagerNames = memoryPoolBean.getMemoryManagerNames();
      if (memoryManagerNames != null)
      {
        output.append("\n\tMemory Manager Names: ");
        for (String memoryManagerName : memoryManagerNames)
          output.append(memoryManagerName + " ");
        output.append("\n");
        _lineCount += 2;
      }

      output.append("\n");
      _lineCount += 1;
    }

    info = output.toString() + getTopInfo();
    System.out.println(info);
    return info;
  }

  private String memoryUsage(MemoryUsage memUsage, int indent)
  {
    StringBuffer output = new StringBuffer();
    for (int i = 0; i < indent; i++)
      output.append("\t");
    output.append("Initial = " + memUsage.getInit() + " (" + (memUsage.getInit() >> 10) + "K)\n");
    for (int i = 0; i < indent; i++)
      output.append("\t");
    output.append("Used = " + memUsage.getUsed() + " (" + (memUsage.getUsed() >> 10) + "K)\n");
    for (int i = 0; i < indent; i++)
      output.append("\t");
    output.append("Committed = " + memUsage.getCommitted() + " (" + (memUsage.getCommitted() >> 10) + "K)\n");
    for (int i = 0; i < indent; i++)
      output.append("\t");
    output.append("Max = " + memUsage.getMax() + " (" + (memUsage.getMax() >> 10) + "K)\n");
    return output.toString();
  }

  public String getTopInfo()
  {
    try
    {
      ProcessBuilder builder = new ProcessBuilder(new String[] {"/usr/bin/top", "-b", "-n", "1"});
      builder.redirectErrorStream(true);
      Process process = builder.start();
      InputStream is = process.getInputStream();
      InputStreamReader isr = new InputStreamReader(is);
      BufferedReader br = new BufferedReader(isr);
      StringBuffer output = new StringBuffer("\nTop Output:\n-----------\n");
      _lineCount += 3;
      String line;
      while ((line = br.readLine()) != null)
      {
        output.append(line).append("\n");
        _lineCount++;
      }
      return output.toString();
    }
    catch (Throwable th)
    {
      StringWriter writer = new StringWriter();
      th.printStackTrace(new PrintWriter(writer));
      _lineCount += 3 + writer.toString().split("\\n").length;
      return "\nError running top:\n------------------\n" + writer;
    }
  }

}
