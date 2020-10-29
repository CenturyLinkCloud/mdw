package com.centurylink.mdw.workflow.activity.java;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.activity.types.JavaActivity;
import com.centurylink.mdw.annotations.Activity;
import com.centurylink.mdw.java.*;
import com.centurylink.mdw.model.variable.Variable;
import com.centurylink.mdw.model.workflow.ActivityRuntimeContext;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.services.process.ActivityLogger;
import com.centurylink.mdw.util.log.StandardLogger.LogLevel;
import com.centurylink.mdw.util.timer.Tracked;
import com.centurylink.mdw.workflow.activity.DefaultActivityImpl;
import org.apache.commons.lang.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tracked(LogLevel.TRACE)
@Activity(value="Dynamic Java", category=JavaActivity.class, icon="com.centurylink.mdw.base/java.png",
        pagelet="com.centurylink.mdw.base/dynamicJava.pagelet")
public class DynamicJavaActivity extends DefaultActivityImpl implements JavaActivity {

    public static final String JAVA_CODE = "Java";
    public static final String CLASS_NAME = "ClassName";

    private String javaCode;

    protected JavaExecutor executorInstance;

    private ClassLoader executorClassLoader;
    public ClassLoader getExecutorClassLoader() {

        if (executorClassLoader == null)
            executorClassLoader = getClass().getClassLoader();  // fallback in case not set by activity provider
        if (isLogDebugEnabled())
            logDebug("Dynamic Java ClassLoader: " + executorClassLoader);

        return executorClassLoader;
    }
    public void setExecutorClassLoader(ClassLoader loader) { this.executorClassLoader = loader; }

    @Override
    protected void initialize(ActivityRuntimeContext runtimeContext) throws ActivityException {
        if (getPerformanceLevel() < 9)
            runtimeContext.setLogPersister(ActivityLogger::persist);

        javaCode = runtimeContext.getAttributes().get(JAVA_CODE);

        if (StringUtils.isBlank(javaCode))
            throw new ActivityException("Missing attribute: " + JAVA_CODE);

        // output docs
        setOutputDocuments(getAttributes().containsKey(OUTPUTDOCS) ? getAttributes().getList(OUTPUTDOCS).toArray(new String[0]) : new String[0]);

        // initialize the executor
        try {
            getExecutorInstance().initialize(runtimeContext);
        }
        catch (Exception ex) {
            logError(ex.getMessage(), ex);
            throw new ActivityException(ex.getMessage(), ex);
        }
    }

    @Override
    public void execute() throws ActivityException {
        try {
            // run the executor
            Process process = getMainProcessDefinition();
            List<Variable> variables = process.getVariables();
            Map<String,Object> bindings = new HashMap<>();
            for (Variable variable: variables) {
                bindings.put(variable.getName(), getValue(variable.getName()));
            }

            Object result = getExecutorInstance().execute(bindings);

            for (Variable variable: variables) {
                String variableName = variable.getName();
                Object bindValue = bindings.get(variableName);
                String variableType = variable.getType();
                Object value = bindValue;
                if (variableType.equals("java.lang.String") && value != null)
                    value = value.toString();
                setVariableValue(variableName, variableType, value);
            }

            if (result != null) {
                setReturnCode(result.toString());
            }
        }
        catch (MdwJavaException ex) {
            logError(ex.getMessage(), ex);
            throw new ActivityException(ex.getMessage(), ex);
        }
    }

    public JavaExecutor getExecutorInstance() throws MdwJavaException {
        if (executorInstance == null) {
            try {
                String className = getClassName();

                setExecutorClassLoader(getPackage().getClassLoader());
                Class<?> clazz = CompiledJavaCache.getClass(getExecutorClassLoader(), getPackage(), className, javaCode);
                if (clazz == null)
                    throw new ClassNotFoundException(className);

                executorInstance = (JavaExecutor) clazz.newInstance();
            }
            catch (Exception ex) {
                getLogger().error(ex.getMessage(), ex);
                throw new MdwJavaException(ex.getMessage(), ex);
            }
        }
        return executorInstance;
    }

    protected String getClassName() {
        String packageName = getPackageName();

        String className = getAttributeValue(CLASS_NAME);
        if (className == null) {
            // fall back to old logic based on activity name, which can lead to collisions
            className = JavaNaming.getValidClassName(getActivityName() + "_" + getAttributeValue("LOGICAL_ID"));
        }

        Process process = getMainProcessDefinition();
        if (process.isArchived()) {
            // inflight loaded from asset history
            String newClassName = className + "_" + process.getId();
            javaCode = javaCode.replace(className, newClassName);
            className = newClassName;
        }
        else if (process.getId() == null || process.getId() == 0L) {
            // edited instance definition
            String newClassName = className + "_" + getProcessInstanceId();
            javaCode = javaCode.replace(className, newClassName);
            className = newClassName;
        }

        return packageName + "." + className;
    }

    protected String getPackageName() {
        return JavaNaming.getValidPackageName(getPackage().getName());
    }
}

