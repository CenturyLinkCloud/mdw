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
package com.centurylink.mdw.spring;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericXmlApplicationContext;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import com.centurylink.mdw.activity.types.GeneralActivity;
import com.centurylink.mdw.cache.CacheService;
import com.centurylink.mdw.cache.impl.AssetCache;
import com.centurylink.mdw.cache.impl.PackageCache;
import com.centurylink.mdw.dataaccess.BaselineData;
import com.centurylink.mdw.dataaccess.file.CombinedBaselineData;
import com.centurylink.mdw.dataaccess.file.MdwBaselineData;
import com.centurylink.mdw.event.EventHandler;
import com.centurylink.mdw.model.asset.Asset;
import com.centurylink.mdw.model.workflow.Package;
import com.centurylink.mdw.util.file.FileHelper;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import com.centurylink.mdw.variable.VariableTranslator;

/**
 * Currently only used in Tomcat.  Allows injection through Spring workflow assets.
 */
public class SpringAppContext implements CacheService {

    public static final String SPRING_CONTEXT_FILE = "spring/application-context.xml";
    public static final String MDW_SPRING_MESSAGE_PRODUCER = "messageProducer";

    private static final Object pkgContextLock = new Object();

    // These are to keep track of variable translators, activity implementors, and event handlers that we
    // already know don't have a bean defined in a package-specific context, so avoid loading the class and
    // trying to get bean from context, since under heavy load, this can cause a bottleneck, especially for
    // variable translators.
    private static volatile Map<String, Map<String, Boolean>> undefinedVariableBeans = new ConcurrentHashMap<String, Map<String, Boolean>>();
    private static volatile Map<String, Map<String, Boolean>> undefinedActivityBeans = new ConcurrentHashMap<String, Map<String, Boolean>>();
    private static volatile Map<String, Map<String, Boolean>> undefinedEventBeans = new ConcurrentHashMap<String, Map<String, Boolean>>();

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    /**
     * Only to be called from CacheRegistration.
     */
    public SpringAppContext() {
    }

    private static SpringAppContext instance;
    public static SpringAppContext getInstance() {
        if (instance == null)
            instance = new SpringAppContext();
        return instance;
    }

    public void shutDown() {
        if (packageContexts != null) {
            synchronized (pkgContextLock) {
                for (MdwCloudAppContext appContext : packageContexts.values())
                    shutDown(appContext);
            }
        }

        if (springAppContext != null) {
            springAppContext.close();
        }
    }

    private void shutDown(MdwCloudAppContext pkgContext) {
        pkgContext.close();
    }

    private GenericXmlApplicationContext springAppContext;
    public synchronized ApplicationContext getApplicationContext() throws IOException {
        if (springAppContext == null) {
            String springContextFile = SPRING_CONTEXT_FILE;
            Resource resource = new ByteArrayResource(FileHelper.readConfig(springContextFile).getBytes());
            springAppContext = new GenericXmlApplicationContext();
            springAppContext.load(resource);
            springAppContext.refresh();
        }
        return springAppContext;
    }

    private static Map<String,MdwCloudAppContext> packageContexts;

    public ApplicationContext getApplicationContext(Package pkg) throws IOException {
        ApplicationContext appContext = getApplicationContext();
        if (pkg != null) {
            if (packageContexts == null) {
                synchronized (pkgContextLock) {
                    packageContexts = loadPackageContexts(appContext);
                }
            }
            MdwCloudAppContext pkgContext = packageContexts.get(pkg.getName());
            if (pkgContext != null)
                appContext = pkgContext;
        }
        return appContext;
    }

    public void loadPackageContexts() throws IOException {
        synchronized (pkgContextLock) {
            if (packageContexts == null)
                packageContexts = loadPackageContexts(getApplicationContext());
        }
    }

    public Map<String,MdwCloudAppContext> loadPackageContexts(ApplicationContext parent) throws IOException {
        Map<String,MdwCloudAppContext> contexts = new HashMap<String,MdwCloudAppContext>();
        for (Asset springAsset : AssetCache.getAssets(Asset.SPRING)) {
            try {
                Package pkg = PackageCache.getAssetPackage(springAsset.getId());
                if (pkg != null) {
                    String url = MdwCloudAppContext.MDW_SPRING_URL_PREFIX + pkg.getName() + "/" + springAsset.getName();
                    logger.info("Loading Spring asset: " + url + " from " + pkg.getLabel());
                    MdwCloudAppContext pkgContext = new MdwCloudAppContext(url, parent);
                    pkgContext.setClassLoader(pkg.getCloudClassLoader());
                    pkgContext.refresh();
                    contexts.put(pkg.getName(), pkgContext);  // we only support one Spring asset per package
                }
            }
            catch (Exception ex) {
                // do not let this prevent other package contexts from loading
                logger.severeException(ex.getMessage(), ex);
            }
        }
        return contexts;
    }

    public Object getBean(String name) throws IOException {
        try {
            return getApplicationContext().getBean(name);
        }
        catch (NoSuchBeanDefinitionException e) {  // Search in pkg contexts if not found in ApplicationContext
            if (packageContexts != null) {
                Object bean = null;
                for (ApplicationContext pkgContext : packageContexts.values()) {
                    bean = getBean(name, pkgContext);
                    if (bean != null)
                        return bean;
                }
            }
            throw e;
        }
    }

    private Object getBean(String name, ApplicationContext context) {
        try {
            return context.getBean(name);
        }
        catch (NoSuchBeanDefinitionException e) {
            return null;
        }
    }

    public Object getBean(String name, boolean optional) throws IOException {
        if (optional) {
            try {
                return getBean(name);
            } catch (NoSuchBeanDefinitionException e) {
                return null;
            }
        }
        else {
            return getBean(name);
        }
    }

    public boolean isBeanDefined(String name) {

        try {
            Object bean = getBean(name);
            return (bean !=null);
        } catch (Exception e) {
            //Catch it and return false;
            return false;
        }
    }

    /**
     * Returns an implementation for an MDW injectable type.
     * If no custom implementation is found, use the MDW default.
     * @param type
     * @param mdwImplClass
     * @return the instance
     */
    public Object getInjectable(Class<?> type, Class<?> mdwImplClass) throws IOException {
        Map<String,?> beans = getApplicationContext().getBeansOfType(type);
        Object mdw = null;
        Object injected = null;
        for (Object bean : beans.values()) {
            if (bean.getClass().getName().equals(mdwImplClass.getName()))
                mdw = bean;
            else
                injected = bean;
        }
        return injected == null ? mdw : injected;
    }

    public Object getBean(Class<?> type) {
        try {
            Map<String,?> beans = getApplicationContext().getBeansOfType(type);
            if (beans == null || beans.isEmpty())
                return null;
            if (beans.values().size() != 1)
                throw new IOException("Too many bean definitions for type: " + type + " (" + beans.values().size() + ")");
            else
                return beans.values().iterator().next();
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            return null;
        }
    }

    /**
     * Prefers any non-MDW BaselineData implementation.
     */
    public BaselineData getBaselineData() {
        try {
            List<BaselineData> baselineDatas = new ArrayList<BaselineData>();
            Map<String,? extends BaselineData> beans = getApplicationContext().getBeansOfType(BaselineData.class);
            if (beans != null)
                baselineDatas.addAll(beans.values());

            synchronized (pkgContextLock) {
                if (packageContexts == null)
                    packageContexts = loadPackageContexts(getApplicationContext());

                for (ApplicationContext pkgContext : packageContexts.values()) {
                    beans = pkgContext.getBeansOfType(BaselineData.class);
                    if (beans != null)
                        baselineDatas.addAll(beans.values());
                }
            }

            BaselineData mdwBaselineData = null;
            BaselineData injectedBaselineData = null;
            for (BaselineData baselineData : baselineDatas) {
                String className = baselineData.getClass().getName();
                logger.mdwDebug("Found BaselineData: " + className);
                if (className.equals(MdwBaselineData.class.getName()))
                    mdwBaselineData = baselineData;
                else
                    injectedBaselineData = baselineData;
            }
            if (baselineDatas.size() > 2) {
                List<BaselineData> injectedBaselineDatas = new ArrayList<>();
                for (BaselineData bd : baselineDatas) {
                    if (bd != mdwBaselineData)
                        injectedBaselineDatas.add(bd);
                }
                injectedBaselineData = new CombinedBaselineData(injectedBaselineDatas);
            }
            return injectedBaselineData == null ? mdwBaselineData : injectedBaselineData;
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            return null;
        }
    }

    public GeneralActivity getActivityImplementor(String type, Package pkg) throws IOException, ClassNotFoundException {
        String key = pkg == null ? "SpringRootContext" : pkg.toString();
        Map<String, Boolean> set = undefinedActivityBeans.get(key);
        if (set != null && set.get(type) != null)
            return null;

        try {
            Class<? extends GeneralActivity> implClass;
            if (pkg == null)
                implClass = Class.forName(type).asSubclass(GeneralActivity.class);
            else
                implClass = pkg.getCloudClassLoader().loadClass(type).asSubclass(GeneralActivity.class);
            for (String beanName : getApplicationContext(pkg).getBeanNamesForType(implClass)) {
                if (getApplicationContext(pkg).isSingleton(beanName))
                    throw new IllegalArgumentException("Bean declaration for injected activity '" + beanName + "' must have scope=\"prototype\"");
            }
            return getApplicationContext(pkg).getBean(implClass);
        }
        catch (NoSuchBeanDefinitionException ex) {
            // Add to map of known classes that have no bean declared
            set = undefinedActivityBeans.get(key);
            if (set == null) {
                set = new ConcurrentHashMap<String, Boolean>();
                undefinedActivityBeans.put(key, set);
            }
            set.put(type,true);

            return null; // no bean declared
        }
    }

    public EventHandler getEventHandler(String type, Package pkg) throws IOException, ClassNotFoundException {
        String key = pkg == null ? "SpringRootContext" : pkg.toString();
        Map<String, Boolean> set = undefinedEventBeans.get(key);
        if (set != null && set.get(type) != null)
            return null;

        try {
            Class<? extends EventHandler> implClass;
            if (pkg == null)
                implClass = Class.forName(type).asSubclass(EventHandler.class);
            else
                implClass = pkg.getCloudClassLoader().loadClass(type).asSubclass(EventHandler.class);
            for (String beanName : getApplicationContext(pkg).getBeanNamesForType(implClass)) {
                if (getApplicationContext(pkg).isSingleton(beanName))
                    throw new IllegalArgumentException("Bean declaration for injected event handler '" + beanName + "' must have scope=\"prototype\"");
            }
            return getApplicationContext(pkg).getBean(implClass);
        }
        catch (NoSuchBeanDefinitionException ex) {
            // Add to map of known classes that have no bean declared
            set = undefinedEventBeans.get(key);
            if (set == null) {
                set = new ConcurrentHashMap<String, Boolean>();
                undefinedEventBeans.put(key, set);
            }
            set.put(type,true);

            return null; // no bean declared
        }
    }

    public VariableTranslator getVariableTranslator(String type, Package pkg) throws IOException, ClassNotFoundException {
        String key = (pkg == null ? "SpringRootContext" : pkg.toString());
        Map<String, Boolean> set = undefinedVariableBeans.get(key);
        if (set != null && set.get(type) != null)
            return null;

        try {
            Class<? extends VariableTranslator> implClass;
              if (pkg == null)
                  implClass = Class.forName(type).asSubclass(VariableTranslator.class);
              else
                  implClass = pkg.getCloudClassLoader().loadClass(type).asSubclass(VariableTranslator.class);
            return getApplicationContext(pkg).getBean(implClass);
        }
        catch (NoSuchBeanDefinitionException ex) {
            // Add to map of known classes that have no bean declared
            set = undefinedVariableBeans.get(key);
            if (set == null) {
                set = new ConcurrentHashMap<String, Boolean>();
                undefinedVariableBeans.put(key, set);
            }
            set.put(type, true);

            return null; // no bean declared
        }
    }

    @Override
    public void refreshCache() throws Exception {
        clearCache(); // for lazily reloading
    }

    @Override
    public void clearCache() {
        synchronized (pkgContextLock) {
            if (packageContexts != null) {
                for (MdwCloudAppContext appContext : packageContexts.values())
                    shutDown(appContext);
            }
            packageContexts = null;
        }
    }
}
