/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.centurylink.mdw.model.data.monitor.ScheduledJob;

public class MdwServiceRegistry extends ServiceRegistry {

    public static final List<String> mdwServices = new ArrayList<String>(Arrays.asList(new String[] {JsonService.class.getName(), XmlService.class.getName(),
            ScheduledJob.class.getName(), RequestRoutingStrategy.class.getName()}));

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
            instance = new MdwServiceRegistry(services);
        }
        return instance;
    }

    public JsonService getJsonService(String className) {
        // retrieve from Cloud mode
        JsonService dynamicJsonService = getDynamicService(null, JsonService.class, className);
        if (dynamicJsonService != null)
            return dynamicJsonService;
        for (JsonService service : getServices(JsonService.class)) {
            if (className.equals(service.getClass().getName()))
                return service;
        }
        return null;
    }

    public XmlService getXmlService(String className) {
        //retrieve from Cloud mode
        XmlService dynamicXmlService = getDynamicService(null, XmlService.class, className);
        if (dynamicXmlService != null)
            return dynamicXmlService;
        for (XmlService service : getServices(XmlService.class)) {
            if (className.equals(service.getClass().getName()))
                return service;
        }
        return null;
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

    public RequestRoutingStrategy getRequestRoutingStrategy(String className) {
        // Cloud mode
        RequestRoutingStrategy dynamicStrategy = getDynamicService(null, RequestRoutingStrategy.class, className);
        if (dynamicStrategy != null)
            return dynamicStrategy;
        for (RequestRoutingStrategy routingStrategy : getServices(RequestRoutingStrategy.class)) {
            if (className.equals(routingStrategy.getClass().getName()))
                return routingStrategy;
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

}
