DECLARE
  t_exists NUMBER;
  c_exists NUMBER;
  def_value varchar2(14) := 'TASK INSTANCE';
  nullCheck varchar2(1);
BEGIN
  DBMS_OUTPUT.put_line ('Script to convert mdw schema from 6.0.x to 6.1 version');	
  SELECT COUNT(*) INTO t_exists FROM user_tables WHERE table_name ='ASSET_REF';
  IF t_exists = 0 THEN
    EXECUTE IMMEDIATE 'CREATE TABLE ASSET_REF(      
    DEFINITION_ID       NUMBER(38)   NOT NULL,      
    NAME                VARCHAR(512)    NOT NULL,      
    REF                 VARCHAR(64)     NOT NULL,      
    ARCHIVE_DT          DATE DEFAULT SYSDATE NOT NULL     
    )';
    EXECUTE IMMEDIATE 'ALTER TABLE ASSET_REF ADD (  
    CONSTRAINT ASSET_REF_PK  
    PRIMARY KEY (DEFINITION_ID)  
    USING INDEX)';
    EXECUTE IMMEDIATE 'CREATE INDEX ASSET_REF_NAME_IDX   
    ON ASSET_REF(NAME)';
    EXECUTE IMMEDIATE 'CREATE INDEX ASSET_REF_ARCHIVE_DT_IDX   
    ON ASSET_REF (ARCHIVE_DT)';
    DBMS_OUTPUT.put_line ('ASSET_REF table created');
  ELSE 
    SELECT count(*)
    INTO c_exists FROM
    user_indexes WHERE table_name = 'ASSET_REF' and index_name='ASSET_REF_PK';
    IF c_exists = 0 THEN
      EXECUTE IMMEDIATE 'ALTER TABLE ASSET_REF ADD (  
      CONSTRAINT ASSET_REF_PK  
      PRIMARY KEY (DEFINITION_ID)  
      USING INDEX)';
    END IF;
  END IF;  
  SELECT COUNT(*)
  INTO c_exists
  FROM user_tab_columns
  WHERE table_name='DOCUMENT'
  AND COLUMN_NAME ='PATH';
  IF c_exists     = 0 THEN
    EXECUTE IMMEDIATE 'ALTER TABLE DOCUMENT ADD(PATH VARCHAR2(1000))';
    DBMS_OUTPUT.put_line ('Document table altered');
  END IF;
  SELECT COUNT(*)
  INTO t_exists
  FROM user_tables
  WHERE table_name ='INSTANCE_INDEX';
  IF t_exists      = 0 THEN
    SELECT count(*)
    INTO c_exists FROM
    user_indexes WHERE table_name = 'TASK_INST_INDEX' and index_name='TASK_INST_INDEX_PK';
    IF c_exists = 1 THEN
     EXECUTE IMMEDIATE 'ALTER TABLE TASK_INST_INDEX DROP PRIMARY KEY DROP INDEX';
    END IF;
    SELECT count(*)
    INTO c_exists FROM
    user_indexes WHERE table_name = 'TASK_INST_INDEX' and index_name='TASKINSTIDX_TASKINST_FK';
    IF c_exists = 1 THEN
     EXECUTE IMMEDIATE 'ALTER TABLE TASK_INST_INDEX DROP CONSTRAINT TASKINSTIDX_TASKINST_FK';
    END IF;
    EXECUTE IMMEDIATE 'ALTER TABLE TASK_INST_INDEX rename column TASK_INSTANCE_ID TO INSTANCE_ID';
    EXECUTE IMMEDIATE 'ALTER TABLE TASK_INST_INDEX add (OWNER_TYPE VARCHAR2(30 BYTE) DEFAULT '''||def_value||''' NOT NULL)';
    EXECUTE IMMEDIATE 'ALTER TABLE TASK_INST_INDEX rename to INSTANCE_INDEX';
    EXECUTE IMMEDIATE 'ALTER TABLE INSTANCE_INDEX ADD (  
    CONSTRAINT INSTANCE_INDEX_PK  
    PRIMARY KEY (INSTANCE_ID,OWNER_TYPE,INDEX_KEY)  
    USING INDEX)';
    --EXECUTE IMMEDIATE 'CREATE INDEX INSTANCEIDX_IDXKEY_FK 
    --ON INSTANCE_INDEX (INDEX_KEY)';
    DBMS_OUTPUT.put_line ('TASK_INST_INDEX table changed to INSTANCE_INDEX');
  END IF;
  SELECT COUNT(*)
  INTO c_exists
  FROM user_tab_columns
  WHERE table_name='TASK_INSTANCE'
  AND COLUMN_NAME ='TASK_TITLE';
  IF c_exists     = 0 THEN
    EXECUTE IMMEDIATE 'ALTER TABLE TASK_INSTANCE ADD(TASK_TITLE VARCHAR2(512))';
    DBMS_OUTPUT.put_line ('TASK_INSTANCE table altered');
  END IF;
  SELECT COUNT(*)
  INTO c_exists
  FROM user_tab_columns
  WHERE table_name='ATTACHMENT'
  AND COLUMN_NAME ='ATTACHMENT_STATUS';
  IF c_exists     = 1 THEN
    EXECUTE IMMEDIATE 'ALTER TABLE ATTACHMENT DROP (ATTACHMENT_STATUS,COMMENTS)';
    DBMS_OUTPUT.put_line ('ATTACHMENT table altered');
  END IF;
  SELECT COUNT(*)
  INTO c_exists
  FROM user_tab_columns
  WHERE table_name='INSTANCE_NOTE'
  AND COLUMN_NAME ='COMMENTS';
  IF c_exists     = 1 THEN
    EXECUTE IMMEDIATE 'ALTER TABLE INSTANCE_NOTE DROP COLUMN COMMENTS';
    DBMS_OUTPUT.put_line ('INSTANCE_NOTE table altered');
  END IF;
  --Check for not null constraint,otherwise it throws ORA-01442: column to be modified to NOT NULL is already NOT NULL
  SELECT nullable 
  INTO nullCheck
  FROM user_tab_cols
  WHERE table_name = 'ATTACHMENT'
  AND column_name = 'ATTACHMENT_CONTENT_TYPE';
  IF nullCheck = 'N' THEN
    EXECUTE IMMEDIATE 'ALTER TABLE ATTACHMENT 
    MODIFY (CREATE_USR VARCHAR2(100 BYTE),
           ATTACHMENT_CONTENT_TYPE VARCHAR2(1000))';
    DBMS_OUTPUT.put_line ('ATTACHMENT table altered');
  END IF;
  
  SELECT nullable 
  INTO nullCheck
  FROM user_tab_cols
  WHERE table_name = 'INSTANCE_NOTE'
  AND column_name = 'INSTANCE_NOTE_NAME';
  IF nullCheck = 'N' THEN
    EXECUTE IMMEDIATE 'ALTER TABLE INSTANCE_NOTE 
    MODIFY (CREATE_USR VARCHAR2(100 BYTE),
            INSTANCE_NOTE_NAME VARCHAR2(256 BYTE))';               
    DBMS_OUTPUT.put_line ('INSTANCE_NOTE table altered');
  END IF;
  
  EXECUTE IMMEDIATE 'ALTER TABLE TASK_INSTANCE modify (TASK_INSTANCE_REFERRED_AS VARCHAR2(500 BYTE),CREATE_DT TIMESTAMP DEFAULT SYSTIMESTAMP, MOD_DT TIMESTAMP, TASK_START_DT TIMESTAMP, TASK_END_DT TIMESTAMP)';
  EXECUTE IMMEDIATE 'ALTER TABLE WORK_TRANSITION_INSTANCE modify (CREATE_DT TIMESTAMP DEFAULT SYSTIMESTAMP, MOD_DT TIMESTAMP, START_DT TIMESTAMP, END_DT TIMESTAMP)';
  EXECUTE IMMEDIATE 'ALTER TABLE VARIABLE_INSTANCE modify (CREATE_DT TIMESTAMP DEFAULT SYSTIMESTAMP, MOD_DT TIMESTAMP)';
  EXECUTE IMMEDIATE 'ALTER TABLE ACTIVITY_INSTANCE modify (CREATE_DT TIMESTAMP DEFAULT SYSTIMESTAMP, MOD_DT TIMESTAMP, START_DT TIMESTAMP, END_DT TIMESTAMP)';
  EXECUTE IMMEDIATE 'ALTER TABLE PROCESS_INSTANCE modify (CREATE_DT TIMESTAMP DEFAULT SYSTIMESTAMP, MOD_DT TIMESTAMP, START_DT TIMESTAMP, END_DT TIMESTAMP)';
  EXECUTE IMMEDIATE 'ALTER TABLE EVENT_WAIT_INSTANCE modify (CREATE_DT TIMESTAMP DEFAULT SYSTIMESTAMP, MOD_DT TIMESTAMP)';
  EXECUTE IMMEDIATE 'ALTER TABLE EVENT_INSTANCE modify (CREATE_DT TIMESTAMP DEFAULT SYSTIMESTAMP, CONSUME_DT TIMESTAMP)';
  EXECUTE IMMEDIATE 'ALTER TABLE EVENT_LOG modify (CREATE_DT TIMESTAMP DEFAULT SYSTIMESTAMP, MOD_DT TIMESTAMP)';
  EXECUTE IMMEDIATE 'ALTER TABLE DOCUMENT modify (CREATE_DT TIMESTAMP DEFAULT SYSTIMESTAMP, MODIFY_DT TIMESTAMP)';
 
END;