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
package com.centurylink.mdw.workflow.activity.java;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.cache.impl.PackageCache;
import com.centurylink.mdw.java.CompiledJavaCache;
import com.centurylink.mdw.java.DynamicJavaImplementor;
import com.centurylink.mdw.java.JavaExecutor;
import com.centurylink.mdw.java.JavaNaming;
import com.centurylink.mdw.java.MdwJavaException;
import com.centurylink.mdw.model.variable.Variable;
import com.centurylink.mdw.model.workflow.ActivityRuntimeContext;
import com.centurylink.mdw.model.workflow.Package;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.util.StringHelper;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import com.centurylink.mdw.util.log.StandardLogger.LogLevel;
import com.centurylink.mdw.util.timer.Tracked;
import com.centurylink.mdw.workflow.activity.DefaultActivityImpl;

@Tracked(LogLevel.TRACE)
public class DynamicJavaActivity extends DefaultActivityImpl implements DynamicJavaImplementor {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    public static final String JAVA_CODE = "Java";

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
        javaCode = runtimeContext.getAttributes().get(JAVA_CODE);

        if (StringHelper.isEmpty(javaCode))
            throw new ActivityException("Missing attribute: " + JAVA_CODE);

        // output docs
        String temp = getAttributeValue(OUTPUTDOCS);
        setOutputDocuments(temp == null ? new String[0] : StringHelper.parseList(temp).toArray(new String[0]));

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
                bindings.put(varVO.getVariableName(), getVariableValue(varVO.getVariableName()));
            }

            Object retObj = getExecutorInstance().execute(bindings);

            for (Variable variableVO: varVOs) {
                String variableName = variableVO.getVariableName();
                Object bindValue = bindings.get(variableName);
                String varType = variableVO.getVariableType();
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
                Class<?> clazz = CompiledJavaCache.getClass(getPackage(), className, javaCode);
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
        String raw = getActivityName() + "_" + getAttributeValue("LOGICAL_ID");
        return getPackageName() + JavaNaming.getValidClassName(raw);
    }

    /**
     * Uses design-time package name.
     * Runtime package is mainly for classloader
     */
    protected String getPackageName() throws ActivityException {
        Package pkg = PackageCache.getProcessPackage(getMainProcessDefinition().getId());
        if (pkg.isDefaultPackage())
            return "";
        else
            return JavaNaming.getValidPackageName(pkg.getPackageName() + ".");
    }

}

