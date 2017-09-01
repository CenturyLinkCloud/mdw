## Package com.centurylink.mdw.demo.intro

The MDW demo intro package contains assets for executing the HandleOrder process.
Use the sample request below as the external event to trigger HandleOrder 
(changing the OrderNumber element to something unique).

The assets in this package illustrate the following MDW features:
 - Embedded process documentation
 - Custom and AutoForm manual tasks
 - Dynamic Java activity
 - Custom task actions
 - Dynamic Java object variable
 - External Event Handlers
 - REST services and Swagger API docs
 - Web service adapter activity
 - Markdown documentation (like this)
 - MDW Automated tests
   (under intro/tests)
 - TODO: Fallout/Error handling
 
 Sample Request:
 ```
 <donsOrder>
  <orderNumber>X000901</orderNumber>
  <customerId>DHO115360</customerId>
</donsOrder>
```

TODO: more thorough docs
