/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.project.model;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import org.eclipse.wst.server.core.IServer;

import com.centurylink.mdw.plugin.PluginUtil;
import com.centurylink.mdw.plugin.preferences.model.PreferenceConstants;

/**
 * Represents a workflow project's Server settings.
 */
public class ServerSettings implements PreferenceConstants
{
  public static final String DEFAULT_HOST = "localhost";

  public enum ContainerType
  {
    JBoss,
    ServiceMix,
    Fuse,
    Tomcat,
    WebLogic
  }
  private ContainerType containerType = ContainerType.ServiceMix;
  public ContainerType getContainerType() { return containerType; }
  public void setContainerType(ContainerType type) { this.containerType = type; }
  public String getContainerName()
  {
    return containerType.toString();
  }
  public boolean isWebLogic()
  {
    return containerType.equals(ContainerType.WebLogic);
  }
  public boolean isJBoss()
  {
    return containerType.equals(ContainerType.JBoss);
  }
  public boolean isServiceMix()
  {
    return containerType.equals(ContainerType.ServiceMix);
  }
  public boolean isFuse()
  {
    return containerType.equals(ContainerType.Fuse);
  }
  public boolean isTomcat()
  {
    return containerType.equals(ContainerType.Tomcat);
  }
  public static ContainerType getContainerTypeFromClass(String className)
  {
    for (ContainerType type : ContainerType.values())
    {
      if (className.startsWith(type.toString()))
        return type;
    }
    return null;
  }

  private String containerVersion;
  public String getContainerVersion() { return containerVersion; }
  public void setContainerVersion(String version) { this.containerVersion = version; }

  private String home;
  public String getHome() { return home; }
  public void setHome(String s) { home = s; }
  public String getHomeWithFwdSlashes()
  {
    return getHome().replace('\\', '/');
  }

  public String getHomeParentDir()
  {
    return getHome() + System.getProperty("file.separator") + "..";
  }

  private String jdkHome;
  public String getJdkHome() { return jdkHome; }
  public void setJdkHome(String s) { jdkHome = s; }
  public String getJdkHomeWithFwdSlashes()
  {
    return getJdkHome() == null ? "" : getJdkHome().replace('\\', '/');
  }

  private String host;
  public String getHost() { return host; }
  public void setHost(String s) { host = s; }

  private int port;
  public int getPort() { return port; }
  public void setPort(int i) { port = i; }

  private int commandPort;
  public int getCommandPort() { return commandPort; }
  public void setCommandPort(int i) { this.commandPort = i; }

  private String user;
  public String getUser() { return user; }
  public void setUser(String s) { user = s; }

  private String password;
  public String getPassword() { return password; }
  public void setPassword(String s) { password = s; }

  private String serverLoc;
  public String getServerLoc() { return serverLoc; }
  public void setServerLoc(String s) { serverLoc = s; }
  public String getServerLocWithFwdSlashes()
    { return getServerLoc().replace('\\', '/'); }
  public String getServerParentDir()
  {
    String domainLoc = getServerLocWithFwdSlashes();
    if (domainLoc.endsWith("/"))
      domainLoc = domainLoc.substring(0, domainLoc.length() - 1);
    return domainLoc.substring(0, domainLoc.lastIndexOf("/"));
  }

  private String domainName;
  public String getDomainName() { return domainName; }
  public void setDomainName(String s) { domainName = s; }

  private String serverName;
  public String getServerName() { return serverName; }
  public void setServerName(String s) { serverName = s; }

  public String getUrlBase()
  {
    return "http://" + host + ":" + port;
  }

  public String getConsoleUrl()
  {
    if (isWebLogic())
      return getUrlBase() + "/console";
    else if (isTomcat())
      return getUrlBase();
    else if (isServiceMix() || isFuse())
      return getUrlBase();
    else
      return getUrlBase() + "/jmx-console";
  }

  private String javaOptions;
  public String getJavaOptions()
  {
    // TODO: currently these are manually kept in sync with the constants in ServiceMixServerBehavior & FuseServerBehavior - should refactor
    if (javaOptions == null && isServiceMix())
      return "-server -Xms512m -Xmx1024m -XX:MaxPermSize=256m -Dderby.system.home=\"%KARAF_DATA%\\derby\" -Dderby.storage.fileSyncTransactionLog=true -Dcom.sun.management.jmxremote -Dkaraf.delay.console=false";
    else if (javaOptions == null && isFuse())
      return "-server -Xms512m -Xmx1024m -XX:MaxPermSize=256m -Dderby.system.home=\"%KARAF_DATA%\\derby\" -Dderby.storage.fileSyncTransactionLog=true -Dcom.sun.management.jmxremote -Dkaraf.delay.console=false -XX:+UnlockDiagnosticVMOptions -XX:+UnsyncloadClass";
    return javaOptions;
  }
  public void setJavaOptions(String opts) { this.javaOptions = opts; }

  private WorkflowProject project;
  public WorkflowProject getProject() { return project; }

  private boolean debug = true;
  public boolean isDebug() { return debug; }
  public void setDebug(boolean b) { debug = b; }

  private int debugPort;
  public int getDebugPort() { return debugPort; }
  public void setDebugPort(int i) { debugPort = i; }

  private boolean suspend;
  public boolean isSuspend() { return suspend; }
  public void setSuspend(boolean b) { suspend = b; }

  private String logWatcherHost;
  public String getLogWatcherHost() { return logWatcherHost; }
  public void setLogWatcherHost(String host) { this.logWatcherHost = host; }

  private int logWatcherPort;
  public int getLogWatcherPort() { return logWatcherPort; }
  public void setLogWatcherPort(int port) { this.logWatcherPort = port; }

  private int logWatcherTimeout;
  public int getLogWatcherTimeout() { return logWatcherTimeout; }
  public void setLogWatcherTimeout(int timeout) { this.logWatcherTimeout = timeout; }

  private String stubServerHost;
  public String getStubServerHost() { return stubServerHost; }
  public void setStubServerHost(String host) { this.stubServerHost = host; }

  private int stubServerPort;
  public int getStubServerPort() { return stubServerPort; }
  public void setStubServerPort(int port) { this.stubServerPort = port; }

  private int stubServerTimeout;
  public int getStubServerTimeout() { return stubServerTimeout; }
  public void setStubServerTimeout(int timeout) { this.stubServerTimeout = timeout; }

  public String getNamingProvider()
  {
    if (isServiceMix() || isFuse())
      return "OSGi";
    else
      return getContainerName();
  }

  public String getDataSourceProvider()
  {
    if (isServiceMix() || isFuse())
      return "OSGi";
    else if (isTomcat())
      return "MDW";
    else
      return getContainerName();
  }

  public String getJmsProvider()
  {
    if (isJavaEE())
      return getContainerName();
    else
      return "ActiveMQ";
  }

  public String getThreadPoolProvider()
  {
    if (isJavaEE())
      return getContainerName();
    else
      return "MDW";
  }

  public String getMessenger()
  {
    return "jms";
  }

  public boolean isJavaEE()
  {
    return isWebLogic() || isJBoss();
  }

  public boolean isWar()
  {
    return isTomcat();
  }

  public boolean isOsgi()
  {
    return isServiceMix() || isFuse();
  }

  public ServerSettings(WorkflowProject workflowProject)
  {
    this.project = workflowProject;
  }

  public ServerSettings(ServerSettings cloneFrom)
  {
    this.project = cloneFrom.project;
    this.home = cloneFrom.home;
    this.host = cloneFrom.host;
    this.port = cloneFrom.port;
    this.commandPort = cloneFrom.commandPort;
    this.containerType = cloneFrom.containerType;
    this.containerVersion = cloneFrom.containerVersion;
    this.jdkHome = cloneFrom.jdkHome;
    this.serverLoc = cloneFrom.serverLoc;
    this.user = cloneFrom.user;
    this.password = cloneFrom.password;
    this.serverName = cloneFrom.serverName;
    this.domainName = cloneFrom.domainName;

    // mdw server runner settings
    this.javaOptions = cloneFrom.javaOptions;
    this.debug = cloneFrom.debug;
    this.debugPort = cloneFrom.debugPort;
    this.suspend = cloneFrom.suspend;

    // log watcher settings
    this.logWatcherHost = cloneFrom.logWatcherHost;
    this.logWatcherPort = cloneFrom.logWatcherPort;
    this.logWatcherTimeout = cloneFrom.logWatcherTimeout;

    // stub server settings
    this.stubServerHost = cloneFrom.stubServerHost;
    this.stubServerPort = cloneFrom.stubServerPort;
    this.stubServerTimeout = cloneFrom.stubServerTimeout;
  }


  public String getServerProcessId() throws IOException, InterruptedException
  {
    // run jps to list the processes
    Process p = Runtime.getRuntime().exec(new String[] {PluginUtil.getJdkBin() + File.separator + "jps", "-l"});
    BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
    String line = null;
    String pid = null;
    while ((line = reader.readLine()) != null)
    {
      // TODO handle JBoss and ServiceMix
      if (isWebLogic())
      {
        int idx = line.indexOf("weblogic.Server");
        if (idx > 0)
        {
          pid = line.substring(0, idx - 1);
        }
      }
    }
    p.waitFor();

    return pid;
  }

  /**
   * Checks a string value for validity.
   *
   * @param s the string to check
   * @return whether the string is valid
   */
  public boolean checkString(String s)
  {
    return (s != null) && (s.length() > 0);
  }

  /**
   * Checks an int value for validity.
   *
   * @param s the int to check
   * @return whether the int is valid
   */
  public boolean checkInt(int i)
  {
    return i > 0;
  }

  // if settings are created by a ServiceMix server
  private IServer server;
  public IServer getServer() { return server; }
  public void setServer(IServer server) { this.server = server; }

  public String toString()
  {
    return "ServerSettings:\n"
      + "----------------\n"
      + "home: " + getHome() + "\n"
      + "host: " + getHost() + "\n"
      + "port: " + getPort() + "\n"
      + "containerType: " + getContainerType() + "\n"
      + "containerVersion: " + getContainerVersion() + "\n"
      + "jdkHome: " + getJdkHome() + "\n"
      + "serverLoc: " + getServerLoc() + "\n"
      + "user: " + getUser() + "\n"
      + "password: " + getPassword() + "\n"
      + "serverName: " + getServerName() + "\n"
      + "domainName: " + getDomainName() + "\n"
      + "debug: " + isDebug() + "\n"
      + "debug port: " + getDebugPort() + "\n"
      + "debug suspend: " + isSuspend() + "\n"
      + "logHost: " + getLogWatcherHost() + "\n"
      + "logPort: " + getLogWatcherPort() + "\n"
      + "logTimeout: " + getLogWatcherTimeout() + "\n"
      + "stubHost: " + getStubServerHost() + "\n"
      + "stubPort: " + getStubServerPort() + "\n"
      + "stubTimeout: " + getStubServerTimeout() + "\n"
      + "server: " + server;
  }

  public boolean equals(Object o)
  {
    if (!(o instanceof ServerSettings))
      return false;

    ServerSettings other = (ServerSettings) o;

    return isEqual(home, other.home)
      && isEqual(host, other.host)
      && port == other.port
      && isEqual(containerType, other.containerType)
      && isEqual(containerVersion, other.containerVersion)
      && isEqual(jdkHome, other.jdkHome)
      && isEqual(serverLoc, other.serverLoc)
      && isEqual(user, other.user)
      && isEqual(password, other.password)
      && isEqual(serverName, other.serverName)
      && isEqual(domainName, other.domainName)
      && isEqual(debug, other.debug)
      && debugPort == other.debugPort
      && suspend == other.suspend
      && isEqual(logWatcherHost, other.logWatcherHost)
      && logWatcherPort == other.logWatcherPort
      && logWatcherTimeout == other.logWatcherTimeout
      && isEqual(stubServerHost, other.stubServerHost)
      && stubServerPort == other.stubServerPort
      && stubServerTimeout == other.stubServerTimeout;
  }

  private boolean isEqual(Object one, Object two)
  {
    if (one == null)
      return two == null;
    else
      return one.equals(two);
  }
}
