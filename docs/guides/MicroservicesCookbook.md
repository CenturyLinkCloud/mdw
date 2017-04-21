### MDW Microservices

This document contains information about creating, exposing and consuming services through a RESTFul Web Service. For a SOAP document-style Web Service, refer to [TomcatCookbook](TomcatCookbook.md).

### Prerequisites
 - Eclipse Neon for JavaEE Developers:                                                                                 
   [http://www.eclipse.org/downloads](http://www.eclipse.org/downloads)
 - Required Plugins:
     - MDW Designer:                                                    
       http://centurylinkcloud.github.io/mdw/designer/updateSite
     - Buildship Plugin:   
       http://download.eclipse.org/buildship/updates/e46/releases/2.x
 - Recommended Plugins:
     - Groovy:                                   
       http://dist.springsource.org/snapshot/GRECLIPSE/e4.6
     - Yaml:                                             
       http://dadacoalition.org/yedit
 - Servers:
     - Refer to `Supported Java Containers` in this tutorial 
 - Chrome and Postman
     - https://www.google.com/chrome
	  - https://chrome.google.com/webstore/detail/postman/fhbjgbiflinjbdggehcddcbncdddomop

### Java Containers
You can perform many cloud development activities using a remote workflow project.  However, there are certain advantages to being able to deploy locally.  The differences between local and remote development are described in later sections of this tutorial.  To be able to develop locally you need one of the following containers installed.  At certain points in this tutorial, we'll link to container-specific steps in the Cookbooks for each supported container.
  
### Supported Java Containers: 
-   Apache Tomcat 8:
    - [https://tomcat.apache.org](https://tomcat.apache.org)
    - [TomcatCookbook](TomcatCookbook.md)
-   Pivotal Cloud Foundry 2.x:
    - [http://pivotal.io/platform](http://pivotal.io/platform)
    - [CloudFoundryCookbook](CloudFoundryCookbook.md)
 
### MDW Database:
- MDW saves the workflow assets you create on your local file system until you commit them to a version control repository such as Git.  Runtime data is stored in a MySQL or Oracle 
  database. Generally for cloud development you'll point to a pre-existing central database.  If you want to host your own database, you'll need to configure an instance of MySQL 
  with the MDW db schema. The SQL scripts for installing the MDW schema are available here: [this readme](../../mdw/database/mysql/readme.txt).
  
### Local Development:
#### 1. Create a Local Project
A local project is useful if you want to debug your custom Java source code and Groovy scripts.  The standard MDW war file is deployed as part of the steps outlined in this tutorial.

##### Open the Designer Perspective in Eclipse:
- Launch Eclipse (with the MDW Plug-In installed).
- From the menus select Window > Open Perspective > Other > MDW Designer.
- For detailed documentation covering the designer, refer to the [MDW Designer User Guide](../designer/DesignerUserGuide.md)
 
##### Launch the Local Project wizard:
- Right-click inside the blank Process Explorer view and select New > Local Project.  Select the Supported Java Container you'll be deploying in, and the type of [Asset Persistence](../help/assetPersistence.html)  you'll use.

  ![xml formatter](images/workflow.png)
- When you click Next, you'll be presented with the Tomcat for your local development.  Enter the settings for your environment.  For details about these settings, refer to the 
  server-specific cookbooks listed above under "Supported Java Containers" section.
  
  ![xml formatter](images/tomcatSetting.png)
- Click Next again and enter your database connection info.  The password for database is "mdw". 

  ![xml formatter](images/dbSetting.png)
  
- Click Finish to generate your local project.

### The MDW Base Package:
- When you create design artifacts in MDW, these are organized into workflow packages, which are different from Java packages in that they can contain assets in a wide variety of formats.  Much of the MDW framework's core functionality itself is delivered this way.  The essential assets required by MDW are included in the packages "com.centurylink.mdw.base" and "com.centurylink.mdw.hub".  If you choose the built-in database asset persistence, these base packages will already exist, and you can skip down to Section 2.  Otherwise, if you're using a new database or VCS asset persistence, you'll need to import these packages locally from the MDW repository as follows.
- Expand your newly-created workflow project in Process Explorer and you'll see that it currently contains no packages.  Right-click on the project and select Import > Package.  Choose the "Discover" option and leave the repository location as the default.

  ![xml formatter](images/importBasePackages.png)

- After you click Next it'll take a few moments for Designer to locate the available packages.  Once these are displayed, expand the base, db and hub packages and select the same MDW version as you did when creating the project.

  ![xml formatter](images/importBasePackages2.png)
- Click Finish, and the packages will be downloaded and become visible in your Process Explorer project tree.
 
### Workflow Services

#### 2. Create a Service Process
##### Create a Workflow Package:
- The top-level branches in the Process Explorer project tree represent workflow packages.  Your work should be incorporated in a dedicated package, which will be used for managing resources and for insulating your work from that of other users.  For further details refer to the Eclipse Cheat Sheet (Help > Cheat Sheets > MDW Workflow > Importing, Exporting and Versioning).
- Create your workflow package by right-clicking on your project and selecting New > MDW Package.  Note: make sure your package name complies with Java package naming requirements (eg: no spaces) since it will contain dynamic Java resources.  Leave the Workgroup dropdown blank.

   ![xml formatter](images/mdwWorkflowMyServicePackage.png)

##### Create the Service Process:
-  Right-click on your new package in Process Explorer and select New > MDW Process.  Enter the process name and description (no workgroup), and click Finish.

   ![xml formatter](images/myOrderProcess.png)
 
-  After your process is created, double-click on the process title or on a blank area somewhere in the canvas to display the Properties View.  Select the Design properties tab and check "Service Process" to identify MyOrderProcess as a synchronous process returning a response. 

   ![xml formatter](images/myOrderProcess2.png)
 
##### Add some Process Variables:
-  The convention in MDW is that a service request variable is named "request" and a service response variable is named "response".  There's the option to name these differently, but for simplicity let's go along with the convention here.  On the Variables property tab, create these two variables in your process with type org.json.JSONObject.  Set the mode for the request variable to be Input, and the mode for the response to be Output.  Add String variables orderId and validationResult.

   ![xml formatter](images/myOrderProcessVariable.png)
   
- Save your process design by selecting File > Save from the menu (or by clicking the disk icon in the Eclipse toolbar, or by typing ctrl-s).  Elect to overwrite the current version and to keep the process locked after saving.  During iterative development for convenience you'll sometimes overwrite the existing version of a process definition.  However once you've exported to another environment you'll want to increment the version since you cannot re-import a changed process with the same version number.  Details are covered under Help > Cheat Sheets > MDW Workflow > Importing, Exporting and Versioning.  

   ![xml formatter](images/saveMyOrderProcess.png)

##### Create a Dynamic Java Custom Activity:
- Right-click on your package in Process Explorer and select New > Activity > General Activity.
- On the first page of the wizard, enter a label to identify your activity in the Toolbox view.

   ![xml formatter](images/myOrderValidator.png)
   
- Click Next and enter a class name for your activity implementor. The Java package name is the same as your workflow package name.

   ![xml formatter](images/myOrderValidatorActivity.png)
   
- When you click Finish the Java code for a skeleton implementation is generated. You will also see the Java class under your package in Process Explorer. 
- This source code resides under src/main/workflow and is known as a Dynamic Java workflow asset. It's dynamic because it can be changed without needing any kind of application deployment. Naturally there are rigorous controls in place to prevent unauthorized modifications.
- In step 1 you were granted permissions in the MDW environment to create and modify workflow assets.
- With Dynamic Java, as with all types of workflow assets, MDW provides facilities for versioning, rollback and import/export for migrating between environments.

- Update the generated Java source code to resemble the following:

```java  
package MyServices;
import com.centurylink.mdw.util.log.StandardLogger.LogLevel;
import com.centurylink.mdw.util.timer.Tracked;
import com.centurylink.mdw.java.JavaExecutionException;
import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.model.request.Request;
import com.centurylink.mdw.model.workflow.ActivityRuntimeContext;
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
- Your activity can be dragged like this and used in other processes designed by other users. Actually the proper term in MDW for this reusable element in the Toolbox is activity implementor. This conveys the idea that itâ€™s actually a template to be dragged and configured as an activity in the canvas, and it also conveys the fact that it always corresponds to a Java class. To take this reuse concept a step further, your activity implementor can be made discoverable so that it can easily be imported into other environments and reused across domains. If you click on the light bulb icon at the top of the Toolbox youâ€™ll get an idea how items in the palette can be imported from a file or discovered in the corporate repository.
- Double click the activity in the canvas, and in its Definition property tab change the label to something like Validate Order. When you click back on the canvas the activity reflects its new label.

   ![xml formatter](images/myOrderValidatorActivity2.png)
   
- Note: If you select the Design property tab for your activity youâ€™ll see that itâ€™s blank. A non-trivial activity would allow certain aspects (such as endpoint URLs) to be configurable, so that it could readily be reused. For example, take a look at the Design tab for the Start activity. You control what appears on the Design tab through the pagelet XML for the activity implementor. In the creation wizard we left the pagelet XML blank, so the Design tab for our activity is empty. But to continue with the example of the start activity, find the Process Start icon in the Toolbox and view its Design tab (for the implementor, not the activity on the canvas). This gives you an idea of how the pagelet XML relates to the fields on the Design tab for the activity user. Since weâ€™re on the subject you may be interested to know how you can customize the icon for your activity implementor. On the Definition tab you can choose one of the built-in shapes, or more flexibly choose any GIF, JPG or PNG asset that you can easily add to your workflow package.

##### Add Multiple Activity Outcomes:
- Drag a Process Finish activity from the Toolbox, and add another outbound transition from Validate Order. Assign Result Code values of true and false to the respective transitions as illustrated below. Save your process definition. The value passed in setReturnCode() in your activityâ€™s execute() method dictates which of these two paths will be.

   ![xml formatter](images/myOrderValidatorActivity3.png)

##### Get Your Server Running:
- Depending on which supported container you're using, you can follow one of the server setup exercises.  You'll need to follow the steps from one of these guides to the point where MDW is deployed and you're able to start and stop your server from the Eclipse Servers view. 
    - [Tomcat Server Setup](TomcatCookbook.md)
    - [Cloud Foundry Setup](CloudFoundryCookbook.md)
- You can confirm that MDW was successfully deployed by accessing MDWHub in your browser:
     - Tomcat:                                                                                  
       [http://localhost:8080/mdw](http://localhost:8080/mdw)
 
- Troubleshooting: if you encounter an exception like below, it means you need to download the JCE 8 Unlimited Strength Jurisdiction Policy Files for JRE 1.8.  Or JCE 8 Unlimited Strength Jurisdiction Policy Files  for JRE 1.8
 
	    java.security.InvalidKeyException: Illegal key size or default parameters
 
##### Open the Process Launch Dialog:
- Right-click on the MyOrderProcess process in Process Explorer view and Select Run.  Designer will present the launch dialog and open a connection to the server to confirm that it's running `(required for launching a process)`.
- On the Process tab in the launch dialog, select "Monitor Runtime Log" and "Process Instance Live View" to get a feel for how you can watch your process flow in real time.

  ![xml formatter](images/myOrderProcessRunConfig.png)
 
##### Populate the Input Variable:
- Select the Variables tab in the launch dialog, and populate the orderId input.

  ![xml formatter](images/populateOrderInputVariable.png)
 
##### Launch and View an Instance:
- Click Run on the launch dialog to run an instance of your process.  In the Live View you should see the new instance progress down the happy path with the Validate Order outcome equal to 'true'.  For processes not displayed in Live View, you can open an instance manually by right-clicking on your process in Process Explorer view and selecting View Instances.  The latest instance will appear at the top of the Process Instances list, and you can double-click to open its runtime view. 

- In Designer Perspective, a legend appears showing what the borders surrounding the activities mean.  To inspect the runtime variable values for the instance, click the Values property tab.

  ![xml formatter](images/viewOrderInstance.png)
 
#### 3. Expose a RESTFul Web Service using JAX-RS API
#### 1. Implement a JAX-RS Web Service

Besides implementing services by way of an MDW workflow process, you can easily expose your Dynamic Java class as a REST service using JAX-RS annotations.
 
##### Create a Java Asset to Implement a Resource Service:
- Right-click on your package in Process Explorer view and select new > Java Source.  By convention the Java class name will also be the name of your service resource.  Also by convention your workflow package name is the root of the REST endpoint URL path that consumers will use to access your service.  For this simple example we're using the MyServices.  In a real-world app you'll probably use a qualified package name like com.centurylink.my.services, and in that case you can specify a simplified URL path through the JAX-RS Path annotation, which will be illustrated later.
  
  ![xml formatter](images/createOrderProcessJavaAsset.png)

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
  
  @Path("/Orders")
  public class Orders extends JsonRestService {
	@Override
	public JSONObject post(String path, JSONObject content, Map<String, String> headers) throws ServiceException{
		Map<String,Object> stringParams = new HashMap<String,Object>();
		WorkflowServices workflowServices = ServiceLocator.getWorkflowServices();
		Object response = workflowServices.invokeServiceProcess("MyServices/MyOrderProcess", content, null, stringParams, headers);
		return (JSONObject) response;
	}
  }
```   
- Access your service using a POST request from your browser with a URL like the following:

    - [http://localhost:8080/mdw/Services/MyServices/Orders](http://localhost:8080/mdw/Services/MyServices/Orders)            

- Save your Dynamic Java asset, and use the MDWHub HTTP Poster tool to submit a POST request to add an order from your browser and you will see the response showing in the JSON format.
   ![xml formatter](images/restPostRequestAndResponse.png)
   
#### 2. Add Swagger API Annotations

With MDW REST services you can automatically generate Swagger documentation just by adding the appropriate annotations to your Dynamic Java.  This is not only a convenient way to maintain this documentation, but it also means that it's always up-to-date with the implementation.  Swagger documentation is a powerful way to communicate the specifics of your REST interface to potential consumers.  Swagger annotations represent a convenient mechanism for service developers to produce this documentation without having to hand-craft the JSON or YAML results.
 
##### Add the @Api Annotation to Your Service:
- The Swagger Api annotation goes on your class declaration along with the JAX-RS Path annotation.  The tag value in your annotation provides a high-level description of the its purpose:
```swagger
@Path("/Orders")	
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

@Path("/Orders")
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
```swager
@ApiModel(value="Order", description="Centurylink Order")
public class Orders extends JsonRestService {
// Add your logic.
```
#### 3. View Generated REST APIs in MDWHub
MDWHub comes with a UI for displaying your generated Swagger API documentation, along with the standard MDW REST APIs.
 
##### Access the MDWHub Service API Page for Your Service:
- Open MDW in your browser and click on the Services tab.  Notice that API path for your service (/MyServices/Orders) includes its package name to distinguish it from standard MDW services.
   ![xml formatter](images/restServiceAPIs.png)

- Click on the /MyServices/Orders link.  The JSON and YAML tabs include the Swagger Spec API definitions for the Orders endpoint.  Click on the YAML tab to view a human-readable representation of your Orders API.  Notice that much of the information is provided by annotations from the MDW base service class.
   ![xml formatter](images/yamlExample.png)

- Scroll down to the "definitions" section to see the Orders object definition as well as other referenced types.
- Now click on the Swagger subtab to explore the friendly swagger-editor UI for your service.
   ![xml formatter](images/swaggerExample.png)
 
##### Add a Sample Request and Response:
- Sample payloads in MDW are by convention kept in an asset package under the service package whose name ends with "api.samples".  Each sample should be named to indicate its path and purpose, with an underscore separating these two parts.  Create a new MDW package named "MyServices.api.samples" and add a JSON asset named Orders_Create.json with the following content:
```jason
// POST request to Services/MyServices/Orders
{
"orderId":"12345678"
}
```
##### View the Samples in MDWHub:
- Now that your sample requests have been created in accordance with the MDW naming convention, they'll automatically be associated with the corresponding service path.  And they'll also be displayed in the Samples tab for your service in MDWHub:
   ![xml formatter](images/jsonSamples.png)

#### 4. Consume a RESTFul Web Service
MDW comes with Adapter activities for consuming services over many protocols from within your workflow processes.  In this exercise we'll use the REST Service Adapter activity to invoke the MyOrderProcess service you just created.
 
##### Create a Process with a REST Service Activity:
- Open the same process definition you started building in the sections above.  
- Create a new process to consume your service.  From the Toolbox view drag a RESTful Service Adapter onto the canvas and insert it into your process flow. Label the web service activity "Check Orders", and give it two separate outcomes corresponding to true and false, just like the validation activity.
   ![xml formatter](images/consumeMyOrderProcess.png)
   
- On the Design tab for the web service activity, set the HTTP Method to POST and enter the same REST endpoint URL you used for testing your service in Section 3.  [http://localhost:8080/mdw/Services/MyServices/Orders](http://localhost:8080/mdw/Services/MyServices/Orders)

##### Implement MDW REST Activity API:
- With the REST activity in a real-world workflow, you might bind document variables to the service input and output through the Request Variable and Response Variable dropdowns pictured above.  To simplify this tutorial, we will implement a very simple java code to use the mdw built-in operations to return the request JSON posted to the service :

```java
package com.centurylink.mdw.workflow.order.activity;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;
import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.connector.adapter.AdapterException;
import com.centurylink.mdw.connector.adapter.ConnectionException;
import com.centurylink.mdw.util.StringHelper;
import com.centurylink.mdw.workflow.adapter.rest.RestServiceAdapter;
  
public class CheckOrdersRest extends RestServiceAdapter {
	@Override
	public String invoke(Object conn, String request, int timeout, Map<String, String> headers)
		throws ConnectionException, AdapterException {		
		if (conn != null) {
			return super.invoke(conn, request, timeout, headers);
		} else {
			logger.debug("Order service is disabled, continuing with flow");
			return "Ok";
		}
	}
	@Override
	public String getRequestData() throws ActivityException {
		if (StringHelper.isEmpty(varname)) {
			throw new ActivityException("Variable not found for Check Orders");
		}
		JSONObject orderRequest = new JSONObject();
		try {
			orderRequest.put("orderId", varname);
		} catch (JSONException e) {
			logger.severe("Unable to build response : message " + e.getMessage());
		}
		return orderRequest.toString();
	}
	@Override
	public Map<String, String> getRequestHeaders() {
		Map<String, String> requestHeaders = super.getRequestHeaders();
		if (requestHeaders == null)
			requestHeaders = new HashMap<String, String>();
			requestHeaders.put("Content-Type", "application/json");
			return requestHeaders;
		}
	}
}
```  
##### Save and Run Your Process:
- Launch your process, entering the orderId as you did in previous steps.  View the instance to confirm that the orderId was populated as expected.
- In the process instance view, double-click the Invoke MyOrderProcess activity instance.  Then on the Instance property tab, double-click on the activity instance row.  The Activity Instance dialog shows you the raw request and response values that were sent over the wire.  You can also view the same results like the following:

   ![xml formatter](images/orderProcessActivityInstance.png) 
   
   ![xml formatter](images/orderProcessActivityInstance2.png) 
 
##### Stub Mode and Response Simulation:
- At times when performing services orchestration using MDW you may be designing a flow before one or more of your consumed services is not yet available.  Or you may not be ready to make an actual call because you're still debugging your workflow.  For situations like this MDW provides Stub Mode and Response Simulation.  Stub Mode is for local development and Automated Testing.  Response Simulation is used to hardwire the responses for specific adapter activities within a given environment.  Both of these features are accessed via the Simulation property tab.  Click this tab for the Invoke Check Orders REST adapter in the process you just build.  To try out Stub Mode, depress the Stub Server button (no need to Configure since the defaults should be fine).

   ![xml formatter](images/orderProcessStubMode.png) 
   
- Note that this is a global setting; meaning once the stub server's running it intercepts all adapter activity requests.  Note also that it can be difficult to determine whether the button is depressed (i.e. stubbing is on).

- Once you've got stub mode turned on, run the process again and you'll be presented with a dialog prompting you for the desired response for this case.

   //TODO: Need to replace this screenshot with a new one.
   ![xml formatter](images/stubResponse.png) 
   
- Whatever is typed in the Response Message textbox will be returned to your process as the adapter response, and you should be able to confirm this by checking the runtime values of the process instance.
- To simulate a response, disable the stub server and instead set Simulation Mode to On.  Then provide a Return Code (not currently used), Chance (weighted probability when multiple responses), and Response value for each different hardwired response scenario.

   //TODO: Need to replace this screenshot with a new one.
   ![xml formatter](images/simulateResponse.png) 

- These simulated response settings are meant to be per-environment, so they don't get saved with the process definition but rather as so-called "override attributes".  For this reason there's a Save button directly on the Simulation property tab.
