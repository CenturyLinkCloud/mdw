/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.task.types;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.centurylink.mdw.common.service.RegisteredService;
import com.centurylink.mdw.common.service.ServiceRegistry;
import com.centurylink.mdw.model.workflow.Package;
import com.centurylink.mdw.observer.task.AutoAssignStrategy;
import com.centurylink.mdw.observer.task.PrioritizationStrategy;
import com.centurylink.mdw.observer.task.RoutingStrategy;
import com.centurylink.mdw.observer.task.SubTaskStrategy;
import com.centurylink.mdw.observer.task.TaskIndexProvider;
import com.centurylink.mdw.observer.task.TaskNotifier;

public class TaskServiceRegistry extends ServiceRegistry {

    // Task Registered Services
    public static final List<String> taskServices = new ArrayList<String>(Arrays.asList(new String[] {PrioritizationStrategy.class.getName(), RoutingStrategy.class.getName(),
            AutoAssignStrategy.class.getName(), SubTaskStrategy.class.getName(), TaskNotifier.class.getName(), TaskIndexProvider.class.getName()}));

    protected TaskServiceRegistry(List<Class<? extends RegisteredService>> serviceInterfaces) {
        super(serviceInterfaces);
    }

    private static TaskServiceRegistry instance;
    public static TaskServiceRegistry getInstance() {
        if (instance == null) {
            List<Class<? extends RegisteredService>> services = new ArrayList<Class<? extends RegisteredService>>();
            services.add(PrioritizationStrategy.class);
            services.add(RoutingStrategy.class);
            services.add(AutoAssignStrategy.class);
            services.add(SubTaskStrategy.class);
            services.add(TaskNotifier.class);
            services.add(TaskIndexProvider.class);
            instance = new TaskServiceRegistry(services);
        }
        return instance;
    }

    public <T extends RegisteredService> T getStrategy(Class<T> strategyInterface, String className) {
        for (T strategy : super.getServices(strategyInterface)) {
            if (className.equals(strategy.getClass().getName()))
                return strategy;
        }
        return null;
    }

    /**
     * Cloud mode
     * @param packageVO
     * @param strategyInterface
     * @param strategyClassName
     * @return
     */
    public Object getDynamicStrategy(Package packageVO, Class<? extends RegisteredService> strategyInterface, String strategyClassName) {
        return super.getDynamicService(packageVO, strategyInterface, strategyClassName);
    }

    public TaskNotifier getNotifier(String className) {
        for (TaskNotifier notifier : super.getServices(TaskNotifier.class)) {
            if (className.equals(notifier.getClass().getName()))
                return notifier;
        }
        return null;
    }


    /**
     * Cloud mode
     * @param packageVO
     * @param class1
     * @param notifierClassName
     * @return
     */
    public TaskNotifier getDynamicNotifier(Package packageVO, String className) {
        return super.getDynamicService(packageVO, TaskNotifier.class, className);
    }

    public TaskIndexProvider getIndexProvider(Package packageVO, String className) {
        TaskIndexProvider provider = getDynamicService(packageVO, TaskIndexProvider.class, className);
        if (provider == null) {
            for (TaskIndexProvider otherProvider : getServices(TaskIndexProvider.class)) {
                if (className.equals(otherProvider.getClass().getName())) {
                    return otherProvider;
                }
            }
        }
        return provider;
    }

}

