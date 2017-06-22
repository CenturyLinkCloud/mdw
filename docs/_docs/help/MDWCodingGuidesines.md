---
permalink: /docs/help/MDWCodingGuidesines/
title: MDW Coding Guidelines
---
 
### MDW Coding Guidelines

The following standards should be followed as generally accepted good programming practices when developing MDW Workflow projects or the MDW Framework.

##### General
1. To ensure compliance with coding style and formatting conventions, import the following into your Eclipse workspace preferences (Window > Preferences > Java > Code Style > Code Templates and Formatter 
     - Templates: [https://github.com/CenturyLinkCloud/mdw/blob/master/docs/_docs/code/MDWCodeTemplates.xml](https://github.com/CenturyLinkCloud/mdw/blob/master/docs/_docs/code/MDWCodeTemplates.xml)
     - Formatter: [https://github.com/CenturyLinkCloud/mdw/blob/master/docs/_docs/code/MDWCodeFormatter](https://github.com/CenturyLinkCloud/mdw/blob/master/docs/_docs/code/MDWCodeFormatter.xml)
       
2. Incorporate JavaDoc comments into your code for each top-level type (class or interface), and for each method whose implementation contains significant processing logic.  A method whose purpose is readily apparent from its name (such as a simple getter or setter) does not require a JavaDoc comment.

3. Method names should be verb-based to indicate the action or outcome which will result from invoking the method (eg: createProcessInstance()).

4. Maximize readability by keeping your code modular.  Avoid long and confusing method implementations by keeping your methods granular and chaining method calls where each call performs a discreet portion of your logic.  For situations involving complex processing where lengthy methods are unavoidable, include comments in your code to explain what is happening so that other developers will be able to understand the flow.

##### Logging
1. Application logging should employ an MDW StandardLogger instance obtained by calling LoggerUtil.getInstance().getStandardLogger().  Do not use System.out.println().  Store the logger as an instance variable, and avoid accessing the logger instance of your superclass (this can lead to confusion since the class reported in the log file will be the superclass instead of the subclass where the logger method was invoked).

2. Note: When implementing a custom activity, use the BaseActivity superclass methods such as:
```java
 logdebug(String), logwarn(String), logsevere(String), logexception(String, Exception), etc. 
 ```
 These methods have built in formatting which contains information about process, process instance, activity and activity instance.
```java
logdebug("Listener Invoked: Message is " + msg.getString());
```

   From within Groovy script or Dynamic Java activities, access to these methods is provided via the ActivityRuntimeContext:
   ```java
   runtimeContext.logdebug(String)
   ```

3. For logging output at INFO or DEBUG levels, test whether the appropriate logging level is enabled before building your output parameter string value.  This avoids the overhead of concatenating and building the output string when it may be discarded by the logger if logging is not enabled at the specified level.
Incorrect:
```java
logdebug("Listener Invoked: Message is " + msg.getString());
```
Correct:
```java
if (isDebugEnabled()) {
    logdebug ("Listener Invoked: Message is " + msg.getString());
}
```

##### Exception Handling
1. When deciding whether to catch an Exception, consider whether your code is the appropriate place to handle the exception.  If you have catch blocks for multiple exceptions which all contain the same behavior, it is more readable and maintainable to catch a common base exception type.     
Incorrect:
```java
catch (ServiceLocatorException ex) {
    logger.severeException(ex.getMessage(), ex);
}
catch (EventException ex) {
    logger.severeException(ex.getMessage(), ex);
}
catch (DataAccessException ex) {
    logger.severeException(ex.getMessage(), ex);
}
```
Correct:
```java
catch (MDWException ex) {
    logger.severeException(ex.getMessage(), ex);
}
```

2. It is legitimate to catch an exception in order to rethrow it as an exception type declared in your message header.  In this case it is best to wrap the originating exception in your rethrown exception so that when it is logged the entire exception causality chain will be available.   
Incorrect:
```java
catch (EventException ex) {
    throw new BusListenerException(EXCEPTION_CD, ex.getMessage());
}
```
Correct:
```java
catch (EventException ex) {
    throw new BusListenerException(EXCEPTION_CD, ex.getMessage(), ex);
}
```

##### Collections
1. Specify the general java.util collection Interfaces (List, Map, etc) in your APIs rather than specific implementations (ArrayList, HashMap, etc).  This provides a level of encapsulation which will enable the underlying implementation to change without requiring an interface change.
When accessing a collection returned by an API, treat the returned value as the general purpose entity rather than casting to the underlying type:   
   Incorrect:
   ```java
   ArrayList orderItems = (ArrayList) service.getOrderItems();
   OrderItem firstItem = (OrderItem) orderItems.get(0);
   ```
   Correct:
   ```java
   List<OrderItem> orderItems = service.getOrderItems();
   for (OrderItem item: orderItems){
      //Do stuff with item:
   }
   ```

##### MDW
1. Access to specific object-relational mapping tools, such as the Hibernate-specific model implementation classes in your workflow should be encapsulated in your Data Access Objects.  The goal is to isolate references to our persistence layer implementation so that if desired we can easily decouple from Hibernate and plug in another Object/Relational mapping library.
2. Much commonly-used functionality is encapsulated in the base classes provided by the MDWFramework.  Developers should take some time familiarizing themselves with the MDWFramework API so that they are aware of what's available.  The JavaDoc information for MDWFramework is published to the following URL:
[MDW Framework JavaDocs](http://centurylinkcloud.github.io/mdw/docs/javadoc/index.html)

##### XMLBeans
1. XMLBeans provide the binding framework for converting XML documents to Java objects based on their schema definitions.  The XMLBean document objects are usually closely coupled to the particular system interface defined in the XSD (for example an interface between NotificationWorkflow and NFS).  Avoid directly accessing elements in your XMLBeans in your mainline workflow processing so that your workflow is insulated from interface changes.  One strategy have been employed to provide this separation:
Create XMLBean wrapper classes to expose an interface-independent view of your XML data.  The wrapper classes encapsulate the logic for mapping between the XML document and the data model.  For an example of this approach see OrderDetailsWrapper.java in ESOWF project.
Either way, the convention is to include your structure/wrapper classes in a project module called WorkflowNameStructures.  These structures should not have dependencies on anything other than your schemas module and your model module.

2. XMLBeans provide an Enum implementation for XSD elements whose values are governed by xsd:string enumeration restrictions.  To take advantage of the type safety built into the Enums, always test and set these element values using enumeration constants:

   ```TODO: The following examples need to be rewritten as the RequestDocument class does not exist and the Request class does not provide Enums.```  

   Incorrect:
   ```java
   if (request.requestType().toString().equals("NEW")) {
        //Do stuff
   }
   ```
   Correct:
   ```java
   if (request.getRequestType().equals(RequestDocument.Request.NEW)) {
         //Do stuff
   }
   ```

