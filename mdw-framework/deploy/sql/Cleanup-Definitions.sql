DECLARE
  startTime timestamp;
  endTime timestamp;
    

BEGIN
     select sysdate into startTime from dual;
   DBMS_OUTPUT.PUT_LINE('Start Time -->' || startTime);
   
   


   -- external event tables
   -- delete from EXT_EVENT_PROCESS_MAPP;
   delete from external_event; 
   
   -- work flow related tables   
   -- delete from PROCESS_ALIAS_MAPPING;
   -- delete from PROCESS_CUSTOM_SETUP;
   delete from work_synchronization;
   delete from process;
   delete from activity;
   delete from activity_implementor;
   -- delete from WORK_TRANSITION_VALDATION;
   delete from work_transition;

   -- delete from WORK_TRANSITION_DEPENDENCY;
   delete from work;
   
   -- variable related tables
   delete from variable_mapping;
   delete from variable;
   
   
   
     
   -- user related tables
   delete from USER_ROLE_MAPPING;
   delete from USER_GROUP_MAPPING;
   delete from TASK_USR_GRP_MAPP;
   delete from user_group;
   delete from TASK_ACTN_USR_ROLE_MAPP;

   delete from user_role;
   delete from user_info;
   delete from user_group;
   
   -- task related tables

   delete from task;
   delete from task_category;
   delete from task_action;
   
   -- common tables
   delete from sla;
   delete from attribute;
   delete from attachment;
   delete from rule_set;
   

   
   
   select sysdate into endTime from dual;
   DBMS_OUTPUT.PUT_LINE('End Time -->' || endTime);
Exception
   When Others then
   DBMS_OUTPUT.PUT_LINE('Error in cleaning process data: Error:' ||sqlerrm); 
   rollback;
END;


commit;