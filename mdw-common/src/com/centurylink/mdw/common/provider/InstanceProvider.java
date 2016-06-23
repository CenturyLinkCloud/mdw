/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.provider;

import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.osgi.context.BundleContextAware;

/**
 * Base implementation of the MDW Provider interface.
 *
 * @param <T>
 */
public class InstanceProvider<T> implements Provider<T>, BundleContextAware, BeanFactoryAware {

    public static final String ALIAS = "alias";
    
    private Map<String,String> properties;
    public String getProperty(String name) {
        if (properties == null)
            return null;
        return properties.get(name);
    }
    public void setProperty(String name, String value) {
        if (properties == null)
            properties = new HashMap<String,String>();
        properties.put(name, value);
    }
    
    public Map<String,String> getProperties() { return properties; }
    public void setProperties(Map<String,String> props) { this.properties = props; }
    
    public String getAlias() throws ProviderException {
        String alias = getProperty(ALIAS);
        if (alias == null)
            throw new ProviderException("Service provider must have unique property '" + ALIAS + "'");
        return alias;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public T getInstance(String type)
    throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        return (T) Class.forName(type).newInstance();
    }

    private BeanFactory beanFactory;
    public BeanFactory getBeanFactory() { return beanFactory; }
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }
    
    private BundleContext bundleContext;
    public BundleContext getBundleContext() { return bundleContext; }
    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }    
}
