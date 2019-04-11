---
permalink: /docs/guides/mdw-cookbook/
title: MDW Cookbook
---

Let's walk through a few of the essential features of MDW.  All the source code referenced in this guide
is available to clone in its completed state from the [mdw-demo repository](https://github.com/CenturyLinkCloud/mdw-demo).  

## Sections in this Guide
  MDW is a [workflow framework](../../intro) specializing in [microservice orchestration](../../presentations/microservices).
  We'll focus on these topics, and dive into other key features like manual task handling and web UI development.
  Our use case is a bug management workflow.
  1. [Implement a REST Service](#1-implement-a-rest-service)
     - 1.1 [Setup](#11-setup) 
     - 1.2 [Create a service process](#12-create-a-service-process)
     - 1.3 [Run the process and view its instances](#13-run-the-process-and-view-its-instances)
     - 1.4 [Expose the process as a REST service](#14-expose-the-process-as-a-rest-service)
     - 1.5 [Document the service API using Swagger](#15-document-the-service-api-using-swagger)
     - 1.6 [Auto-validate requests against the API](#16-auto-validate-requests-against-the-api)
  2. [Build out the Bugs Workflow](#2-build-out-the-bugs-workflow)
     - 2.1 [Implement a custom activity](#21-implement-a-custom-activity)
     - 2.2 [Invoke a subprocess](#22-invoke-a-subprocess)
     - 2.3 [Add a manual task activity](#23-add-a-manual-task-activity)
     - 2.4 [Create a Spring asset](#24-create-a-spring-asset)
     - 2.5 [Consume a REST service](#25-consume-a-rest-service)
  3. [Design a Custom Web UI](#3-design-a-custom-web-ui)
     - 3.1 [Build a JSX task page](#31-build-a-jsx-task-page)
     - 3.2 [Create a process start page](#32-create-a-process-start-page)
     - 3.3 [Add a new tab to MDWHub](#33-add-a-new-tab-to-mdwhub)
     - 3.4 [Introduce collaboration through Slack](#34-introduce-collaboration-through-slack)
  4. [Explore other Features](#4-explore-other-features)
     - 4.1 [Add markdown workflow documentation](#41-add-markdown-workflow-documentation)
     - 4.2 [Unit test an activity using MockRuntimeContext](#42-unit-test-an-activity-using-mockruntimecontext)
     - 4.3 [Automate service tests with workflow verification](#43-automate-service-tests-with-workflow-verification)
     - 4.4 [Designate a package-level error handler](#44-designate-a-package-level-error-handler)
     - 4.5 [Create custom dashboard charts](#45-create-custom-dashboard-charts)

## 1. Implement a REST Service

### 1.1 Setup
  - **Java8 JDK**  
    Make sure you're running Java from a JDK installation and not just a JRE (in other words, the JDK bin directory
    must precede the JRE bin directory on your system PATH).  This is needed in order for MDW to compile dynamic Java assets.
  - **Install MDW Studio**  
    <http://centurylinkcloud.github.io/mdw/docs/guides/mdw-studio>

### 1.2 Create a service process
  - Create a new project as described in section 1.2 of the [MDW Studio Guide](http://127.0.0.1:4000/docs/guides/mdw-studio/#12-create-and-open-a-project).
    Name the project "my-mdw".
  - Expand the project tree in MDW Studio's Project tool window.  If the "assets" folder is empty, right-click on it and select Update MDW Assets.

#### Create a package  
  Everything in MDW is an [Asset](../../help/assets.html), and these live in packages, which are really just Java
  packages except they're tagged with MDW metadata and can contain a whole lot more besides Java source files.
  Let's create a package for our bug handling workflow:
  - Right-click on the assets folder and select New > Package.  Name the package "com.centurylink.mdw.demo.bugs".
  
#### Create a process  
  - Right-click on the bugs package you just created and select New > MDW Process.  Name the process "Create Bug", and for Kind select "Service Process".   
    ![Create Process](../images/create-proc.png)
  
    Click OK and we can start designing.
  
  - With the Create Bug process open, look at the Configurator tool window at the bottom of the canvas. 
    Configurator tabs are how workflow process elements are configured.
  - Select the Design tab and confirm that the Service Process checkbox is checked.  This tells MDW that this process is expected to run 
    synchronously and return a response in real time.
  
#### Add some process variables
  - Now click on the Variables property tab.  Because you created a service process, some conventional variables have already been added.
  - Besides these, add a new string variable named 'bugTitle' and make sure its Mode is Input.
  - Also change the type of 'request' to org.json.JSONObject.
    Here's what the Variables property tab should look like when you're finished:
    ![Add Variables](../images/add-vars.png)

#### Write a script activity 
  In MDW, an *activity* is an individual step in a workflow,and an *activity implementor* is a Java class that defines an activity's behavior.  
  MDW comes with a boatload of [prebuilt activities](../../workflow/built-in-activities) that are displayed in the Toolbox view.
  Quite possibly you'll want to extend the toolbox with your own custom activities.  We'll cover that in another section of the cookbook.
  - Your process already has an activity with Groovy script to generate a default response.  Click the Respond activity, and notice that the
    Configurator now displays tabs with attributes for the activity instead of the process.  On the Definition tab change the activity Name to 
    "Validate Request".  You can also drag a corner of the activity box to resize it and make the label fit better.
  - On the Design tab make sure the language is Groovy, and then click the Edit link.
    This opens an IntelliJ Groovy editor.  Modify the script to confirm that the bug title contains less than 512 characters:
    ```groovy
    import com.centurylink.mdw.model.Status
    import com.centurylink.mdw.model.StatusResponse
    
    if (bugTitle.length() <= 512) {
        response = new StatusResponse(Status.OK)
    }
    else {
        response = new StatusResponse(Status.BAD_REQUEST, "Bug title is too long")
    }
    ```
    Notice how this script has implicit access to the `bugTitle` and `response` variables, for both reading and assigning.
    

### 1.3. Run the process and view its instances

#### Build your Spring Boot jar
  - Open the Gradle (or Maven) tool window in IntellJ.  Run the "build" task in Gradle (or the "package" goal in Maven).
  - Check the console output for any build errors.  A successful build will produce a boot jar.

#### Create a Run Configuration and start the server
  - From the menu: Run > Edit Configurations...
  - Click the `+` icon and select "Jar Application" from the dropdown.
  - Name the configuration "my-mdw jar", and browse to the boot jar output file.
  - Enter these vm options: `-Dmdw.runtime.env=dev -Dmdw.config.location=config`
  - Make sure the Working directory is your project directory.
    ![Run configuration](../images/studio/run-configuration.png)
  - From the menu: Run > Debug... > select "my-mdw jar".  Server output appears in the console.
  
#### Run your process
  - Right-click on the Create Bug process in the project view and select Run Process (this menu item is disabled until the server is fully started up).
    Selecting Run Process should open you browser to MDWHub on the standard process start page.  Enter something for the bug title.
    
    ![Set Variables](../images/set-vars.png)
    
  - Click the Run button to execute your process.
  
#### View the process instance in MDWHub
  - A process instance is one particular execution of your process.  On clicking Run, you should have been navigated to the instance.  You can watch
    progress in real time, although our simple flow is likely to have been completed by the time you navigate to it in Hub.
    
  - Click the process title or a blank place in the MDWHub runtime view.  The Inspector pane appears.  Click the Values tab in Inspector to view
    variable values:
    ![Process Instance](../images/proc-inst-hub.png)
    
  - To see how a process definition looks in MDWHub, click on the Definition Nav link for the instance.  Click a blank spot in the canvas to view
    design-time attributes for the process.

#### Modify the process with conditional branching
  - Switch back to the "Create Bug" process editor again in MDW Studio.  Select the link (or *transition* in MDW-speak) connecting Validate Request 
    to the Stop activity.  On the Configurator Definition tab enter "valid" for the result.  You can reposition the result label if desired.
    Now change the name of the Stop activity to "Created".  
  - From the Toolbox, drag a second Process Stop activity onto the canvas and place it below the Validate Request activity.  Name this activity "Rejected".
  - Next comes the trickiest part of mastering MDW Studio.  We use the so-called "shift-click-drag" mechanism for linking activities on the canvas.
    Hold down the Shift key on your keyboard, click on the Validate Request activity, and continue holding down the mouse left click button while 
    dragging the cursor to the Rejected activity (shift-click-drag).  Set the result for this new transition to "invalid".  Your process should look like this:   
    ![Multiple Outcomes](../images/multiple-outcomes.png)
  
  - With these changes in place our script can drive the direction of flow by way of its return value.  On the Validate Request Design tab, click
    the Edit link and change the script content to this:
    ```groovy
    import com.centurylink.mdw.model.Status
    import com.centurylink.mdw.model.StatusResponse
    
    if (bugTitle.length() <= 512) {
        response = new StatusResponse(Status.CREATED)
        return "valid"
    }
    else {
        response = new StatusResponse(Status.BAD_REQUEST, "Bug title too long")
        return "invalid"
    }
    ```
    
  - In preparation for running the process, we need to apply our changes, letting the server know to refresh its asset cache.
    From the menu select Tools > MDW > Sync Server.  When sync is completed, a notification will pop up in MDW Studio and in IntelliJ's Event Log view.
    For convenience, a sync button <img src="../images/sync-btn.png" class="inline" style="position:relative;top:6px" alt="syncsync@2x.png button"> is available on 
    the IntelliJ toolbar to perform the same action.   

#### Find and run the process in MDWHub
  - Switch back to MDWHub in your browser.  On the Workflow tab, click the Definitions nav link.  Open Create Bug and click the Run button.
    Enter a bug title and execute the process.
    
  - Unless you entered a super long bug title, your flow should have proceeded down the "valid" path, and the response should be 201 Created.
    

### 1.4. Expose the process as a REST service
  Okay, things are about to get real.  An actual REST API for reporting a bug would take as input a lot more information than our simple 
  bugTitle string.  Now that you've had a taste of designing workflows, we'll switch whip up a model object to represent a bug and bind it to JSON.

#### Code a model object
  If you're an experienced Java developer, you're used to compiling your code and creating a deployable archive through a build script.
  In MDW things work differently.  You can still have statically compiled code in your src/main/java folder, but all your asset code is dynamic, 
  meaning it's compiled on-the-fly in real time by MDW as needed.  Check out the [Spring Boot Guide](../spring-boot) to learn more about best practices
  for project structure.  In this section we'll create our model class as a Java asset.
  
  - In MDW Studio, right-click on the com.centurylink.mdw.demo.bugs package and select New > Java Class.  Name the class Bug,
    and edit the code to look like this:
    ```java
    package com.centurylink.mdw.demo.bugs;
    
    import org.json.JSONObject;
    import com.centurylink.mdw.model.Jsonable;
    
    public class Bug implements Jsonable {
    
        public Bug(JSONObject json) {
            bind(json);
        }
    
        private Long id;
        public Long getId() { return id; }
    
        private String title;
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
    
        private String description;
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    
        private Integer severity;
        public Integer getSeverity() { return severity; }
        public void setSeverity(Integer severity) { this.severity = severity; }
        
        @Override
        public String toString() {
            return getJson().toString(2);
        }
    }
    ```
    This code illustrates a few concepts essential to understanding how MDW JSON binding works:
      - Our model class implements [Jsonable](../../javadoc/com/centurylink/mdw/model/Jsonable.html).
        The Jsonable interface provides an ultra-convenient mechanism for autobinding and serializing from/to JSON.
      - By convention every Jsonable implements a constructor that takes an org.json.JSONObject.  In our constructor we invoke the `bind()` method,
        which is where the autobinding magic happens.
      - Thanks to Java 8, unlike typical interfaces Jsonable comes with default behavior.  The two critical default methods are `bind()` and `getJson()`.
        These provide autobinding and serialization, respectively.  Yet, being an interface, Jsonable can be mixed in with any existing object model.
    
#### Change to Jsonable process variables    
  - After creating Bug.java, reopen the Create Bug process.  Delete variable "bugTitle" and change "request" to type com.centurylink.mdw.model.Jsonable.
    It's a convention in MDW that service processes declare a "request" input variable and a "response" output variable.  Since we're adopting this convention, 
    the content of incoming HTTP requests and outgoing responses are automatically bound to our variables.

  - Take another look at the Script property tab for Validate Request, under the Documents section notice that the "response" variable is Writable.
    The reason for this is that so-called [document variables](../../workflow/built-in-variable-types) in MDW are passed by reference
    (whereas strings and native types are passed by value).  Passing by reference is nice because we can update the same document from many different subflows.
    But a byproduct of this is that the engine needs to lock this variable for update when our activity is invoked.  Hence the need to mark "response" as writable.
    ![Writable Doc](../images/writable-doc.png) 
    
  - Edit the Validate Request script content to look something like this:
    ```groovy
    import com.centurylink.mdw.model.Status
    import com.centurylink.mdw.model.StatusResponse
    
    if (request.title.length() <= 512) {
        response = new StatusResponse(Status.CREATED)
        return "valid"
    }
    else {
        response = new StatusResponse(Status.BAD_REQUEST, "Bug title too long")
        return "invalid"
    }  
    ```   
    Here we changed `bugTitle` to `request.title`, counting on Jsonable autobinding so that we can acccess request title through its getter method, and we 
    expect it to contain the JSON "title" property value. We assign response to a StatusResponse, which is a built-in Jsonable representing 
    an [HTTP Status Code](https://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html).
      
#### Implement a JAX-RS REST endpoint

  - To facilitate path-based request handling, MDW supports [@Path](http://docs.oracle.com/javaee/6/api/javax/ws/rs/Path.html) annotations.
    Paths are qualified by the containing asset package name, so for this let's create a separate, friendly-named package to house our REST service.
    Right-click on "assets" folder in your project and select New > Package.  Name this new package simply "demo".
    
  - Right-click on the 'demo' package and select New > MDW REST > Java Service.  Name the service "Bugs" and overwrite its content with this:
    ```java
    package demo;
    
    import java.util.Map;
    import javax.ws.rs.Path;
    
    import org.json.JSONException;
    import org.json.JSONObject;
    
    import com.centurylink.mdw.common.service.ServiceException;
    import com.centurylink.mdw.demo.bugs.Bug;
    import com.centurylink.mdw.services.rest.JsonRestService;
    
    @Path("/bugs")
    public class Bugs extends JsonRestService {
    
        @Override
        public JSONObject post(String path, JSONObject content, Map<String,String> headers)
                throws ServiceException, JSONException {
            String requestId = Long.toHexString(System.nanoTime());
            return invokeServiceProcess("com.centurylink.mdw.demo.bugs/Create Bug",
                    new Bug(content), requestId, null, headers);
        }
    }
    ```
    The root path of our bugs service is governed by the @Path annotation of the class.  We override `post()`
    from [JsonRestService](../../javadoc/com/centurylink/mdw/services/rest/JsonRestService.html)
    to handle HTTP POST requests.  When one is received, we use the superclass helper method  
    [invokeServiceProcess()](../../javadoc/com/centurylink/mdw/services/rest/JsonRestService.html#invokeServiceProcess-java.lang.String-java.lang.Object-java.lang.String-java.util.Map-java.util.Map-).
    to programmatically launch our Create Bug process, and we return the result as JSON.
    
  - After syncing the server again, we're ready to POST a JSON request to http://localhost:8080/mdw/api/demo/bugs.
    For this you can use a tool like [Postman](https://www.getpostman.com/).
    Or if you don't feel like biting that off just now, MDWHub's System tab has a Messaging feature where you can submit simple HTTP requests.
    
  - Whatever submit mechanism you use, post a request with a body something like this:
    ```json
    {
      "title": "This is a good bug",
      "description": "Whatever that means"
    }
    ```
  - Here's what a request and response look like when submitted through MDWHub:
    ![Post Bug](../images/post-bug.png)  
  
    Something to point out about our development cycle: Notice that we didn't perform any compilation step on our java source assets.
    We simply preformed a sync, and MDW Studio told the server to refresh its asset cache to reflect our changes next time we ran.
  
  - After submitting a request, click the Workflow tab in MDWHub.  Click the Filter button, and select status = \[Any] so that completed
    processes are shown.  Your latest submittal should be at the top.  Confirm that the "request" and "response" values are populated as expected.    
    
### 1.5. Document the Service API using Swagger

  [Swagger](https://swagger.io/) documentation is a powerful way to communicate the specifics of your REST interface to potential consumers.
  MDW REST services will automatically generate Swagger documentation on the fly if a few key annotations are added to the Java source asset.
  This is not only a convenient way to maintain this documentation, but it also means that it's always up-to-date versus the implementation.  

#### Add the @Api Annotation to Your Service:
  The Swagger [@Api](https://github.com/swagger-api/swagger-core/wiki/Annotations-1.5.X#api) annotation goes on your class declaration along 
  with the JAX-RS Path annotation. The tag value in your annotation provides a high-level description of the its purpose:

  ```java
  @Path("/bugs")
  @Api("Bugs API")
  public class Bugs extends JsonRestService {
  ```
  - At this point, instead of copy/pasting code from this guide, why not download the completed Bugs.java artifact from the mdw-demo
    GitHub repository?  Here's the asset on GitHub:
    <a href="https://github.com/CenturyLinkCloud/mdw-demo/blob/master/assets/demo/Bugs.java" target="_blank">https://github.com/CenturyLinkCloud/mdw-demo/blob/master/assets/demo/Bugs.java</a>.   
    From the GitHub source view, right-click on Raw and select Save Link As, overwriting Bugs.java in the assets/demo folder in your project.
    Or just click the link to view the raw source in your browser, and copy/paste it into the asset. 

  - Open Bugs.java in MDW Studio.  Check the
    [@ApiOperation](https://github.com/swagger-api/swagger-core/wiki/Annotations-1.5.X#apioperation) and 
    [@ApiImplicitParams](https://github.com/swagger-api/swagger-core/wiki/Annotations-1.5.X#apiimplicitparam-apiimplicitparams)
    annotations to see how these document the POST operation and indicate its expected body content.
    
  - Perform the Sync Server action via the toolbar or Tools menu.
    
#### View generated API documentation in MDWHub 
  - Once the server cache has been refreshed, access MDWHub in your browser and click the Services tab.  Most of what you'll see listed here
    are paths for the built-in MDW services.  Scroll to the bottom to find your /demo/bugs resource path.  Click this link to investigate your service API docs.
    Swagger uses [JSON Schema](http://www.yaml.org/spec/1.2/spec.html#id2803231) syntax, so the definitive word on your API contract is depicted on the JSON subtab.
    A more readable [YAML](http://www.yaml.org/) representation can be viewed on the YAML subtab.  The raw content of both of these tabs is itself available via MDW's
    built-in service API (in fact, that's how MDWHub gets them).
    
  - MDW incorporates [Swagger UI](https://swagger.io/swagger-ui/) to make available a handy user interface for surveying REST APIs.
    Click on the Swagger subtab to check out the POST operation's docs.  Swagger-UI renders expandable elements for each model object.
    If you expand POST, you can even click the "Try it out" button to submit a request from here.
    ![Swagger UI](../images/swagger-ui.png)


### 1.6. Auto-validate requests against the API
  Swagger uses Java reflection to automatically determine the property types in our Bug.java model class.  In some situations the default interpretation 
  is not sufficient, or maybe you want to add clarifying text to certain fields.  This is where [API model annotations](https://github.com/swagger-api/swagger-core/wiki/Annotations-1.5.X#model-declaration)
  come in.
  
#### Add API model annotations
  - Grab the completed version of mdw-demo's Bug.java asset, and let's take a look at how to incorporate API model annotations.  Here's
    [Bug.java on GitHub](https://github.com/CenturyLinkCloud/mdw-demo/blob/master/assets/com/centurylink/mdw/demo/bugs/Bug.java).  Overwrite your
    current workspace version with this latest code.
    
  - In MDW Studio open the Bug.java asset you just downloaded.  Note this annotation on the `id` field:
    ```java
    @ApiModelProperty(readOnly=true)
    private Long id;  
    ```
    Since id is a generated value, we disallow its inclusion in submitted requests by setting it to *readOnly*.
    
  - Now check out the `title` property:
    ```java
    @ApiModelProperty(value="Headline bug information", required=true)
    @Size(max=512)    
    private String title;
    ```
    To include descriptive text in the generated Swagger docs, we've set a value on @ApiModelProperty.  We've also flagged it as a required field.
    And we've introduced a new annotation *@Size* which is not from Swagger, but actually from the Java [JSR-303 Validations](http://beanvalidation.org/1.0/spec/)
    standard.  This tells consumers of our API about our maximum length constraint.
    
  - Sync the server and revisit the Services tab on MDWHub.  On the Swagger subtab, check the Bug item under Models to confirm that these changes are reflected in the docs.
  
#### Incorporate auto-validation
  To validate the incoming request of Bugs workflow for conformance with Swagger schema, we can replace our validation script with the
  [Swagger Validator](http://centurylinkcloud.github.io/mdw/docs/help/SwaggerValidator.html) activity. 
  - Edit the Create Bug process to insert a Swagger Validator step from the Toolbox into your workflow.  Name the activity "Valid Request?".
  - On the Validator's Design configurator tab, select Body and Path to be validated, and check the Strict checkbox.
  - Set the Result values of the outbound transitions to "true" and "false". 
    ![Swagger Validator](../images/swaggerValidator.png)
 
  - Post a request with this body to test failed validation:
    ```json
    {
      "title": "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas varius vitae ante sit amet aliquam. Proin sodales orci sapien, ut consectetur turpis porttitor vel. Nullam arcu ex, bibendum ac condimentum eu, interdum at odio. Morbi a maximus odio. Aenean faucibus lacus libero, eget bibendum lacus imperdiet non. Phasellus quam elit, finibus eu orci nec, facilisis fringilla dolor. Duis porttitor, nisi id congue iaculis, orci lectus eleifend tellus, nec interdum augue eros id lacus. Donec sollicitudin cursus imperdiet.",
      "description": "This title is too long"
    }    
    ```
    You'll get an HTTP 400 response, and you can view the process instance to confirm that it took the 'invalid' path.
    
    
## 2. Build Out the Bugs Workflow

### 2.1 Implement a custom activity
  A bug probably requires someone to take action.  In MDW, steps requiring manual intervention are represented by the 
  [TaskInstance](../../javadoc/com/centurylink/mdw/model/task/TaskInstance.html) model object.
  To integrate our Bug object into the Create Bug workflow, we'll use the TaskInstance model, and map the data to our Bug object.
  
#### Create an activity implementor asset
  - Fire up MDW Studio, right-click the com.centurylink.mdw.demo.bugs package, and select New > MDW Activity > Java Activity.
    ![Create Activity](../images/create-activity.png)
  - Replace the generated Java code with
    [PersistBugActivity.java on GitHub](https://github.com/CenturyLinkCloud/mdw-demo/blob/master/assets/com/centurylink/mdw/demo/bugs/PersistBugActivity.java).
  - The appearance of our new activity in the Toolbox is governed by the `@Activity` annotation:
    ```java
    @Activity(value="Persist Bug", icon="com.centurylink.mdw.demo.bugs/bug.png")
    public class PersistBugActivity extends DefaultActivityImpl {    
    ```   
    Here we've labeled the activity Persist Bug, and specified an icon according to MDW's asset path notation: \<package_name>/\<asset_file>.
    (Unlike a qualified Java class name, an asset path contains a slash separating the package from the asset).

  - Download [bug.png from GitHub](https://github.com/CenturyLinkCloud/mdw-demo/blob/master/assets/com/centurylink/mdw/demo/bugs/bug.png)
    into assets/com/centurylink/mdw/demo/bugs in your project.  Then click the Reload toolbar button on the Toolbox view to reflect this new addition. 
  
  - In PersistBugActivity we've implemented `execute()` to tell MDW's workflow engine what to do.  These lines from `execute()` take care of creating the task:
    ```java
    public Object execute(ActivityRuntimeContext runtimeContext) throws ActivityException {
      Bug requestBug = (Bug)getValue("request");
      String taskTemplate = getTaskTemplate();
      try {
        AssetVersionSpec templateAsset = new AssetVersionSpec(taskTemplate, "0");
          TaskTemplate template = TaskTemplateCache.getTaskTemplate(templateAsset);
          if (template == null)
              throw new DataAccessException("Task template not found: " + template);
  
          TaskServices taskServices = ServiceLocator.getTaskServices();
          TaskInstance instance = taskServices.createTask(template.getTaskId(), getMasterRequestId(), 
              getProcessInstanceId(), null, null, requestBug.getTitle(), requestBug.getDescription());
  
          loginfo("Created task instance " + instance.getId() + " (" + template.getTaskName() + ")");
      ....
    ```
    Here we're calling [TaskServices.createTask()](../../javadoc/com/centurylink/mdw/services/TaskServices.html#createTask-java.lang.Long-java.lang.String-java.lang.Long-java.lang.String-java.lang.Long-java.lang.String-java.lang.String-), 
    which requires a [Task Template](../../help/taskTemplates.html) asset to specify a pattern.  We'll create the task template asset in the next step.
    
  - Drag the Persist Bug activity from the Toolbox onto the Create Bug process design.  Name it "Save Bug", and link it 
    into your process like so:   
    ![Create Bug 2](../images/create-bug-2.png)<br>

#### Create a task template asset
  - Right-click on the com.centurylink.mdw.demo.bugs package and select New > MDW Task.  Name the task ResolveBugAutoform, and 
    make sure its type is AutoForm:<br>
    ![Create Task Template](../images/create-task-template.png)<br>
    
  - In the MDW Studio task editor, find the tabs at the bottom of the pane.
    On the General tab, for Task Name enter the [Java Expression](http://docs.oracle.com/javaee/6/tutorial/doc/gjddd.html) `${bug.title}`, 
    which tells MDW to use the *bug* process variable's *title* value.<br>
    ![Task Template Editor](../images/task-template-editor.png)<br>
    
  - Then on the Workgroups tab, add Developers to the Selected Groups.  That's all we need to do for the moment.  Later on we'll
    learn how to customize the web UI and other aspects of our task.  
     
#### Run the revised process and view the task
  - We want to submit a (valid) bug request via HTTP POST as before.  But you may have noticed that getTaskTemplate() in the GitHub version
    of PersistBugActivity returns ResolveBugAutoform only conditionally.  To meet that condition, you'll want to use your posting tool to add a 
    header `autoform` with a value of `true`.  If that's not convenient, then simply change getTaskTemplate() to return a hardcoded response:
    ```java
    private String getTaskTemplate() throws ActivityException {
        return "com.centurylink.mdw.demo.bugs/ResolveBugAutoform.task";
    }
    ```
    After submitting, the process flow should run to completion.  Find the process in MDWHub, and on the Inspector Values tab, click the 
    responseHeaders variable 'Document:' link.  
    When submitting you may have noticed that the response headers now include *Location*.  Review the code in PersistBugActivity.execute() to see
    how responseHeaders is populated.  It's another example of a convention in MDW whereby you can readily control the behavior of 
    your REST service.

  - Still in MDWHub, and click on the Tasks tab.  You should see the top task named whatever you submitted for the bug's title.
    Click to drill into the task.  What the...?  You got a blank page!  Task pages are implemented as React JSX assets (more about that later).
    When running in development mode, `-Dmdw.runtime.env=dev`, JSX assets are compiled just in time, so the first time you click on one you'll get a blank
    page.  Just refresh your browser to display the task.
    ![Task Instance](../images/task-instance.png)<br>
  
### 2.2 Invoke a subprocess
  What to do when a bug gets created?  With human action required, it'll be a long-running workflow, which obviously cannot respond in real time to an HTTP request.
  Whereas Create Bug is a service process and should respond within a few hundred milliseconds, once the bug is created we want to spawn a separate flow
  that can proceed at length.  The pattern to accomplish this in MDW is to invoke a subprocess.
  
#### Use the Invoke Subprocess activity
  - First create a new process under package com.centurylink.mdw.demo.bugs.  Call this process "A Bug's Life", and this time leave Kind as Workflow Process.
    Add an Input variable named 'bug' with type com.centurylink.mdw.model.Jsonable.
    
  - Return to the Create Bug process, and from the Toolbox drag an Invoke Subprocess activity.  Name it "Invoke Bug Subflow", and Link it 
    after Save Bug and before Created.  On the Configurator Design tab for Invoke Bug Subflow select "A Bug's Life" for it's Subprocess. 
    Also uncheck Synchronous since we want to fire and forget this subflow so we can respond right away.  The Bindings table 
    displays all the input variables from our selected subprocess.  Enter ${response} as the Binding Expression for the 'bug' input variable:
    ![Create Bug 3](../images/create-bug-3.png)<br>
    
    The [Java expression](http://docs.oracle.com/javaee/6/tutorial/doc/gjddd.html) ${response} in this context means that the 'response' 
    variable from Create Bug will be bound to input 'bug' in the subflow.
    
  - Sync the server and invoke the service as before.  View the latest "Create Bug" process instance in MDWHub, and select the
    Invoke Bug Workflow activity.  On the Subprocesses Inspector tab, click the "A Bug's Life" instance ID.
    View the subflow Values to confirm that 'bug' is populated correctly.

### 2.3 Add a manual task activity

#### Insert a manual task 
  - Returning to "A Bug's Life" in MDW Studio, drag in an Autoform Manual Task activity.  You'll be prompted as to whether you'd like to create
    and associate a task template.  Since we've already created our template, answer No to this.  Name the activity "Await Resolution", and link
    it between Start and Stop.  On the Design tab, select the ResolveBugAutoform task template we create.  For Instance ID Variable instead of selecting
    from the dropdown, type the expression `${bug.id}`.  This tells MDW that we've already created the task (in Save Bug), and we just need 
    our activity to await task completion.<br>
    ![A Bug's Life](../images/a-bugs-life.png)<br>
  - Sync the server and submit a request.  Find the process instance in MDWHub and confirm that the status of Await Resolution in "A Bug's Life" 
    is *Waiting*.  This indicates that flow will resume once the associated manual task is completed.

#### Perform actions on the task
  - In MDWHub click the Tasks tab, find the just-created bug task, and drill into it.  Click the Action button and select Claim.
    You'll observe the status change to *Assigned*, with yourself as the assignee.  By default you must be assigned a task to complete it 
    (although this behavior [can be overridden](../../help/taskAction.html)).  Click the Action button again and select Complete.
    
  - Return to the Bug's Life instance in MDWHub, and confirm that now the subflow has run to completion.
    Manual tasks provide a great mechanism for humans to interact with workflow.  Aside from triggering actions, another key aspect
    of manual tasks in MDWHub is enabling data entry.  In [section 2.5](#25-consume-a-rest-service) we'll explore how, with 
    [AutoForm tasks](../../javadoc/com/centurylink/mdw/workflow/activity/task/AutoFormManualTaskActivity.html), 
    data entry can be tied to variables through configuration.  Later, in [section 3](#3-integrate-a-custom-web-ui), we'll create an entirely custom 
    web UI based around a [Custom task](../../javadoc/com/centurylink/mdw/workflow/activity/task/CustomManualTaskActivity.html).
    
  - Before we move on, make a couple more changes to "A Bug's Life".  First, change the Stop activity's name to "Close",
    signifying that the end of it's lifecycle.
  - Also, on the Definition property tab of the outbound link from the manual task activity, change the Result from blank to "Resolve" like so:<br> 
    ![Custom Action](../images/custom-action.png)<br>
    Setting the outbound link's result is a convenient way to customize the actions available for a given task.  Now if you resubmit
    and claim the corresponding task, you'll see that "Resolve" replaces "Complete" in MDWHub's task action popover.  By branching into
    two or more outgoing links from a manual task, you can give users the power to drive workflow outcomes through their task actions. 
    
### 2.4 Create a Spring asset
  MDW uses [Spring](https://spring.io/) internally, and one of the key extensibility mechanisms MDW provides is the ability to create
  your own custom Spring contexts.  Unlike regular Spring app contexts, your Spring assets are dynamic in that changes can be applied without
  a redeployment.
  
#### Customize baseline data  
  - In MDW Studio, open ResolveBugAutoform.task.  On the General tab check out the options available in the Category dropdown.  None of these
    seem like a good fit for a bug.  It would be nice to add our own values as options.  This is the problem we'll solve by creating a
    Spring asset.
    
  - Right-click on the com.centurylink.mdw.demo.bugs package and select New > File.  Name the asset bugsContext.spring, and click Finish.
    Paste this into bugsContext.spring:
    ```xml
    <?xml version="1.0" encoding="UTF-8"?>
    <beans
      xmlns="http://www.springframework.org/schema/beans"
      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
      xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd">
    
      <!-- custom baseline data -->
      <bean id="bugCategories" class="com.centurylink.mdw.demo.bugs.Categories" />
    </beans>
    ``` 
    Aside from namespace boilerplate, this context contains a single Spring bean declaration with an *id* of bugCategories.
    The *id* is not significant, but the *class* attribute is.  This is where we plug in our custom behavior.
    
  - Create a new Java class in the same package named Categories.  Paste its content from
    [Categories.java on GitHub](https://github.com/CenturyLinkCloud/mdw-demo/blob/master/assets/com/centurylink/mdw/demo/bugs/Categories.java).
    The important thing to note about this code is this:
    ```java
    public class Categories extends MdwBaselineData {
        private List<TaskCategory> bugCategories;
  
        public Categories() {
            bugCategories = new ArrayList<>();
            bugCategories.add(new TaskCategory(201L, "ISSUE", "Bug"));
        }
    ....    
    ```     
    We extend [MdwBaselineData](../../javadoc/com/centurylink/mdw/dataaccess/file/MdwBaselineData.html), which provides the entry-point 
    for us to designate our own values for certain reference data in MDW.  In the Categories constructor, we're building our own list
    of TaskCategories to include along with the MDW built-in ones.

  - This configures server-side baseline data.  MDW Studio uses a different mechanism involving project.yaml.  We'll explore that later, but in the meantime
    we'll set the category using MDWHub.  Sync your server and then click the Admin tab in MDWHub.  There click on the Assets nav link, drill into the
    com.centurylink.mdw.demo.bugs package and open ResolveBugAutoform.task.
  - Click on Edit button and in the Category dropdown select Bug.  Click Save and choose to overwrite version 0.1 and not to commit and push changes:
    ![Hub Task](../images/hub-task.png)<br>
    Click 'Save and Close'.
  - This illustrates how any type of asset, including processes, can be edited in MDWHub.
    
#### Configure the ResolveBugAutoform task template    
  - Returning to MDW Studio let's explore some other aspects of manual tasks.  We could specify a [due interval](../../help/taskSLA.html)
    in minutes or hours.  But a hard-wired interval turns out to be not that useful.  Instead let's implement a
    [PrioritizationStrategy](../../javadoc/com/centurylink/mdw/observer/task/PrioritizationStrategy.html)
    to dynamically determine a task's priority level as well as when it's due.  In the com.centurylink.mdw.demo.bugs package
    create a new Java class named Prioritization with 
    [code from GitHub](https://github.com/CenturyLinkCloud/mdw-demo/blob/master/assets/com/centurylink/mdw/demo/bugs/Prioritization.java).
    The logic here is very straightforward, but it's worth mentioning this annotation we haven't seen before:
    ```java
    @RegisteredService(PrioritizationStrategy.class)
    public class Prioritization ...
    ```
    The @RegisteredService annotation gives your dynamic Java code an extensibility hook into MDW.  To see the available implementations
    you can plug in, have a look at the [All Known Subinterfaces](../../javadoc/com/centurylink/mdw/common/service/RegisteredService.html) 
    section of the javadoc.
    
  - Now return to ResolveBugAutoform.task and on the General tab paste the fully qualified name of the class we just created into
    Prioritization Strategy: `com.centurylink.mdw.demo.bugs.Prioritization`.  This strategy pattern is common to many attributes of
    a task template:
      - [PrioritizationStrategy](../../javadoc/com/centurylink/mdw/observer/task/PrioritizationStrategy.html) - priority and due interval
      - [SubtaskStrategy](../../javadoc/com/centurylink/mdw/observer/task/SubTaskStrategy.html) - rules for creating subtasks
      - [RoutingStrategy](../../javadoc/com/centurylink/mdw/observer/task/RoutingStrategy.html) - autoroute to designated workgroups
      - [AutoAssignStrategy](../../javadoc/com/centurylink/mdw/observer/task/AutoAssignStrategy.html) - assign to a specific person

    All these strategies have access to the instance runtime state if they extend ParameterizedStrategy like we've done.  They all also have various built-in
    strategy options available from their dropdowns.  All the dropdowns include the **Rules-Based** option, which enables you to
    maintain the rules in an Excel spreadsheet asset which acts as a [Drools decision table](https://docs.jboss.org/drools/release/6.5.0.Final/drools-docs/html/ch06.html#d0e5713).
    (Rules-based strategies depend on the optional com.centurylink.mdw.drools asset package). 
      
  - You can poke around the task template design tabs and click on the help links to explore additional template attributes,
    such as [Email and Slack notifications](../../help/taskNotices.html) based on HTML template assets.
  
  - If you're done for now, save ResolveBugAutoform.task and resubmit with various values for 'severity' to confirm its impact on the due interval
    by checking the Tasks tab in MDWHub.
    
### 2.5 Consume a REST service
  Back in [section 1](#1-implement-a-rest-service) we covered how to produce a REST service.  Now we want to consume a REST API from within our workflow.  
  Let's say during the bug lifecycle we want to retrieve any associated commit on GitHub.  For this we can use the [GitHub REST API](https://developer.github.com/v3/).
  The MDW term for an activity that invokes an external service (whatever the protocol), is *adapter activity* (or simply *adapter*).
  In this section we'll use MDW's RESTful Service Adapter activity to access the GitHub API.

#### Add an adapter activity  
  - In MDW Studio open "A Bug's Life" and drag from the toolbox a RESTful Service Adapter.  Link it between "Await Resolution" and "Close", and 
    label it "Retrieve Commit".  We need a variable to hold the adapter response, so on the process Variables tab add a local variable named 'commit'.
    We're not going to build a Jsonable model for this object, so just make the 'commit' variable type org.json.JSONObject.
    
  - On the REST adapter Design tab, make sure the GET method is selected, and paste this for Resource URL:<br>
    `https://api.github.com/repos/CenturyLinkCloud/mdw-demo/commits/${bug.commitId}`  
        
  - Also on the activity Design tab, in the Response Variable dropdown select 'response'.  We don't need a request
    variable for an HTTP GET.  Here's what the process should look like:
    ![a bugs life 2](../images/a-bugs-life-2.png)<br> 
    
#### Capture commitId via Autoform    
  - Now we need a way for users to enter the commit ID when they resolve a bug.  Open ResolveBugAutoform.task again.  On the Variables design tab
    enter expressions to show and capture data from the *bug* runtime value:<br>
    ![autoform variables](../images/autoform-variables.png)<br>
  
  - Sync the server.  Then post a new request with something like the following, (remembering to include the
    `autoform` header):
    ```json
    {
      "title": "Missing documentation on latest whiz-bang feature",
      "description": "Someone needs to step up.",
      "severity": 3
    }
    ```

  - Now check the Tasks tab in MDWHub and drill into the task for the bug you just submitted.  Claim the task so that you can edit its data.
    Now click on the Values nav link and you'll see the results of our autoform variables configuration.  Enter `9758b56` for Commit:<br>
    ![AutoForm](../images/autoform.png)<br>
    The commitId above is the short form of a valid commit hash in mdw-demo on GitHub.  Click Save, and then perform the Resolve action.
    
  - Click over to the Workflow tab in MDWHub.  Select "All Processes" in the dropdown, and set the Filter to \[Any] to see your the latest 
    "A Bug's Life" at the top.  In tht process the value of *bug* to confirm that the commit id we entered is reflected back into our workflow.
    Check the value of *commit* to see what came back from GitHub.  Another item of note: if you click on the Retrieve Commit adapter you'll
    see that it has Requests and Responses property tabs where you can drill in to see other information like the URL and HTTP headers.

#### Access task values through REST
  - One of the many built-in REST APIs in MDW is for viewing and updating task values.  To illustrate, paste this URL into your browser:
    `http://localhost:8080/mdw/api/Tasks/{id}/values` (where *{id}* is the task id for an instance of the bug task).
    Notice that the Autoform UI descriptor elements such as *label* and *display* are returned along with the data.  This exposes a nice clean
    interface for external systems to interact with MDW tasks via REST.  Even if the tasks UI is a completely separate system, it can use REST
    to perform the same actions as MDWHub, while reserving control about what can be updated in the hands of the workflow developer.    
    
## 3. Design a Custom Web UI

### 3.1 Build a JSX task page
  The autoform task layout we built in [section 2](#2-build-out-the-bugs-workflow) is great for quick and simple data entry.
  But its limitations are obvious.  For complex data entry with client-side and cross-field validations, you'll want to develop a
  custom page.  In MDW, the built-in tasks UI is implemented using [React](https://facebook.github.io/react/), and that's exactly what we'll
  use in this section as we create a custom task JSX asset.
  
#### Switch to custom task
  - Create a new task under com.centurylink.mdw.demo.bugs named ResolveBugCustom.task, and this time make it a Custom Task:
    ![Create Task Custom](../images/create-task-custom.png)<br>
  - We'll want to set this task's category to the 'Bug' category we added in bugsContext.spring in [Section 2.4](#24-create-a-spring-asset).
    MDW Studio must not rely on a running server to find its baseline data (otherwise you'd not be able to edit a task reliably unless your
    server was running).  So, to make our new category available in MDW Studio, we'll edit the file project.yaml in the root of our project directory
    to add the task.categories section under data:
    ```yaml
    data:
      workgroups:
        - Developers
        - MDW Support
      task:
        categories:
          Bug: ISSUE
          Documentation: DOCS  
    ```
  - Now if you close and reopen ResolveBugCustom.task, you can select Bug for its Category.  Also set the Name and Prioritization Strategy
    to have the same values as our old autoform task:<br>
    ![custom task template](../images/custom-task-template.png)<br>
    
  - Also just like autoform, select Developers on the Workgroups tab (notice that there's no Variables tab this time).
    
  - Open "A Bug's Life" and click the "Await Resolution" activity.  On Configurator's Definition tab, click the Set button next to 
    the Implementor input.  Set the implementor to `com.centurylink.mdw.workflow.activity.task.CustomManualTaskActivity` instead of 
    AutoFormManualTaskActivity. (This is a shortcut that saves us having to delete and re-drag a different activity from the Toolbox.)
    
  - On the Design tab select the ResolveBugCustom.task asset we just created.
    Also, if you hard-coded the the return value from PersistBugActivity.getTaskTemplate() in [Section 2.1](#21-implement-a-custom-activity), 
    undo that so it returns our new custom task template.
    
  - Sync your server and submit a new request (with the `autoform` header removed or set to false).
    
  - Drill in to the just-created task in MDWHub and click on the Values nav link.  It's empty!  In fact MDW does not automatically populate
    any values for custom tasks (except for [Task Indexes](../../help/taskIndexes.html), which is a subject we'll return to in 
    swaggerValidator.png.  We wouldn't have switched to custom task if we wanted MDW to populate Values.
    In a moment we'll build a complete replacement task view anyway. 
    
#### Create a JSX page asset
  - In MDW Studio, right-click on the 'demo' package (**not** 'com.centurylink.mdw.demo') and select New > File.
    Name the file "Bug.jsx".
    
  - To start with, paste the following into Bug.jsx:
    ```typescript
    import React, {Component} from '../com/centurylink/mdw/node/node_modules/react';
    
    class Bug extends Component {
        
      constructor(...args) {
        super(...args);
        this.state = {bug: {}};
      }
    
      componentDidMount() {
        var bugId = location.hash.substring(9);
        fetch(new Request('/mdw/api/demo/bugs/' + bugId, {
          method: 'GET',
          headers: {Accept: 'application/json'}
        }))
        .then(response => {
          return response.json();
        })
        .then(bug => {
          this.setState({bug: bug}); 
        });
      }
      
      render() {
        return (
          <div>
            <h2>Bug:</h2>
            <pre>{JSON.stringify(this.state.bug, null, 2)}</pre>
          </div>
        );
      }
    }
    
    export default Bug;
    ```
    Here using [React JSX syntax](https://facebook.github.io/react/docs/jsx-in-depth.html) we're fetching bug details
    in the [componentDidMount](https://facebook.github.io/react/docs/react-component.html#componentdidmount) React
    component lifecycle method.  This submits an HTTP GET request, which is handled by the get() method in our
    [Bugs.java](https://github.com/CenturyLinkCloud/mdw-demo/blob/master/assets/demo/Bugs.java) REST service asset.  Then, 
    in the render() method of our component, we're simply stringifying the bug to JSON (and calling that a UI
    <img src="../images/shucks.png" style="margin-top:-20px;margin-bottom:0;position:relative;top:5px;" alt="embarrassed">).
    
  - Returning to ResolveBugCustom.task, on the General Design tab, select Bug.jsx for Custom Page.
    
#### Set up a navigation route
  - This provides necessary linkage between MDWHub and our JSX asset.  Create a new package named "mdw-hub.js".
    Ignore IntelliJ's warnings that this is an invalid Java package.  We won't keep any Java assets here. 
    This package name must be exactly "mdw-hub.js", because we're taking advantage of an MDWHub extensibility hook 
    which enables you to override any of Hub's built-in web artifacts (we'll return to this topic in the next subsection).
    Right-click on mdw-hub.js and select New > File to create an asset named routes.json (again, exact naming).
    Paste this into routes.json:
    ```json
    [
      {
        "path": "/issues/:id",
        "asset": "demo/Bug.jsx"
      }
    ]
    ```
  
  - Because it's our first-ever mdw-hub package override, this last change requires a server restart.  
    So do that and then submit a new request (always without the `autoform` header from this point forward).  
    In MDWHub drill in to the latest bug task.  It should look something like this:<br>
    ![raw bug](../images/raw-bug.png)<br>
    Not very impressive.  Let's get busy making this into a real working page.
    
#### Build out the Bug.jsx asset
  - For the next step in the evolution of Bug.jsx, grab [this version from GitHub](https://github.com/CenturyLinkCloud/mdw-demo/blob/a1db92eee7b5da3586a5747b97da9dc3a7e4c950/assets/demo/Bug.jsx).
    This implements a simple custom form for updating and resolving a bug.  Since this guide is not a [React tutorial](https://facebook.github.io/react/tutorial/tutorial.html),
    we won't go into all the specifics of Bug.jsx.  But let's touch on a few points.  First, take a look at the imports:
    ```typescript
    import React, {Component} from '../com/centurylink/mdw/node/node_modules/react';
    import {Button, Glyphicon} from '../com/centurylink/mdw/node/node_modules/react-bootstrap';
    ``` 
    Notice the relative path to node_modules.  If you've done [Node.js](https://nodejs.org/en/) development before, you're probably used to imports that look
    more like:
    ```typescript
    import React, {Component} from 'react';
    import {Button, Glyphicon} from 'react-bootstrap';
    ```
    The background on this is that MDW's base package *com.centurylink.mdw.node* already comes with a node_modules folder that includes all the React-related
    packages you're ever likely to need (such as react-bootstrap in the example above).  If you want to bring your own node_modules, you'll need to include it 
    in one of your asset packages, configure it [as described here](https://github.com/CenturyLinkCloud/mdw/blob/master/mdw-workflow/assets/com/centurylink/mdw/react/readme.md),
    and point your imports to that location.
    
  - Sync your server, and then submit a request and visit the newly-created bug task in MDWHub.  It should look like this:<br>
    ![custom bug](../images/custom-bug.png)<br>
    
  - When a user clicks on the Save button, the following code from handleClick() is executed: 
    ```typescript
    if (event.currentTarget.name === 'save') {
      fetch(new Request('/mdw/api/demo/bugs/' + this.state.bug.id, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json'},
        body: JSON.stringify(this.state.bug)
      }))
      ...
    ```
    This performs an HTTP PUT request that's handled by the put() method in Bugs.java.
    
    Here's what happens when the Resolve button is clicked:
    ```typescript
    else if (event.currentTarget.name === 'resolve') {
      fetch(new Request('/mdw/api/Tasks/' +  this.state.bug.id + '/Resolve', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json'},
        body: JSON.stringify({
          action: 'Resolve',
          user: $mdwUi.authUser.cuid, 
          taskInstanceId: this.state.bug.id})
      }))
      ...
    ```    
    This performs a POST to "http://localhost:8080/mdw/api/Tasks/{id}/Resolve" to perform the Resolve action.  The distinction here is that
    this uses the MDW built-in Tasks API instead of our demo API.  As you build out your UI, you'll want to lean heavily on the MDW APIs, and the easy
    way to get familiar with them is by perusing their Swagger docs.  Another trick that's very useful is to use your browser's developer tools to
    watch the REST network traffic between MDWHub and the server to see how MDW itself leverages these APIs.
    
### 3.2 Create a process start page
  To this point the only way of triggering our bugs workflow is by using Postman or MDWHub to submit a JSON payload.  That's fine if your
  client is an upstream system.  For our bug reporting use case, however, we expect end users to be able to submit bugs themselves from their browser.
  To facilitate this, we'll create a custom start page that's similar in concept to the custom task page we just built.
    
#### Compare MDWHub's default process run page
  - On the Workflow tab in MDWHub, click the Definitions nav link.  Drill into the "Create Bug" process and click the Run button.  This is what you'll see:<br>
    ![Default Start Page](../images/default-start-page.png)<br>
    Not very user-friendly.  This default process start page is meant for testing.

#### Set Bug.jsx as our process start page
  - Open the "Create Bug" process in MDW Studio.  Check out the Variables property tab.  You may have already noticed the UI Label and Sequence columns, which
    can autogenerate a layout similar to the task autoform feature we explored in [Section 2.3](#23-add-a-manual-task-activity).
    Instead of going that route, let's use the custom page we've already built.  Click on the Design tab for the process and select Bug.jsx as its Custom Start Page:<br>
    ![Custom Start Page](../images/custom-start-page.png)<br>
  
  - Edit the asset mdw-hub.js/routes.json and save as follows:
    ```json
    [
      {
        "path": "/issues/new",
        "asset": "demo/Bug.jsx"
      },
      {
        "path": "/issues/:id",
        "asset": "demo/Bug.jsx"
      }
    ]
    ```
    Here we've added a new route for creating a bug.
    
  - Modify Bug.jsx to handle the process start scenario.  Use [this version on GitHub](https://github.com/CenturyLinkCloud/mdw-demo/blob/8ed9f4c1513e3c695e8c1e16008a81ad8ea9ab12/assets/demo/Bug.jsx).
    Note the conditional handling for the case where `this.state.bug.id === 0`, which is true when creating a new bug.  Now, instead of always submitting
    an HTTP POST request to run "Create Bug", you can use your start page.
    
  - Sync your server, and go to MDWHub's Workflow tab, click the Definitions nav link and drill into "Create Bug".  If you click the Run button now, it'll take you to your custom start page.
    More importantly, you can publicize the direct URL (eg: "http://localhost:8080/mdw/#/issues/new") to users so they can report a bug through your UI.
    An instance of the Create Bug process is invoked every time someone reports a bug.
    
### 3.3 Add a new tab to MDWHub 
  You can access the MDWHub Tasks tab to view an up-to-date list of bugs.  But there's a drawback in that the Tasks tab displays ALL manual tasks, potentially
  mixing in completely unrelated tasks.  Let's create an Issues tab to track only bug reports.
  
#### Override MDWHub's tabs
  Remember creating the mdw-hub.js package?  Packages with root name *mdw-hub* have special meaning.  They allow you to override these MDWHub artifacts:
  
  | Package/Asset                | Asset Types       | MDWHub Source                                                                          |
  | :--------------------------- | :---------------- | :------------------------------------------------------------------------------------- |
  | mdw-hub.js/\*\*/\(*.js|json) | javascript, json  | [mdw-hub/web/js](https://github.com/CenturyLinkCloud/mdw/tree/master/mdw-hub/web/js)   |
  | mdw-hub.css/\*\*/\*.css      | stylesheets       | [mdw-hub/web/css](https://github.com/CenturyLinkCloud/mdw/tree/master/mdw-hub/web/css) |
  | mdw-hub/\*\*/\*.html         | html              | [mdw-hub/web](https://github.com/CenturyLinkCloud/mdw/tree/master/mdw-hub/web)         |
  | mdw-hub.images/\*\*/\*       | png, gif, jpg     | [mdw-hub/web](https://github.com/CenturyLinkCloud/mdw/tree/master/mdw-hub/web/images)  |
  | mdw-hub.layout/\*\*/\*.html  | templates & menus | [mdw-hub/web](https://github.com/CenturyLinkCloud/mdw/tree/master/mdw-hub/web/layout)  |

  You have absolute power to override any file MDWHub sends to the browser.  You can even replace index.html.  Naturally you'll want to begin with
  some understanding of how the MDWHub artifacts are arranged and what they do.  (That's the purpose of the source links above).  Furthermore, you can 
  always link to your own custom (non-overriding) assets through a URL like */mdw/assets/mypkgpath/myasset.ext*.  For example, here's a link you 
  can try in your browser pointing to the bug.png asset we imported earlier: `http://localhost:8080/mdw/asset/com.centurylink.mdw.demo.bugs/bug.png`.
  
  Using stylesheets, you can adapt MDWHub's look-and-feel to integrate it into existing sites. Unless you're replacing whole-hog replacing the MDW styles, 
  a better approach than replacing Hub's defaults would be to create your own css assets and link to those from html:
  ```html
  <link rel="stylesheet" href="/mdw/asset/demo/super-cool.css">
  ```
  
  - To override tabs and nav links, right-click on mdw-hub.js and select New > JSON.  Name the asset nav.json.
    This will now shade [MDWHub's nav.json](https://github.com/CenturyLinkCloud/mdw/blob/master/mdw-hub/web/js/nav.json).
    Paste in the content of [mdw-demo's nav.json](https://github.com/CenturyLinkCloud/mdw-demo/blob/master/assets/mdw-hub/js/nav.json).
    This is the same as MDWHub's version but defines a new tab immediately following the tasksTab in the JSON array:
    ```json
    {
      "id": "issuesTab",
      "label": "Issues",
      "icon": "tasks.png",
      "url": "#/issues",
      "routes": ["/issues"],
      "navs": [
        {
          "links": [
            {
              "label": "Tasks",
              "path": "tasks/tasks",
              "href": "#/tasks",
              "priority": 1
            },
            {
              "label": "Issues",
              "path": "demo/issues.html",
              "href": "#/issues",
               "priority": 1
            },
            {
              "label": "Fallout",
              "path": "fallout/*",
              "href": "#/fallout"
            },
            {
              "label": "Templates",
              "path": "taskTemplates/*",
              "href": "#/taskTemplates",
              "priority": 1
            }
          ]
        }
      ]
    },
    ```
        
  - Now nav.json points to a url (*#/issues*) for a route that doesn't exist.  Modify routes.json in the same package:
    ```json
    [
      {
        "path": '/issues', 
        "asset": 'demo/issues.html', 
        "controller": 'BugsController'
      },
      {
        "path": "/issues/new",
        "asset": "demo/Bug.jsx"
      },
      {
        "path": "/issues/:id",
        "asset": "demo/Bug.jsx"
      }  
    ]  
    ```
    
  - The new route at the top references a controller (*BugsController*) that doesn't exist.  Create a new js asset in
    mdw-hub.js named demo.js with the following content:
    ```javascript
    'use strict';
    
    var app = angular.module('adminApp');
    
    app.controller('BugsController', ['$scope', '$controller',
          function ($scope, $controller, TasksController) {
      // initialize and extend built-in TasksController
      angular.extend(this, $controller('TasksController', {$scope: $scope}));
      
      $scope.authUser.setActiveTab('/issues');
      $scope.tasksLabel = 'Issues';
      $scope.model.taskFilter.category = 'ISSUE';
    }]);
    ```
    This extends the MDW TasksController, which uses [AngularJS](https://angularjs.org/).  The important points are that
    we're overriding the tasksLabel to be 'Issues', and we're wiring the taskFilter.category to be 'ISSUE'.
    But this illustrates another important point about overriding MDWHub.  Any *.js asset that lives in under the mdw-hub.js package
    is **automatically** loaded in index.html served up by MDWHub.  This gives you the ability to execute any custom javascript you create.
    
  - The issues route also references an HTML template, *issues.html*, create that in the demo package (not under mdw-hub) like this:
    ```html
    <div ng-include="'tasks/tasks.html'">
    </div>    
    ```
  - Sync your server and refresh your browser on MDWHub.  You should see the new Issues tab, and if you click on it you'll
    see a task list very much like that on the Tasks tab, except that it only displays tasks whose category is ISSUE.
    
  - This is a good time be harken back to ResolveBugCustom.task and its Indexes tab..  
    We're not going to add indexes now, but let's briefly mention their purpose, which is to provide a performant mechanism for displaying
    aggregate data on a list page like our new Issues tab.  Say we wanted to display the commitId field for every bug in the list.
    Without indexes, the only way to do this would be to retrieve the runtime data for every single task, which would be terribly expensive.
    When you decide to tackle a requirement of this kind, you can read up on [Task Indexes](../../help/taskIndexes.html) and learn how to
    populate them during the task lifecyle, either through expressions on the Indexes tab or by implementing 
    a [TaskIndexProvider](../../javadoc/com/centurylink/mdw/observer/task/TaskIndexProvider.html). 

### 3.4 Introduce collaboration through Slack 
  When workflow is intertwined with human activities, workgroup users and managers need to stay on top of the tasks they're responsible for.
  Users can't be expected to continually monitor MDWHub to look for updates on assigned tasks and pending actions.
  [MDW Mobile](../mdw-mobile) provides a hub-like experience with complete workflow visibility, along with configurable device notifications.
  Another great option is [Slack](https://slack.com/), which will have particular appeal to teams already using it for workforce communication.
  In this section we'll explore the ways to integrate Slack into an MDW workflow.

#### Enable Slack notifications
  The easy way to get started is by configuring a [Slack webhook](https://api.slack.com/incoming-webhooks) for one or more of your workgroup's channels.
  This gives you simple one-way interaction with friendly Slack messages when any selected outcome occurs.  Notices typically contain a brief 
  description and a link back to MDWHub's task view.  Later we'll describe how to install the [MDW Slack App](install-the-mdw-slack-app) for more advanced
  two-way integration.
  
  MDW delivers much of its functionality by way of asset packages like those we've been developing in this guide.  The project creation wizard
  imports a few fundamental packages to get started with, but for additional features like Slack integration, we'll need to import 
  `com.centurylink.mdw.slack` into our project.
  
  - From the IntelliJ menu select Tools > MDW > Discover New Assets.  Expand the MDW GitHub repository tags and select the version that corresponds
    to your project's MDW version.  In Packages to Import, select 'com.centurylink.mdw.slack':
    
    ![Discover Assets](../images/discover-assets.png)<br>
    **Note:** Besides MDW's packages, you can discover your own and other third-party packages by adding their Git repository URL to the Discovery 
    URLs in MDW Studio. 
   
  - Configure an [incoming webhook](https://api.slack.com/incoming-webhooks) as described in the Slack documentation, and add it to [mdw.yaml](../configuration/):
    ```
    mdw.slack.webhook.url=https://hooks.slack.com/services/XXXXXXX/XXXXXXXXX/XXXXXXXXXXXXXXXXXXXXXXXX
    ```
    Then restart your server to pick up this configuration change.
  
#### Create a Slack notice template
  - In MDW Studio, right-click on package `com.centurylink.mdw.demo.bugs` and select New > File.  Name the file slackNotice.json.
    Edit the asset to look something like this:
    ```
    {
      "text": "*<${taskInstanceUrl}|${request.title} \#${taskInstanceId}>*\\n${request.description}"
    }
    ```
    This is a simple template that'll post a message containing the bug's title (linking back to MDWHub), along with its description.
    Note the ${} expression placeholders that should be familiar to you by now.  These expressions operate on an instance of
    [TaskRuntimeContext](../../javadoc/com/centurylink/mdw/model/task/TaskRuntimeContext.html), which gives you access to all your process
    variables (e.g.: ${request.title}) as well as some special values like ${taskInstanceUrl}.
    
  - See the Slack documentation on [message formatting](https://api.slack.com/docs/message-formatting) for more on how you can beautify your messages.
        
  - Now let's tell MDW to make use of your notice template.  Open task asset ResolveBugCustom.task.  Click the Notices tab, 
    which displays a table of configured notifications per outcome.  In the Template column dropdown for the **Open** outcome, select slackNotice.json.
    Paste `com.centurylink.mdw.slack.TaskNotifier` into the NotifierClass(es) column.
    ![task slack notice](../images/task-slack-notice.png)
    
    As an aside for future consideration, this is a comma-delimited list where you and also enter your own custom notifier classes which implement the 
    [TemplatedNotifier](../../javadoc/com/centurylink/mdw/observer/task/TemplatedNotifier.html) interface and carry the @RegisteredService annotation.
    For now just save with the built-in notifier class for your selected template.
    
  - If you haven't already, [install the Slack app](https://slack.com/downloads) or open it's webapp in your browser.  
    Make sure you've subscribed to the channel where you  targeted the webhook you've configured.  
    Now run Create Bug, either through MDWHub or by POSTing to its service endpoint.  You should see the corresponding message in Slack.
    Here's what it looks like in Windows:<br>
    ![slack task open](../images/slack-task-open.png)
    
#### Install the MDW Slack App
  To get the full benefit of MDW's integration with Slack, you'll want to install the [mdw app]() in your Slack workspace.
  
  TODO - "Add to Slack" button, task actions performed from Slack, message threads, task Discussion tab, etc.
  
  ![mdw slack interactions](../images/mdw-slack-interactions.png)
  
#### Put it all together
  At this point we've built enough of the Issues UI to convey the key points around designing a custom UI for MDWHub.  Now is a good time
  to completely sync with mdw-demo from GitHub to see how it all comes together.
  
  - Follow these instructions from the repository readme for cloning and running mdw-demo:
    <https://github.com/CenturyLinkCloud/mdw-demo/blob/master/README.md>
    
  - Import mdw-demo into MDW Studio and try out the Bugs functionality we've just developed.

  
## 4. Explore other Features
**TODO**

### 4.1 Add markdown workflow documentation

### 4.2 Unit test an activity using MockRuntimeContext
     
### 4.3 Automate service tests with workflow verification

### 4.4 Designate a package-level error handler

### 4.5 Create custom dashboard charts

    
    
    