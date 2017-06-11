spool add_primary_keys.lst;

  
ALTER TABLE ACTIVITY_INSTANCE ADD (
  CONSTRAINT ACTIVITY_INSTANCE_PK
  PRIMARY KEY (ACTIVITY_INSTANCE_ID)
  USING INDEX);
  
ALTER TABLE ATTACHMENT ADD (
  CONSTRAINT ATTACHMENT_PK
  PRIMARY KEY (ATTACHMENT_ID)
  USING INDEX);
  
ALTER TABLE ATTRIBUTE ADD (
  CONSTRAINT ATTRIBUTE_PK
  PRIMARY KEY (ATTRIBUTE_ID)
  USING INDEX);
  
ALTER TABLE DOCUMENT ADD (
  CONSTRAINT DOCUMENT_PK
  PRIMARY KEY (DOCUMENT_ID)
  USING INDEX);
  
ALTER TABLE EVENT_INSTANCE ADD (
  CONSTRAINT EVENT_INSTANCE_PK
  PRIMARY KEY (EVENT_NAME)
  USING INDEX);
  
ALTER TABLE EVENT_LOG ADD (
  CONSTRAINT EVENT_LOG_PK
  PRIMARY KEY (EVENT_LOG_ID)
  USING INDEX);
    
ALTER TABLE EVENT_WAIT_INSTANCE ADD (
  CONSTRAINT EVENT_WAIT_INSTANCE_PK
  PRIMARY KEY (EVENT_WAIT_INSTANCE_ID)
  USING INDEX);
  
ALTER TABLE INSTANCE_NOTE ADD (
  CONSTRAINT INSTANCE_NOTE_PK
  PRIMARY KEY (INSTANCE_NOTE_ID)
  USING INDEX);
   
ALTER TABLE PROCESS_INSTANCE ADD (
  CONSTRAINT PROCESS_INSTANCE_PK
  PRIMARY KEY (PROCESS_INSTANCE_ID)
  USING INDEX);

ALTER TABLE TASK_INSTANCE ADD (
  CONSTRAINT TASK_INSTANCE_PK
  PRIMARY KEY (TASK_INSTANCE_ID)
  USING INDEX);
    
ALTER TABLE USER_GROUP ADD (
  CONSTRAINT USER_GROUP_PK
  PRIMARY KEY (USER_GROUP_ID)
  USING INDEX);
  
ALTER TABLE USER_GROUP_MAPPING ADD (
  CONSTRAINT USER_GROUP_MAPPING_PK
  PRIMARY KEY (USER_GROUP_MAPPING_ID)
  USING INDEX);
  
ALTER TABLE USER_INFO ADD (
  CONSTRAINT USER_INFO_PK
  PRIMARY KEY (USER_INFO_ID)
  USING INDEX);
  
ALTER TABLE USER_ROLE ADD (
  CONSTRAINT USER_ROLE_PK
  PRIMARY KEY (USER_ROLE_ID)
  USING INDEX);
  
ALTER TABLE USER_ROLE_MAPPING ADD (
  CONSTRAINT USER_ROLE_MAPPING_PK
  PRIMARY KEY (USER_ROLE_MAPPING_ID)
  USING INDEX);
  
ALTER TABLE VARIABLE_INSTANCE ADD (
  CONSTRAINT VARIABLE_INSTANCE_PK
  PRIMARY KEY (VARIABLE_INST_ID)
  USING INDEX);
 
ALTER TABLE WORK_TRANSITION_INSTANCE ADD (
  CONSTRAINT WORK_TRANS_INST_PK
  PRIMARY KEY (WORK_TRANS_INST_ID)
  USING INDEX);
  
ALTER TABLE TASK_INST_GRP_MAPP ADD (
  CONSTRAINT TASK_INST_GRP_MAPP_PK
  PRIMARY KEY (TASK_INSTANCE_ID,USER_GROUP_ID)
  USING INDEX);
  
ALTER TABLE TASK_INST_INDEX ADD (
  CONSTRAINT TASK_INST_INDEX_PK
  PRIMARY KEY (TASK_INSTANCE_ID,INDEX_KEY)
  USING INDEX);
  
ALTER TABLE SOLUTION ADD (
  CONSTRAINT SOLUTION_ID_PK
  PRIMARY KEY (SOLUTION_ID)
  USING INDEX
);

ALTER TABLE SOLUTION_MAP ADD  
(   
   PRIMARY KEY (SOLUTION_ID,MEMBER_TYPE,MEMBER_ID)
);

ALTER TABLE value ADD  
(   
   PRIMARY KEY (Name,Owner_type,owner_id)
);
  
spool off;