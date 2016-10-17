/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services.task.factory;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import com.centurylink.mdw.common.cache.impl.PackageVOCache;
import com.centurylink.mdw.common.constant.TaskAttributeConstant;
import com.centurylink.mdw.common.exception.ObserverException;
import com.centurylink.mdw.common.exception.StrategyException;
import com.centurylink.mdw.common.task.TaskServiceRegistry;
import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.model.value.activity.ActivityVO;
import com.centurylink.mdw.model.value.attribute.AssetVersionSpec;
import com.centurylink.mdw.model.value.process.PackageVO;
import com.centurylink.mdw.model.value.process.ProcessVO;
import com.centurylink.mdw.model.value.task.TaskVO;
import com.centurylink.mdw.observer.task.TaskNotifier;
import com.centurylink.mdw.observer.task.TemplatedNotifier;
import com.centurylink.mdw.services.EventManager;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.dao.task.cache.TaskTemplateCache;

public class TaskInstanceNotifierFactory {

    private static final String NOTIFIER_PACKAGE = "com.centurylink.mdw.workflow.task.notifier";
    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    private static TaskInstanceNotifierFactory instance;
    public static TaskInstanceNotifierFactory getInstance() {
        if (instance == null)
            instance = new TaskInstanceNotifierFactory();
        return instance;
    }

    /**
     * Returns a list of notifier class name and template name pairs, delimited by colon
     * @param taskId
     * @param outcome
     * @return the registered notifier or null if not found
     */
    public List<String> getNotifierSpecs(Long taskId, String outcome) throws ObserverException {
        TaskVO taskVO = TaskTemplateCache.getTaskTemplate(taskId);
        if (taskVO != null) {
            String noticesAttr = taskVO.getAttribute(TaskAttributeConstant.NOTICES);
            if (!StringHelper.isEmpty(noticesAttr) && !"$DefaultNotices".equals(noticesAttr)) {
                return parseNoticiesAttr(noticesAttr, outcome);
            }
        }
        return null;
    }
    /**
     * Return a list of notifier class name and template name and version pairs
     * Get the template name and notifier class names based on the process activity attributes to get the relevant values for in flight and new processes
     * @param taskId
     * @param processInstanceId
     * @param outcome
     * @return
     * @throws ObserverException
     */
    public List<String> getNotifierSpecs(Long taskId, Long processInstanceId, String outcome) throws ObserverException {
        String noticesAttr = null;
        EventManager eventManager = ServiceLocator.getEventManager();
        try {
            if (processInstanceId != null) {
                ProcessVO process = eventManager.findProcessByProcessInstanceId(processInstanceId);
                if (process != null && process.getActivities() != null) {
                    TaskVO taskVO = TaskTemplateCache.getTaskTemplate(taskId);
                    for (ActivityVO activity : process.getActivities()) {
                        if (taskVO.getLogicalId().equals(activity.getAttribute(TaskAttributeConstant.TASK_LOGICAL_ID))) {
                            noticesAttr = activity.getAttribute(TaskAttributeConstant.NOTICES);
                            break;
                        }
                    }
                }
            }
            if (!StringHelper.isEmpty(noticesAttr)) {
                return parseNoticiesAttr(noticesAttr, outcome);
              }
            return getNotifierSpecs(taskId, outcome); // For compatibility
        } catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            return null;
        }
    }

    /**
     * To parse notices attribute
     * @param noticesAttr
     * @return
     */
    private List<String> parseNoticiesAttr(String noticesAttr, String outcome) {
        List<String> notifiers = new ArrayList<String>();
        int columnCount = StringHelper.delimiterColumnCount(noticesAttr.substring(0, noticesAttr.indexOf(";")), ",", "\\,")+1;
        int notifierClassColIndex = columnCount > 3 ? 3 : 2 ;
        List<String[]> rows = StringHelper.parseTable(noticesAttr, ',', ';', columnCount);
        for (String[] row : rows) {
            if (!StringHelper.isEmpty(row[1]) && row[0].equals(outcome)) {
                StringTokenizer st = new StringTokenizer(row[notifierClassColIndex], "# ,;");
                boolean hasCustomClass = false;
                String templateVerSpec = columnCount > 3 ? ":"+row[2] : "";
                while (st.hasMoreTokens()) {
                    String className = st.nextToken();
                    className = getNotifierClassName(className);
                    notifiers.add(className + ":" + row[1] + templateVerSpec);
                    hasCustomClass = true;
                }
                if (!hasCustomClass) {
                    notifiers.add(NOTIFIER_PACKAGE + ".TaskEmailNotifier:" +":" + row[1] + templateVerSpec);
                }
            }
        }
        return notifiers;
    }

    public List<TaskNotifier> getNotifiers(Long taskId, String outcome) throws ObserverException {
        List<TaskNotifier> notifiers = new ArrayList<TaskNotifier>();
        List<String> notifierSpecs = getNotifierSpecs(taskId, outcome);
        if (notifierSpecs != null && !notifierSpecs.isEmpty()) {
            for (String notifierSpec : notifierSpecs) {
                TaskNotifier notifier = getNotifier(notifierSpec, null);
                if (notifier != null)
                    notifiers.add(notifier);
            }
        }
        return notifiers;
    }

    public List<TaskNotifier> getNotifiers(Long taskId, Long processInstanceId, String outcome) throws ObserverException {
        if (processInstanceId == null) return getNotifiers(taskId, outcome);
        List<TaskNotifier> notifiers = new ArrayList<TaskNotifier>();
        List<String> notifierSpecs = getNotifierSpecs(taskId, processInstanceId, outcome);
        if (notifierSpecs != null && !notifierSpecs.isEmpty()) {
            for (String notifierSpec : notifierSpecs) {
                TaskNotifier notifier = getNotifier(notifierSpec, processInstanceId);
                if (notifier != null)
                    notifiers.add(notifier);
            }
        }
        return notifiers;
    }

    public TaskNotifier getNotifier(String notifierSpec, Long processInstanceId) {
        TaskNotifier notifier = null;
        PackageVO packageVO = null;

        int k = notifierSpec.indexOf(':');
        int lastIndex = notifierSpec.lastIndexOf(":") == k ? 0 : notifierSpec.lastIndexOf(":");
        String className = k == -1 ? notifierSpec : notifierSpec.substring(0, k);

        try {
            if (processInstanceId != null) {
                EventManager eventManager = ServiceLocator.getEventManager();
                ProcessVO process = eventManager.findProcessByProcessInstanceId(processInstanceId);
                packageVO = PackageVOCache.getProcessPackage(process.getId());
            }
            notifier = getNotifierInstance(getNotifierClassName(className), packageVO);
            if (notifier instanceof TemplatedNotifier) {
                if (lastIndex > 0) {
                    ((TemplatedNotifier) notifier).setTemplateSpec(new AssetVersionSpec(notifierSpec.substring(k + 1, lastIndex), notifierSpec.substring(lastIndex + 1)));
                } else {
                    ((TemplatedNotifier) notifier).setTemplate(notifierSpec.substring(k + 1));
                }
            }
            return notifier;
        }
        catch (Exception ex) {
            // don't disrupt processing; just log exception
            logger.severeException(ex.getMessage(), ex);
            return null;
        }
    }

     public TaskNotifier getNotifierInstance(String notifierClassName, PackageVO packageVO)
    throws StrategyException {
        try {
            TaskServiceRegistry registry = TaskServiceRegistry.getInstance();
            // Cloud mode
            TaskNotifier notifier = registry.getDynamicNotifier(packageVO, notifierClassName);
            if (notifier == null) {
                return packageVO.getClassLoader().loadClass(notifierClassName).asSubclass(TaskNotifier.class).newInstance();
            }
            return notifier;
        }
        catch (Exception ex) {
            throw new StrategyException(ex.getMessage(), ex);
        }
    }

    private String getNotifierClassName(String attrValue) {
        String logicalName;
        if (isXml(attrValue)) {
            logicalName = "something";  // TODO
        }
        else {
            logicalName = attrValue;
        }

        if (logicalName.indexOf(".") > 0) {
            return logicalName;  // full package specified
        }
        else {
            return NOTIFIER_PACKAGE + "." + logicalName;
        }
    }

    private boolean isXml(String attrValue) {
        return attrValue.indexOf('<') >= 0 || attrValue.indexOf('>') >= 0;
    }


}
