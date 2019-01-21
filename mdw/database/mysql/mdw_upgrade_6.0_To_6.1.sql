DROP procedure IF EXISTS `upgrade`;

DELIMITER $$
CREATE PROCEDURE `upgrade`()
BEGIN
  DECLARE t_exists   INT;
  DECLARE c_exists INT;
  DECLARE def_value varchar(14)  DEFAULT  'TASK INSTANCE';
  DECLARE nullCheck varchar(3);
BEGIN
  select ("MDW 6.1 Upgrade Script");
SELECT 
    COUNT(*)
INTO t_exists FROM   
    information_schema.tables
WHERE
    table_name = 'ASSET_REF';
  IF t_exists = 0 THEN
  
  CREATE TABLE ASSET_REF (
         DEFINITION_ID    	  BIGINT                    NOT NULL,
         NAME	 		      VARCHAR(512)              NOT NULL,
         REF           	      VARCHAR(64)               NOT NULL,
         ARCHIVE_DT           TIMESTAMP			        NOT NULL,
  PRIMARY KEY (DEFINITION_ID));
  CREATE INDEX ASSET_REF_NAME_IDX  ON ASSET_REF (NAME);
  CREATE INDEX ASSET_REF_ARCHIVE_DT_IDX   
    ON ASSET_REF (ARCHIVE_DT);
SELECT ('ASSET_REF table created');
 END IF;
SELECT 
    COUNT(*)
INTO c_exists FROM
    information_schema.columns
WHERE
    table_name = 'DOCUMENT'
        AND COLUMN_NAME = 'PATH';
  IF c_exists     = 0 THEN
    ALTER TABLE DOCUMENT ADD(PATH VARCHAR(1000));
SELECT ('Document table altered');
  END IF;
SELECT 
    COUNT(*)
INTO t_exists FROM
    information_schema.tables
WHERE
    table_name = 'INSTANCE_INDEX';
  IF t_exists = 0 THEN
    SELECT count(*)
    INTO c_exists FROM
    information_schema.statistics WHERE table_name = 'TASK_INST_INDEX' and index_name='TASK_INST_INDEX_PK';
 --   IF c_exists = 1 THEN
SELECT 
    COUNT(*)
INTO c_exists FROM
    information_schema.statistics
WHERE
    table_name = 'TASK_INST_INDEX'
        AND index_name = 'TASKINSTIDX_TASKINST_FK';
    IF c_exists = 1 THEN
	alter table TASK_INST_INDEX drop foreign key TASKINSTIDX_TASKINST_FK; 
    END IF;
    ALTER TABLE TASK_INST_INDEX change column TASK_INSTANCE_ID  INSTANCE_ID BIGINT NOT NULL;
  
    ALTER TABLE TASK_INST_INDEX add column OWNER_TYPE VARCHAR(30) DEFAULT 'TASK INSTANCE';
    
    ALTER TABLE TASK_INST_INDEX rename to INSTANCE_INDEX;
   
    CREATE INDEX INSTANCEIDX_IDXKEY_FK
    ON INSTANCE_INDEX (INDEX_KEY);
SELECT ('TASK_INST_INDEX table changed  INSTANCE_INDEX');
  END IF;
SELECT 
    COUNT(*)
INTO c_exists FROM
    information_schema.columns
WHERE
    table_name = 'TASK_INSTANCE'
        AND COLUMN_NAME = 'TASK_TITLE';
  IF c_exists     = 0 THEN
    ALTER TABLE TASK_INSTANCE ADD(TASK_TITLE VARCHAR(512));
SELECT ('TASK_INSTANCE table altered');
  END IF;
SELECT 
    COUNT(*)
INTO c_exists FROM
    information_schema.columns
WHERE
    table_name = 'ATTACHMENT'
        AND COLUMN_NAME = 'ATTACHMENT_STATUS';
  IF c_exists     = 1 THEN
   ALTER TABLE ATTACHMENT DROP column ATTACHMENT_STATUS, DROP column COMMENTS;
SELECT ('ATTACHMENT table altered');
  END IF;
SELECT 
    COUNT(*)
INTO c_exists FROM
    information_schema.columns
WHERE
    table_name = 'INSTANCE_NOTE'
        AND COLUMN_NAME = 'COMMENTS';
  IF c_exists     = 1 THEN
    ALTER TABLE INSTANCE_NOTE DROP COLUMN COMMENTS;
SELECT ('INSTANCE_NOTE table altered');
  END IF;
  -- Check for not null constraint,otherwise it throws ORA-01442: column to be modified to NOT NULL is already NOT NULL
SELECT 
    is_nullable
INTO nullCheck FROM
    INFORMATION_SCHEMA.COLUMNS
WHERE
    table_name = 'ATTACHMENT'
        AND column_name = 'ATTACHMENT_CONTENT_TYPE';
  IF nullCheck = 'NO' THEN
    ALTER TABLE ATTACHMENT 
    MODIFY column CREATE_USR VARCHAR(100),  
    MODIFY column ATTACHMENT_CONTENT_TYPE VARCHAR(1000) NULL;
SELECT ('ATTACHMENT table altered');
  END IF;
  
SELECT 
    is_nullable
INTO nullCheck FROM
    INFORMATION_SCHEMA.COLUMNS
WHERE
    table_name = 'INSTANCE_NOTE'
        AND column_name = 'INSTANCE_NOTE_NAME';
  IF nullCheck = 'NO' THEN
    ALTER TABLE INSTANCE_NOTE 
    MODIFY column CREATE_USR VARCHAR(100),
     MODIFY column INSTANCE_NOTE_NAME VARCHAR(256) NULL;
SELECT ('INSTANCE_NOTE table altered');
  END IF;
END;

 END$$

DELIMITER ;

ALTER TABLE TASK_INSTANCE 
modify column TASK_INSTANCE_REFERRED_AS VARCHAR(500),
modify column CREATE_DT DATETIME(6), 
modify column MOD_DT TIMESTAMP(6), 
modify column TASK_START_DT DATETIME(6), 
modify column TASK_END_DT DATETIME(6); 

ALTER TABLE ACTIVITY_INSTANCE 
modify column CREATE_DT DATETIME(6), 
modify column MOD_DT TIMESTAMP(6), 
modify column START_DT DATETIME(6), 
modify column END_DT DATETIME(6);

ALTER TABLE PROCESS_INSTANCE 
modify column CREATE_DT DATETIME(6), 
modify column MOD_DT TIMESTAMP(6), 
modify column START_DT DATETIME(6), 
modify column END_DT DATETIME(6);

ALTER TABLE EVENT_WAIT_INSTANCE  
modify column CREATE_DT DATETIME(6),
modify column  MOD_DT TIMESTAMP(6); 

ALTER TABLE EVENT_INSTANCE 
modify column CREATE_DT DATETIME(6), 
modify column CONSUME_DT DATETIME(6),
modify column STATUS_CD SMALLINT;

ALTER TABLE EVENT_LOG 
modify column CREATE_DT  DATETIME(6), 
modify column MOD_DT TIMESTAMP(6);

ALTER TABLE DOCUMENT 
modify column  CREATE_DT DATETIME(6), 
modify column  MODIFY_DT TIMESTAMP(6),
modify column  STATUS_CODE SMALLINT;

ALTER TABLE PROCESS_INSTANCE ADD TEMPLATE VARCHAR(256);

CREATE TABLE INSTANCE_TIMING
(
  INSTANCE_ID  BIGINT       NOT NULL,
  OWNER_TYPE   VARCHAR(30)  NOT NULL,
  ELAPSED_MS   BIGINT       NOT NULL
);

ALTER TABLE INSTANCE_TIMING ADD
(
  CONSTRAINT TIMING_PRIMARY_KEY PRIMARY KEY(INSTANCE_ID,OWNER_TYPE)
);

-- these reporting indexes may take a long time to run on existing dbs
--CREATE INDEX PROCINST_PROC_ID_IDX
--ON PROCESS_INSTANCE (PROCESS_ID);
--
--CREATE INDEX DOCUMENT_PATH_IDX
--ON DOCUMENT (PATH);