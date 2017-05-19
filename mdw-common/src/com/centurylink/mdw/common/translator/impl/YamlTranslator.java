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
package com.centurylink.mdw.common.translator.impl;

import java.beans.IntrospectionException;
import java.util.Set;

import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.constructor.ConstructorException;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.representer.Representer;

import com.centurylink.mdw.bpm.ProcessExecutionPlanDocument;
import com.centurylink.mdw.model.Jsonable;
import com.centurylink.mdw.java.CompiledJavaCache;
import com.centurylink.mdw.model.ExecutionPlan;
import com.centurylink.mdw.translator.DocumentReferenceTranslator;
import com.centurylink.mdw.translator.JsonTranslator;
import com.centurylink.mdw.translator.TranslationException;
import com.centurylink.mdw.translator.XmlDocumentTranslator;

/**
 * By convention, the first line of serialized yaml is a comment that
 * declares the type (class name) of the JavaBean object.
 * Implements XmlDocumentTranslator only for serializing as ExecutionPlan (TODO fix that).
 */
public class YamlTranslator extends DocumentReferenceTranslator implements XmlDocumentTranslator, JsonTranslator {

    public Object realToObject(String str) throws TranslationException {
        try {
            Representer representer = new Representer();
            representer.getPropertyUtils().setSkipMissingProperties(true);
            try {
                return new Yaml(representer).load(str);
            }
            catch (ConstructorException ex) {
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
        }
        catch (Exception e) {
            throw new TranslationException(e.getMessage(), e);
        }
    }

    public String realToString(Object obj) throws TranslationException {
        try {
            return new Yaml(getRepresenter(), getDumperOptions()).dump(obj);
        }
        catch (Exception ex) {
            throw new TranslationException(ex.getMessage(), ex);
        }
    }

    @Override
    public Document toDomDocument(Object obj) throws TranslationException {
        try {
            ExecutionPlan exePlan = (ExecutionPlan) obj;
            return (Document)exePlan.toDocument().getDomNode();
        }
        catch (Exception ex) {
            throw new TranslationException(ex.getMessage(), ex);
        }
    }

    @Override
    public Object fromDomNode(Node domNode) throws TranslationException {
        try {
            ProcessExecutionPlanDocument exePlanDoc = ProcessExecutionPlanDocument.Factory.parse(domNode);
            ExecutionPlan exePlan = new ExecutionPlan();
            exePlan.fromDocument(exePlanDoc);
            return exePlan;
        }
        catch (Exception ex) {
            throw new TranslationException(ex.getMessage(), ex);
        }
    }

    @Override
    public JSONObject toJson(Object obj) throws TranslationException {
        try {
            if (obj instanceof Jsonable) {
                // prefer jsonable serialization
                return ((Jsonable)obj).getJson();
            }
            else {
                throw new UnsupportedOperationException("No Yaml > Json serialization for non-Jsonables");
            }
        }
        catch (Exception ex) {
            throw new TranslationException(ex.getMessage(), ex);
        }
    }

    @Override
    public Object fromJson(JSONObject json) throws TranslationException {
        try {
            if (json.has(JSONABLE_TYPE)) {
                return createJsonable(json);
            }
            else {
                throw new UnsupportedOperationException("No Json > Yaml deserialization for non-Jsonables");
            }
        }
        catch (Exception ex) {
            throw new TranslationException(ex.getMessage(), ex);
        }
    }


    protected DumperOptions getDumperOptions() {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.FLOW);
        options.setPrettyFlow(true);
        options.setIndent(2);
        return options;
    }

    protected Representer getRepresenter() {
        return new Representer() {
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
    }

}
