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
package com.centurylink.mdw.services.system;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.cache.impl.PackageCache;
import com.centurylink.mdw.cli.Download;
import com.centurylink.mdw.cli.Unzip;
import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.config.PropertyUtil;
import com.centurylink.mdw.config.YamlPropertyManager;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.container.ThreadPoolProvider;
import com.centurylink.mdw.container.plugin.CommonThreadPool;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.dataaccess.db.DocumentDb;
import com.centurylink.mdw.model.system.Mbean;
import com.centurylink.mdw.model.system.SysInfo;
import com.centurylink.mdw.model.system.SysInfoCategory;
import com.centurylink.mdw.services.SystemServices;
import com.centurylink.mdw.util.ClasspathUtil;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

import javax.management.*;
import java.io.*;
import java.lang.management.*;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.*;

public class SystemServicesImpl implements SystemServices {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    public List<SysInfoCategory> getSysInfoCategories(SysInfoType type, Query query)
            throws ServiceException {
        List<SysInfoCategory> sysInfoCats = new ArrayList<>();
        if (type == SysInfoType.System) {
            // Request and Session info added by REST servlet
            sysInfoCats.add(getSystemInfo());
            sysInfoCats.add(getDbInfo());
            sysInfoCats.add(getSystemProperties());
            sysInfoCats.add(getMdwProperties());
        }
        else if (type == SysInfoType.Thread) {
            sysInfoCats.add(getThreadDump());
            sysInfoCats.add(getPoolStatus());
        }
        else if (type == SysInfoType.Memory) {
            sysInfoCats.add(getMemoryInfo());
        }
        else if (type == SysInfoType.Class) {
            String className = query.getFilter("className");
            if (className == null)
                throw new ServiceException("Missing parameter: className");
            String classLoader = query.getFilter("classLoader");
            if (classLoader == null) {
                sysInfoCats.add(findClass(className));
            }
            else {
                ClassLoader loader;
                if (classLoader.equals(ClasspathUtil.class.getClassLoader().getClass().getName()))
                    loader = ClasspathUtil.class.getClassLoader();
                else
                    loader = PackageCache.getPackage(classLoader).getCloudClassLoader();
                sysInfoCats.add(findClass(className, loader));
            }
        }
        else if (type == SysInfoType.CLI) {
            String cmd = query.getFilter("command");
            if (cmd == null)
                throw new ServiceException("Missing parameter: command");
            List<SysInfo> cmdInfo = new ArrayList<>();
            try {
                String output = runCliCommand(cmd);
                cmdInfo.add(new SysInfo(cmd, output));  // TODO actual output
                sysInfoCats.add(new SysInfoCategory("CLI Command Output", cmdInfo));
            }
            catch (Exception ex) {
                throw new ServiceException(ServiceException.INTERNAL_ERROR, ex.getMessage(), ex);
            }
        }
        else if (type == SysInfoType.MBean) {
            sysInfoCats.add(getMbeanInfo());
        }

        return sysInfoCats;
    }

    public SysInfoCategory getThreadDump() {
        List<SysInfo> threadDumps = new ArrayList<>();
        Map<Thread, StackTraceElement[]> threads = Thread.getAllStackTraces();
        threadDumps.add(new SysInfo("Total Threads", "(" + new Date() + ") " + threads.size()));
        for (Thread thread : threads.keySet()) {
            StringBuilder output = new StringBuilder();
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
                    output.append("Thread CPU Time (ms): ").append(threadBean.getThreadCpuTime(thread.getId())).append("\n");
                    output.append("Thread User Time (ms): ").append(threadBean.getThreadUserTime(thread.getId())).append("\n");
                }
                ThreadInfo threadInfo = threadBean.getThreadInfo(thread.getId());
                if (threadInfo != null) {
                    if (threadBean.isThreadContentionMonitoringSupported()
                            && threadBean.isThreadContentionMonitoringEnabled()) {
                        output.append("Blocked Count: ").append(threadInfo.getBlockedCount()).append("\n");
                        output.append("Blocked Time (ms): ").append(threadInfo.getBlockedTime()).append("\n");
                    }
                    if (threadInfo.getLockName() != null) {
                        output.append("Lock Name: ").append(threadInfo.getLockName()).append("\n");
                        output.append("Lock Owner: ").append(threadInfo.getLockOwnerName()).append("\n");
                        output.append("Lock Owner Thread ID: ").append(threadInfo.getLockOwnerId()).append("\n");
                    }
                    output.append("Waited Count: ").append(threadInfo.getWaitedCount()).append("\n");
                    output.append("Waited Time (ms): ").append(threadInfo.getWaitedTime()).append("\n");
                    output.append("Is In Native: ").append(threadInfo.isInNative()).append("\n");
                    output.append("Is Suspended: ").append(threadInfo.isSuspended());
                    System.out.println(output.toString());
                }
            }
            catch (Exception ex) {
                // don't let an exception here interfere with display of stack info
            }
            threadDumps.add(new SysInfo(thread.getName(),  output.toString()));
        }
        try {
            StringBuilder mxBeanOutput = new StringBuilder();
            ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
            long[] blockedThreadIds = threadBean.findMonitorDeadlockedThreads();

            if (blockedThreadIds != null) {
                StringBuilder blocked = new StringBuilder("Blocked Thread IDs : ");
                for (long id : blockedThreadIds)
                    blocked.append(id).append(" ");
                mxBeanOutput.append(blocked).append("\n");
            }
            mxBeanOutput.append("\nThread Count: ").append(threadBean.getThreadCount()).append("\n");
            mxBeanOutput.append("Peak Thread Count: ").append(threadBean.getPeakThreadCount());
            threadDumps.add(new SysInfo("Thread MXBean",  mxBeanOutput.toString()));
            System.out.println(mxBeanOutput.toString());
        }
        catch (Exception ex) {
            // don't let an exception here interfere with display of stack info
        }
        return new SysInfoCategory("Thread Dump", threadDumps);
    }

    public SysInfoCategory getMemoryInfo() {
        List<SysInfo> memoryInfo = new ArrayList<>();
        memoryInfo.add(new SysInfo(memoryInfo()));
        return new SysInfoCategory("Memory Info", memoryInfo);
    }

    private String memoryInfo() {
        String info;
        StringBuilder output = new StringBuilder();

        MemoryMXBean memBean = ManagementFactory.getMemoryMXBean();

        MemoryUsage heapMemUsage = memBean.getHeapMemoryUsage();
        output.append("Heap Memory:\n------------\n");
        output.append(memoryUsage(heapMemUsage, 0));

        output.append("\n");

        MemoryUsage nonHeapMemUsage = memBean.getNonHeapMemoryUsage();
        output.append("Non-Heap Memory:\n----------------\n");
        output.append(memoryUsage(nonHeapMemUsage, 0));

        output.append("\n");

        output.append("Objects Pending Finalization: ").append(memBean.getObjectPendingFinalizationCount()).append("\n\n");

        output.append("Memory Pools:\n-------------\n");
        List<MemoryPoolMXBean> memoryPoolBeans = ManagementFactory.getMemoryPoolMXBeans();
        for (MemoryPoolMXBean memoryPoolBean : memoryPoolBeans) {
            output.append(memoryPoolBean.getName()).append(" ");
            output.append("(type=").append(memoryPoolBean.getType()).append("):\n");

            if (memoryPoolBean.isUsageThresholdSupported()) {
                output.append("\tUsage Threshold:").append(memoryPoolBean.getUsageThreshold()).append(" (").append(memoryPoolBean.getUsageThreshold() >> 10).append("K)\n");
                output.append("\tUsage Threshold Count:").append(memoryPoolBean.getUsageThresholdCount()).append(" (").append(memoryPoolBean.getUsageThresholdCount() >> 10).append("K)\n");
                output.append("\tUsage Threshold Exceeded: ").append(memoryPoolBean.isUsageThresholdExceeded()).append("\n");
            }

            if (memoryPoolBean.isCollectionUsageThresholdSupported()) {
                output.append("\tCollection Usage Threshold: ").append(memoryPoolBean.getCollectionUsageThreshold()).append(" (").append(memoryPoolBean.getCollectionUsageThreshold() >> 10).append("K)\n");
                output.append("\tCollection Usage Threshold Count: ").append(memoryPoolBean.getCollectionUsageThresholdCount()).append(" (").append(memoryPoolBean.getCollectionUsageThresholdCount() >> 10).append("K)\n");
                output.append("\tCollection Usage Threshold Exceeded: ").append(memoryPoolBean.isCollectionUsageThresholdExceeded()).append("\n");
            }

            if (memoryPoolBean.isUsageThresholdSupported() && memoryPoolBean.getUsage() != null) {
                output.append("\n\tUsage:\n\t------\n").append(memoryUsage(memoryPoolBean.getUsage(), 1));
            }
            if (memoryPoolBean.isCollectionUsageThresholdSupported() && memoryPoolBean.getCollectionUsage() != null) {
                output.append("\n\tCollection Usage:\n\t-----------------\n").append(memoryUsage(memoryPoolBean.getCollectionUsage(), 1));
            }
            if (memoryPoolBean.getPeakUsage() != null) {
                output.append("\n\tPeak Usage:\n\t-----------\n").append(memoryUsage(memoryPoolBean.getPeakUsage(), 1));
            }

            String[] memoryManagerNames = memoryPoolBean.getMemoryManagerNames();
            if (memoryManagerNames != null) {
                output.append("\n\tMemory Manager Names: ");
                for (String memoryManagerName : memoryManagerNames)
                    output.append(memoryManagerName).append(" ");
                output.append("\n");
            }

            output.append("\n");
        }

        info = output.toString() + getTopInfo();
        System.out.println(info);
        return info;
    }

    private String memoryUsage(MemoryUsage memUsage, int indent) {
        StringBuilder output = new StringBuilder();
        for (int i = 0; i < indent; i++)
            output.append("\t");
        output.append("Initial = ").append(memUsage.getInit()).append(" (").append(memUsage.getInit() >> 10).append("K)\n");
        for (int i = 0; i < indent; i++)
            output.append("\t");
        output.append("Used = ").append(memUsage.getUsed()).append(" (").append(memUsage.getUsed() >> 10).append("K)\n");
        for (int i = 0; i < indent; i++)
            output.append("\t");
        output.append("Committed = ").append(memUsage.getCommitted()).append(" (").append(memUsage.getCommitted() >> 10).append("K)\n");
        for (int i = 0; i < indent; i++)
            output.append("\t");
        output.append("Max = ").append(memUsage.getMax()).append(" (").append(memUsage.getMax() >> 10).append("K)\n");
        return output.toString();
    }

    public SysInfoCategory getMbeanInfo() {
        List<SysInfo> mbeanInfo = new ArrayList<>();
        mbeanInfo.add(new SysInfo(mbeanInfo()));
        return new SysInfoCategory("MBean Info", mbeanInfo);
    }

    private String mbeanInfo() {
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        Set<ObjectInstance> instances = server.queryMBeans(null, null);
        Iterator<ObjectInstance> iterator = instances.iterator();
        StringBuilder sb = new StringBuilder();
        Map<String,Mbean> domainMbeans = new TreeMap<>();
        while (iterator.hasNext()) {
            ObjectInstance instance = iterator.next();
            ObjectName objectName = instance.getObjectName();
            try {
                MBeanInfo mbeanInfo = server.getMBeanInfo(objectName);
                String type = objectName.getKeyProperty("type");
                String domain = objectName.getDomain();
                Mbean mbean = domainMbeans.get(domain);
                if (mbean == null) {
                    mbean = new Mbean(domain, type);
                    domainMbeans.put(domain, mbean);
                }
                for (MBeanAttributeInfo attrInfo : mbeanInfo.getAttributes()) {
                    String name = attrInfo.getName();
                    try {
                        Object value = server.getAttribute(objectName, name);
                        if (type != null)
                            name = type + "/" + name;
                        mbean.getValues().put(name, String.valueOf(value));
                    }
                    catch (Exception ex) {
                        logger.trace(ex.getMessage());
                    }
                }

            }
            catch (Exception ex) {
                logger.severeException(ex.getMessage(), ex);
            }
        }

        for (String domain : domainMbeans.keySet()) {
            sb.append(domain).append(":\n");
            for (int i = 0; i <= domain.length(); i++)
                sb.append("-");
            sb.append("\n");
            Mbean mbean = domainMbeans.get(domain);
            for (String name : mbean.getValues().keySet()) {
                String value = mbean.getValues().get(name);
                int lf = value.indexOf('\n');
                if (lf > 0)
                    value = value.substring(0, lf) + "...";
                sb.append("   ").append(name).append(": ").append(value).append("\n");
            }
            sb.append("\n");
        }

        System.out.println("\n\n" + sb);
        return sb.toString();
    }


    public String getTopInfo() {
        try {
            ProcessBuilder builder = new ProcessBuilder("/usr/bin/top", "-b", "-n", "1");
            builder.redirectErrorStream(true);
            Process process = builder.start();
            InputStream is = process.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            StringBuilder output = new StringBuilder("\nTop Output:\n-----------\n");
            String line;
            while ((line = br.readLine()) != null) {
                output.append(line).append("\n");
            }
            return output.toString();
        }
        catch (Throwable th) {
            StringWriter writer = new StringWriter();
            th.printStackTrace(new PrintWriter(writer));
            return "\nError running top:\n------------------\n" + writer;
        }
    }

    public SysInfoCategory getSystemInfo() {
        List<SysInfo> systemInfos = new ArrayList<>();
        systemInfos.add(new SysInfo("MDW build", ApplicationContext.getMdwVersion() + " (" + ApplicationContext.getMdwBuildTimestamp() + ")"));
        systemInfos.add(new SysInfo("Server host", ApplicationContext.getServerHost()));
        try {
            systemInfos.add(new SysInfo("Server hostname", InetAddress.getLocalHost().getHostName()));
        }
        catch (UnknownHostException ex) {
            systemInfos.add(new SysInfo("Server hostname", String.valueOf(ex)));
        }
        systemInfos.add(new SysInfo("Server port", String.valueOf(ApplicationContext.getServerPort())));
        systemInfos.add(new SysInfo("Server name", ApplicationContext.getServer().toString()));
        systemInfos.add(new SysInfo("Runtime env", ApplicationContext.getRuntimeEnvironment()));
        systemInfos.add(new SysInfo("Startup dir", System.getProperty("user.dir")));
        File bootJar = ApplicationContext.getBootJar();
        if (bootJar != null)
            systemInfos.add(new SysInfo("Boot jar", bootJar.getAbsolutePath()));

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
        systemInfos.add(new SysInfo("Default ClassLoader", ClasspathUtil.class.getClassLoader().getClass().getName()));

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
        List<SysInfo> dbInfos = new ArrayList<>();
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

            if (DatabaseAccess.getDocumentDb() != null) {
                DocumentDb docDb = DatabaseAccess.getDocumentDb();
                dbInfos.add(new SysInfo("Document DB", docDb.getClass().getName() + " " + docDb));
            }
        }
        catch (Exception ex) {
            // don't let runtime exceptions prevent page display
            logger.severeException(ex.getMessage(), ex);
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
        List<SysInfo> sysPropInfos = new ArrayList<>();
        Properties properties = System.getProperties();
        List<String> propNames = new ArrayList<>();
        for (Object o : properties.keySet()) propNames.add(String.valueOf(o));
        Collections.sort(propNames);
        for (String propName : propNames) {
            sysPropInfos.add(new SysInfo(propName, properties.getProperty(propName)));
        }
        return new SysInfoCategory("System Properties", sysPropInfos);
    }

    public SysInfoCategory getMdwProperties() {
        List<SysInfo> mdwPropInfos = new ArrayList<>();
        PropertyManager propMgr = null;
        try {
            propMgr = PropertyUtil.getInstance().getPropertyManager();
            Properties properties = propMgr.getAllProperties();
            List<String> propNames = new ArrayList<>();
            for (Object o : properties.keySet()) {
                propNames.add(String.valueOf(o));
            }
            Collections.sort(propNames);
            for (String propName : propNames) {
                try {
                    if (!propName.toLowerCase().contains("password") && !propMgr.isEncrypted(propName))
                        mdwPropInfos.add(new SysInfo(propName, properties.getProperty(propName)));
                    else
                        mdwPropInfos.add(new SysInfo(propName, "********"));
                }
                catch (Exception ex) {
                    logger.severeException(ex.getMessage(), ex);
                    mdwPropInfos.add(new SysInfo("Error", String.valueOf(ex)));
                }
            }
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            mdwPropInfos.add(new SysInfo("Error", String.valueOf(ex)));
        }

        SysInfoCategory category = new SysInfoCategory("MDW Properties", mdwPropInfos);
        if (propMgr instanceof YamlPropertyManager)
            category.setDescription("Never-accessed properties are not displayed.  Null means the property has been read and not found.");
        return category;
    }

    private SysInfoCategory getPoolStatus() {
        ThreadPoolProvider threadPool = ApplicationContext.getThreadPoolProvider();
        List<SysInfo> poolStatus = new ArrayList<>();
        if (!(threadPool instanceof CommonThreadPool)) {
            poolStatus.add(new SysInfo ("Error getting thread pool status" , "ThreadPoolProvider is not MDW CommonThreadPool"));
        }
        else
            poolStatus.add(new SysInfo ("Current Status" , threadPool.currentStatus()));
        return new SysInfoCategory("Pool Status", poolStatus);
    }

    public SysInfoCategory findClass(String className, ClassLoader classLoader) {
        List<SysInfo> classInfo = new ArrayList<>();
        classInfo.add(new SysInfo(className, ClasspathUtil.locate(className, classLoader)));
        return new SysInfoCategory("Class Info", classInfo);
    }

    public SysInfoCategory findClass(String className) {
        List<SysInfo> classInfo = new ArrayList<>();
        classInfo.add(new SysInfo(className, ClasspathUtil.locate(className)));
        return new SysInfoCategory("Class Info", classInfo);
    }

    public String runCliCommand(String command) throws IOException, InterruptedException {
        List<String> cmd = new ArrayList<>();
        cmd.add(System.getProperty("java.home") + File.separator + "bin" + File.separator + "java");

        cmd.add("-jar");
        String mdwHome = System.getenv("MDW_HOME");
        if (mdwHome == null) {
            // fall back to system property
            mdwHome = System.getProperty("mdw.home");
            if (mdwHome == null) {
                mdwHome = ApplicationContext.getTempDirectory() + File.separator + "MDW_HOME";
                System.setProperty("mdw.home", mdwHome);
            }
        }

        File cliJar = new File(mdwHome + File.separator + "mdw-cli.jar");
        if (!cliJar.exists()) {
            if (!cliJar.getParentFile().isDirectory() && !cliJar.getParentFile().mkdirs())
                throw new IOException("Cannot create dir: " + cliJar.getParentFile().getAbsolutePath());
            // grab cli zip from github
            String v = ApplicationContext.getMdwVersion();
            URL cliUrl = new URL("https://github.com/CenturyLinkCloud/mdw/releases/download/" + v + "/mdw-cli-" + v + ".zip");
            File tempZip = Files.createTempFile("mdw-cli", ".jar").toFile();
            new Download(cliUrl, tempZip, 210L).run(); // TODO progress via websocket
            // unzip into MDW_HOME
            new Unzip(tempZip, cliJar.getParentFile()).run();
        }
        cmd.add(cliJar.getAbsolutePath());

        // running direct command instead of through bat/sh to avoid permissions issues
        List<String> mdwCmd = new ArrayList<>(Arrays.asList(command.trim().split("\\s+")));
        if (mdwCmd.get(0).equals("mdw"))
            mdwCmd.remove(0);
        if (!mdwCmd.isEmpty()) {
            if (ApplicationContext.isCloudFoundry() && System.getProperty("mdw.config.location") == null) {
                String configLoc = ApplicationContext.getTempDirectory() + File.separator + "config";
                System.setProperty("mdw.config.location", configLoc);
                Files.createDirectories(Paths.get(configLoc));
                // write pseudo-config if needed
                if (System.getProperty("mdw.property.manager").equalsIgnoreCase("com.centurylink.mdw.config.PaasPropertyManager")) {
                    Properties props = PropertyManager.getInstance().getAllProperties();
                    props.store(new FileOutputStream(configLoc + "/mdw.properties"), null);
                }
                else {
                    String yaml = System.getenv("mdw_settings");
                    Files.write(Paths.get(configLoc + "/mdw.yaml"), yaml.getBytes());
                }
            }
            String first = mdwCmd.get(0);
            if (first.equals("archive") || first.equals("import") || first.equals("install")
                    || first.equals("status") || first.equals("test") || first.equals("update")) {
                mdwCmd.add("--config-loc=" + System.getProperty("mdw.config.location"));
            }
        }

        cmd.addAll(mdwCmd);
        logger.debug("Running MDW CLI command: '" + cmd);

        ProcessBuilder builder = new ProcessBuilder(cmd);
        builder.environment().put("MDW_HOME", mdwHome);
        builder.directory(new File(PropertyManager.getProperty(PropertyNames.MDW_GIT_LOCAL_PATH)));
        builder.redirectErrorStream(true);
        Process process = builder.start();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(output));
        new Thread(() -> {
            try (BufferedReader out = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                out.lines().forEach(writer::println);
            }
            catch (IOException ex) {
                ex.printStackTrace();
            }
        }).start();
        process.waitFor();
        writer.flush();
        return new String(output.toByteArray());
    }
}