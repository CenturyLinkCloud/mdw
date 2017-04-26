---
title: MDW Database Tables
permalink: /docs/db/MdwDatabaseTables/
---


| Table Name | Reference/Design/Runtime | Description |
|--------------------------|:------------------------:|-------------------------------------------------------------------------------------------|
| ACTIVITY_INSTANCE | Runtime | Runtime instance of an Activity. |
| ATTACHMENT | Runtime | Attachments can reference either a DOCUMENT in the database or a file on the file system. |
| ATTRIBUTE | Design | Attributes of users or workgroups etc. (OWNER_TYPE and OWNER_ID identify this). |
| DOCUMENT | Runtime | Document variable values in CLOB form. |
| EVENT_INSTANCE | Runtime | An instance of an internal or external event (event_name is unique). |
| EVENT_LOG | Runtime | All user and system actions are logged here to provide an audit trail. |
| EVENT_WAIT_INSTANCE | Runtime | Registered event waits (unregistered when EVENT_INSTANCE arrives). |
| INSTANCE_NOTE | Runtime | Text-based Notes attached to workflow entities. |
| PROCESS_INSTANCE | Runtime | Runtime storage for Process Instance high-level info. |
| TASK_INSTANCE | Runtime | Runtime instance of a manual Task. |
| TASK_INST_GRP_MAPP | Runtime | Join table between TASK_INSTANCE and USER_GROUP. |
| TASK_INST_INDEX | Runtime | Queryable runtime state info for Task Instances. |
| USER_GROUP | Runtime | Workgroups (defined by users via TaskManager). |
| USER_GROUP_MAPPING | Runtime | Join table for USER_INFO to USER_GROUPS. |
| USER_INFO | Runtime | High-level User information. |
| USER_ROLE | Runtime | Extensible user Roles (defaults can be supplemented via TaskManager). |
| USER_ROLE_MAPPING | Runtime | OWNER is either a USER or a USER_GROUP_MAPPING. |
| VARIABLE_INSTANCE | Runtime | Runtime value for a Process Variable. |
| WORK_TRANSITION_INSTANCE | Runtime | Runtime instance of a workflow Transition. |

<div class="note">
  <h5> Terminology </h5>
</div>

|----------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Activity | The most granular unit of work in a process definition   (a single step executed in the flow). |
| Activity Implementor | The business logic underlying an activity (implemented   in Java code).  Registered implementors   appear in the Designer Toolbox view. |
| Adapter | Sends outgoing messages to an external system (service   invocation). |
| Attachment | Logical workflow entities such as Orders and Tasks can   have attachments (typically a binary document like a Word doc uploaded by a   user). |
| Attribute | Configurable aspect of an activity or process.  These are manipulated via the property tabs   in Designer. |
| Document | Special type of variable for storing large XML or   other data.  Document variables are   accessed by reference. |
| Event Handler | Responds to incoming request from external systems   (service implementation).  Designer   associates event patterns with associated implementation classes. |
| Note | Text-based annotations attached to workflow entities   such as Orders or Tasks. |
| Package | A container for aggregating processes and Workflow,   for namespace resolution and to facilitate importing/exporting between   environment.  |
| Process | A set of linked activities (either automated or   manual) designed to deliver value to our business client. |
| Task | An activity requiring manual intervention.  MDW TaskManager is used to perform manual   tasks. |
| Transition | A workflow link between activities.  Defines a possible outcome from the   upstream step. |
| Variable | Data element configured for a process which can hold a   dynamic value at runtime. |
| Workflow Asset | A versionable resource that's maintained as part of a   workflow deployment.  |
| Workgroup | Bucketization mechanism for manual Tasks.  Users belong to the workgroups whose manual   tasks they can act upon. |
