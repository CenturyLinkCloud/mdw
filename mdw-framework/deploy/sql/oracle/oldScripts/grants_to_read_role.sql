spool grants_to_read_role.lst;
undefine read_role_name

-- MDW table grants
Grant select on  EVENT_TYPE to &&read_role_name;			
Grant select on  VARIABLE_TYPE to &&read_role_name;			
Grant select on  ACTIVITY_IMPLEMENTOR to &&read_role_name;		
Grant select on  USER_INFO to &&read_role_name;			
Grant select on  USER_GROUP to &&read_role_name;			
Grant select on  USER_GROUP_MAPPING to &&read_role_name;		
Grant select on  USER_ROLE to &&read_role_name;			
Grant select on  USER_ROLE_MAPPING to &&read_role_name;		
Grant select on  TASK to &&read_role_name;				
Grant select on  TASK_INSTANCE to &&read_role_name;			
Grant select on  VARIABLE_INSTANCE to &&read_role_name;		
Grant select on  WORK_TRANSITION_INSTANCE to &&read_role_name;	
Grant select on  TASK_STATUS to &&read_role_name;			
Grant select on  WORK_STATUS to &&read_role_name;			
Grant select on  WORK_TRANSITION_STATUS to &&read_role_name;		
Grant select on  TASK_TYPE to &&read_role_name;			
Grant select on  TASK_CATEGORY to &&read_role_name;			
Grant select on  ATTACHMENT to &&read_role_name;			
Grant select on  TASK_STATE to &&read_role_name;			
Grant select on  EXTERNAL_EVENT to &&read_role_name;			
Grant select on  ACTIVITY_INSTANCE to &&read_role_name;		
Grant select on  PROCESS_INSTANCE to &&read_role_name;		
Grant select on  ATTRIBUTE to &&read_role_name;					
Grant select on  RULE_SET to &&read_role_name;			
Grant select on  EVENT_LOG to &&read_role_name;			
Grant select on  INSTANCE_NOTE to &&read_role_name;			
Grant select on  EVENT_WAIT_INSTANCE to &&read_role_name;		
Grant select on  PACKAGE to &&read_role_name;			
Grant select on  PACKAGE_ACTIVITY_IMPLEMENTORS to &&read_role_name;	
Grant select on  PACKAGE_EXTERNAL_EVENTS to &&read_role_name;
Grant select on  PACKAGE_RULESETS to &&read_role_name;
Grant select on  EVENT_INSTANCE to &&read_role_name;		
Grant select on  DOCUMENT to &&read_role_name;
Grant select on TASK_INST_GRP_MAPP to &&read_role_name;
Grant select on TASK_INST_INDEX to &&read_role_name;
Grant select on RESOURCE_TYPE to &&read_role_name;
      
-- WWFFW_MDW sequence grants
Grant select  on MDW_COMMON_ID_SEQ to &&read_role_name;	
Grant select  on VARIABLE_INST_ID_SEQ to &&read_role_name;	
Grant select  on WORK_TRANS_INST_ID_SEQ to &&read_role_name;	
Grant select  on MDW_COMMON_INST_ID_SEQ to &&read_role_name;		
Grant select  on ATTACHMENT_ID_SEQ to &&read_role_name;		
Grant select  on ACTIVITY_INSTANCE_ID_SEQ to &&read_role_name;	
Grant select  on EVENT_LOG_ID_SEQ to &&read_role_name;		
Grant select  on INSTANCE_NOTE_ID_SEQ to &&read_role_name;		
Grant select  on EVENT_WAIT_INSTANCE_ID_SEQ to &&read_role_name;	

undefine read_role_name
spool off;
