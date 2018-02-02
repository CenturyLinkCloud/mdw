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
package com.centurylink.mdw.system.filepanel;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;

import org.cloudfoundry.client.lib.ApplicationLogListener;
import org.cloudfoundry.client.lib.CloudCredentials;
import org.cloudfoundry.client.lib.CloudFoundryClient;
import org.cloudfoundry.client.lib.HttpProxyConfiguration;
import org.cloudfoundry.client.lib.domain.ApplicationLog;
import org.json.JSONObject;

import com.centurylink.mdw.annotations.RegisteredService;
import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.startup.StartupException;
import com.centurylink.mdw.startup.StartupService;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

@RegisteredService(StartupService.class)
public class CfLogListener implements StartupService {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    private CloudFoundryClient client;

    @Override
    public void onStartup() throws StartupException {
        if (ApplicationContext.isPaaS()) {
            List<String> filepanelDirs = PropertyManager.getListProperty("mdw." + PropertyNames.FILEPANEL_ROOT_DIRS);
            if (filepanelDirs != null) {
                // make sure environment variables are set (TODO: oauth2)
                String cfApiUser = System.getenv("CF_API_USER");
                String cfApiPassword = System.getenv("CF_API_PASSWORD");
                if (cfApiUser == null || cfApiPassword == null) {
                    logger.severe("***** WARNING: Missing CF_API_USER or CF_API_PASSWORD environment variables.\n"
                            + "*****          FilePanel log accumulation will not function.");
                    return;
                }
                String logDir0 = filepanelDirs.get(0);
                File logDir = new File(logDir0);
                try {
                    if (!logDir.isDirectory() && !logDir.mkdirs())
                        throw new IOException("Log dir does not exist and cannot be created: " + logDir.getAbsolutePath());
                    File outputFile = new File(logDir + "/mdw.log");
                    // infer the cf api url
                    URL servicesUrl = new URL(ApplicationContext.getServicesUrl());
                    String serviceHost = servicesUrl.getHost();
                    int dot = serviceHost.indexOf('.');
                    URL cfApiUrl = new URL("https://api" + serviceHost.substring(dot));
                    JSONObject appJson = new JSONObject(System.getenv("VCAP_APPLICATION"));
                    String appName = appJson.getString("application_name");

                    CloudCredentials credentials = new CloudCredentials(cfApiUser, cfApiPassword);
                    logger.info("Connecting to Cloud Foundry API: " + cfApiUrl);
                    client = new CloudFoundryClient(credentials, cfApiUrl, (HttpProxyConfiguration)null, false);
                    client.login();

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            client.streamLogs(appName, new LogCollector(outputFile));
                        }
                    }, "LogCollector").start();
                }
                catch (Exception ex) {
                    throw new StartupException("Startup error: " + ex.getMessage(), ex);
                }
            }
        }
    }

    @Override
    public void onShutdown() {
        if (client != null) {
            client.logout();
        }
    }

    /**
     * Poor man's listener.  Just appends to designated file.
     * TODO: rotation based on (size?, duration?)
     */
    public class LogCollector implements ApplicationLogListener {

        private Path outputPath;

        public LogCollector(File file) {
            this.outputPath = Paths.get(file.getPath());
        }

        @Override
        public void onMessage(ApplicationLog log) {
            try {
                if (log.getSourceName() != null && !log.getSourceName().startsWith("APP/PROC/WEB"))
                    Files.write(outputPath, log.getMessage().getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            }
            catch (IOException ex) {
                logger.severeException("LogCollector error: " + ex.getMessage(), ex);
            }
        }

        @Override
        public void onError(Throwable th) {
            logger.severeException("LogCollector error: " + th.getMessage(), th);
            try {
                Files.write(outputPath, th.toString().getBytes(), StandardOpenOption.APPEND);
            }
            catch (IOException ex) {
                // well, we tried
            }
        }

        @Override
        public void onComplete() {
            // TODO Auto-generated method stub
        }
    }
}

