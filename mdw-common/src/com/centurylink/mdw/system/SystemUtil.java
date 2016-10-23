/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.system;

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
import java.util.Date;
import java.util.List;
import java.util.Map;

public class SystemUtil {

    public synchronized String getThreadDumpCount() {
        return Integer.toString(Thread.getAllStackTraces().size());
    }

    public synchronized String getThreadDump() {
        String dump = "";
        Map<Thread, StackTraceElement[]> threads = Thread.getAllStackTraces();
        StringBuffer output = new StringBuffer();
        output.append("Total Threads (").append(new Date()).append("): ").append(threads.size()).append("\n-------------------------------------------------\n\n");
        for (Thread thread : threads.keySet()) {
            output.append(thread.getName()).append(" (");
            output.append("priority=").append(thread.getPriority()).append(" ");
            output.append("group=").append(thread.getThreadGroup()).append(" ");
            output.append("state=").append(thread.getState()).append(" ");
            output.append("id=").append(thread.getId());
            output.append("):\n");
            StackTraceElement[] elements = threads.get(thread);
            if (elements != null) {
                for (StackTraceElement element : elements) {
                    output.append("\tat ").append(element).append("\n");
                }
            }
            output.append("\n");

            try {
                ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
                if (threadBean.isThreadCpuTimeSupported() && threadBean.isThreadCpuTimeEnabled()) {
                    output.append("Thread CPU Time (ms): "
                            + threadBean.getThreadCpuTime(thread.getId()) + "\n");
                    output.append("Thread User Time (ms): "
                            + threadBean.getThreadUserTime(thread.getId()) + "\n");
                }
                ThreadInfo threadInfo = threadBean.getThreadInfo(thread.getId());
                if (threadInfo != null) {
                    if (threadBean.isThreadContentionMonitoringSupported()
                            && threadBean.isThreadContentionMonitoringEnabled()) {
                        output.append("Blocked Count: " + threadInfo.getBlockedCount() + "\n");
                        output.append("Blocked Time (ms): " + threadInfo.getBlockedTime() + "\n");
                    }
                    if (threadInfo.getLockName() != null) {
                        output.append("Lock Name: " + threadInfo.getLockName() + "\n");
                        output.append("Lock Owner: " + threadInfo.getLockOwnerName() + "\n");
                        output.append("Lock Owner Thread ID: " + threadInfo.getLockOwnerId() + "\n");
                    }
                    output.append("Waited Count: " + threadInfo.getWaitedCount() + "\n");
                    output.append("Waited Time (ms): " + threadInfo.getWaitedTime() + "\n");
                    output.append("Is In Native: " + threadInfo.isInNative() + "\n");
                    output.append("Is Suspended: " + threadInfo.isSuspended() + "\n");
                    output.append("\n");
                }
            }
            catch (Exception ex) {
                // don't let an exception here interfere with display of stack info
            }
        }

        try {
            ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
            long[] blockedThreadIds = threadBean.findMonitorDeadlockedThreads();

            if (blockedThreadIds != null) {
                String blocked = "Blocked Thread IDs : ";
                for (long id : blockedThreadIds)
                    blocked += id + " ";
                output.append(blocked + "\n");
            }

            output.append("Thread Count: " + threadBean.getThreadCount() + "\n");
            output.append("Peak Thread Count: " + threadBean.getPeakThreadCount() + "\n");
            output.append("\n");
            dump = output.toString();
            System.out.println(dump);
        }
        catch (Exception ex) {
            // don't let an exception here interfere with display of stack info
        }

        return dump;
    }

    public synchronized String getMemoryInfo() {
        String info = "";
        StringBuffer output = new StringBuffer();

        MemoryMXBean memBean = ManagementFactory.getMemoryMXBean();

        MemoryUsage heapMemUsage = memBean.getHeapMemoryUsage();
        output.append("Heap Memory:\n------------\n");
        output.append(memoryUsage(heapMemUsage, 0));

        output.append("\n");

        MemoryUsage nonHeapMemUsage = memBean.getNonHeapMemoryUsage();
        output.append("Non-Heap Memory:\n----------------\n");
        output.append(memoryUsage(nonHeapMemUsage, 0));

        output.append("\n");

        output.append("Objects Pending Finalization: "
                + memBean.getObjectPendingFinalizationCount() + "\n\n");

        output.append("Memory Pools:\n-------------\n");
        List<MemoryPoolMXBean> memoryPoolBeans = ManagementFactory.getMemoryPoolMXBeans();
        for (MemoryPoolMXBean memoryPoolBean : memoryPoolBeans) {
            output.append(memoryPoolBean.getName() + " ");
            output.append("(type=" + memoryPoolBean.getType() + "):\n");

            if (memoryPoolBean.isUsageThresholdSupported()) {
                output.append("\tUsage Threshold:" + memoryPoolBean.getUsageThreshold() + " ("
                        + (memoryPoolBean.getUsageThreshold() >> 10) + "K)\n");
                output.append("\tUsage Threshold Count:" + memoryPoolBean.getUsageThresholdCount()
                        + " (" + (memoryPoolBean.getUsageThresholdCount() >> 10) + "K)\n");
                output.append("\tUsage Threshold Exceeded: "
                        + memoryPoolBean.isUsageThresholdExceeded() + "\n");
            }

            if (memoryPoolBean.isCollectionUsageThresholdSupported()) {
                output.append("\tCollection Usage Threshold: "
                        + memoryPoolBean.getCollectionUsageThreshold() + " ("
                        + (memoryPoolBean.getCollectionUsageThreshold() >> 10) + "K)\n");
                output.append("\tCollection Usage Threshold Count: "
                        + memoryPoolBean.getCollectionUsageThresholdCount() + " ("
                        + (memoryPoolBean.getCollectionUsageThresholdCount() >> 10) + "K)\n");
                output.append("\tCollection Usage Threshold Exceeded: "
                        + memoryPoolBean.isCollectionUsageThresholdExceeded() + "\n");
            }

            if (memoryPoolBean.isUsageThresholdSupported() && memoryPoolBean.getUsage() != null) {
                output.append("\n\tUsage:\n\t------\n").append(
                        memoryUsage(memoryPoolBean.getUsage(), 1));
            }
            if (memoryPoolBean.isCollectionUsageThresholdSupported()
                    && memoryPoolBean.getCollectionUsage() != null) {
                output.append("\n\tCollection Usage:\n\t-----------------\n").append(
                        memoryUsage(memoryPoolBean.getCollectionUsage(), 1));
            }
            if (memoryPoolBean.getPeakUsage() != null) {
                output.append("\n\tPeak Usage:\n\t-----------\n").append(
                        memoryUsage(memoryPoolBean.getPeakUsage(), 1));
            }

            String[] memoryManagerNames = memoryPoolBean.getMemoryManagerNames();
            if (memoryManagerNames != null) {
                output.append("\n\tMemory Manager Names: ");
                for (String memoryManagerName : memoryManagerNames)
                    output.append(memoryManagerName + " ");
                output.append("\n");
            }

            output.append("\n");
        }

        info = output.toString();
        System.out.println(info);
        return info;
    }

    private String memoryUsage(MemoryUsage memUsage, int indent) {
        StringBuffer output = new StringBuffer();
        for (int i = 0; i < indent; i++)
            output.append("\t");
        output.append("Initial = " + memUsage.getInit() + " (" + (memUsage.getInit() >> 10)
                + "K)\n");
        for (int i = 0; i < indent; i++)
            output.append("\t");
        output.append("Used = " + memUsage.getUsed() + " (" + (memUsage.getUsed() >> 10) + "K)\n");
        for (int i = 0; i < indent; i++)
            output.append("\t");
        output.append("Committed = " + memUsage.getCommitted() + " ("
                + (memUsage.getCommitted() >> 10) + "K)\n");
        for (int i = 0; i < indent; i++)
            output.append("\t");
        output.append("Max = " + memUsage.getMax() + " (" + (memUsage.getMax() >> 10) + "K)\n");
        return output.toString();
    }

    public String getTopInfo() {
        try {
            ProcessBuilder builder = new ProcessBuilder(new String[] {"/usr/bin/top", "-b", "-n", "1"});
            builder.redirectErrorStream(true);
            Process process = builder.start();
            InputStream is = process.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            StringBuffer output = new StringBuffer();
            String line;
            while ((line = br.readLine()) != null)
              output.append(line).append("\n");
            return output.toString();
        }
        catch (Throwable th) {
            StringWriter writer = new StringWriter();
            th.printStackTrace(new PrintWriter(writer));
            return "\nError running top:\n------------------\n" + writer;
        }
    }
}
