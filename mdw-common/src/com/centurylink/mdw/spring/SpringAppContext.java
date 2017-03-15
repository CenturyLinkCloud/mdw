/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
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
import com.centurylink.mdw.cache.CacheEnabled;
import com.centurylink.mdw.cache.impl.AssetCache;
import com.centurylink.mdw.cache.impl.PackageCache;
import com.centurylink.mdw.dataaccess.BaselineData;
import com.centurylink.mdw.dataaccess.file.MdwBaselineData;
import com.centurylink.mdw.event.ExternalEventHandler;
import com.centurylink.mdw.model.asset.Asset;
import com.centurylink.mdw.model.workflow.Package;
import com.centurylink.mdw.provider.CacheService;
import com.centurylink.mdw.util.file.FileHelper;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import com.centurylink.mdw.variable.VariableTranslator;

/**
 * Currently only used in Tomcat.  Allows injection through Spring workflow assets.
 */
public class SpringAppContext implements CacheEnabled, CacheService {

    public static final String SPRING_CONTEXT_FILE = "spring/application-context.xml";

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
                    appContext.close();
            }
        }

        if (springAppContext != null) {
            springAppContext.close();
        }
    }

    private GenericXmlApplicationContext springAppContext;
    public synchronized ApplicationContext getApplicationContext() throws IOException {
        if (springAppContext == null) {
            String springContextFile = SPRING_CONTEXT_FILE;
            Resource resource = new ByteArrayResource(FileHelper.readConfig(springContextFile, true).getBytes());
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
        return getApplicationContext().getBean(name);
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
            if (baselineDatas.size() > 2)
                throw new IOException("Too many BaselineData implementations.");
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
            //     set = undefinedActivityBeans.get(key);
            if (set == null)
                set = new ConcurrentHashMap<String, Boolean>();
            set.put(type,true);
            undefinedActivityBeans.put(key, set);

            return null; // no bean declared
        }
    }

    public ExternalEventHandler getEventHandler(String type, Package pkg) throws IOException, ClassNotFoundException {
        String key = pkg == null ? "SpringRootContext" : pkg.toString();
        Map<String, Boolean> set = undefinedEventBeans.get(key);
        if (set != null && set.get(type) != null)
            return null;

        try {
            Class<? extends ExternalEventHandler> implClass;
            if (pkg == null)
                implClass = Class.forName(type).asSubclass(ExternalEventHandler.class);
            else
                implClass = pkg.getCloudClassLoader().loadClass(type).asSubclass(ExternalEventHandler.class);
            for (String beanName : getApplicationContext(pkg).getBeanNamesForType(implClass)) {
                if (getApplicationContext(pkg).isSingleton(beanName))
                    throw new IllegalArgumentException("Bean declaration for injected event handler '" + beanName + "' must have scope=\"prototype\"");
            }
            return getApplicationContext(pkg).getBean(implClass);
        }
        catch (NoSuchBeanDefinitionException ex) {
            // Add to map of known classes that have no bean declared
            //         set = undefinedEventBeans.get(key);
            if (set == null)
                set = new ConcurrentHashMap<String, Boolean>();
            set.put(type,true);
            undefinedEventBeans.put(key, set);

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
            //    set = undefinedVariableBeans.get(key);
            if (set == null)
                set = new ConcurrentHashMap<String, Boolean>();
            set.put(type, true);
            undefinedVariableBeans.put(key, set);

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
            packageContexts = null;
        }
    }
}
