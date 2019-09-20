package com.centurylink.mdw.boot;

import com.centurylink.mdw.annotations.Activity;
import com.centurylink.mdw.model.workflow.ActivityImplementor;
import com.centurylink.mdw.service.data.activity.ImplementorCache;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AnnotationsScanner implements BeanFactoryAware {

    private BeanFactory beanFactory;
    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
        findSpringAnnotatedClasses();
    }

    /**
     * Source files under src/main/java are scanned for @Activity annotations.
     */
    public void findSpringAnnotatedClasses() {
        List<String> scanPackages = AutoConfigurationPackages.get(beanFactory);
        ClassPathScanningCandidateComponentProvider provider = createScannerComponentProvider();
        for (String scanPackage : scanPackages) {
            for (BeanDefinition beanDef : provider.findCandidateComponents(scanPackage)) {
                addImplementor(beanDef);
            }
        }
    }

    private ClassPathScanningCandidateComponentProvider createScannerComponentProvider() {
        ClassPathScanningCandidateComponentProvider provider
                = new ClassPathScanningCandidateComponentProvider(false);
        provider.addIncludeFilter(new AnnotationTypeFilter(Activity.class));
        return provider;
    }

    private ActivityImplementor addImplementor(BeanDefinition beanDef) {
        try {
            Class<?> cl = Class.forName(beanDef.getBeanClassName());
            Activity annotation = cl.getAnnotation(Activity.class);
            ActivityImplementor implementor = new ActivityImplementor(cl.getName(), annotation);
            ImplementorCache.addImplementor(implementor);
        }
        catch (Exception ex) {
            System.err.println("Cannot load annotations for bean: " + beanDef);
            ex.printStackTrace();
        }
        return null;
    }
}
