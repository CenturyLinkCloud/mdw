/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.translator.impl;

import java.beans.IntrospectionException;
import java.util.Set;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.constructor.ConstructorException;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.representer.Representer;

import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.common.exception.TranslationException;
import com.centurylink.mdw.common.translator.DocumentReferenceTranslator;
import com.centurylink.mdw.java.CompiledJavaCache;

/**
 * By convention, the first line of serialized yaml is a comment that
 * declares the type (class name) of the JavaBean object.
 */
public class YamlTranslator extends DocumentReferenceTranslator {

    public Object realToObject(String string) throws TranslationException {
        return realToObject(string, ApplicationContext.isOsgi());
    }

    protected Object realToObject(String str, boolean tryProviders) throws TranslationException {
        try {
            if (tryProviders)
                return providerDeserialize(str);

            Representer representer = new Representer();
            representer.getPropertyUtils().setSkipMissingProperties(true);
            try {
                return new Yaml(representer).load(str);
            }
            catch (ConstructorException ex) {
                if (ApplicationContext.isCloud()) {
                    Yaml yaml = new Yaml(new Constructor() {
                        protected Class<?> getClassForName(String name) throws ClassNotFoundException {
                            try {
                                return CompiledJavaCache.getResourceClass(name, getClass().getClassLoader(), getPackage());
                            }
                            catch (Exception ex) {
                                throw new ClassNotFoundException(name, ex);
                            }
                        }
                    }, representer);
                    return yaml.load(str);
                }
                else {
                    throw ex;
                }
            }
        }
        catch (Exception e) {
            throw new TranslationException(e.getMessage(), e);
        }
    }

    public String realToString(Object object) throws TranslationException {
        try {
            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.FLOW);
            options.setPrettyFlow(true);
            options.setIndent(2);
            Representer representer = new Representer() {
                @Override
                protected Set<Property> getProperties(Class<? extends Object> type) throws IntrospectionException {
                    Set<Property> props = super.getProperties(type);
                    Property toRemove = null;
                    for (Property prop : props) {
                        if (prop.getName().equals("metaClass")) {
                            toRemove = prop;
                            break;
                        }
                    }
                    if (toRemove != null)
                        props.remove(toRemove);
                    return props;
                }
            };
            return new Yaml(representer, options).dump(object);
        }
        catch (Exception ex) {
            throw new TranslationException(ex.getMessage(), ex);
        }
    }
}
