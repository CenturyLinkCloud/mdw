spool add_primary_keys.lst;
undefine index_tbs_name

ALTER TABLE ACTIVITY_IMPLEMENTOR ADD (
  CONSTRAINT ACTIVITY_IMPLEMENTOR_PK
  PRIMARY KEY(ACTIVITY_IMPLEMENTOR_ID)
  USING INDEX 
    TABLESPACE &&index_tbs_name
    PCTFREE    10
    INITRANS   2
    MAXTRANS   255
    STORAGE    (
                INITIAL          64K
                MINEXTENTS       1
                MAXEXTENTS       UNLIMITED
                PCTINCREASE      0
               ));
ALTER TABLE ACTIVITY_INSTANCE ADD (
  CONSTRAINT ACTIVITY_INSTANCE_PK
  PRIMARY KEY (ACTIVITY_INSTANCE_ID)
  USING INDEX 
    TABLESPACE &&index_tbs_name
    PCTFREE    10
    INITRANS   2
    MAXTRANS   255
    STORAGE    (
                INITIAL          64K
                MINEXTENTS       1
                MAXEXTENTS       UNLIMITED
                PCTINCREASE      0
               ));
ALTER TABLE ATTACHMENT ADD (
  CONSTRAINT ATTACHMENT_PK
  PRIMARY KEY (ATTACHMENT_ID)
  USING INDEX 
    TABLESPACE &&index_tbs_name
    PCTFREE    10
    INITRANS   2
    MAXTRANS   255
    STORAGE    (
                INITIAL          64K
                MINEXTENTS       1
                MAXEXTENTS       UNLIMITED
                PCTINCREASE      0
               ));
ALTER TABLE ATTRIBUTE ADD (
  CONSTRAINT ATTRIBUTE_PK
  PRIMARY KEY (ATTRIBUTE_ID)
  USING INDEX 
    TABLESPACE &&index_tbs_name
    PCTFREE    10
    INITRANS   2
    MAXTRANS   255
    STORAGE    (
                INITIAL          64K
                MINEXTENTS       1
                MAXEXTENTS       UNLIMITED
                PCTINCREASE      0
               ));
ALTER TABLE DOCUMENT ADD (
  CONSTRAINT DOCUMENT_PK
  PRIMARY KEY (DOCUMENT_ID)
  USING INDEX 
    TABLESPACE &&index_tbs_name
    PCTFREE    10
    INITRANS   2
    MAXTRANS   255
    STORAGE    (
                INITIAL          64K
                MINEXTENTS       1
                MAXEXTENTS       2147483645
                PCTINCREASE      0
               ));
ALTER TABLE EVENT_INSTANCE ADD (
  CONSTRAINT EVENT_INSTANCE_PK
  PRIMARY KEY (EVENT_NAME)
  USING INDEX
    TABLESPACE &&index_tbs_name
    PCTFREE    10
    INITRANS   2
    MAXTRANS   255
    STORAGE    (
                INITIAL          64K
                MINEXTENTS       1
                MAXEXTENTS       UNLIMITED
                PCTINCREASE      0
               ));
ALTER TABLE EVENT_LOG ADD (
  CONSTRAINT EVENT_LOG_PK
  PRIMARY KEY (EVENT_LOG_ID)
  USING INDEX
    TABLESPACE &&index_tbs_name
    PCTFREE    10
    INITRANS   2
    MAXTRANS   255
    STORAGE    (
                INITIAL          64K
                MINEXTENTS       1
                MAXEXTENTS       UNLIMITED
                PCTINCREASE      0
               ));
ALTER TABLE EVENT_TYPE ADD (
  CONSTRAINT EVENT_TYPE_PK
  PRIMARY KEY (EVENT_TYPE_ID)
  USING INDEX 
    TABLESPACE &&index_tbs_name
    PCTFREE    10
    INITRANS   2
    MAXTRANS   255
    STORAGE    (
                INITIAL          64K
                MINEXTENTS       1
                MAXEXTENTS       UNLIMITED
                PCTINCREASE      0
               ));
ALTER TABLE EVENT_WAIT_INSTANCE ADD (
  CONSTRAINT EVENT_WAIT_INSTANCE_PK
  PRIMARY KEY (EVENT_WAIT_INSTANCE_ID)
  USING INDEX
    TABLESPACE &&index_tbs_name
    PCTFREE    10
    INITRANS   2
    MAXTRANS   255
    STORAGE    (
                INITIAL          64K
                MINEXTENTS       1
                MAXEXTENTS       UNLIMITED
                PCTINCREASE      0
               ));
ALTER TABLE EXTERNAL_EVENT ADD (
  CONSTRAINT EXTERNAL_EVENT_PK
  PRIMARY KEY (EXTERNAL_EVENT_ID),
  UNIQUE (EVENT_NAME)
  USING INDEX 
    TABLESPACE &&index_tbs_name
    PCTFREE    10
    INITRANS   2
    MAXTRANS   255
    STORAGE    (
                INITIAL          64K
                MINEXTENTS       1
                MAXEXTENTS       UNLIMITED
                PCTINCREASE      0
               ));
ALTER TABLE INSTANCE_NOTE ADD (
  CONSTRAINT INSTANCE_NOTE_PK
  PRIMARY KEY (INSTANCE_NOTE_ID)
  USING INDEX 
    TABLESPACE &&index_tbs_name
    PCTFREE    10
    INITRANS   2
    MAXTRANS   255
    STORAGE    (
                INITIAL          64K
                MINEXTENTS       1
                MAXEXTENTS       UNLIMITED
                PCTINCREASE      0
               ));
ALTER TABLE PACKAGE ADD (
  CONSTRAINT PACKAGE_PK
  PRIMARY KEY (PACKAGE_ID)
  USING INDEX 
    TABLESPACE &&index_tbs_name
    PCTFREE    10
    INITRANS   2
    MAXTRANS   255
    STORAGE    (
                INITIAL          64K
                MINEXTENTS       1
                MAXEXTENTS       2147483645
                PCTINCREASE      0
               ));
ALTER TABLE PACKAGE_ACTIVITY_IMPLEMENTORS ADD (
  CONSTRAINT PACKAGE_ACT_IMPLEMENTORS_PK
  PRIMARY KEY (PACKAGE_ID, ACTIVITY_IMPLEMENTOR_ID)
  USING INDEX 
    TABLESPACE &&index_tbs_name
    PCTFREE    10
    INITRANS   2
    MAXTRANS   255
    STORAGE    (
                INITIAL          64K
                MINEXTENTS       1
                MAXEXTENTS       2147483645
                PCTINCREASE      0
               ));
ALTER TABLE PACKAGE_EXTERNAL_EVENTS ADD (
  CONSTRAINT PACKAGE_EXTERNAL_EVENTS_PK
  PRIMARY KEY (PACKAGE_ID, EXTERNAL_EVENT_ID)
  USING INDEX 
    TABLESPACE &&index_tbs_name
    PCTFREE    10
    INITRANS   2
    MAXTRANS   255
    STORAGE    (
                INITIAL          64K
                MINEXTENTS       1
                MAXEXTENTS       2147483645
                PCTINCREASE      0
               ));
ALTER TABLE PACKAGE_RULESETS ADD (
  CONSTRAINT PACKAGE_RULESETS_PK
  PRIMARY KEY (PACKAGE_ID, RULE_SET_ID)
  USING INDEX 
    TABLESPACE &&index_tbs_name
    PCTFREE    10
    INITRANS   2
    MAXTRANS   255
    STORAGE    (
                INITIAL          64K
                MINEXTENTS       1
                MAXEXTENTS       2147483645
                PCTINCREASE      0
               ));
ALTER TABLE PROCESS_INSTANCE ADD (
  CONSTRAINT PROCESS_INSTANCE_PK
  PRIMARY KEY (PROCESS_INSTANCE_ID)
  USING INDEX 
    TABLESPACE &&index_tbs_name
    PCTFREE    10
    INITRANS   2
    MAXTRANS   255
    STORAGE    (
                INITIAL          64K
                MINEXTENTS       1
                MAXEXTENTS       UNLIMITED
                PCTINCREASE      0
               ));
ALTER TABLE RULE_SET ADD (
  CONSTRAINT RULE_SET_PK
  PRIMARY KEY (RULE_SET_ID)
  USING INDEX 
    TABLESPACE &&index_tbs_name
    PCTFREE    10
    INITRANS   2
    MAXTRANS   255
    STORAGE    (
                INITIAL          64K
                MINEXTENTS       1
                MAXEXTENTS       UNLIMITED
                PCTINCREASE      0
               ));
ALTER TABLE TASK ADD (
  CONSTRAINT TASK_PK
  PRIMARY KEY (TASK_ID)
  USING INDEX 
    TABLESPACE &&index_tbs_name
    PCTFREE    10
    INITRANS   2
    MAXTRANS   255
    STORAGE    (
                INITIAL          64K
                MINEXTENTS       1
                MAXEXTENTS       UNLIMITED
                PCTINCREASE      0
               ));            
ALTER TABLE TASK_CATEGORY ADD (
  CONSTRAINT TASK_CATEGORY_PK
  PRIMARY KEY (TASK_CATEGORY_ID)
  USING INDEX 
    TABLESPACE &&index_tbs_name
    PCTFREE    10
    INITRANS   2
    MAXTRANS   255
    STORAGE    (
                INITIAL          64K
                MINEXTENTS       1
                MAXEXTENTS       UNLIMITED
                PCTINCREASE      0
               ));
ALTER TABLE TASK_INSTANCE ADD (
  CONSTRAINT TASK_INSTANCE_PK
  PRIMARY KEY (TASK_INSTANCE_ID)
  USING INDEX 
    TABLESPACE &&index_tbs_name
    PCTFREE    10
    INITRANS   2
    MAXTRANS   255
    STORAGE    (
                INITIAL          64K
                MINEXTENTS       1
                MAXEXTENTS       UNLIMITED
                PCTINCREASE      0
               ));
ALTER TABLE TASK_STATE ADD (
  CONSTRAINT TASK_STATE_PK 
  PRIMARY KEY (TASK_STATE_ID)
  USING INDEX 
    TABLESPACE &&index_tbs_name
    PCTFREE    10
    INITRANS   2
    MAXTRANS   255
    STORAGE    (
                INITIAL          64K
                MINEXTENTS       1
                MAXEXTENTS       UNLIMITED
                PCTINCREASE      0
               ));
ALTER TABLE TASK_STATUS ADD (
  CONSTRAINT TASK_STATUS_PK
  PRIMARY KEY (TASK_STATUS_ID)
  USING INDEX 
    TABLESPACE &&index_tbs_name
    PCTFREE    10
    INITRANS   2
    MAXTRANS   255
    STORAGE    (
                INITIAL          64K
                MINEXTENTS       1
                MAXEXTENTS       UNLIMITED
                PCTINCREASE      0
               ));
ALTER TABLE TASK_TYPE ADD (
  CONSTRAINT TASK_TYPE_PK
  PRIMARY KEY (TASK_TYPE_ID)
  USING INDEX 
    TABLESPACE &&index_tbs_name
    PCTFREE    10
    INITRANS   2
    MAXTRANS   255
    STORAGE    (
                INITIAL          64K
                MINEXTENTS       1
                MAXEXTENTS       UNLIMITED
                PCTINCREASE      0
               ));
ALTER TABLE USER_GROUP ADD (
  CONSTRAINT USER_GROUP_PK
  PRIMARY KEY (USER_GROUP_ID)
  USING INDEX 
    TABLESPACE &&index_tbs_name
    PCTFREE    10
    INITRANS   2
    MAXTRANS   255
    STORAGE    (
                INITIAL          64K
                MINEXTENTS       1
                MAXEXTENTS       UNLIMITED
                PCTINCREASE      0
               ));               
ALTER TABLE USER_GROUP_MAPPING ADD (
  CONSTRAINT USER_GROUP_MAPPING_PK
  PRIMARY KEY (USER_GROUP_MAPPING_ID)
  USING INDEX 
    TABLESPACE &&index_tbs_name
    PCTFREE    10
    INITRANS   2
    MAXTRANS   255
    STORAGE    (
                INITIAL          64K
                MINEXTENTS       1
                MAXEXTENTS       UNLIMITED
                PCTINCREASE      0
               ));
ALTER TABLE USER_INFO ADD (
  CONSTRAINT USER_INFO_PK
  PRIMARY KEY (USER_INFO_ID)
  USING INDEX 
    TABLESPACE &&index_tbs_name
    PCTFREE    10
    INITRANS   2
    MAXTRANS   255
    STORAGE    (
                INITIAL          64K
                MINEXTENTS       1
                MAXEXTENTS       UNLIMITED
                PCTINCREASE      0
               ));
ALTER TABLE USER_ROLE ADD (
  CONSTRAINT USER_ROLE_PK
  PRIMARY KEY (USER_ROLE_ID)
  USING INDEX 
    TABLESPACE &&index_tbs_name
    PCTFREE    10
    INITRANS   2
    MAXTRANS   255
    STORAGE    (
                INITIAL          64K
                MINEXTENTS       1
                MAXEXTENTS       UNLIMITED
                PCTINCREASE      0
               ));
ALTER TABLE USER_ROLE_MAPPING ADD (
  CONSTRAINT USER_ROLE_MAPPING_PK
  PRIMARY KEY (USER_ROLE_MAPPING_ID)
  USING INDEX 
    TABLESPACE &&index_tbs_name
    PCTFREE    10
    INITRANS   2
    MAXTRANS   255
    STORAGE    (
                INITIAL          64K
                MINEXTENTS       1
                MAXEXTENTS       UNLIMITED
                PCTINCREASE      0
               ));
ALTER TABLE VARIABLE_INSTANCE ADD (
  CONSTRAINT VARIABLE_INSTANCE_PK
  PRIMARY KEY (VARIABLE_INST_ID)
  USING INDEX 
    TABLESPACE &&index_tbs_name
    PCTFREE    10
    INITRANS   2
    MAXTRANS   255
    STORAGE    (
                INITIAL          64K
                MINEXTENTS       1
                MAXEXTENTS       UNLIMITED
                PCTINCREASE      0
               ));
ALTER TABLE VARIABLE_TYPE ADD (
  CONSTRAINT VARIABLE_TYPE_PK
  PRIMARY KEY (VARIABLE_TYPE_ID)
  USING INDEX 
    TABLESPACE &&index_tbs_name
    PCTFREE    10
    INITRANS   2
    MAXTRANS   255
    STORAGE    (
                INITIAL          64K
                MINEXTENTS       1
                MAXEXTENTS       UNLIMITED
                PCTINCREASE      0
               ));
ALTER TABLE WORK_STATUS ADD (
  CONSTRAINT WORK_STATUS_PK
  PRIMARY KEY (WORK_STATUS_ID)
  USING INDEX 
    TABLESPACE &&index_tbs_name
    PCTFREE    10
    INITRANS   2
    MAXTRANS   255
    STORAGE    (
                INITIAL          64K
                MINEXTENTS       1
                MAXEXTENTS       UNLIMITED
                PCTINCREASE      0
               ));
ALTER TABLE WORK_TRANSITION_INSTANCE ADD (
  CONSTRAINT WORK_TRANS_INST_PK
  PRIMARY KEY (WORK_TRANS_INST_ID)
  USING INDEX 
    TABLESPACE &&index_tbs_name
    PCTFREE    10
    INITRANS   2
    MAXTRANS   255
    STORAGE    (
                INITIAL          64K
                MINEXTENTS       1
                MAXEXTENTS       UNLIMITED
                PCTINCREASE      0
               ));
ALTER TABLE WORK_TRANSITION_STATUS ADD (
  CONSTRAINT WORK_TRANSITION_STATUS_PK
  PRIMARY KEY (WORK_TRANSITION_STATUS_ID)
  USING INDEX 
    TABLESPACE &&index_tbs_name
    PCTFREE    10
    INITRANS   2
    MAXTRANS   255
    STORAGE    (
                INITIAL          64K
                MINEXTENTS       1
                MAXEXTENTS       UNLIMITED
                PCTINCREASE      0
               ));
ALTER TABLE TASK_INST_GRP_MAPP ADD (
  CONSTRAINT TASK_INST_GRP_MAPP_PK
  PRIMARY KEY (TASK_INSTANCE_ID,USER_GROUP_ID)
  USING INDEX 
    TABLESPACE &&index_tbs_name
    PCTFREE    10
    INITRANS   2
    MAXTRANS   255
    STORAGE    (
                INITIAL          64K
                MINEXTENTS       1
                MAXEXTENTS       UNLIMITED
                PCTINCREASE      0
               ));
ALTER TABLE TASK_INST_INDEX ADD (
  CONSTRAINT TASK_INST_INDEX_PK
  PRIMARY KEY (TASK_INSTANCE_ID,INDEX_KEY)
  USING INDEX 
    TABLESPACE &&index_tbs_name
    PCTFREE    10
    INITRANS   2
    MAXTRANS   255
    STORAGE    (
                INITIAL          64K
                MINEXTENTS       1
                MAXEXTENTS       UNLIMITED
                PCTINCREASE      0
               ));
ALTER TABLE RESOURCE_TYPE ADD (
  CONSTRAINT RESOURCE_TYPE_PK
  PRIMARY KEY (RESOURCE_TYPE_NAME)
  USING INDEX 
    TABLESPACE &&index_tbs_name
    PCTFREE    10
    INITRANS   2
    MAXTRANS   255
    STORAGE    (
                INITIAL          64K
                MINEXTENTS       1
                MAXEXTENTS       UNLIMITED
                PCTINCREASE      0
               ));

undefine index_tbs_name        
spool off;
