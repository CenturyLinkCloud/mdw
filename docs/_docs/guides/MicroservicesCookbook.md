
#### 2. Add Swagger API Annotations

With MDW REST services you can automatically generate Swagger documentation just by adding the appropriate annotations to your Dynamic Java.  
This is not only a convenient way to maintain this documentation, but it also means that it's always up-to-date with the implementation.  
Swagger documentation is a powerful way to communicate the specifics of your REST interface to potential consumers.  
Swagger annotations represent a convenient mechanism for service developers to produce this documentation without having to hand-craft the JSON or YAML results.
 
##### Add the @Api Annotation to Your Service:
- The Swagger Api annotation goes on your class declaration along with the JAX-RS Path annotation.  
The tag value in your annotation provides a high-level description of the its purpose:

  ```swagger
@Path("/Order")	
@Api("CenturyLink orders service")
public class Orders extends JsonRestService {
```

##### Add @ApiOperation Annotations to Your Methods:
- The ApiOperation annotation documents the specifics of a service endpoint operation, including any input or output model types.  
The ApiImplicitParams annotation is useful for indicating the body content of a POST or PUT requests.  
After adding these annotations to Orders.java, the code will look something like this:   

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
- To enable consumers to easily create request content and interpret responses, you can annotate the related model objects so that they're
 discovered when documentation is generated.  In the Orders dynamic Java class, add the following class-level annotation:

  ```swagger
@ApiModel(value="Order", description="Centurylink Order")	
public class Orders extends JsonRestService {
// Add your logic.
```

#### 3. View Generated REST APIs in MDWHub
MDWHub comes with a UI for displaying your generated Swagger API documentation, along with the standard MDW REST APIs.
 
##### Access the MDWHub Service API Page for Your Service:
- Open MDW in your browser and click on the Services tab.  Notice that API path for your service (/MyServices/Order) includes its package 
name to distinguish it from standard MDW services.
   ![alt text](../images/restServiceAPIs.png "restServiceAPIs")

- Click on the /MyServices/Orders link.  The JSON and YAML tabs include the Swagger Spec API definitions for the Order endpoint.  
Click on the YAML tab to view a human-readable representation of your Order API.  Notice that much of the information is provided 
by annotations from the MDW base service class.
   ![yamlExample](../images/yamlExample.png "yamlExample")

- Scroll down to the "definitions" section to see the Order object definition as well as other referenced types.
- Now click on the Swagger subtab to explore the friendly swagger-editor UI for your service.
   ![alt text](../images/swaggerExample.png "swaggerExample")
 
##### Add a Sample Request and Response:
- Sample payloads in MDW are by convention kept in an asset package under the service package whose name ends with "api.samples".  
Each sample should be named to indicate its path and purpose, with an underscore separating these two parts.  Create a new MDW package
 named "MyServices.api.samples" and add a JSON asset named Order_Create.json with the following content:

  ```jason
 // POST request to Services/MyServices/Order
  {
   "orderId":"12345678"
  }
```

##### View the Samples in MDWHub:
- Now that your sample requests have been created in accordance with the MDW naming convention, they will automatically be 
associated with the corresponding service path.  And they'll also be displayed in the Samples tab for your service in MDWHub:
   ![alt text](../images/jsonSamples.png "jsonSamples")

### Consuming a RESTFul Web Service
MDW comes with Adapter activities for consuming services over many protocols from within your workflow processes.  
In this exercise we'll use the REST Service Adapter activity to invoke the OrderProcess service you just created.
 
##### Create a Process with a REST Service Activity:
- Open the same process definition you started building in the sections above.  
- Create a new process to consume a service.  From the Toolbox view drag a RESTful Service Adapter 
onto the canvas and insert it into your process flow. Label the web service activity "Submit Order" as shown on the image bellow.
   ![alt text](../images/consumeMyOrderProcess.png "consumeMyOrderProcess")
   
- On the Design tab for the web service activity, set the HTTP Method to POST and enter the same REST endpoint 
URL to consume a service from within your workflow. This example shows how to consume an existing service:  
http://lxdenvmtc143.dev.qintra.com:8515/mdw/services/Ping  
  ![alt text](../images/consumeMyOrderProcess2.png "consumeMyOrderProcess2")
  
##### Save and Run Your Process:
- Send a POST request from your browser with a URL like the following as you did in section 3:
http://localhost:8080/mdw/Services/MyServices/Order
- On the Designer, view the instance to confirm that the orderId was populated as expected.
- In the process instance view, double-click the OrderProcess activity instance.  Then on the Instance property tab, double-click 
on the activity instance row.  The Activity Instance dialog shows you the raw request and response values that were sent over the wire. 

   ![alt text](../images/orderProcessActivityInstance.png "orderProcessActivityInstance")   
   ![alt text](../images/orderProcessActivityInstance2.png "orderProcessActivityInstance2")    
 
