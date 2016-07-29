/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.workflow.provider;

import com.centurylink.mdw.common.provider.InstanceProvider;
import com.centurylink.mdw.common.provider.VariableTranslatorProvider;
import com.centurylink.mdw.translator.DynamicJavaTranslator;
import com.centurylink.mdw.variable.VariableTranslator;

/**
 * Default VariableTranslatorProvider implementation for the MDW Framework.  Client bundles
 * can register this implementation directly or extend/replace with their own version.
 * This class is included directly on a client bundle's classpath (typically under lib)
 * and registered in the bundle's MANIFEST.MF (via Bundle-ClassPath: ./lib/mdw-base-xxxx.jar)
 * so that it is loaded through the bundle's ClassLoader and has access to its classes and resources.
 * For this reason it's also purposely excluded from the Import-Package list in the bundle manifest.
 *
 * Registration of this provider in a Spring Beans file looks like this:
 * <code>
 *   &lt;bean id="myVariableTranslatorProvider"
 *    class="com.centurylink.mdw.workflow.provider.VariableTranslatorProviderBean" /&gt;
 *  &lt;osgi:service ref="myVariableTranslatorProvider"&gt;
 *    &lt;osgi:interfaces&gt;
 *      &lt;value&gt;com.centurylink.mdw.common.provider.VariableTranslatorProvider&lt;/value&gt;
 *    &lt;/osgi:interfaces&gt;
 *    &lt;osgi:service-properties&gt;
 *      &lt;entry key="alias" value="myMdwVariableTranslators"/&gt;
 *    &lt;/osgi:service-properties&gt;
 *  &lt;/osgi:service&gt;
 * </code>
 */
public class VariableTranslatorProviderBean extends InstanceProvider<VariableTranslator> implements VariableTranslatorProvider {

    @Override
    public VariableTranslator getInstance(String type)
    throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        if (type.endsWith("JavaObjectTranslator"))
            return new DynamicJavaTranslator();  // delegate classloading to client bundle
        else if (type.endsWith("JaxbElementTranslator")) {
            try {
                if (getBeanFactory().containsBean("jaxbTranslator"))
                    return (VariableTranslator)getBeanFactory().getBean("jaxbTranslator");
                else
                    return null;
            }
            catch (NoClassDefFoundError er) {
                // can happen for org.springframework.beans.factory.BeanFactory for unsuspecting bundles
                // that declare themselves as variable translator providers
                throw new ClassNotFoundException(er.getMessage(), er);
            }
        }
        return Class.forName(type).asSubclass(VariableTranslator.class).newInstance();
    }

    @Override
    public Class<?> getClass(String className) throws ClassNotFoundException {
        return Class.forName(className);
    }
}
