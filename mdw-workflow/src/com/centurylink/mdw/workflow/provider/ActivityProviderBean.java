/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.workflow.provider;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;

import com.centurylink.mdw.activity.types.GeneralActivity;
import com.centurylink.mdw.common.provider.ActivityProvider;
import com.centurylink.mdw.common.provider.InstanceProvider;
import com.centurylink.mdw.java.DynamicJavaImplementor;

/**
 * Default ActivityProvider implementation for the MDW Framework.  Client bundles
 * can register this implementation directly or extend/replace with their own version.
 * This class is included directly on a client bundle's classpath (typically under lib)
 * and registered in the bundle's MANIFEST.MF (via Bundle-ClassPath: ./lib/mdw-base-xxxx.jar)
 * so that it is loaded through the bundle's ClassLoader and has access to its classes and resources.
 * For this reason it's also purposely excluded from the Import-Package list in the bundle manifest.
 *
 * Registration of this provider in a Spring Beans file looks like this:
 * <code>
 *   &lt;bean id="myActivityProvider"
 *     class="com.centurylink.mdw.workflow.provider.ActivityProviderBean" /&gt;
 *   &lt;osgi:service ref="myActivityProvider"&gt;
 *     &lt;osgi:interfaces&gt;
 *       &lt;value&gt;com.centurylink.mdw.common.provider.ActivityProvider&lt;/value&gt;
 *     &lt;osgi:interfaces&gt;
 *     &lt;osgi:service-properties&gt;
 *       &lt;entry key="alias" value="myMdwActivities"/&gt;
 *     &lt;/osgi:service-properties&gt;
 *   &lt;/osgi:service&gt;
 * </code>
 *
 */
public class ActivityProviderBean extends InstanceProvider<GeneralActivity> implements ActivityProvider {

    @Override
    public GeneralActivity getInstance(String type)
    throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        GeneralActivity activity = null;
        Class<? extends GeneralActivity> actClass = Class.forName(type).asSubclass(GeneralActivity.class);
        if (getBeanFactory() != null) {
            try {
                if (getBeanFactory() instanceof ListableBeanFactory) {
                    for (String beanName : ((ListableBeanFactory)getBeanFactory()).getBeanNamesForType(actClass)) {
                        if (getBeanFactory().isSingleton(beanName))
                            throw new IllegalArgumentException("Bean declaration for injected activity '" + beanName + "' must have scope=\"prototype\"");
                    }
                }
                activity = getBeanFactory().getBean(actClass);
            }
            catch (NoSuchBeanDefinitionException ex) {
                // no bean declared
            }
        }

        if (activity == null)
            activity = actClass.newInstance();

        // Default behavior is to set the dynamic java classloader to that of the providing bundle.
        // In 5.2 the providing bundle is designated by the BSN attribute on the dynamic java activity
        // (which is honored during the service registry lookup).
        // Either way, this can be overridden by specifying the OsgiBundleSymbolicName and Version workflow package props.
        if (activity instanceof DynamicJavaImplementor)
            ((DynamicJavaImplementor)activity).setExecutorClassLoader(this.getClass().getClassLoader());

        return activity;
    }
}
