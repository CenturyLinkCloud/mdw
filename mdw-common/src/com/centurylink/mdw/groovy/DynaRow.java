/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.groovy;

import groovy.util.Expando;
import groovy.util.Node;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.xmlbeans.XmlObject;
import org.w3c.dom.Document;

import com.centurylink.mdw.common.translator.DocumentReferenceTranslator;
import com.centurylink.mdw.common.translator.VariableTranslator;

/**
 * Extends the Groovy Expando concept to provide a dynamic datarow
 * object whose columns can be changed as needed.
 */
@SuppressWarnings("unchecked")
public class DynaRow extends Expando implements Serializable {

    private static final long serialVersionUID = 1L;

    transient private Map<String,Object> properties;

    public DynaRow() {
    }

    public DynaRow(String[] names, String[] values) {
        this();
        for (int i = 0; i < names.length; i++) {
            this.setProperty(names[i], values[i]);
        }
    }

    public DynaRow(Map<?,?> properties) {
        this.properties = getProperties();
        for (Object key : properties.keySet()) {
            setProperty((String)key, properties.get(key));
        }
    }

    @Override
    protected Map<?,?> createMap() {
        properties = new HashMap<String,Object>();
        return properties;
    }

    @Override
    public void setProperty(String property, Object newValue) {
        // store documents as Strings to avoid ClassNotFoundException
        // during deserialization (schema types in XMLBeans, for example)
        String type = newValue.getClass().getName();

        // in case of sub-types
        if (newValue instanceof XmlObject)
            type = XmlObject.class.getName();
        else if (newValue instanceof Node)
            type = Node.class.getName();
        else if (newValue instanceof Document)
            type = Document.class.getName();

        if (VariableTranslator.isDocumentReferenceVariable(type)) {
            newValue = DocumentReferenceTranslator.realToString(type, newValue);
        }
        super.setProperty(property, newValue);
    }

    public String[] getPropertyNames() {
        int size = getProperties().keySet().size();
        String[] propNames = new String[size];
        int i = 0;
        for (Object key : getProperties().keySet()) {
            propNames[i++] = key.toString();
        }
        return propNames;
    }

    public String[] getPropertyNamesSorted() {
        String[] propNames = getPropertyNames();
        Arrays.sort(propNames);
        return propNames;
    }

    /**
     * Override deserialization to read the properties map.
     */
    private void readObject(java.io.ObjectInputStream s) throws java.io.IOException, ClassNotFoundException {

        s.defaultReadObject();

        // read in properties map
        Map<String,Object> props = (HashMap<String,Object>)s.readObject();
        for (String key : props.keySet()) {
            setProperty(key, props.get(key));
        }
        properties = getProperties();
    }

    /**
     * Override serialization to write the properties map.
     */
    private void writeObject(java.io.ObjectOutputStream s) throws java.io.IOException {

        s.defaultWriteObject();

        // write out properties map
        s.writeObject(properties);
    }
}
