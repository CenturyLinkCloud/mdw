---
permalink: /docs/guides/MicroservicesCookbook/
title: Microservices Cookbook
---
### MDW Microservices

This document contains information about creating, exposing and consuming services through a RESTFul Web Service. For a SOAP document-style Web Service, refer to [SOAP Service](../SOAPService/).

##### Before you can start working with MDW framework, you will need to do a one-time setup. Please follow [this link](../SetupGuideForTomcat/) to setup your workspace and return to this guide to continue.
  
### Local Development:
#### 1. Create a Local Project
A local project is useful if you want to debug your custom Java source code and Groovy scripts.  The standard MDW war file is deployed as part of the steps outlined in this tutorial.

##### Open the Designer Perspective in Eclipse:
- Launch Eclipse (with the MDW Plug-In installed).
- From the menus select Window > Open Perspective > Other > MDW Designer.
- For detailed documentation covering the designer, refer to the [MDW Designer User Guide](../../designer/user-guide/)
 
##### Launch the Local Project wizard:
- Right-click inside the blank Process Explorer view and select New > Local Project.  Select the Supported Java Container you will be deploying in, and the type of [Asset Persistence](../../help/assetPersistence.html)  you will use.

  ![alt text](../images/workflow.png "workflow")
- When you click Next, you'll be presented with the Tomcat for your local development.  Enter the settings for your environment. For the password, you can enter `tomcat`.  For details about these settings, refer to the server-specific cookbooks listed above under "Supported Java Containers" section.
  
  ![alt text](../images/tomcatSetting.png "tomcatSetting")
- Click Next again and enter your database connection info.  The password for database is `mdw`. 

  ![alt text](../images/dbSetting.png "dbSetting")
  
- Click Finish to generate your local project.

### The MDW Base Package:
- When you create design artifacts in MDW, these are organized into workflow packages, which are different from Java packages in that they can contain assets in a wide variety of formats.  Much of the MDW framework's core functionality itself is delivered this way.  The essential assets required by MDW are included in the packages `com.centurylink.mdw.base` and `com.centurylink.mdw.hub`.  If you choose the built-in database asset persistence, these base packages will already exist, and you can skip down to Section 2.  Otherwise, if you're using a new database or VCS asset persistence, you'll need to import these packages locally from the MDW repository as follows.
- Expand your newly-created workflow project in Process Explorer and you will see that it currently contains no packages.  Right-click on the project and select Import > Package.  Choose the `Discover` option and leave the repository location as the default. 

  ![alt text](../images/importBasePackages.png "importBasePackages")

- After you click Next it will take a few moments for Designer to locate the available packages.  Once these are displayed, put a check mark on base, db and hub packages.

  ![alt text](../images/importBasePackages2.png "importBasePackages2")
  
- Click Finish, and the packages will be downloaded and become visible in your Process Explorer project tree.
 
### Workflow Services

#### 2. Create a Service Process
##### Create a Workflow Package:
- The top-level branches in the Process Explorer project tree represent workflow packages.  Your work should be incorporated in a dedicated package, which will be used for managing resources and for insulating your work from that of other users.  For further details refer to the Eclipse Cheat Sheet (Help > Cheat Sheets > MDW Workflow > Importing, Exporting and Versioning).
- Create your workflow package by right-clicking on your project and selecting New > MDW Package.  Note: make sure your package name complies with Java package naming requirements (eg: no spaces) since it will contain dynamic Java resources.  Leave the Workgroup dropdown blank.

   ![alt text](../images/mdwWorkflowMyServicePackage.png "mdwWorkflowMyServicePackage")

##### Create the Service Process:
-  Right-click on your new package in Process Explorer and select New > MDW Process.  Enter the process name and description (no workgroup), and click Finish.

   ![alt text](../images/myOrderProcess.png "myOrderProcess")
 
-  After your process is created, double-click on the process title or on a blank area somewhere in the canvas to display the Properties View.  Select the Design properties tab and check "Service Process" to identify OrderProcess as a synchronous process returning a response. 

   ![alt text](../images/myOrderProcess2.png "myOrderProcess2")
 
##### Add some Process Variables:
-  The convention in MDW is that a service request variable is named "request" and a service response variable is named "response".  There's the option to name these differently, but for simplicity let's go along with the convention here.  On the Variables property tab, create these two variables in your process with type org.json.JSONObject.  Set the mode for the request variable to be Input, and the mode for the response to be Output.  Add String variables orderId and validationResult.

   ![alt text](../images/myOrderProcessVariable.png "myOrderProcessVariable")
   
- Save your process design by selecting File > Save from the menu (or by clicking the disk icon in the Eclipse toolbar, or by typing ctrl-s).  Elect to overwrite the current version and to keep the process locked after saving.  During iterative development for convenience you'll sometimes overwrite the existing version of a process definition.  However once you've exported to another environment you'll want to increment the version since you cannot re-import a changed process with the same version number.  Details are covered under Help > Cheat Sheets > MDW Workflow > Importing, Exporting and Versioning.  

   ![alt text](../images/saveMyOrderProcess.png "saveMyOrderProcess")

##### Create a Dynamic Java Custom Activity:
- Right-click on your package in Process Explorer and select New > Activity > General Activity.
- On the first page of the wizard, enter a label to identify your activity in the Toolbox view.

   ![alt text](../images/myOrderValidator.png "myOrderValidator")
   
- Click Next and enter a class name for your activity implementor. The Java package name is the same as your workflow package name.

   ![alt text](../images/myOrderValidatorActivity.png "myOrderValidatorActivity")
   
- When you click Finish the Java code for a skeleton implementation is generated. You will also see the Java class under your package in Process Explorer. 
- This source code resides under src/main/workflow and is known as a Dynamic Java workflow asset. It's dynamic because it can be changed without needing any kind of application deployment. Naturally there are rigorous controls in place to prevent unauthorized modifications.
- In step 1 you were granted permissions in the MDW environment to create and modify workflow assets.
- With Dynamic Java, as with all types of workflow assets, MDW provides facilities for versioning, rollback and import/export for migrating between environments.

- Update the generated Java source code to resemble the following:  

```java
package MyServices;
import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.model.workflow.ActivityRuntimeContext;
import com.centurylink.mdw.util.log.StandardLogger.LogLevel;
import com.centurylink.mdw.util.timer.Tracked;
import com.centurylink.mdw.workflow.activity.DefaultActivityImpl;
import org.json.JSONObject;
  
@Tracked(LogLevel.TRACE)
public class MyOrderValidatorActivity extends DefaultActivityImpl {
	@Override
	public Object execute(ActivityRuntimeContext runtimeContext) throws ActivityException {
		loginfo("Validating order...");
		boolean valid = false;
		try {
			JSONObject jsonObj = (JSONObject) getVariableValue("request");
			String orderId = (String) jsonObj.get("orderId");
			setVariableValue("orderId", orderId);
			String msg = "Success";
			if (!jsonObj.has("orderId")){
				msg = "Missing order ID.";
			}
			else if (!Character.isDigit(orderId.charAt(0))) {
				msg = "Order ID must begin with a digit.";	        
			}
			valid = msg.equals("Success");
			setVariableValue("validationResult", msg);
		} catch (Exception ex) {
			throw new ActivityException(ex.getMessage(), ex);
		}
		return valid;
	}
}
```

- Now if you switch back to your process the new activity should appear in the Toolbox View. From the toolbox, drag your activity onto the canvas and insert it into your process flow between the Start and Stop activities.
- Tip: To draw a link (or transition in MDW terminology) between activities on the designer canvas, hold down the Shift key on your keyboard, Click on the upstream activity, and continue holding down the mouse left click button while dragging the cursor to the downstream activity (shift+click+drag).
- Your activity can be dragged like this and used in other processes designed by other users. Actually the proper term in MDW for this reusable element in the Toolbox is activity implementor. This conveys the idea that it is actually a template to be dragged and configured as an activity in the canvas, and it also conveys the fact that it always corresponds to a Java class. To take this reuse concept a step further, your activity implementor can be made discoverable so that it can easily be imported into other environments and reused across domains. If you click on the light bulb icon at the top of the Toolbox you will get an idea how items in the palette can be imported from a file or discovered in the corporate repository.
- Double click the activity in the canvas, and in its Definition property tab change the label to something like Validate Order. When you click back on the canvas the activity reflects its new label.

   ![alt text](../images/myOrderValidatorActivity2.png "myOrderValidatorActivity2")
   
- Note: If you select the Design property tab for your activity you will see that it is blank. A non-trivial activity would allow certain aspects (such as endpoint URLs) to be configurable, so that it could readily be reused. For example, take a look at the Design tab for the Start activity. You control what appears on the Design tab through the pagelet XML for the activity implementor. In the creation wizard we left the pagelet XML blank, so the Design tab for our activity is empty. But to continue with the example of the start activity, find the Process Start icon in the Toolbox and view its Design tab (for the implementor, not the activity on the canvas). This gives you an idea of how the pagelet XML relates to the fields on the Design tab for the activity user. Since we are on the subject you may be interested to know how you can customize the icon for your activity implementor. On the Definition tab you can choose one of the built-in shapes, or more flexibly choose any GIF, JPG or PNG asset that you can easily add to your workflow package.

##### Add Multiple Activity Outcomes:
- Drag a Process Finish activity from the Toolbox, and add another outbound transition from Validate Order. Assign Result Code values of true and false to the respective transitions as illustrated below. Save your process definition. The value passed in setReturnCode() in your activity execute() method dictates which of these two paths will be.

   ![alt text](../images/myOrderValidatorActivity3.png "myOrderValidatorActivity3")

##### Get Your Server Running:
- Depending on which supported container you're using and if you have not done a `one-time setup`, you can follow one of the server setup exercises.  You will need to follow the steps from one of these guides to the point where MDW is deployed and you're able to start and stop your server from the Eclipse Servers view. 
    - [Setup Guide for Tomcat](../SetupGuideForTomcat/)
    - [Setup Guide for CloudFoundry](../SetupGuideForCloudFoundry/)
- You can confirm that MDW was successfully deployed by accessing MDWHub in your browser:
     - Tomcat:                                                                                  
       [http://localhost:8080/mdw](http://localhost:8080/mdw)
 
- Troubleshooting: if you encounter an exception like below, it means you need to download the JCE 8 Unlimited Strength Jurisdiction Policy Files for JRE 1.8.  Or JCE 8 Unlimited Strength Jurisdiction Policy Files  for JRE 1.8
 
	    java.security.InvalidKeyException: Illegal key size or default parameters
 
##### Open the Process Launch Dialog:
- Right-click on the MyOrderProcess process in Process Explorer view and Select Run.  Designer will present the launch dialog and open a connection to the server to confirm that it is running `(required for launching a process)`.
- On the Process tab in the launch dialog, select "Monitor Runtime Log" and "Process Instance Live View" to get a feel for how you can watch your process flow in real time.

  ![alt text](../images/myOrderProcessRunConfig.png "myOrderProcessRunConfig")
 
##### Populate the Input Variable:
- Select the Variables tab in the launch dialog, and populate the orderId input.

  ![alt text](../images/populateOrderInputVariable.png "populateOrderInputVariable")
 
##### Launch and View an Instance:
- Click Run on the launch dialog to run an instance of your process.  In the Live View you should see the new instance progress down the happy path with the Validate Order outcome equal to 'true'.  For processes not displayed in Live View, you can open an instance manually by right-clicking on your process in Process Explorer view and selecting View Instances.  The latest instance will appear at the top of the Process Instances list, and you can double-click to open its runtime view. 

- In Designer Perspective, a legend appears showing what the borders surrounding the activities mean.  To inspect the runtime variable values for the instance, click the Values property tab.

  ![alt text](../images/viewOrderInstance.png "viewOrderInstance")
 
#### 3. Expose a RESTFul Web Service using JAX-RS API
#### 1. Implement a JAX-RS Web Service

Besides implementing services by way of an MDW workflow process, you can easily expose your Dynamic Java class as a REST service using JAX-RS annotations.
 
##### Create a Java Asset to Implement a Resource Service:
- Right-click on your package in Process Explorer view and select new > Java Source.  By convention the Java class name will also be the name of your service resource.  Also by convention your workflow package name is the root of the REST endpoint URL path that consumers will use to access your service.  For this simple example we're using the MyServices.  In a real-world app you'll probably use a qualified package name like com.centurylink.my.services, and in that case you can specify a simplified URL path through the JAX-RS Path annotation, which will be illustrated later.
  
  ![alt text](../images/createOrderProcessJavaAsset.png "createOrderProcessJavaAsset")

- Implement a REST service, using the JAX-RS @Path annotation and extending the MDW JsonRestService class:
  ```java  
  package MyServices;
  import java.util.HashMap;
  import java.util.Map;
  import javax.ws.rs.Path;
  import org.json.JSONObject;
  import com.centurylink.mdw.common.service.ServiceException;
  import com.centurylink.mdw.services.ServiceLocator;
  import com.centurylink.mdw.services.WorkflowServices;
  import com.centurylink.mdw.services.rest.JsonRestService;
  
  @Path("/Order")
  public class Orders extends JsonRestService {
	@Override
	public JSONObject post(String path, JSONObject content, Map<String, String> headers) throws ServiceException{
		Map<String,Object> stringParams = new HashMap<String,Object>();
		WorkflowServices workflowServices = ServiceLocator.getWorkflowServices();
		Object response = workflowServices.invokeServiceProcess("MyServices/OrderProcess", content, null, stringParams, headers);
		return (JSONObject) response;
	}
  }
```   
- Access your service using a POST request from your browser with a URL like the following:

  - [http://localhost:8080/mdw/Services/MyServices/Order](http://localhost:8080/mdw/Services/MyServices/Order)            

- Save your Dynamic Java asset, and use the MDWHub HTTP Poster tool to submit a POST request to add an order from your browser and you will see the response showing in the JSON format.
   ![alt text](../images/restPostRequestAndResponse.png "restPostRequestAndResponse")
   
#### 2. Add Swagger API Annotations

With MDW REST services you can automatically generate Swagger documentation just by adding the appropriate annotations to your Dynamic Java.  This is not only a convenient way to maintain this documentation, but it also means that it's always up-to-date with the implementation.  Swagger documentation is a powerful way to communicate the specifics of your REST interface to potential consumers.  Swagger annotations represent a convenient mechanism for service developers to produce this documentation without having to hand-craft the JSON or YAML results.
 
##### Add the @Api Annotation to Your Service:
- The Swagger Api annotation goes on your class declaration along with the JAX-RS Path annotation.  The tag value in your annotation provides a high-level description of the its purpose:

  ```swagger
@Path("/Order")	
@Api("CenturyLink orders service")
public class Orders extends JsonRestService {
```

##### Add @ApiOperation Annotations to Your Methods:
- The ApiOperation annotation documents the specifics of a service endpoint operation, including any input or output model types.  The ApiImplicitParams annotation is useful for indicating the body content of a POST or PUT requests.  After adding these annotations to Orders.java, the code will look something like this:   

```java
package MyServices;
import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.Path;
import org.json.JSONObject;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.common.service.types.StatusMessage;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.WorkflowServices;
import com.centurylink.mdw.services.rest.JsonRestService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;

@Path("/Order")
@Api("CenturyLink orders service")
public class Orders extends JsonRestService {
	@Override
	@ApiOperation(value="Create an order",
	notes="Does not actually create anything as yet.", response=StatusMessage.class)
	@ApiImplicitParams({@ApiImplicitParam(name="Order", paramType="body", dataType="MyServices.Order")})
	public JSONObject post(String path, JSONObject content, Map<String, String> headers) throws ServiceException{
		Map<String,Object> stringParams = new HashMap<String,Object>();
		WorkflowServices workflowServices = ServiceLocator.getWorkflowServices();
		Object response = workflowServices.invokeServiceProcess("MyServices/MyOrderProcess", content, null, stringParams, headers);
		return (JSONObject) response;
	}
}
```

##### Add Swagger Annotations to the Orders Class:
- To enable consumers to easily create request content and interpret responses, you can annotate the related model objects so that they're discovered when documentation is generated.  In the Orders dynamic Java class, add the following class-level annotation:

  ```swagger
@ApiModel(value="Order", description="Centurylink Order")	
public class Orders extends JsonRestService {
// Add your logic.
```

#### 3. View Generated REST APIs in MDWHub
MDWHub comes with a UI for displaying your generated Swagger API documentation, along with the standard MDW REST APIs.
 
##### Access the MDWHub Service API Page for Your Service:
- Open MDW in your browser and click on the Services tab.  Notice that API path for your service (/MyServices/Order) includes its package name to distinguish it from standard MDW services.
   ![alt text](../images/restServiceAPIs.png "restServiceAPIs")

- Click on the /MyServices/Orders link.  The JSON and YAML tabs include the Swagger Spec API definitions for the Order endpoint.  Click on the YAML tab to view a human-readable representation of your Order API.  Notice that much of the information is provided by annotations from the MDW base service class.
   ![yamlExample](../images/yamlExample.png "yamlExample")

- Scroll down to the "definitions" section to see the Order object definition as well as other referenced types.
- Now click on the Swagger subtab to explore the friendly swagger-editor UI for your service.
   ![alt text](../images/swaggerExample.png "swaggerExample")
 
##### Add a Sample Request and Response:
- Sample payloads in MDW are by convention kept in an asset package under the service package whose name ends with "api.samples".  Each sample should be named to indicate its path and purpose, with an underscore separating these two parts.  Create a new MDW package named "MyServices.api.samples" and add a JSON asset named Order_Create.json with the following content:

  ```jason
 // POST request to Services/MyServices/Order
  {
   "orderId":"12345678"
  }
```

##### View the Samples in MDWHub:
- Now that your sample requests have been created in accordance with the MDW naming convention, they will automatically be associated with the corresponding service path.  And they'll also be displayed in the Samples tab for your service in MDWHub:
   ![alt text](../images/jsonSamples.png "jsonSamples")

#### 4. Consume a RESTFul Web Service
MDW comes with Adapter activities for consuming services over many protocols from within your workflow processes.  In this exercise we'll use the REST Service Adapter activity to invoke the OrderProcess service you just created.
 
##### Create a Process with a REST Service Activity:
- Open the same process definition you started building in the sections above.  
- Create a new process to consume a service.  From the Toolbox view drag a RESTful Service Adapter onto the canvas and insert it into your process flow. Label the web service activity "Submit Order" as shown on the image bellow.
   ![alt text](../images/consumeMyOrderProcess.png "consumeMyOrderProcess")
   
- On the Design tab for the web service activity, set the HTTP Method to POST and enter the same REST endpoint URL to consume a service from within your workflow. This example shows how to consume an existing service:  http://lxdenvmtc143.dev.qintra.com:8515/mdw/services/Ping  
  ![alt text](../images/consumeMyOrderProcess2.png "consumeMyOrderProcess2")
  
##### Save and Run Your Process:
- Send a POST request from your browser with a URL like the following as you did in section 3:
http://localhost:8080/mdw/Services/MyServices/Order
- On the Designer, view the instance to confirm that the orderId was populated as expected.
- In the process instance view, double-click the OrderProcess activity instance.  Then on the Instance property tab, double-click on the activity instance row.  The Activity Instance dialog shows you the raw request and response values that were sent over the wire. 

   ![alt text](../images/orderProcessActivityInstance.png "orderProcessActivityInstance")   
   ![alt text](../images/orderProcessActivityInstance2.png "orderProcessActivityInstance2")    
 
