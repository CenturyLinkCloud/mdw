/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.service.api;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.Path;

import org.reflections.Reflections;

import com.centurylink.mdw.common.service.JsonService;
import com.centurylink.mdw.common.service.MdwServiceRegistry;
import com.centurylink.mdw.common.service.XmlService;
import com.centurylink.mdw.service.rest.Assets;

import io.swagger.annotations.Api;
import io.swagger.config.Scanner;

public class MdwScanner implements Scanner {

    private String servicePath; // must begin with '/'

    public MdwScanner() {
        this("/", true);
    }

    public MdwScanner(String servicePath) {
        this(servicePath, true);
    }

    public MdwScanner(String servicePath, boolean prettyPrint) {
        this.servicePath = servicePath;
        this.prettyPrint = prettyPrint;
    }

    private boolean prettyPrint = true;
    public boolean getPrettyPrint() { return prettyPrint; }
    public void setPrettyPrint(boolean prettyPrint) { this.prettyPrint = prettyPrint; }

    @Override
    public Set<Class<?>> classes() {
        Set<Class<?>> classes = new HashSet<Class<?>>();

        // swagger definition
        classes.add(RestApiDefinition.class);
        // rest services
        // classes.addAll(new Reflections(RestService.class.getPackage().getName()).getTypesAnnotatedWith(Api.class));
        for (Class<?> c : new Reflections(Assets.class.getPackage().getName()).getTypesAnnotatedWith(Api.class)) {
            if ("/".equals(servicePath))
                classes.add(c);
            else {
                String path = c.getSimpleName();
                Path pathAnnotation = c.getAnnotation(Path.class);
                if (pathAnnotation != null && pathAnnotation.value() != null)
                    path = pathAnnotation.value();
                if (path.startsWith(servicePath) || ("/" + path).startsWith(servicePath))
                    classes.add(c);
            }
        }

        // model packages are processed automatically if they're used
        // resourcePackages.add(AuthenticatedUser.class.getPackage().getName());
        // classes.addAll(new Reflections(resourcePackage).getTypesAnnotatedWith(ApiModel.class));

        // dynamic java service classes
        for (Class<? extends JsonService> jsonServiceClass : MdwServiceRegistry.getInstance().getDynamicServiceClasses((JsonService.class))) {
            if (jsonServiceClass.getAnnotation(Api.class) != null && !classes.contains(jsonServiceClass)) {
                if ("/".equals(servicePath))
                    classes.add(jsonServiceClass);
                else {
                    String path = jsonServiceClass.getName().replace('.', '/');
                    Path pathAnnotation = jsonServiceClass.getAnnotation(Path.class);
                    if (pathAnnotation != null && pathAnnotation.value() != null) {
                        path = jsonServiceClass.getPackage().getName().replace('.', '/');
                        if (!pathAnnotation.value().startsWith("/"))
                            path += "/";
                        path += pathAnnotation.value();
                    }
                    if (path.startsWith(servicePath) || ("/" + path).startsWith(servicePath))
                        classes.add(jsonServiceClass);
                }
            }
        }
        for (Class<? extends XmlService> xmlServiceClass : MdwServiceRegistry.getInstance().getDynamicServiceClasses((XmlService.class))) {
            if (xmlServiceClass.getAnnotation(Api.class) != null && !classes.contains(xmlServiceClass)) {
                if ("/".equals(servicePath))
                    classes.add(xmlServiceClass);
                else {
                    String path = xmlServiceClass.getName().replace('.', '/');
                    Path pathAnnotation = xmlServiceClass.getAnnotation(Path.class);
                    if (pathAnnotation != null && pathAnnotation.value() != null) {
                        path = xmlServiceClass.getPackage().getName();
                        if (!pathAnnotation.value().startsWith("/"))
                            path += "/";
                        path += pathAnnotation.value();
                    }
                    if (path.startsWith(servicePath) || ("/" + path).startsWith(servicePath))
                        classes.add(xmlServiceClass);
                }
            }
        }
        return classes;
    }
}
