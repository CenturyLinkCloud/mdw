/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.service.types;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


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

    private final static QName _Task_QNAME = new QName("http://mdw.centurylink.com/services", "Task");
    private final static QName _TaskAction_QNAME = new QName("http://mdw.centurylink.com/services", "TaskAction");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: com.centurylink.mdw.common.service.types
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link Error }
     * 
     */
    public Error createError() {
        return new Error();
    }

    /**
     * Create an instance of {@link Action }
     * 
     */
    public Action createAction() {
        return new Action();
    }

    /**
     * Create an instance of {@link Task }
     * 
     */
    public Task createTask() {
        return new Task();
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
     * Create an instance of {@link TaskAction }
     * 
     */
    public TaskAction createTaskAction() {
        return new TaskAction();
    }

    /**
     * Create an instance of {@link ResourceRequest }
     * 
     */
    public ResourceRequest createResourceRequest() {
        return new ResourceRequest();
    }

    /**
     * Create an instance of {@link Status }
     * 
     */
    public Status createStatus() {
        return new Status();
    }

    /**
     * Create an instance of {@link Resource }
     * 
     */
    public Resource createResource() {
        return new Resource();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Task }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://mdw.centurylink.com/services", name = "Task")
    public JAXBElement<Task> createTask(Task value) {
        return new JAXBElement<Task>(_Task_QNAME, Task.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link TaskAction }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://mdw.centurylink.com/services", name = "TaskAction")
    public JAXBElement<TaskAction> createTaskAction(TaskAction value) {
        return new JAXBElement<TaskAction>(_TaskAction_QNAME, TaskAction.class, null, value);
    }

}
