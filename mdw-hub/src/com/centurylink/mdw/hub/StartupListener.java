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
package com.centurylink.mdw.hub;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.container.NamingProvider;

@WebListener
public class StartupListener implements ServletContextListener {

    private static MdwMain mdwMain;

    @Override
    public void contextInitialized(ServletContextEvent contextEvent) {
        ServletContext servletContext = contextEvent.getServletContext();
        mdwMain = new MdwMain();
        String container = NamingProvider.TOMCAT; // TODO
        if (ApplicationContext.isSpringBoot()) {
            mdwMain.startup(container, SpringBootApplication.getBootDir().toString(), servletContext.getContextPath());
        }
        else {
            mdwMain.startup(container, servletContext.getRealPath("/"), servletContext.getContextPath());
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent contextEvent) {
        if (mdwMain != null)
            mdwMain.shutdown();
    }

}
