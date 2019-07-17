/*
 * Copyright (C) 2017 CenturyLink, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.centurylink.mdw.service.api;

import com.centurylink.mdw.common.service.JsonService;
import com.centurylink.mdw.common.service.MdwServiceRegistry;
import com.centurylink.mdw.common.service.TextService;
import com.centurylink.mdw.common.service.XmlService;
import com.centurylink.mdw.service.rest.Assets;
import io.swagger.annotations.Api;
import io.swagger.config.Scanner;
import org.reflections.Reflections;

import javax.ws.rs.Path;
import java.util.HashSet;
import java.util.Set;

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
        Set<Class<?>> classes = new HashSet<>();

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
                if (pathAnnotation != null)
                    path = pathAnnotation.value();
                if (servicePath.startsWith(path) || servicePath.startsWith("/" + path))
                    classes.add(c);
            }
        }

        // model packages are processed automatically if they're used in dynamic java services

        // dynamic java service classes
        for (Class<? extends JsonService> jsonServiceClass : MdwServiceRegistry.getInstance().getDynamicServiceClasses((JsonService.class))) {
            if (jsonServiceClass.getAnnotation(Api.class) != null && !classes.contains(jsonServiceClass)) {
                if (isMatch(jsonServiceClass, servicePath)) {
                    classes.add(jsonServiceClass);
                }
            }
        }
        for (Class<? extends XmlService> xmlServiceClass : MdwServiceRegistry.getInstance().getDynamicServiceClasses((XmlService.class))) {
            if (xmlServiceClass.getAnnotation(Api.class) != null && !classes.contains(xmlServiceClass)) {
                if (isMatch(xmlServiceClass, servicePath)) {
                    classes.add(xmlServiceClass);
                }
            }
        }
        return classes;
    }

    private boolean isMatch(Class<? extends TextService> serviceClass, String servicePath) {
        if ("/".equals(servicePath)) {
            return true;
        }
        else {
            String path = serviceClass.getName().replace('.', '/');
            Path pathAnnotation = serviceClass.getAnnotation(Path.class);
            if (pathAnnotation != null) {
                path = serviceClass.getPackage().getName().replace('.', '/');
                if (!pathAnnotation.value().startsWith("/"))
                    path += "/";
                if (!pathAnnotation.value().equals("/"))
                    path += pathAnnotation.value();
            }
            if (path.equals(servicePath) || ("/" + path).equals(servicePath)) {
                return true;
            }
            else if (servicePath.contains("/{") && servicePath.endsWith("}")) {
                // parameterized -- matches if all remaining path segments are parameterized
                String nonparam = servicePath.substring(0, servicePath.indexOf("/{"));
                if (path.equals(nonparam) || ("/" + path).equals(nonparam))
                    return true;
            }
        }
        return false;
    }
}
