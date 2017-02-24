/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services.system;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.config.PropertyUtil;
import com.centurylink.mdw.container.ThreadPoolProvider;
import com.centurylink.mdw.container.plugin.CommonThreadPool;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.model.system.SysInfo;
import com.centurylink.mdw.model.system.SysInfoCategory;
import com.centurylink.mdw.services.SystemServices;
import com.centurylink.mdw.util.log.LoggerUtil;

public class SystemServicesImpl implements SystemServices {

    public List<SysInfoCategory> getSysInfoCategories(SysInfoType type) {
        List<SysInfoCategory> sysInfoCats = new ArrayList<SysInfoCategory>();
        if (type == SysInfoType.System) {
            // Request and Session info added by REST servlet
            sysInfoCats.add(getSystemInfo());
            sysInfoCats.add(getDbInfo());
            sysInfoCats.add(getSystemProperties());
            sysInfoCats.add(getEnvironmentVariables());
            sysInfoCats.add(getMdwProperties());
        }
        else if (type == SysInfoType.Thread) {
            sysInfoCats.add(getThreadDump());
            sysInfoCats.add(getPoolStatus());
        }
        else if (type == SysInfoType.JMS) {

        }
        else if (type == SysInfoType.Caches) {

        }
        else if (type == SysInfoType.MBean) {

        }

        return sysInfoCats;
    }

    public SysInfoCategory getThreadDump() {
        List<SysInfo> threadDumps = new ArrayList<SysInfo>();
        Map<Thread, StackTraceElement[]> threads = Thread.getAllStackTraces();
        threadDumps.add(new SysInfo("Total Threads", "(" + new Date() + ") " + threads.size()));
        for (Thread thread : threads.keySet()) {
            StringBuffer output = new StringBuffer();
            output.append(" (");
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
                    output.append("Is Suspended: " + threadInfo.isSuspended());
                    System.out.println(output.toString());
                }
            }
            catch (Exception ex) {
                // don't let an exception here interfere with display of stack info
            }
            threadDumps.add(new SysInfo(thread.getName(),  output.toString()));
        }
        try {
            StringBuffer mxBeanOutput = new StringBuffer();
            ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
            long[] blockedThreadIds = threadBean.findMonitorDeadlockedThreads();

            if (blockedThreadIds != null) {
                String blocked = "Blocked Thread IDs : ";
                for (long id : blockedThreadIds)
                    blocked += id + " ";
                mxBeanOutput.append(blocked + "\n");
            }
            mxBeanOutput.append("\nThread Count: " + threadBean.getThreadCount() + "\n");
            mxBeanOutput.append("Peak Thread Count: " + threadBean.getPeakThreadCount());
            threadDumps.add(new SysInfo("Thread MXBean",  mxBeanOutput.toString()));
            System.out.println(mxBeanOutput.toString());
        }
        catch (Exception ex) {
            // don't let an exception here interfere with display of stack info
        }
        return new SysInfoCategory("Thread Dump", threadDumps);
    }

    public SysInfoCategory getSystemInfo() {
        List<SysInfo> systemInfos = new ArrayList<SysInfo>();
        systemInfos.add(new SysInfo("MDW build", ApplicationContext.getMdwVersion() + " (" + ApplicationContext.getMdwBuildTimestamp() + ")"));
        systemInfos.add(new SysInfo("Server host", ApplicationContext.getServerHost()));
        try {
            systemInfos.add(new SysInfo("Server hostname", InetAddress.getLocalHost().getHostName()));
        }
        catch (UnknownHostException ex) {
            systemInfos.add(new SysInfo("Server hostname", String.valueOf(ex)));
        }
        systemInfos.add(new SysInfo("Server port", String.valueOf(ApplicationContext.getServerPort())));
        systemInfos.add(new SysInfo("Server name", ApplicationContext.getServerHostPort()));
        systemInfos.add(new SysInfo("Master server", ApplicationContext.getMasterServer()));
        systemInfos.add(new SysInfo("Runtime env", ApplicationContext.getRuntimeEnvironment()));
        systemInfos.add(new SysInfo("Startup dir", System.getProperty("user.dir")));
        systemInfos.add(new SysInfo("App user", System.getProperty("user.name")));
        systemInfos.add(new SysInfo("System time", String.valueOf(new Date(System.currentTimeMillis()))));
        systemInfos.add(new SysInfo("Startup time", String.valueOf(ApplicationContext.getStartupTime())));
        systemInfos.add(new SysInfo("Java version", System.getProperty("java.version")));
        systemInfos.add(new SysInfo("Java VM", System.getProperty("java.vm.name") + " " + System.getProperty("java.vm.version")));
        systemInfos.add(new SysInfo("OS", System.getProperty("os.name")));
        systemInfos.add(new SysInfo("OS version", System.getProperty("os.version")));
        systemInfos.add(new SysInfo("OS arch", System.getProperty("os.arch")));
        Runtime runtime = Runtime.getRuntime();
        systemInfos.add(new SysInfo("Max memory", runtime.maxMemory()/1024/1024 + " MB"));
        systemInfos.add(new SysInfo("Free memory", runtime.freeMemory()/1024/1024 + " MB"));
        systemInfos.add(new SysInfo("Total memory", runtime.totalMemory()/1024/1024 + " MB"));
        systemInfos.add(new SysInfo("Available processors", String.valueOf(runtime.availableProcessors())));


        String pathSep = System.getProperty("path.separator");
        String cp = System.getProperty("java.class.path");
        if (cp != null) {
            SysInfo cpInfo = new SysInfo("System classpath");
            String[] cpEntries = cp.split(pathSep);
            cpInfo.setValues(Arrays.asList(cpEntries));
            systemInfos.add(cpInfo);
        }
        String p = System.getenv("PATH");
        if (p != null) {
            SysInfo pInfo = new SysInfo("PATH environment variable");
            String[] pEntries = p.split(pathSep);
            pInfo.setValues(Arrays.asList(pEntries));
            systemInfos.add(pInfo);
        }
        return new SysInfoCategory("System Details", systemInfos);
    }

    public SysInfoCategory getDbInfo() {
        List<SysInfo> dbInfos = new ArrayList<SysInfo>();
        DatabaseAccess dbAccess = null;
        try {
            dbAccess = new DatabaseAccess(null);
            Connection conn = dbAccess.openConnection();
            DatabaseMetaData metadata = conn.getMetaData();
            dbInfos.add(new SysInfo("Database", metadata.getDatabaseProductName()));
            dbInfos.add(new SysInfo("DB version", metadata.getDatabaseProductVersion()));
            dbInfos.add(new SysInfo("JDBC Driver", metadata.getDriverName()));
            dbInfos.add(new SysInfo("Driver version", metadata.getDriverVersion()));
            dbInfos.add(new SysInfo("JDBC URL", metadata.getURL()));
            dbInfos.add(new SysInfo("DB user", metadata.getUserName()));
            dbInfos.add(new SysInfo("DB time", String.valueOf(new Date(dbAccess.getDatabaseTime()))));
        }
        catch (Exception ex) {
            // don't let runtime exceptions prevent page display
            LoggerUtil.getStandardLogger().severeException(ex.getMessage(), ex);
            dbInfos.add(new SysInfo("Error", String.valueOf(ex)));
        }
        finally {
            if (dbAccess != null) {
                dbAccess.closeConnection();
            }
        }

        return new SysInfoCategory("Database Details", dbInfos);
    }

    public SysInfoCategory getSystemProperties() {
        List<SysInfo> sysPropInfos = new ArrayList<SysInfo>();
        Properties properties = System.getProperties();
        List<String> propNames = new ArrayList<String>();
        for (Iterator<?> iter = properties.keySet().iterator(); iter.hasNext(); )
            propNames.add(String.valueOf(iter.next()));
        Collections.sort(propNames);
        for (String propName : propNames) {
            sysPropInfos.add(new SysInfo(propName, properties.getProperty(propName)));
        }
        return new SysInfoCategory("System Properties", sysPropInfos);
    }

    public SysInfoCategory getEnvironmentVariables() {
        List<SysInfo> envVarInfos = new ArrayList<SysInfo>();

        List<String> envVarNames = new ArrayList<String>();
        for (String envVarName : System.getenv().keySet())
          envVarNames.add(envVarName);
        Collections.sort(envVarNames);
        for (String envVarName : envVarNames) {
            envVarInfos.add(new SysInfo(envVarName, System.getenv().get(envVarName)));
        }
        return new SysInfoCategory("Environment Variables", envVarInfos);
    }

    public SysInfoCategory getMdwProperties() {
        List<SysInfo> mdwPropInfos = new ArrayList<SysInfo>();
        try {
            PropertyManager propMgr = PropertyUtil.getInstance().getPropertyManager();
            Properties properties = propMgr.getAllProperties();
            List<String> propNames = new ArrayList<String>();
            for (Iterator<?> iter = properties.keySet().iterator(); iter.hasNext();)
                propNames.add(String.valueOf(iter.next()));
            Collections.sort(propNames);
            for (String propName : propNames) {
                try {
                    if (propName.toLowerCase().indexOf("password") == -1)
                        mdwPropInfos.add(new SysInfo(propName, properties.getProperty(propName)));
                    else
                        mdwPropInfos.add(new SysInfo(propName, "********"));
                }
                catch (Exception ex) {
                    LoggerUtil.getStandardLogger().severeException(ex.getMessage(), ex);
                    mdwPropInfos.add(new SysInfo("Error", String.valueOf(ex)));
                }
            }
        }
        catch (Exception ex) {
            LoggerUtil.getStandardLogger().severeException(ex.getMessage(), ex);
            mdwPropInfos.add(new SysInfo("Error", String.valueOf(ex)));
        }

        return new SysInfoCategory("MDW Properties", mdwPropInfos);
    }

    private SysInfoCategory getPoolStatus() {
        ThreadPoolProvider threadPool = ApplicationContext.getThreadPoolProvider();
        List<SysInfo> poolStatus = new ArrayList<SysInfo>();
        if (!(threadPool instanceof CommonThreadPool)) {
            poolStatus.add(new SysInfo ("Error getting thread pool status" , "ThreadPoolProvider is not MDW CommonThreadPool"));
        }
        else
            poolStatus.add(new SysInfo ("Current Status" , ((CommonThreadPool)threadPool).currentStatus()));
        return new SysInfoCategory("Pool Status", poolStatus);
    }
}