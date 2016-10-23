/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.task.types;

import javax.xml.bind.annotation.XmlRegistry;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the com.ctl.temp package. 
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
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: com.ctl.temp
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link SubTask }
     * 
     */
    public SubTask createSubTask() {
        return new SubTask();
    }

    /**
     * Create an instance of {@link SubTaskPlan }
     * 
     */
    public SubTaskPlan createSubTaskPlan() {
        return new SubTaskPlan();
    }

}
