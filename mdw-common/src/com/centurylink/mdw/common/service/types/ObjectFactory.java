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

}
