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
package com.centurylink.mdw.common.service.types;

import javax.xml.bind.annotation.XmlRegistry;


/**
 * This object contains factory methods for each
 * Java content interface and Java element interface
 * generated in the com.centurylink.mdw.common.service.types package.
 * <p>An ObjectFactory allows you to programatically
 * construct new instances of the Java representation
 * for XML content. The Java representation of XML
 * content can consist of schema derived interfaces
 * and classes representing the binding of schema
 * type definitions, element declarations and model
 * groups.  Factory methods for each of these are
 * provided in this class.
 *
 */
@XmlRegistry
public class ObjectFactory {

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: com.centurylink.mdw.common.service.types
     *
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link Action }
     *
     */
    public Action createAction() {
        return new Action();
    }

    /**
     * Create an instance of {@link Parameter }
     *
     */
    public Parameter createParameter() {
        return new Parameter();
    }

    /**
     * Create an instance of {@link Content }
     *
     */
    public Content createContent() {
        return new Content();
    }

    /**
     * Create an instance of {@link ActionRequest }
     *
     */
    public ActionRequest createActionRequest() {
        return new ActionRequest();
    }

    /**
     * Create an instance of {@link ResourceRequest }
     *
     */
    public ResourceRequest createResourceRequest() {
        return new ResourceRequest();
    }

    /**
     * Create an instance of {@link Resource }
     *
     */
    public Resource createResource() {
        return new Resource();
    }
}
