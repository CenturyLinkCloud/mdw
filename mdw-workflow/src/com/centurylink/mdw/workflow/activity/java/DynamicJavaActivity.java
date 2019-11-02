/*
 * Copyright (C) 2018 CenturyLink, Inc.
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
package com.centurylink.mdw.workflow.activity.java;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.annotations.Activity;
import com.centurylink.mdw.cache.impl.PackageCache;
import com.centurylink.mdw.java.*;
import com.centurylink.mdw.model.attribute.Attribute;
import com.centurylink.mdw.model.variable.Variable;
import com.centurylink.mdw.model.workflow.ActivityRuntimeContext;
import com.centurylink.mdw.model.workflow.Package;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.services.process.ActivityLogger;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import com.centurylink.mdw.util.log.StandardLogger.LogLevel;
import com.centurylink.mdw.util.timer.Tracked;
import com.centurylink.mdw.workflow.activity.DefaultActivityImpl;
import org.apache.commons.lang.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tracked(LogLevel.TRACE)
@Activity(value="Dynamic Java", icon="com.centurylink.mdw.base/java.jpg",
        pagelet="com.centurylink.mdw.base/dynamicJava.pagelet")
public class DynamicJavaActivity extends DefaultActivityImpl implements DynamicJavaImplementor {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    public static final String JAVA_CODE = "Java";
    public static final String CLASS_NAME = "ClassName";

    private Package tempPkg;

    private String javaCode;
    public String getJavaCode() { return javaCode; }

    protected JavaExecutor executorInstance;

    private ClassLoader executorClassLoader;
    public ClassLoader getExecutorClassLoader() {

        if (executorClassLoader == null)
            executorClassLoader = getClass().getClassLoader();  // fallback in case not set by activity provider
        if (isLogDebugEnabled())
            logdebug("Dynamic Java ClassLoader: " + executorClassLoader);

        return executorClassLoader;
    }
    public void setExecutorClassLoader(ClassLoader loader) { this.executorClassLoader = loader; }

    @Override
    protected void initialize(ActivityRuntimeContext runtimeContext) throws ActivityException {
        runtimeContext.setLogPersister(ActivityLogger::persist);

        javaCode = runtimeContext.getAttributes().get(JAVA_CODE);

        if (StringUtils.isBlank(javaCode))
            throw new ActivityException("Missing attribute: " + JAVA_CODE);

        // output docs
        String temp = getAttributeValue(OUTPUTDOCS);
        setOutputDocuments(temp == null ? new String[0] : Attribute.parseList(temp).toArray(new String[0]));

        // initialize the executor
        try {
            getExecutorInstance().initialize(runtimeContext);
        }
        catch (Exception ex) {
            logexception(ex.getMessage(), ex);
            throw new ActivityException(ex.getMessage(), ex);
        }
    }

    @Override
    public void execute() throws ActivityException {

        try {

            // run the executor
            Process processVO = getMainProcessDefinition();
            List<Variable> varVOs = processVO.getVariables();
            Map<String,Object> bindings = new HashMap<String,Object>();
            for (Variable varVO: varVOs) {
                bindings.put(varVO.getName(), getVariableValue(varVO.getName()));
            }

            Object retObj = getExecutorInstance().execute(bindings);

            for (Variable variableVO: varVOs) {
                String variableName = variableVO.getName();
                Object bindValue = bindings.get(variableName);
                String varType = variableVO.getType();
                Object value = bindValue;
                if (varType.equals("java.lang.String") && value != null)
                    value = value.toString();  // convert to string
                setVariableValue(variableName, varType, value);
            }

            if (retObj != null) {
                setReturnCode(retObj.toString());
            }
        }
        catch (MdwJavaException ex) {
            logexception(ex.getMessage(), ex);
            throw new ActivityException(ex.getMessage(), ex);
        }
    }

    public JavaExecutor getExecutorInstance() throws MdwJavaException {
        if (executorInstance == null) {
            try {
                String className = getClassName();

                tempPkg = getPackage();
                if (tempPkg.isDefaultPackage()) {  // In case in-flight pulled out of Git history or edited process instance
                    tempPkg = new Package();
                    tempPkg.setName(getProcessDefinition().getPackageName());
                    // Use fake version (negative number) based on process version to uniquely identify the dynamic java version in CompiledJavaCache key (NOT USED it seems)
                    tempPkg.setVersion((-1 * getProcessDefinition().getVersion()));
                    // Since in-flight or edited instance, compile with different name than current code from current process version
                    String oldClassName = className;
                    Long processId = getMainProcessDefinition().getId();  // This gets processId for in-flight definition
                    if (processId == null || processId == 0L)
                        processId = this.getProcessInstanceId();  // For edited instance, processId is null, so use procInstId
                    className = className + "_" + processId;
                    javaCode = javaCode.replace(oldClassName, className);
                    className = tempPkg.getName() + "." + className;
                }

                setExecutorClassLoader(tempPkg.getCloudClassLoader());

                Class<?> clazz = CompiledJavaCache.getClass(getExecutorClassLoader(), tempPkg, className, javaCode);
                if (clazz == null)
                    throw new ClassNotFoundException(className);

                executorInstance = (JavaExecutor) clazz.newInstance();
            }
            catch (Exception ex) {
                logger.severeException(ex.getMessage(), ex);
                throw new MdwJavaException(ex.getMessage(), ex);
            }
        }
        return executorInstance;
    }

    protected String getClassName() throws ActivityException {
        String className = getAttributeValue(CLASS_NAME);
        if (className == null) {
            // old logic based on activity name can lead to collisions
            className = JavaNaming.getValidClassName(getActivityName() + "_" + getAttributeValue("LOGICAL_ID"));
        }
        return getPackageName() + className;
    }

    protected String getPackageName() throws ActivityException {
        Package pkg = PackageCache.getProcessPackage(getMainProcessDefinition().getId());
        if (pkg.isDefaultPackage()) {
            if (tempPkg == null || tempPkg.isDefaultPackage())
                return "";
            else
                return JavaNaming.getValidPackageName(tempPkg.getName() + ".");
        }
        else
            return JavaNaming.getValidPackageName(pkg.getName() + ".");
    }

}

