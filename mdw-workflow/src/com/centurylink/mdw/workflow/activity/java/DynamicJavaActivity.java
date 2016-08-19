/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.workflow.activity.java;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.springframework.osgi.context.BundleContextAware;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.common.cache.impl.PackageVOCache;
import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.common.utilities.logger.StandardLogger.LogLevel;
import com.centurylink.mdw.common.utilities.timer.Tracked;
import com.centurylink.mdw.java.CompiledJavaCache;
import com.centurylink.mdw.java.DynamicJavaImplementor;
import com.centurylink.mdw.java.JavaExecutor;
import com.centurylink.mdw.java.JavaNaming;
import com.centurylink.mdw.java.MdwJavaException;
import com.centurylink.mdw.model.value.activity.ActivityRuntimeContext;
import com.centurylink.mdw.model.value.process.PackageVO;
import com.centurylink.mdw.model.value.process.ProcessVO;
import com.centurylink.mdw.model.value.variable.VariableVO;
import com.centurylink.mdw.osgi.BundleLocator;
import com.centurylink.mdw.osgi.BundleSpec;
import com.centurylink.mdw.workflow.activity.DefaultActivityImpl;

@Tracked(LogLevel.TRACE)
public class DynamicJavaActivity extends DefaultActivityImpl implements DynamicJavaImplementor, BundleContextAware {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    public static final String JAVA_CODE = "Java";

    private String javaCode;
    public String getJavaCode() { return javaCode; }

    protected JavaExecutor executorInstance;

    private ClassLoader executorClassLoader;
    /**
     * If workflow package configs OsgiBundleSymbolicName and/or OsgiBundleVersion are specified,
     * these are used to look up the classloader.  Otherwise it will have been set to that of the providing
     * bundle in ActivityProviderBean.
     */
    public ClassLoader getExecutorClassLoader() {

        if (bundleContext != null && getPackage() != null && getPackage().getBundleSpec() != null) {
            // honor package BundleSpec
            BundleSpec bundleSpec = getPackage().getBundleSpec();
            executorClassLoader = new BundleLocator(bundleContext).getClassLoader(bundleSpec);
        }
        else {
            if (executorClassLoader == null)
                executorClassLoader = getClass().getClassLoader();  // fallback in case not set by activity provider
        }

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
        setOutputDocuments(temp == null ? new String[0] : temp.split("#"));

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
            ProcessVO processVO = getMainProcessDefinition();
            List<VariableVO> varVOs = processVO.getVariables();
            Map<String,Object> bindings = new HashMap<String,Object>();
            for (VariableVO varVO: varVOs) {
                bindings.put(varVO.getVariableName(), getVariableValue(varVO.getVariableName()));
            }

            Object retObj = getExecutorInstance().execute(bindings);

            for (VariableVO variableVO: varVOs) {
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
                Class<?> clazz;
                if (ApplicationContext.isOsgi()) {
                    clazz = CompiledJavaCache.getClass(getExecutorClassLoader(), getPackage(), className, javaCode);
                }
                else {
                    clazz = CompiledJavaCache.getClass(getPackage(), className, javaCode);
                }
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
        PackageVO pkg = PackageVOCache.getProcessPackage(getMainProcessDefinition().getId());
        if (pkg.isDefaultPackage())
            return "";
        else
            return JavaNaming.getValidPackageName(pkg.getPackageName() + ".");
    }

    private BundleContext bundleContext;
    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }
}

