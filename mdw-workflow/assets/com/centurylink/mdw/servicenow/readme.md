# ServiceNow Package
In your MDW workflow you can trigger ServiceNow incident creation in two ways: using the ServiceNow Adapter or
through the ServiceNow Task activity.  The adapter requires that you build the 
[Incident model](https://github.com/CenturyLinkCloud/mdw/blob/master/mdw-workflow/assets/com/centurylink/mdw/servicenow/Incident.java)
programmatically yourself.  The task activity creates an MDW manual task and automatically maps its fields when
invoking the ServiceNow API.  

## ServiceNow Adapter
The adapter is a good choice if you want to create a ServiceNow incident and continue flow.  There is no 2-way feedback.
It extends the REST Adapter and works with an Incident object.  To use the adapter, in your process definition create a 
Jsonable variable to hold the incident, and populate its fields with the values you'd like reflected in ServiceNow.

The [ServiceNow Incident](https://github.com/CenturyLinkCloud/mdw/blob/master/mdw-workflow/assets/com/centurylink/mdw/tests/services/ServiceNow%20Incident.proc)
test process shows an example of how to build an incident and use it to invoke the ServiceNow API.

## ServiceNow Task  
MDW's ServiceNow task halts workflow progress until the task is completed.  It's a special type of manual task
activity whose default template includes values appropriate for interfacing with the ServiceNow API.  The
[ServiceNow Task](https://github.com/CenturyLinkCloud/mdw/blob/master/mdw-workflow/assets/com/centurylink/mdw/tests/tasks/ServiceNow%20Task.proc)
test process is an example of how to use the activity.

To use the ServiceNow task, in MDW Studio drag the activity from the Toolbox onto your process canvas.  Then create a task
template of type ServiceNow by right-clicking on the target package and selecting New > MDW Task:
<img src="https://raw.githubusercontent.com/CenturyLinkCloud/mdw/master/mdw-workflow/assets/com/centurylink/mdw/servicenow/servicenow_task.png" alt="ServiceNow Task" width="600px;display:block"/>
Select this task as the template on the activity's Configurator Designer tab in MDW Studio.

The task is preconfigured with the ServiceNow TaskNotifier, and uses the default 
[IncidentTemplate.yaml](https://github.com/CenturyLinkCloud/mdw/blob/master/mdw-workflow/assets/com/centurylink/mdw/servicenow/IncidentTemplate.yaml)
for mapping values from the TaskInstance to the ServiceNow Incident.  If you require different mappings, you can create your
own YAML template and update the task Notices tab to use that instead.  Now when the MDW task instance is created, a 
corresponding ServiceNow incident will be created as well via the API.

## Dependencies
  - [com.centurylink.mdw.base](https://github.com/CenturyLinkCloud/mdw/blob/master/mdw-workflow/assets/com/centurylink/mdw/base/readme.md)
 