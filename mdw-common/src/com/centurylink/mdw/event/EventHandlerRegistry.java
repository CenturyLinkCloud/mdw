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
package com.centurylink.mdw.event;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.centurylink.mdw.common.service.RegisteredService;
import com.centurylink.mdw.common.service.ServiceRegistry;

/**
 * Registry for MDW Default External Event Handlers.
 */
public class EventHandlerRegistry extends ServiceRegistry {

    public static final List<String> eventHandlerServices = new ArrayList<String>(Arrays.asList(new String[] {DefaultExternalEventHandler.class.getName()}));

    protected EventHandlerRegistry(List<Class<? extends RegisteredService>> serviceInterfaces) {
        super(serviceInterfaces);
    }

    private static EventHandlerRegistry instance;
    public static EventHandlerRegistry getInstance() {
        if (instance == null) {
            List<Class<? extends RegisteredService>> services = new ArrayList<Class<? extends RegisteredService>>();
            services.add(DefaultExternalEventHandler.class);
            instance = new EventHandlerRegistry(services);
        }
        return instance;
    }

    public List<DefaultExternalEventHandler> getDefaultEventHandlers() {
        List<DefaultExternalEventHandler> eventHandlers = new ArrayList<DefaultExternalEventHandler>();
        eventHandlers.addAll(super.getDynamicServices(DefaultExternalEventHandler.class));
        return eventHandlers;
    }
}
