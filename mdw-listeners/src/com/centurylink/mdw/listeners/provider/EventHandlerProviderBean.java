/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.listeners.provider;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;

import com.centurylink.mdw.common.provider.EventHandlerProvider;
import com.centurylink.mdw.common.provider.InstanceProvider;
import com.centurylink.mdw.event.ExternalEventHandler;

/**
 * Default EventHandlerProvider implementation for the MDW Framework.  Client bundles
 * can register this implementation directly or extend/replace with their own version.
 * This class is included directly on a client bundle's classpath (typically under lib)
 * and registered in the bundle's MANIFEST.MF (via Bundle-ClassPath: ./lib/mdw-base-xxxx.jar)
 * so that it is loaded through the bundle's ClassLoader and has access to its classes and resources.
 * For this reason it's also purposely excluded from the Import-Package list in the bundle manifest.
 * 
 * Registration of this provider in a Spring Beans file looks like this:
 * <code>
 *  &lt;bean id="myEventHandlerProvider"
 *    class="com.centurylink.mdw.listeners.provider.EventHandlerProviderBean" /&gt;
 *  &lt;osgi:service ref="myEventHandlerProvider"&gt;
 *    &lt;osgi:interfaces&gt;
 *      &lt;value&gt;com.centurylink.mdw.common.provider.EventHandlerProvider&lt;/value&gt;
 *    &lt;/osgi:interfaces&gt;
 *    &lt;osgi:service-properties&gt;
 *      &lt;entry key="alias" value="myMdwEventHandlers"/&gt;
 *    &lt;/osgi:service-properties&gt;
 *  &lt;/osgi:service&gt;
 * </code>
 */
public class EventHandlerProviderBean extends InstanceProvider<ExternalEventHandler> implements EventHandlerProvider {

  @Override
  public ExternalEventHandler getInstance(String type)
  throws ClassNotFoundException, IllegalAccessException, InstantiationException {
      ExternalEventHandler eventHandler = null;
      Class<? extends ExternalEventHandler> ehClass = Class.forName(type).asSubclass(ExternalEventHandler.class);
      if (getBeanFactory() != null) {
          try {
              if (getBeanFactory() instanceof ListableBeanFactory) {
                  for (String beanName : ((ListableBeanFactory)getBeanFactory()).getBeanNamesForType(ehClass)) {
                      if (getBeanFactory().isSingleton(beanName))
                          throw new IllegalArgumentException("Bean declaration for injected event handler '" + beanName + "' must have scope=\"prototype\"");
                  }
              }
              eventHandler = getBeanFactory().getBean(ehClass);
          }
          catch (NoSuchBeanDefinitionException ex) {
              // no bean declared
          }            
      }
      
      if (eventHandler == null)
        eventHandler = ehClass.newInstance();
      
      return eventHandler;
  }
}
