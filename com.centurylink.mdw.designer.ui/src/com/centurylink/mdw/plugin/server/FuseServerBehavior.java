/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.server;

import com.centurylink.mdw.plugin.project.model.ServerSettings;
import com.centurylink.mdw.plugin.project.model.ServerSettings.ContainerType;

public class FuseServerBehavior extends ServiceMixServerBehavior
{
  public static final String DEFAULT_JAVA_OPTS = "-server -Xms512m -Xmx1024m -XX:MaxPermSize=256m -Dderby.system.home=\"%KARAF_DATA%\\derby\" -Dderby.storage.fileSyncTransactionLog=true -Dcom.sun.management.jmxremote -Dkaraf.delay.console=false -XX:+UnlockDiagnosticVMOptions -XX:+UnsyncloadClass";

  @Override
  ServerSettings getServerSettings()
  {
    ServerSettings serverSettings = super.getServerSettings();
    serverSettings.setContainerType(ContainerType.Fuse);
    serverSettings.setJavaOptions(getServer().getAttribute(JAVA_OPTIONS, DEFAULT_JAVA_OPTS));
    return serverSettings;
  }
}
