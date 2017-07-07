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