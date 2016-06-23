spool grants_to_update_role.lst;
undefine update_role_name

Grant select,insert,update,delete on  EVENT_TYPE to &&update_role_name;			
Grant select,insert,update,delete on  VARIABLE_TYPE to &&update_role_name;			
Grant select,insert,update,delete on  ACTIVITY_IMPLEMENTOR to &&update_role_name;		
Grant select,insert,update,delete on  USER_INFO to &&update_role_name;			
Grant select,insert,update,delete on  USER_GROUP to &&update_role_name;			
Grant select,insert,update,delete on  USER_GROUP_MAPPING to &&update_role_name;		
Grant select,insert,update,delete on  USER_ROLE to &&update_role_name;			
Grant select,insert,update,delete on  USER_ROLE_MAPPING to &&update_role_name;		
Grant select,insert,update,delete on  TASK to &&update_role_name;				
Grant select,insert,update,delete on  TASK_INSTANCE to &&update_role_name;			
Grant select,insert,update,delete on  VARIABLE_INSTANCE to &&update_role_name;		
Grant select,insert,update,delete on  WORK_TRANSITION_INSTANCE to &&update_role_name;	
Grant select,insert,update,delete on  TASK_STATUS to &&update_role_name;			
Grant select,insert,update,delete on  WORK_STATUS to &&update_role_name;			
Grant select,insert,update,delete on  WORK_TRANSITION_STATUS to &&update_role_name;		
Grant select,insert,update,delete on  TASK_TYPE to &&update_role_name;			
Grant select,insert,update,delete on  TASK_CATEGORY to &&update_role_name;			
Grant select,insert,update,delete on  ATTACHMENT to &&update_role_name;			
Grant select,insert,update,delete on  TASK_STATE to &&update_role_name;			
Grant select,insert,update,delete on  EXTERNAL_EVENT to &&update_role_name;			
Grant select,insert,update,delete on  ACTIVITY_INSTANCE to &&update_role_name;		
Grant select,insert,update,delete on  PROCESS_INSTANCE to &&update_role_name;		
Grant select,insert,update,delete on  ATTRIBUTE to &&update_role_name;					
Grant select,insert,update,delete on  RULE_SET to &&update_role_name;			
Grant select,insert,update,delete on  EVENT_LOG to &&update_role_name;			
Grant select,insert,update,delete on  INSTANCE_NOTE to &&update_role_name;			
Grant select,insert,update,delete on  EVENT_WAIT_INSTANCE to &&update_role_name;		
Grant select,insert,update,delete on  PACKAGE to &&update_role_name;			
Grant select,insert,update,delete on  PACKAGE_ACTIVITY_IMPLEMENTORS to &&update_role_name;	
Grant select,insert,update,delete on  PACKAGE_EXTERNAL_EVENTS to &&update_role_name;	
Grant select,insert,update,delete on  PACKAGE_RULESETS to &&update_role_name;	
Grant select,insert,update,delete on  EVENT_INSTANCE to &&update_role_name;	
Grant select,insert,update,delete on  DOCUMENT to &&update_role_name;
Grant select,insert,update,delete on TASK_INST_GRP_MAPP to &&update_role_name;
Grant select,insert,update,delete on TASK_INST_INDEX to &&update_role_name;
Grant select,insert,update,delete on RESOURCE_TYPE to &&update_role_name;

undefine update_role_name
spool off;
