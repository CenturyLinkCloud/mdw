/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.provider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

import com.centurylink.mdw.activity.types.GeneralActivity;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.event.ExternalEventHandler;
import com.centurylink.mdw.java.CompiledJavaCache;
import com.centurylink.mdw.model.value.process.PackageVO;
import com.centurylink.mdw.osgi.BundleSpec;
import com.centurylink.mdw.osgi.ProviderLocator;
import com.centurylink.mdw.variable.VariableTranslator;

public class ProviderRegistry {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();
    public static final List<String> providerServices = new ArrayList<String>(Arrays.asList(new String[] {ActivityProvider.class.getName(),
            EventHandlerProvider.class.getName(), VariableTranslatorProvider.class.getName()}));
    private Map<String,List<Provider<?>>> providers = new HashMap<String,List<Provider<?>>>();
    private Map<String,ServiceListener> serviceListeners = new HashMap<String,ServiceListener>();
    private Map<String,Set<String>> dynamicProviders = new HashMap<String,Set<String>>();

    @SuppressWarnings("unchecked")
    public <T> List<Provider<T>> getProviders(Class<? extends Provider<?>> providerType) {
        List<Provider<T>> list = new ArrayList<Provider<T>>();
        for (Provider<?> provider : providers.get(providerType.getName())) {
            list.add((Provider<T>)provider);
        }
        return list;
    }

    @SuppressWarnings("unchecked")
    public <T> List<PackageAwareProvider<T>> getPackageAwareProviders(Class<? extends Provider<?>> providerType) {
        List<PackageAwareProvider<T>> list = new ArrayList<PackageAwareProvider<T>>();
        for (Provider<?> provider : providers.get(providerType.getName())) {
            if (provider instanceof PackageAwareProvider)
              list.add((PackageAwareProvider<T>)provider);
        }
        return list;
    }

    @SuppressWarnings("unchecked")
    public <T> List<Provider<T>> getNonPackageAwareProviders(Class<? extends Provider<?>> providerType) {
        List<Provider<T>> list = new ArrayList<Provider<T>>();
        for (Provider<?> provider : providers.get(providerType.getName())) {
            if (!(provider instanceof PackageAwareProvider))
              list.add((Provider<T>)provider);
        }
        return list;
    }

    protected ProviderRegistry() {
    }

    private static ProviderRegistry instance;
    public static ProviderRegistry getInstance() {
        if (instance == null) {
            instance = new ProviderRegistry();
            instance.providers.put(ActivityProvider.class.getName(), new ArrayList<Provider<?>>());
            instance.providers.put(EventHandlerProvider.class.getName(), new ArrayList<Provider<?>>());
            instance.providers.put(VariableTranslatorProvider.class.getName(), new ArrayList<Provider<?>>());
        }
        return instance;
    }

    public void startup(final BundleContext bundleContext) throws InvalidSyntaxException, ProviderException {
        serviceListeners.put(ActivityProvider.class.getName(), register(bundleContext, ActivityProvider.class));
        serviceListeners.put(EventHandlerProvider.class.getName(), register(bundleContext, EventHandlerProvider.class));
        serviceListeners.put(VariableTranslatorProvider.class.getName(), register(bundleContext, VariableTranslatorProvider.class));
    }

    /**
     * @param bundleContext
     * @throws InvalidSyntaxException
     */
    public void shutdown(final BundleContext bundleContext) throws InvalidSyntaxException {
        List<String> toRemove = new ArrayList<String>();
        for (String providerClass : serviceListeners.keySet()) {
            unregister(bundleContext, serviceListeners.get(providerClass), providerClass);
            toRemove.add(providerClass);
        }
        for (String remove : toRemove)
          serviceListeners.remove(remove);
        dynamicProviders.clear();
    }

    protected <T> ServiceListener register(final BundleContext bundleContext, final T providerType)
    throws InvalidSyntaxException {
        final String providerClass = ((Class<?>)providerType).getName();

        // provider services
        ServiceListener serviceListener = new ServiceListener() {
            @SuppressWarnings("unchecked")
            public void serviceChanged(ServiceEvent ev) {
                ServiceReference serviceRef = ev.getServiceReference();
                Provider<T> provider = (Provider<T>)bundleContext.getService(serviceRef);
                for (String key : serviceRef.getPropertyKeys()) {
                    Object value = serviceRef.getProperty(key);
                    if (value != null)
                      provider.setProperty(key, value.toString());
                }
                String alias = provider.getAlias();
                boolean registered = false;
                for (Provider<?> existProvider : providers.get(providerClass)) {
                    if (alias.equals(existProvider.getAlias())) {
                        registered = true;
                        break;
                    }
                }

                switch (ev.getType()) {
                    case ServiceEvent.REGISTERED: {
                        if (!registered) {
                            logger.info("Registering " + providerClass + " with unique alias '" + alias + "' from bundle " + provider.getBundleContext().getBundle().getSymbolicName());
                            providers.get(providerClass).add(provider);
                        }
                    }
                    break;
                    case ServiceEvent.UNREGISTERING: {
                        if (registered) {
                            logger.info("Unregistering " + providerClass + " with unique alias '" + alias + "'");
                            providers.get(providerClass).remove(provider);
                        }
                    }
                    break;
                }
            }
        };

        String filter = "(objectclass=" + providerClass + ")";
        // notify previously started services
        ServiceReference[] serviceRefs = bundleContext.getServiceReferences(null, filter);
        if (serviceRefs != null) {
            for (ServiceReference serviceRef : serviceRefs) {
                serviceListener.serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, serviceRef));
            }
        }
        bundleContext.addServiceListener(serviceListener, filter);

        return serviceListener;
    }

    protected <T> void unregister(final BundleContext bundleContext, final ServiceListener serviceListener, final String providerClass)
    throws InvalidSyntaxException {
        if (serviceListener != null) {
            ServiceReference[] serviceRefs = bundleContext.getServiceReferences(null, "(objectclass=" + providerClass + ")");
            if (serviceRefs != null) {
                for (ServiceReference serviceRef : serviceRefs) {
                    serviceListener.serviceChanged(new ServiceEvent(ServiceEvent.UNREGISTERING, serviceRef));
                }
            }
        }
    }

    public Provider<GeneralActivity> getMdwActivityProvider() {
        List<Provider<GeneralActivity>> providers = getProviders(ActivityProvider.class);
        for (Provider<GeneralActivity> activityProvider : providers) {
            if ("mdwActivities".equals(activityProvider.getAlias()))
                return activityProvider;
        }
        return null;
    }

    public GeneralActivity getActivityInstance(PackageVO packageVO, String className, String activityBsn)
    throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        BundleSpec bundleSpec = packageVO == null ? null : packageVO.getBundleSpec();
        if (bundleSpec == null && activityBsn != null)
            bundleSpec = new BundleSpec(activityBsn);
        GeneralActivity found = getActivityInstance0(packageVO, className, bundleSpec);
        if (found == null && bundleSpec != null) {
            // activity BSN can fallback to any available
            logger.warn("No activity found for '" + className + "' with bundleSpec=" + bundleSpec + ".  Using any available.");
            found = getActivityInstance0(packageVO, className, null);
        }
        return found;
    }

    private GeneralActivity getActivityInstance0(PackageVO packageVO, String className, BundleSpec bundleSpec)
    throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        List<Provider<GeneralActivity>> providers = getProviders(ActivityProvider.class);
        GeneralActivity pkgAwareActivity = null;
        GeneralActivity activity = null;
        String latestMatchingVer = bundleSpec == null ? null : new ProviderLocator<GeneralActivity>(providers).getLatestMatchingBundleVersion(bundleSpec);

        for (Provider<GeneralActivity> provider : providers) {
            Bundle bundle = provider.getBundleContext().getBundle();
            if (bundleSpec == null || "com.centurylink.mdw.workflow".equals(bundle.getSymbolicName()) || (bundleSpec.meetsSpec(bundle) && bundle.getVersion().toString().equals(latestMatchingVer))) {
                try {
                    if (provider instanceof PackageAwareProvider) {
                        if (pkgAwareActivity == null)
                            pkgAwareActivity = ((PackageAwareProvider<GeneralActivity>)provider).getInstance(packageVO, className);
                    }
                    else {
                        if (activity == null)
                            activity = provider.getInstance(className);
                    }
                }
                catch (ClassNotFoundException ex) {
                    // not located by this provider
                }
            }
        }

        // prefer PackageAware
        if (pkgAwareActivity != null) {
            if (logger.isMdwDebugEnabled())
                logger.mdwDebug("Activity: " + className + " provided by ClassLoader: " + pkgAwareActivity.getClass().getClassLoader());
            return pkgAwareActivity;
        }

        if (activity != null && logger.isMdwDebugEnabled())
            logger.mdwDebug("Activity: " + className + " provided by ClassLoader: " + activity.getClass().getClassLoader());

        return activity;
    }

    public Provider<ExternalEventHandler> getMdwEventHandlerProvider() {
        List<Provider<ExternalEventHandler>> providers = getProviders(EventHandlerProvider.class);
        for (Provider<ExternalEventHandler> eventHandlerProvider : providers) {
            if ("mdwEventHandlers".equals(eventHandlerProvider.getAlias()))
                return eventHandlerProvider;
        }
        return null;
    }

    public ExternalEventHandler getExternalEventHandlerInstance(String className, Object content, Map<String,String> metaInfo)
    throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        List<Provider<ExternalEventHandler>> providers = getProviders(EventHandlerProvider.class);
        ExternalEventHandler contentAwareHandler = null;
        ExternalEventHandler handler = null;
        for (Provider<ExternalEventHandler> provider : providers) {
            try {
                if (provider instanceof ContentAwareProvider) {
                    if (contentAwareHandler == null) {
                        contentAwareHandler = ((ContentAwareProvider<ExternalEventHandler>)provider).getInstance(metaInfo, content, className);
                        if (contentAwareHandler != null) {
                            String bundleLabel = provider.getBundleContext() == null ? "null" : provider.getBundleContext().getBundle().getSymbolicName();
                            logger.debug("EventHandler: " + className + " provided by ContentAware Bundle: " + bundleLabel);
                        }
                    }
                }
                else {
                    if (handler == null)
                        handler = provider.getInstance(className);
                }
            }
            catch (ClassNotFoundException ex) {
                // not located by this provider
            }
        }
        // prefer ContentAware
        if (contentAwareHandler != null) {
            if (logger.isMdwDebugEnabled())
                logger.mdwDebug("EventHandler: " + className + " provided by ContentAware ClassLoader: " + contentAwareHandler.getClass().getClassLoader());
            return contentAwareHandler;
        }

        if (handler != null && logger.isMdwDebugEnabled())
            logger.mdwDebug("EventHandler: " + className + " provided by ClassLoader: " + handler.getClass().getClassLoader());

        return handler;
    }

    public Provider<VariableTranslator> getMdwVariableTranslatorProvider() {
        List<Provider<VariableTranslator>> providers = getProviders(VariableTranslatorProvider.class);
        for (Provider<VariableTranslator> variableTranslatorProvider : providers) {
            if ("mdwVariableTranslators".equals(variableTranslatorProvider.getAlias()))
                return variableTranslatorProvider;
        }
        return null;
    }

    /**
     * Does not support VersionAware.
     */
    public VariableTranslator getVariableTranslator(String className)
    throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        return getVariableTranslator(null, className);
    }

    public VariableTranslator getVariableTranslator(PackageVO packageVO, String className)
    throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        BundleSpec bundleSpec = packageVO == null ? null : packageVO.getBundleSpec();
        VariableTranslator found = getVariableTranslator0(packageVO, className, bundleSpec);
        if (found == null && bundleSpec != null) {
            logger.mdwDebug("No variable translator found for '" + className + "' with bundleSpec=" + bundleSpec + ".  Using any available.");
            found = getVariableTranslator0(packageVO, className, null);
        }
        return found;
    }

    private VariableTranslator getVariableTranslator0(PackageVO packageVO, String className, BundleSpec bundleSpec)
    throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        VariableTranslator pkgAwareTranslator = null;
        VariableTranslator translator = null;
        List<Provider<VariableTranslator>> providers = getProviders(VariableTranslatorProvider.class);

        String latestMatchingVer = bundleSpec == null ? null : new ProviderLocator<VariableTranslator>(providers).getLatestMatchingBundleVersion(bundleSpec);
        for (Provider<VariableTranslator> provider : providers) {
            Bundle bundle = provider.getBundleContext().getBundle();
            if (bundleSpec == null || "com.centurylink.mdw.workflow".equals(bundle.getSymbolicName()) || (bundleSpec.meetsSpec(bundle) && bundle.getVersion().toString().equals(latestMatchingVer))) {
                try {
                    if (provider instanceof PackageAwareProvider) {
                        if (pkgAwareTranslator == null)
                            pkgAwareTranslator = ((PackageAwareProvider<VariableTranslator>)provider).getInstance(packageVO, className);
                    }
                    else {
                        if (translator == null)
                            translator = provider.getInstance(className);
                    }
                }
                catch (ClassNotFoundException ex) {
                    // not located by this provider
                }
            }
        }
        // prefer PackageAware
        if (pkgAwareTranslator != null) {
            if (logger.isMdwDebugEnabled())
                logger.mdwDebug("VariableTranslator: " + className + " provided by ClassLoader: " + pkgAwareTranslator.getClass().getClassLoader());
            return pkgAwareTranslator;
        }

        if (translator != null && logger.isMdwDebugEnabled())
            logger.mdwDebug("VariableTranslator: " + className + " provided by ClassLoader: " + translator.getClass().getClassLoader());

        return translator;
    }

    /**
     * @param serviceInterface
     * @param className
     */
    public void addDynamicProvider(String serviceInterface, String className) {
        if (dynamicProviders.containsKey(serviceInterface)) {
            dynamicProviders.get(serviceInterface).add(className);
        }
        else {
            Set<String> classNamesSet = new HashSet<String>();
            classNamesSet.add(className);
            dynamicProviders.put(serviceInterface, classNamesSet);
        }
    }

    public void clearDynamicProviders() {
        logger.mdwDebug("Clearing Dynamic providers cache in : " + getClass().getName());
        dynamicProviders.clear();
    }

    public Map<String,Set<String>> getDynamicProviders() {
        return dynamicProviders;
    }

    /**
     * To get all dynamic java Registered service providers
     * @param providerType
     * @return
     */
    @SuppressWarnings("unchecked")
    public <T> List<Provider<T>> getDynamicProviders(Class<? extends Provider<?>> providerType) {
        List<Provider<T>> list = new ArrayList<Provider<T>>();
        if (dynamicProviders.containsKey(providerType.getName())) {
            for (String dynamicProviderName : dynamicProviders.get(providerType.getName())) {
                try {
                    Class<?> clazz = CompiledJavaCache.getClassFromAssetName(getClass().getClassLoader(), dynamicProviderName);
                    if (clazz != null)
                        list.add((Provider<T>) (clazz).newInstance());
                }
                catch (Exception ex) {
                    logger.severeException("Failed to get the dynamic provider type: " +providerType.getName() +" class: " + dynamicProviderName + " \n " + ex.getMessage(), ex);
                }
            }
        }
        return list;
    }
    /**
     * To get dynamic java variable translator
     * @param translatorClass
     * @param classLoader
     * @return
     */
    public VariableTranslator getDynamicVariableTranslator(String className, ClassLoader parentLoader) {
        try {
            Class<?> clazz = CompiledJavaCache.getClassFromAssetName(parentLoader, className);
            if (clazz != null)
                return (VariableTranslator) (clazz).newInstance();
        }
        catch (Exception ex) {
            logger.trace("Dynamic VariableTranslatorProvider not found: " + className);
        }
        return null;
    }
}
