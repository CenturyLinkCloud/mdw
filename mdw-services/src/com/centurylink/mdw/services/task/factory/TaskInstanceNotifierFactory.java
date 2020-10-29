package com.centurylink.mdw.services.task.factory;

import com.centurylink.mdw.cache.asset.PackageCache;
import com.centurylink.mdw.common.StrategyException;
import com.centurylink.mdw.constant.TaskAttributeConstant;
import com.centurylink.mdw.model.Attributes;
import com.centurylink.mdw.model.asset.AssetVersionSpec;
import com.centurylink.mdw.model.task.TaskTemplate;
import com.centurylink.mdw.model.workflow.Activity;
import com.centurylink.mdw.model.workflow.Package;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.observer.ObserverException;
import com.centurylink.mdw.observer.task.TaskNotifier;
import com.centurylink.mdw.observer.task.TemplatedNotifier;
import com.centurylink.mdw.service.data.task.TaskTemplateCache;
import com.centurylink.mdw.services.EventServices;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.task.types.TaskServiceRegistry;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

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
    public List<String> getNotifierSpecs(Long taskId, String outcome) throws IOException {
        TaskTemplate taskTemplate = TaskTemplateCache.getTaskTemplate(taskId);
        if (taskTemplate != null) {
            String noticesAttr = taskTemplate.getAttribute(TaskAttributeConstant.NOTICES);
            if (!StringUtils.isBlank(noticesAttr) && !"$DefaultNotices".equals(noticesAttr)) {
                return parseNoticesAttr(taskTemplate.getAttributes(), TaskAttributeConstant.NOTICES, outcome);
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
        EventServices eventManager = ServiceLocator.getEventServices();
        try {
            TaskTemplate taskVO = TaskTemplateCache.getTaskTemplate(taskId);
            if (processInstanceId != null) {
                Process process = eventManager.findProcessByProcessInstanceId(processInstanceId);
                if (process != null && process.getActivities() != null) {
                    // for compatibility with notifiers registered on activities
                    for (Activity activity : process.getActivities()) {
                        if (taskVO.getLogicalId().equals(activity.getAttribute(TaskAttributeConstant.TASK_LOGICAL_ID))) {
                            noticesAttr = activity.getAttribute(TaskAttributeConstant.NOTICES);
                            break;
                        }
                    }
                }
            }
            if (!StringUtils.isBlank(noticesAttr)) {
                return parseNoticesAttr(taskVO.getAttributes(), TaskAttributeConstant.NOTICES, outcome);
            }
            return getNotifierSpecs(taskId, outcome);
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            return null;
        }
    }

    /**
     * To parse notices attribute
     */
    private List<String> parseNoticesAttr(Attributes attributes, String attributeName, String outcome) {
        String noticesAttr = attributes.get(attributeName);
        List<String> notifiers = new ArrayList<>();
        int columnCount = 4;
        int colon = noticesAttr.indexOf(";");
        if (colon != -1) {
            columnCount = delimiterColumnCount(noticesAttr.substring(0, colon), ",", "\\,") + 1;
        }
        int notifierClassColIndex = columnCount > 3 ? 3 : 2;
        List<String[]> rows = attributes.getTable(noticesAttr, ',', ';', columnCount);
        for (String[] row : rows) {
            if (!StringUtils.isBlank(row[1]) && row[0].equals(outcome)) {
                StringTokenizer st = new StringTokenizer(row[notifierClassColIndex], ",");
                boolean hasCustomClass = false;
                String templateVerSpec = columnCount > 3 ? ":" + row[2] : "";
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

    public List<TaskNotifier> getNotifiers(Long taskId, String outcome) throws IOException {
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

    public TaskNotifier getNotifier(String notifierSpec, Long processInstanceId) {
        TaskNotifier notifier = null;
        Package packageVO = null;

        int k = notifierSpec.indexOf(':');
        int lastIndex = notifierSpec.lastIndexOf(":") == k ? 0 : notifierSpec.lastIndexOf(":");
        String className = k == -1 ? notifierSpec : notifierSpec.substring(0, k);

        try {
            if (processInstanceId != null) {
                EventServices eventManager = ServiceLocator.getEventServices();
                Process process = eventManager.findProcessByProcessInstanceId(processInstanceId);
                packageVO = PackageCache.getPackage(process.getPackageName());
            }
            notifier = getNotifierInstance(getNotifierClassName(className), packageVO);
            if (notifier instanceof TemplatedNotifier) {
                TemplatedNotifier templatedNotifier = (TemplatedNotifier) notifier;
                templatedNotifier.setTemplateSpec(new AssetVersionSpec(notifierSpec.substring(k + 1, lastIndex), notifierSpec.substring(lastIndex + 1)));
            }
            return notifier;
        }
        catch (Exception ex) {
            // don't disrupt processing; just log exception
            logger.error(ex.getMessage(), ex);
            return null;
        }
    }

    public TaskNotifier getNotifierInstance(String notifierClassName, Package pkg)
    throws StrategyException {
        try {
            TaskServiceRegistry registry = TaskServiceRegistry.getInstance();
            TaskNotifier notifier = registry.getDynamicNotifier(pkg, notifierClassName);
            if (notifier == null) {
                return pkg.getClass().getClassLoader().loadClass(notifierClassName).asSubclass(TaskNotifier.class).newInstance();
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

    // To count comma separated columns in a row to maintain compatibility
    public static int delimiterColumnCount(String row, String delimeterChar, String escapeChar) {
        if (row.indexOf(escapeChar) > 0)
            return row.replace(escapeChar, " ").length() - row.replace(",", "").length();
        else
            return row.length() - row.replace(",", "").length();
    }
}
