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
package com.centurylink.mdw.services.task.factory;

import com.centurylink.mdw.cache.impl.PackageCache;
import com.centurylink.mdw.common.StrategyException;
import com.centurylink.mdw.common.service.RegisteredService;
import com.centurylink.mdw.model.workflow.Package;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.observer.task.AutoAssignStrategy;
import com.centurylink.mdw.observer.task.PrioritizationStrategy;
import com.centurylink.mdw.observer.task.RoutingStrategy;
import com.centurylink.mdw.observer.task.SubTaskStrategy;
import com.centurylink.mdw.services.EventServices;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.task.types.TaskServiceRegistry;

public class TaskInstanceStrategyFactory {

    public static final String STRATEGY_IMPL_PACKAGE = "com.centurylink.mdw.workflow.task.strategy";
    public static final String DROOLS_IMPL_PACKAGE = "com.centurylink.mdw.drools";

    public enum StrategyType {
        AutoAssignStrategy,
        RoutingStrategy,
        PrioritizationStrategy,
        SubTaskStrategy
    }

    private static TaskInstanceStrategyFactory factoryInstance;
    public static TaskInstanceStrategyFactory getInstance() {
        if (factoryInstance == null)
            factoryInstance = new TaskInstanceStrategyFactory();
        return factoryInstance;
    }

    /**
     * Returns a workgroup routing strategy instance based on an attribute value
     * which can consist of either the routing strategy logical name, or an xml document.
     * @param attributeValue
     * @return the routing strategy implementor instance
     */
    public static RoutingStrategy getRoutingStrategy(String attributeValue) throws StrategyException {
        TaskInstanceStrategyFactory factory = getInstance();
        String className = factory.getStrategyClassName(attributeValue, StrategyType.RoutingStrategy);
        RoutingStrategy strategy = (RoutingStrategy) factory.getStrategyInstance(RoutingStrategy.class, className, null);
        return strategy;
    }

    /**
     *  Returns a workgroup routing strategy instance based on an attribute value and bundle spec
     *  which can consist of either the routing strategy logical name, or an xml document.
     * @param attributeValue
     * @param processInstanceId
     * @return
     * @throws StrategyException
     */
    public static RoutingStrategy getRoutingStrategy(String attributeValue, Long processInstanceId) throws StrategyException {
        Package packageVO = processInstanceId == null ? null : getPackage(processInstanceId);
        TaskInstanceStrategyFactory factory = getInstance();
        String className = factory.getStrategyClassName(attributeValue, StrategyType.RoutingStrategy);
        RoutingStrategy strategy = (RoutingStrategy) factory.getStrategyInstance(RoutingStrategy.class, className, packageVO);
        return strategy;
    }

    /**
     * Returns a subtask strategy instance based on an attribute value
     * which can consist of either the subtask strategy logical name, or an xml document.
     * @param attributeValue
     * @return the subtask strategy implementor instance
     */
    public static SubTaskStrategy getSubTaskStrategy(String attributeValue) throws StrategyException {
        TaskInstanceStrategyFactory factory = getInstance();
        String className = factory.getStrategyClassName(attributeValue, StrategyType.SubTaskStrategy);
        SubTaskStrategy strategy = (SubTaskStrategy) factory.getStrategyInstance(SubTaskStrategy.class, className, null);
        return strategy;
    }

    /**
     * Returns a subtask strategy instance based on an attribute value and bundle spec
     * which can consist of either the subtask strategy logical name, or an xml document.
     * @param attributeValue
     * @return the subtask strategy implementor instance
     */
    public static SubTaskStrategy getSubTaskStrategy(String attributeValue, Long processInstanceId) throws StrategyException {
        Package packageVO = processInstanceId == null ? null : getPackage(processInstanceId);
        TaskInstanceStrategyFactory factory = getInstance();
        String className = factory.getStrategyClassName(attributeValue, StrategyType.SubTaskStrategy);
        SubTaskStrategy strategy = (SubTaskStrategy) factory.getStrategyInstance(SubTaskStrategy.class, className, packageVO);
        return strategy;
    }

    /**
     * Returns a Prioritization routing strategy instance based on an attribute value
     * which can consist of either the routing strategy logical name, or an xml document.
     * @param attributeValue
     * @return the Prioritization strategy implementor instance
     */
    public static PrioritizationStrategy getPrioritizationStrategy(String attributeValue) throws StrategyException {
        TaskInstanceStrategyFactory factory = getInstance();
        String className = factory.getStrategyClassName(attributeValue, StrategyType.PrioritizationStrategy);
        PrioritizationStrategy strategy = (PrioritizationStrategy) factory.getStrategyInstance(PrioritizationStrategy.class, className, null);
        return strategy;
    }

    /**
     * Returns a Prioritization routing strategy instance based on an attribute value and bundle spec
     * which can consist of either the routing strategy logical name, or an xml document.
     * @param attributeValue
     * @return the Prioritization strategy implementor instance
     */
    public static PrioritizationStrategy getPrioritizationStrategy(String attributeValue, Long processInstanceId) throws StrategyException {
        Package packageVO = processInstanceId == null ? null : getPackage(processInstanceId);
        TaskInstanceStrategyFactory factory = getInstance();
        String className = factory.getStrategyClassName(attributeValue, StrategyType.PrioritizationStrategy);
        PrioritizationStrategy strategy = (PrioritizationStrategy) factory.getStrategyInstance(PrioritizationStrategy.class, className, packageVO);
        return strategy;
    }

   /**
     * Returns an auto-assign strategy instance based on the logical name.
     * @param logicalName
     * @return the strategy implementor instance
     */
    public static AutoAssignStrategy getAutoAssignStrategy(String logicalName) throws StrategyException {
        TaskInstanceStrategyFactory factory = getInstance();
        String className = factory.getStrategyClassName(logicalName, StrategyType.AutoAssignStrategy);
        return (AutoAssignStrategy) factory.getStrategyInstance(AutoAssignStrategy.class, className, null);
    }

    /**
     * Returns an auto-assign strategy instance based on the logical name and bundle spec.
     * @param logicalName
     * @return the strategy implementor instance
     */
    public static AutoAssignStrategy getAutoAssignStrategy(String logicalName, Long processInstanceId) throws StrategyException {
        Package packageVO = processInstanceId == null ? null : getPackage(processInstanceId);
        TaskInstanceStrategyFactory factory = getInstance();
        String className = factory.getStrategyClassName(logicalName, StrategyType.AutoAssignStrategy);
        return (AutoAssignStrategy) factory.getStrategyInstance(AutoAssignStrategy.class, className, packageVO);
    }

    public Object getStrategyInstance(Class<? extends RegisteredService> strategyInterface, String strategyClassName, Package packageVO) throws StrategyException {
        try {
            TaskServiceRegistry registry = TaskServiceRegistry.getInstance();
            // cloud mode
            Object strategy = registry.getDynamicStrategy(packageVO, strategyInterface, strategyClassName);
            if (strategy == null) {
                strategy = packageVO.getClassLoader().loadClass(strategyClassName).newInstance();
            }
            return strategy;
        }
        catch (Exception ex) {
            throw new StrategyException(ex.getMessage(), ex);
        }
    }

    private String getStrategyClassName(String attrValue, StrategyType type) {
        String logicalName;
        if (isXml(attrValue)) {
            logicalName = "something";  // TODO
        }
        else {
            logicalName = attrValue;
        }

        if (logicalName.indexOf(".") > 0) {
            return logicalName;  // full package specified (custom strategy)
        }
        else if (logicalName.contains("Rules-Based"))
            return DROOLS_IMPL_PACKAGE + "." + logicalName.replaceAll(" ", "").replaceAll("-", "") + type.toString();
        else {
            return STRATEGY_IMPL_PACKAGE + "." + logicalName.replaceAll(" ", "").replaceAll("-", "") + type.toString();
        }
    }

    private boolean isXml(String attrValue) {
        return attrValue.indexOf('<') >= 0 || attrValue.indexOf('>') >= 0;
    }

    /**
     * Get Package
     * @param processInstanceId
     * @return
     */
    private static Package getPackage(Long processInstanceId) {
        try {
            EventServices eventManager = ServiceLocator.getEventServices();
            Process process = eventManager.findProcessByProcessInstanceId(processInstanceId);
            Package packageVO = PackageCache.getProcessPackage(process.getId());
            return packageVO;
        }
        catch (Exception ex) {
            return null;
        }
    }

}
