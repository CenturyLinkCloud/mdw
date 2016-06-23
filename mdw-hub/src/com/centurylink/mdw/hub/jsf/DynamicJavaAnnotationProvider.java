/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.hub.jsf;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.faces.bean.ManagedBean;
import javax.faces.component.FacesComponent;
import javax.faces.component.behavior.FacesBehavior;
import javax.faces.context.ExternalContext;
import javax.faces.convert.FacesConverter;
import javax.faces.event.NamedEvent;
import javax.faces.render.FacesBehaviorRenderer;
import javax.faces.render.FacesRenderer;
import javax.faces.validator.FacesValidator;

import org.apache.myfaces.shared.util.ClassUtils;
import org.apache.myfaces.spi.AnnotationProvider;

import com.centurylink.mdw.common.cache.impl.PackageVOCache;
import com.centurylink.mdw.common.cache.impl.RuleSetCache;
import com.centurylink.mdw.common.constant.PropertyNames;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.common.utilities.property.PropertyManager;
import com.centurylink.mdw.java.CompiledJavaCache;
import com.centurylink.mdw.java.JavaNaming;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;
import com.centurylink.mdw.model.value.process.PackageVO;

/**
 * Support for managed bean and other JSF features through MDW Dynamic Java workflow assets.
 */
public class DynamicJavaAnnotationProvider extends AnnotationProvider {
    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    private static Set<String> JSF_ANNOTATION_CLASSES;

    static {
        Set<String> classes = new HashSet<String>(10, 1f);
        classes.add(FacesComponent.class.getName());
        classes.add(FacesBehavior.class.getName());
        classes.add(FacesConverter.class.getName());
        classes.add(FacesValidator.class.getName());
        classes.add(FacesRenderer.class.getName());
        classes.add(ManagedBean.class.getName());
        classes.add(NamedEvent.class.getName());
        classes.add(FacesBehaviorRenderer.class.getName());
        JSF_ANNOTATION_CLASSES = Collections.unmodifiableSet(classes);
    }

    private AnnotationProvider wrappedProvider;

    private DynamicJavaClassLoaderExtension classLoaderExtension;

    public DynamicJavaAnnotationProvider(AnnotationProvider wrappedProvider) {
        this.wrappedProvider = wrappedProvider;
    }

    @Override
    public Map<Class<? extends Annotation>, Set<Class<?>>> getAnnotatedClasses(ExternalContext ctx) {
        boolean processClasses = true;
        String prop = PropertyManager.getProperty(PropertyNames.MDW_HUB_PROCESS_DYNAMIC_JAVA_JSF_ANNOTATIONS);
        if (prop != null)
            processClasses = prop.equalsIgnoreCase("true");

        Map<Class<? extends Annotation>, Set<Class<?>>> map = wrappedProvider.getAnnotatedClasses(ctx);

        if (!processClasses)
            return map;

        if (logger.isDebugEnabled())
            logger.debug("Processing JSF annotations for: " + ctx.getContextName());

        long before = System.currentTimeMillis();
        Map<PackageVO,Map<String,String>> packagedJava = new HashMap<PackageVO,Map<String,String>>();
        try {
            for (RuleSetVO javaRuleSet : RuleSetCache.getRuleSets(RuleSetVO.JAVA)) {
                // only process JSF-annotated classes
                if (javaRuleSet.getRuleSet().indexOf("@ManagedBean") > 0 || javaRuleSet.getRuleSet().indexOf("@Faces") > 0) {
                    String className = JavaNaming.getValidClassName(javaRuleSet.getName());
                    PackageVO javaAssetPackage = PackageVOCache.getRuleSetPackage(javaRuleSet.getId());
                    if (javaAssetPackage == null) {
                        logger.severe("Omitting unpackaged Managed Bean from compilation: " + javaRuleSet.getLabel());
                    }
                    else {
                        String qName = JavaNaming.getValidPackageName(javaAssetPackage.getPackageName()) + "." + className;
                        Map<String,String> javaSources = packagedJava.get(javaAssetPackage);
                        if (javaSources == null) {
                            javaSources = new HashMap<String,String>();
                            packagedJava.put(javaAssetPackage, javaSources);
                        }
                        javaSources.put(qName, javaRuleSet.getRuleSet());
                    }
                }
            }
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
        }
        if (!packagedJava.isEmpty()) {
            for (PackageVO pkg : packagedJava.keySet()) {
                try {
                    List<Class<?>> classes = CompiledJavaCache.compileClasses(getClass().getClassLoader(), pkg, packagedJava.get(pkg), true);
                    for (Class<?> clazz : classes)
                        processClass(map, clazz);
                }
                catch (Throwable t) {
                  // let other packages continue to process
                  logger.severeException("Failed to process Dynamic Java Managed Beans for package " + pkg.getLabel() + ": " + t.getMessage(), t);
                }
            }
        }

        if (logger.isDebugEnabled())
            logger.debug("Time to process JSF annotations: " + (System.currentTimeMillis() - before) + " ms");

        return map;
    }

    private void processClass(Map<Class<? extends Annotation>, Set<Class<?>>> map, Class<?> clazz) {
        Annotation[] annotations = clazz.getAnnotations();
        if (annotations.length == 0)
            throw new IllegalStateException("No annotations found for class: " + clazz + " with loader: " + clazz.getClassLoader());
        for (Annotation anno : annotations) {
            Class<? extends Annotation> annotationClass = anno.annotationType();
            if (JSF_ANNOTATION_CLASSES.contains(annotationClass.getName())) {
                if (logger.isDebugEnabled())
                    logger.debug("Processing JSF-annotated class: " + clazz.getName());

                if (annotationClass.getName().equals(ManagedBean.class.getName())) {
                    // avoid failed MDWHub deployment due to MyFaces field access during managed bean config
                    // trigger failure here and avoid adding the managed bean
                    clazz.getDeclaredFields();

                    if (classLoaderExtension == null) {
                        classLoaderExtension = new DynamicJavaClassLoaderExtension();
                        ClassUtils.addClassLoadingExtension(classLoaderExtension, true);
                    }

                    classLoaderExtension.addManagedBeanClass(clazz.getName());
                }
                Set<Class<?>> set = map.get(annotationClass);
                if (set == null) {
                    set = new HashSet<Class<?>>();
                    set.add(clazz);
                    map.put(annotationClass, set);
                }
                else {
                    set.add(clazz);
                }
            }
        }
    }

    @Override
    public Set<URL> getBaseUrls(ExternalContext ctx) throws IOException {
        // specifies where to load faces-config files from
        return wrappedProvider.getBaseUrls(ctx);
    }

    @Override
    @SuppressWarnings("deprecation")
    public Set<URL> getBaseUrls() throws IOException {
        return wrappedProvider.getBaseUrls();
    }
}
