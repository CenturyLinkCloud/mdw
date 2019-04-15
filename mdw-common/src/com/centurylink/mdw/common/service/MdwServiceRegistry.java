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
package com.centurylink.mdw.common.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.centurylink.mdw.container.EmbeddedDbExtension;
import com.centurylink.mdw.model.monitor.ScheduledJob;
import com.centurylink.mdw.model.system.SystemMetric;
import com.centurylink.mdw.util.log.LogLineInjector;

public class MdwServiceRegistry extends ServiceRegistry {

    public static final List<String> mdwServices = new ArrayList<>(Arrays.asList(new String[] {
            JsonService.class.getName(),
            XmlService.class.getName(),
            ScheduledJob.class.getName(),
            RequestRoutingStrategy.class.getName(),
            EmbeddedDbExtension.class.getName(),
            LogLineInjector.class.getName(),
            SystemMetric.class.getName()}));

    protected MdwServiceRegistry(List<Class<? extends RegisteredService>> serviceInterfaces) {
        super(serviceInterfaces);
    }

    private static MdwServiceRegistry instance;
    public static MdwServiceRegistry getInstance() {
        if (instance == null) {
            List<Class<? extends RegisteredService>> services = new ArrayList<Class<? extends RegisteredService>>();
            services.add(JsonService.class);
            services.add(XmlService.class);
            services.add(ScheduledJob.class);
            services.add(RequestRoutingStrategy.class);
            services.add(EmbeddedDbExtension.class);
            services.add(LogLineInjector.class);
            services.add(SystemMetric.class);
            instance = new MdwServiceRegistry(services);
        }
        return instance;
    }

    public ScheduledJob getScheduledJob(String className) {
        // Cloud mode
        ScheduledJob dynamicScheduledJob = getDynamicService(null, ScheduledJob.class, className);
        if (dynamicScheduledJob != null)
            return dynamicScheduledJob;
        for (ScheduledJob scheduledJob : getServices(ScheduledJob.class)) {
            if (className.equals(scheduledJob.getClass().getName()))
                return scheduledJob;
        }
        return null;
    }

    public List<RequestRoutingStrategy> getRequestRoutingStrategies() {
        List<RequestRoutingStrategy> requestRoutingStrategies = new ArrayList<RequestRoutingStrategy>();
        requestRoutingStrategies.addAll(getDynamicServices(RequestRoutingStrategy.class));

        // Now sort them based on strategy's priority - Strategies with lower Priority values get used first
        List<RequestRoutingStrategy> requestRoutingStrategiesPrioritized = new ArrayList<RequestRoutingStrategy>();
        int i;
        for (RequestRoutingStrategy item : requestRoutingStrategies) {
            if (requestRoutingStrategiesPrioritized.size() == 0)
                requestRoutingStrategiesPrioritized.add(item);
            else {
                i = 0;
                while (i < requestRoutingStrategiesPrioritized.size() && item.getPriority() > requestRoutingStrategiesPrioritized.get(i).getPriority())
                    i++;

                requestRoutingStrategiesPrioritized.add(i, item);
            }
        }
        return requestRoutingStrategiesPrioritized;
    }

    public List<EmbeddedDbExtension> getEmbeddedDbExtensions() {
        return getDynamicServices(EmbeddedDbExtension.class);
    }
}
