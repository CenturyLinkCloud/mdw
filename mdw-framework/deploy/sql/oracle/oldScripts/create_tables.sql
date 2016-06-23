spool create_tables.lst;
undefine data_tbs_name
undefine lob_tbs_name
undefine lob_index_tbs_name

-- general rules for ID length: 
--    definition 8
--    proc instance 16
--    act/trans/var/doc instance 20
--    status 2
--    var type 4
--    owner type, user name: 30
--    activity/process name: 80

CREATE TABLE WORK_TRANSITION_INSTANCE
(
  WORK_TRANS_INST_ID  NUMBER(20)                NOT NULL,
  WORK_TRANS_ID       NUMBER(8)                NOT NULL,
  PROCESS_INST_ID     NUMBER(16)                NOT NULL,
  STATUS_CD           NUMBER(2)                NOT NULL,
  START_DT            DATE,
  END_DT              DATE,
  CREATE_DT           DATE                      DEFAULT SYSDATE               NOT NULL,
  CREATE_USR          VARCHAR2(30 BYTE)         DEFAULT USER                  NOT NULL,
  MOD_DT              DATE,
  MOD_USR             VARCHAR2(30 BYTE),
  COMMENTS            VARCHAR2(1000 BYTE),
  DEST_INST_ID        NUMBER(20)
)
TABLESPACE &&data_tbs_name
PCTUSED    0
PCTFREE    10
INITRANS   1
MAXTRANS   255
STORAGE    (
            INITIAL          64K
            MINEXTENTS       1
            MAXEXTENTS       UNLIMITED
            PCTINCREASE      0
            BUFFER_POOL      DEFAULT
           )
NOCOMPRESS 
MONITORING;

CREATE TABLE USER_GROUP_MAPPING
(
  USER_GROUP_MAPPING_ID  NUMBER(8)             NOT NULL,
  USER_INFO_ID           NUMBER(8)             NOT NULL,
  USER_GROUP_ID          NUMBER(8)             NOT NULL,
  CREATE_DT              DATE                   DEFAULT SYSDATE               NOT NULL,
  CREATE_USR             VARCHAR2(30 BYTE)      DEFAULT USER                  NOT NULL,
  MOD_DT                 DATE,
  MOD_USR                VARCHAR2(30 BYTE),
  COMMENTS               VARCHAR2(1000 BYTE)
)
TABLESPACE &&data_tbs_name
PCTUSED    0
PCTFREE    10
INITRANS   1
MAXTRANS   255
STORAGE    (
            INITIAL          64K
            MINEXTENTS       1
            MAXEXTENTS       UNLIMITED
            PCTINCREASE      0
            BUFFER_POOL      DEFAULT
           )
NOCOMPRESS 
MONITORING;

CREATE TABLE VARIABLE_TYPE
(
  VARIABLE_TYPE_ID       NUMBER(4)             NOT NULL,
  VARIABLE_TYPE_NAME     VARCHAR2(80 BYTE)    NOT NULL,
  CREATE_DT              DATE                   DEFAULT SYSDATE               NOT NULL,
  CREATE_USR             VARCHAR2(30 BYTE)      DEFAULT USER                  NOT NULL,
  MOD_DT                 DATE,
  MOD_USR                VARCHAR2(30 BYTE),
  COMMENTS               VARCHAR2(1000 BYTE),
  TRANSLATOR_CLASS_NAME  VARCHAR2(1000 BYTE)    NOT NULL
)
TABLESPACE &&data_tbs_name
PCTUSED    0
PCTFREE    10
INITRANS   1
MAXTRANS   255
STORAGE    (
            INITIAL          64K
            MINEXTENTS       1
            MAXEXTENTS       UNLIMITED
            PCTINCREASE      0
            BUFFER_POOL      DEFAULT
           )
NOCOMPRESS 
MONITORING;

CREATE TABLE USER_ROLE
(
  USER_ROLE_ID    NUMBER(8)                    NOT NULL,
  USER_ROLE_NAME  VARCHAR2(80 BYTE)           NOT NULL,
  CREATE_DT       DATE                          DEFAULT SYSDATE               NOT NULL,
  CREATE_USR      VARCHAR2(30 BYTE)             DEFAULT USER                  NOT NULL,
  MOD_DT          DATE,
  MOD_USR         VARCHAR2(30 BYTE),
  COMMENTS        VARCHAR2(1000 BYTE)
)
TABLESPACE &&data_tbs_name
PCTUSED    0
PCTFREE    10
INITRANS   1
MAXTRANS   255
STORAGE    (
            INITIAL          64K
            MINEXTENTS       1
            MAXEXTENTS       UNLIMITED
            PCTINCREASE      0
            BUFFER_POOL      DEFAULT
           )
NOCOMPRESS 
MONITORING;


CREATE TABLE TASK_STATUS
(
  TASK_STATUS_ID    NUMBER(2)                  NOT NULL,
  TASK_STATUS_DESC  VARCHAR2(1000 BYTE)         NOT NULL
)
TABLESPACE &&data_tbs_name
PCTUSED    0
PCTFREE    10
INITRANS   1
MAXTRANS   255
STORAGE    (
            INITIAL          64K
            MINEXTENTS       1
            MAXEXTENTS       UNLIMITED
            PCTINCREASE      0
            BUFFER_POOL      DEFAULT
           )
NOCOMPRESS 
MONITORING;


CREATE TABLE WORK_TRANSITION_STATUS
(
  WORK_TRANSITION_STATUS_ID    NUMBER(2)       NOT NULL,
  WORK_TRANSITION_STATUS_DESC  VARCHAR2(1000 BYTE) NOT NULL
)
TABLESPACE &&data_tbs_name
PCTUSED    0
PCTFREE    10
INITRANS   1
MAXTRANS   255
STORAGE    (
            INITIAL          64K
            MINEXTENTS       1
            MAXEXTENTS       UNLIMITED
            PCTINCREASE      0
            BUFFER_POOL      DEFAULT
           )
NOCOMPRESS 
MONITORING;

CREATE TABLE TASK
(
  TASK_ID           NUMBER(8)                  NOT NULL,
  TASK_NAME         VARCHAR2(1000 BYTE)         NOT NULL,
  CREATE_DT         DATE                        DEFAULT SYSDATE               NOT NULL,
  CREATE_USR        VARCHAR2(30 BYTE)           DEFAULT USER                  NOT NULL,
  MOD_DT            DATE,
  MOD_USR           VARCHAR2(30 BYTE),
  COMMENTS          VARCHAR2(1000 BYTE),
  TASK_TYPE_ID      NUMBER(1)                  DEFAULT 1                     NOT NULL,
  TASK_CATEGORY_ID  NUMBER(20)                  DEFAULT 1                     NOT NULL,
  LOGICAL_ID		VARCHAR2(128 BYTE)
)
TABLESPACE &&data_tbs_name
PCTUSED    0
PCTFREE    10
INITRANS   1
MAXTRANS   255
STORAGE    (
            INITIAL          64K
            MINEXTENTS       1
            MAXEXTENTS       UNLIMITED
            PCTINCREASE      0
            BUFFER_POOL      DEFAULT
           )
NOCOMPRESS 
MONITORING;

CREATE TABLE USER_INFO
(
  USER_INFO_ID  NUMBER(8)                      NOT NULL,
  CUID          VARCHAR2(30 BYTE)               NOT NULL,
  CREATE_DT     DATE                            DEFAULT SYSDATE               NOT NULL,
  CREATE_USR    VARCHAR2(30 BYTE)               DEFAULT USER                  NOT NULL,
  MOD_DT        DATE,
  MOD_USR       VARCHAR2(30 BYTE),
  END_DATE		DATE,
  NAME			VARCHAR2(30),
  COMMENTS      VARCHAR2(1000 BYTE)
)
TABLESPACE &&data_tbs_name
PCTUSED    0
PCTFREE    10
INITRANS   1
MAXTRANS   255
STORAGE    (
            INITIAL          64K
            MINEXTENTS       1
            MAXEXTENTS       UNLIMITED
            PCTINCREASE      0
            BUFFER_POOL      DEFAULT
           )
NOCOMPRESS 
MONITORING;

CREATE TABLE WORK_STATUS
(
  WORK_STATUS_ID    NUMBER(2)                  NOT NULL,
  WORK_STATUS_DESC  VARCHAR2(1000 BYTE)         NOT NULL
)
TABLESPACE &&data_tbs_name
PCTUSED    0
PCTFREE    10
INITRANS   1
MAXTRANS   255
STORAGE    (
            INITIAL          64K
            MINEXTENTS       1
            MAXEXTENTS       UNLIMITED
            PCTINCREASE      0
            BUFFER_POOL      DEFAULT
           )
NOCOMPRESS 
MONITORING;


CREATE TABLE EVENT_TYPE
(
  EVENT_TYPE_ID    NUMBER(1)                   NOT NULL,
  EVENT_TYPE_DESC  VARCHAR2(1000 BYTE)          NOT NULL
)
TABLESPACE &&data_tbs_name
PCTUSED    0
PCTFREE    10
INITRANS   1
MAXTRANS   255
STORAGE    (
            INITIAL          64K
            MINEXTENTS       1
            MAXEXTENTS       UNLIMITED
            PCTINCREASE      0
            BUFFER_POOL      DEFAULT
           )
NOCOMPRESS 
MONITORING;


CREATE TABLE TASK_INSTANCE
(
  TASK_INSTANCE_ID              NUMBER(20)      NOT NULL,
  TASK_ID                       NUMBER(16)      NOT NULL,
  TASK_INSTANCE_STATUS          NUMBER(2)      NOT NULL,
  TASK_INSTANCE_OWNER           VARCHAR2(30 BYTE) NOT NULL,
  TASK_INSTANCE_OWNER_ID        NUMBER(20)      NOT NULL,
  TASK_CLAIM_USER_ID            NUMBER(8),
  CREATE_DT                     DATE            DEFAULT SYSDATE               NOT NULL,
  CREATE_USR                    VARCHAR2(30 BYTE) DEFAULT USER NOT NULL,
  MOD_DT                        DATE,
  MOD_USR                       VARCHAR2(30 BYTE),
  COMMENTS                      VARCHAR2(1000 BYTE),
  TASK_START_DT                 DATE,
  TASK_END_DT                   DATE,
  TASK_INSTANCE_STATE           NUMBER(1)      DEFAULT 1                     NOT NULL,
  TASK_INSTANCE_REFERRED_AS     VARCHAR2(80 BYTE),
  TASK_INST_SECONDARY_OWNER     VARCHAR2(30 BYTE),
  TASK_INST_SECONDARY_OWNER_ID  NUMBER(20),
  OWNER_APP_NAME                VARCHAR2(1000 BYTE),
  ASSOCIATED_TASK_INST_ID       NUMBER(20),
  DUE_DATE 						DATE,
  PRIORITY						NUMBER(3),
  MASTER_REQUEST_ID				VARCHAR2(128 BYTE)
)
TABLESPACE &&data_tbs_name
PCTUSED    0
PCTFREE    10
INITRANS   1
MAXTRANS   255
STORAGE    (
            INITIAL          64K
            MINEXTENTS       1
            MAXEXTENTS       UNLIMITED
            PCTINCREASE      0
            BUFFER_POOL      DEFAULT
           )
NOCOMPRESS 
MONITORING;


CREATE TABLE VARIABLE_INSTANCE
(
  VARIABLE_INST_ID  NUMBER(20)                  NOT NULL,
  VARIABLE_ID       NUMBER(8)                  NOT NULL,
  PROCESS_INST_ID   NUMBER(16)                  NOT NULL,
  CREATE_DT         DATE                        DEFAULT SYSDATE               NOT NULL,
  CREATE_USR        VARCHAR2(30 BYTE)           DEFAULT USER                  NOT NULL,
  MOD_DT            DATE,
  MOD_USR           VARCHAR2(30 BYTE),
  COMMENTS          VARCHAR2(1000 BYTE),
  VARIABLE_VALUE    VARCHAR2(4000 BYTE),
  VARIABLE_NAME		VARCHAR2(80 BYTE),
  VARIABLE_TYPE_ID	NUMBER(4)
)
TABLESPACE &&data_tbs_name
PCTUSED    0
PCTFREE    10
INITRANS   1
MAXTRANS   255
STORAGE    (
            INITIAL          64K
            MINEXTENTS       1
            MAXEXTENTS       UNLIMITED
            PCTINCREASE      0
            BUFFER_POOL      DEFAULT
           )
NOCOMPRESS 
MONITORING;

CREATE TABLE USER_GROUP
(
  USER_GROUP_ID  NUMBER(8)                     NOT NULL,
  GROUP_NAME     VARCHAR2(80 BYTE)              NOT NULL,
  CREATE_DT      DATE                           DEFAULT SYSDATE               NOT NULL,
  CREATE_USR     VARCHAR2(30 BYTE)              DEFAULT USER                  NOT NULL,
  MOD_DT         DATE,
  MOD_USR        VARCHAR2(30 BYTE),
  END_DATE		 DATE,
  PARENT_GROUP_ID	NUMBER(8),
  COMMENTS       VARCHAR2(1000 BYTE)
)
TABLESPACE &&data_tbs_name
PCTUSED    0
PCTFREE    10
INITRANS   1
MAXTRANS   255
STORAGE    (
            INITIAL          64K
            MINEXTENTS       1
            MAXEXTENTS       UNLIMITED
            PCTINCREASE      0
            BUFFER_POOL      DEFAULT
           )
NOCOMPRESS 
MONITORING;


CREATE TABLE USER_ROLE_MAPPING
(
  USER_ROLE_MAPPING_ID        NUMBER(8)        NOT NULL,
  USER_ROLE_MAPPING_OWNER     VARCHAR2(16 BYTE) NOT NULL,
  USER_ROLE_MAPPING_OWNER_ID  NUMBER(8)        NOT NULL,
  CREATE_DT                   DATE              DEFAULT SYSDATE               NOT NULL,
  CREATE_USR                  VARCHAR2(30 BYTE) DEFAULT USER NOT NULL,
  MOD_DT                      DATE,
  MOD_USR                     VARCHAR2(30 BYTE),
  COMMENTS                    VARCHAR2(1000 BYTE),
  USER_ROLE_ID                NUMBER(8)        NOT NULL
)
TABLESPACE &&data_tbs_name
PCTUSED    0
PCTFREE    10
INITRANS   1
MAXTRANS   255
STORAGE    (
            INITIAL          64K
            MINEXTENTS       1
            MAXEXTENTS       UNLIMITED
            PCTINCREASE      0
            BUFFER_POOL      DEFAULT
           )
NOCOMPRESS 
MONITORING;


CREATE TABLE ACTIVITY_IMPLEMENTOR
(
  ACTIVITY_IMPLEMENTOR_ID  NUMBER(8)           NOT NULL,
  IMPL_CLASS_NAME          VARCHAR2(256 BYTE)  NOT NULL
)
TABLESPACE &&data_tbs_name
PCTUSED    0
PCTFREE    10
INITRANS   1
MAXTRANS   255
STORAGE    (
            INITIAL          64K
            MINEXTENTS       1
            MAXEXTENTS       UNLIMITED
            PCTINCREASE      0
            BUFFER_POOL      DEFAULT
           )
NOCOMPRESS 
MONITORING;



CREATE TABLE TASK_TYPE
(
  TASK_TYPE_ID    NUMBER(1)                    NOT NULL,
  TASK_TYPE_DESC  VARCHAR2(1000 BYTE)           NOT NULL
)
TABLESPACE &&data_tbs_name
PCTUSED    0
PCTFREE    10
INITRANS   1
MAXTRANS   255
STORAGE    (
            INITIAL          64K
            MINEXTENTS       1
            MAXEXTENTS       UNLIMITED
            PCTINCREASE      0
            BUFFER_POOL      DEFAULT
           )
NOCOMPRESS 
MONITORING;


CREATE TABLE TASK_CATEGORY
(
  TASK_CATEGORY_ID    NUMBER(20)                NOT NULL,
  TASK_CATEGORY_CD    VARCHAR2(30 BYTE)       NOT NULL,
  TASK_CATEGORY_DESC  VARCHAR2(1000 BYTE)
)
TABLESPACE &&data_tbs_name
PCTUSED    0
PCTFREE    10
INITRANS   1
MAXTRANS   255
STORAGE    (
            INITIAL          64K
            MINEXTENTS       1
            MAXEXTENTS       UNLIMITED
            PCTINCREASE      0
            BUFFER_POOL      DEFAULT
           )
NOCOMPRESS 
MONITORING;



CREATE TABLE ATTACHMENT
(
  ATTACHMENT_ID            NUMBER(20),
  ATTACHMENT_OWNER         VARCHAR2(30 BYTE)  NOT NULL,
  ATTACHMENT_OWNER_ID      NUMBER(20)           NOT NULL,
  ATTACHMENT_NAME          VARCHAR2(1000 BYTE)  NOT NULL,
  ATTACHMENT_LOCATION      VARCHAR2(1000 BYTE),
  ATTACHMENT_STATUS        NUMBER(1)           NOT NULL,
  CREATE_DT                DATE                 DEFAULT SYSDATE               NOT NULL,
  CREATE_USR               VARCHAR2(30 BYTE)    DEFAULT USER                  NOT NULL,
  MOD_DT                   DATE,
  MOD_USR                  VARCHAR2(30 BYTE),
  COMMENTS                 VARCHAR2(1000 BYTE),
  ATTACHMENT_CONTENT_TYPE  VARCHAR2(1000 BYTE)  NOT NULL
)
TABLESPACE &&data_tbs_name
PCTUSED    0
PCTFREE    10
INITRANS   1
MAXTRANS   255
STORAGE    (
            INITIAL          64K
            MINEXTENTS       1
            MAXEXTENTS       UNLIMITED
            PCTINCREASE      0
            BUFFER_POOL      DEFAULT
           )
NOCOMPRESS 
MONITORING;

CREATE TABLE TASK_STATE
(
  TASK_STATE_ID    NUMBER(1)                   NOT NULL,
  TASK_STATE_DESC  VARCHAR2(1000 BYTE)          NOT NULL
)
TABLESPACE &&data_tbs_name
PCTUSED    0
PCTFREE    10
INITRANS   1
MAXTRANS   255
STORAGE    (
            INITIAL          64K
            MINEXTENTS       1
            MAXEXTENTS       UNLIMITED
            PCTINCREASE      0
            BUFFER_POOL      DEFAULT
           )
NOCOMPRESS 
MONITORING;

CREATE TABLE EXTERNAL_EVENT
(
  EXTERNAL_EVENT_ID  NUMBER(8)                 NOT NULL,
  EVENT_NAME         VARCHAR2(1000 BYTE)        NOT NULL,
  EVENT_HANDLER      VARCHAR2(1000 BYTE)        NOT NULL,
  CREATE_DT          DATE                       DEFAULT SYSDATE               NOT NULL,
  CREATE_USR         VARCHAR2(30 BYTE)          DEFAULT USER                  NOT NULL,
  MOD_DT             DATE,
  MOD_USR            VARCHAR2(30 BYTE),
  COMMENTS           VARCHAR2(1000 BYTE)
)
TABLESPACE &&data_tbs_name
PCTUSED    0
PCTFREE    10
INITRANS   1
MAXTRANS   255
STORAGE    (
            INITIAL          64K
            MINEXTENTS       1
            MAXEXTENTS       UNLIMITED
            PCTINCREASE      0
            BUFFER_POOL      DEFAULT
           )
NOCOMPRESS 
MONITORING;

CREATE TABLE ACTIVITY_INSTANCE
(
  ACTIVITY_INSTANCE_ID  NUMBER(20)              NOT NULL,
  ACTIVITY_ID           NUMBER(8)              NOT NULL,
  PROCESS_INSTANCE_ID   NUMBER(16)              NOT NULL,
  STATUS_CD             NUMBER(2)              NOT NULL,
  START_DT              DATE                    DEFAULT SYSDATE               NOT NULL,
  END_DT                DATE,
  CREATE_DT             DATE                    DEFAULT SYSDATE               NOT NULL,
  CREATE_USR            VARCHAR2(30 BYTE)       DEFAULT USER                  NOT NULL,
  MOD_DT                DATE,
  MOD_USR               VARCHAR2(30 BYTE),
  COMMENTS              VARCHAR2(1000 BYTE),
  STATUS_MESSAGE        VARCHAR2(4000 BYTE),
  COMPCODE				VARCHAR2(80 BYTE),  
  ENGINE_ID				VARCHAR(8)
)
TABLESPACE &&data_tbs_name
PCTUSED    0
PCTFREE    10
INITRANS   1
MAXTRANS   255
STORAGE    (
            INITIAL          64K
            MINEXTENTS       1
            MAXEXTENTS       UNLIMITED
            PCTINCREASE      0
            BUFFER_POOL      DEFAULT
           )
NOCOMPRESS 
MONITORING;



CREATE TABLE PROCESS_INSTANCE
(
  PROCESS_INSTANCE_ID  NUMBER(16)               NOT NULL,
  PROCESS_ID           NUMBER(16)               NOT NULL,
  OWNER                VARCHAR2(30 BYTE)      NOT NULL,
  OWNER_ID             NUMBER(16)               NOT NULL,
  SECONDARY_OWNER      VARCHAR2(30 BYTE),
  SECONDARY_OWNER_ID   NUMBER(20),
  STATUS_CD            NUMBER(2)               NOT NULL,
  START_DT             DATE                     DEFAULT SYSDATE               NOT NULL,
  END_DT               DATE,
  CREATE_DT            DATE                     DEFAULT SYSDATE               NOT NULL,
  CREATE_USR           VARCHAR2(30 BYTE)        DEFAULT USER                  NOT NULL,
  MOD_DT               DATE,
  MOD_USR              VARCHAR2(30 BYTE),
  COMMENTS             VARCHAR2(1000 BYTE),
  MASTER_REQUEST_ID    VARCHAR2(80 BYTE),
  COMPCODE			   VARCHAR2(80 BYTE)
)
TABLESPACE &&data_tbs_name
PCTUSED    0
PCTFREE    10
INITRANS   1
MAXTRANS   255
STORAGE    (
            INITIAL          64K
            MINEXTENTS       1
            MAXEXTENTS       UNLIMITED
            PCTINCREASE      0
            BUFFER_POOL      DEFAULT
           )
NOCOMPRESS 
MONITORING;



CREATE TABLE RULE_SET
(
  RULE_SET_ID       NUMBER(8)                  NOT NULL,
  RULE_SET_NAME     VARCHAR2(80 BYTE)         NOT NULL,
  RULE_SET_DETAILS  CLOB                        NOT NULL,
  CREATE_DT         DATE                        DEFAULT SYSDATE               NOT NULL,
  CREATE_USR        VARCHAR2(30 BYTE)           DEFAULT USER                  NOT NULL,
  MOD_DT            DATE,
  MOD_USR           VARCHAR2(30 BYTE),
  COMMENTS          VARCHAR2(1000 BYTE),
  LANGUAGE			VARCHAR2(30 BYTE),
  VERSION_NO        NUMBER(6),
  REFRESH_INTERVAL  NUMBER(6)                  DEFAULT 24                    NOT NULL,
  GROUP_NAME		VARCHAR2(80 BYTE),
  OWNER_ID 	 		    NUMBER(20),
  OWNER_TYPE        VARCHAR2(30 BYTE)
)
TABLESPACE &&data_tbs_name
PCTUSED    0
PCTFREE    10
INITRANS   1
MAXTRANS   255
STORAGE    (
            INITIAL          64K
            MINEXTENTS       1
            MAXEXTENTS       UNLIMITED
            PCTINCREASE      0
            BUFFER_POOL      DEFAULT
           )
LOB (RULE_SET_DETAILS)
    STORE AS RULE_SET_DT_LOB_SEG (
        TABLESPACE &&lob_tbs_name
        CHUNK 4096
        CACHE
        STORAGE (MINEXTENTS 1)
        INDEX RULE_SET_DT_LOB_IDX (
            TABLESPACE &&lob_index_tbs_name
            STORAGE (MAXEXTENTS UNLIMITED)
        )
    )           
NOCOMPRESS 
MONITORING;


CREATE TABLE ATTRIBUTE
(
  ATTRIBUTE_ID        NUMBER(8)                NOT NULL,
  ATTRIBUTE_OWNER     VARCHAR2(30 BYTE)       NOT NULL,
  ATTRIBUTE_OWNER_ID  NUMBER(8)                NOT NULL,
  ATTRIBUTE_NAME      VARCHAR2(80 BYTE)       NOT NULL,
  ATTRIBUTE_VALUE     VARCHAR2(4000 BYTE),
  CREATE_DT           DATE                      DEFAULT SYSDATE               NOT NULL,
  CREATE_USR          VARCHAR2(30 BYTE)         DEFAULT USER                  NOT NULL,
  MOD_DT              DATE,
  MOD_USR             VARCHAR2(30 BYTE),
  COMMENTS            VARCHAR2(1000 BYTE)
)
TABLESPACE &&data_tbs_name
PCTUSED    0
PCTFREE    10
INITRANS   1
MAXTRANS   255
STORAGE    (
            INITIAL          64K
            MINEXTENTS       1
            MAXEXTENTS       UNLIMITED
            PCTINCREASE      0
            BUFFER_POOL      DEFAULT
           )
NOCOMPRESS 
MONITORING;


CREATE TABLE EVENT_WAIT_INSTANCE
(
  EVENT_WAIT_INSTANCE_ID        NUMBER(20)      NOT NULL,
  EVENT_NAME                    VARCHAR2(1000 BYTE) NOT NULL,
  EVENT_WAIT_INSTANCE_OWNER_ID  NUMBER(20)      NOT NULL,
  EVENT_WAIT_INSTANCE_OWNER     VARCHAR2(30 BYTE) NOT NULL,
  EVENT_SOURCE                  VARCHAR2(80 BYTE) NOT NULL,
  WORK_TRANS_INSTANCE_ID        NUMBER(20)      NOT NULL,
  WAKE_UP_EVENT                 VARCHAR2(30 BYTE) NOT NULL,
  STATUS_CD                     NUMBER(2)      NOT NULL,
  CREATE_DT                     DATE            DEFAULT SYSDATE               NOT NULL,
  CREATE_USR                    VARCHAR2(30 BYTE) DEFAULT USER NOT NULL,
  MOD_DT                        DATE,
  MOD_USR                       VARCHAR2(30 BYTE),
  COMMENTS                      VARCHAR2(1000 BYTE)
)
TABLESPACE &&data_tbs_name
PCTUSED    0
PCTFREE    10
INITRANS   1
MAXTRANS   255
STORAGE    (
            INITIAL          64K
            MINEXTENTS       1
            MAXEXTENTS       UNLIMITED
            PCTINCREASE      0
            BUFFER_POOL      DEFAULT
           )
NOCOMPRESS 
MONITORING;

CREATE TABLE EVENT_INSTANCE
(
  EVENT_NAME                    VARCHAR(1000) NOT NULL,
  DOCUMENT_ID 					NUMBER(20),
  STATUS_CD                     NUMBER(2) NOT NULL,
  CREATE_DT                     DATE DEFAULT SYSDATE NOT NULL,
  CONSUME_DT                    DATE,
  PRESERVE_INTERVAL				NUMBER(10),
  AUXDATA						VARCHAR2(4000),
  REFERENCE						VARCHAR2(1000),
  COMMENTS                      VARCHAR(1000)
)
TABLESPACE &&data_tbs_name
PCTUSED    0
PCTFREE    10
INITRANS   1
MAXTRANS   255
STORAGE    (
            INITIAL          64K
            MINEXTENTS       1
            MAXEXTENTS       UNLIMITED
            PCTINCREASE      0
            BUFFER_POOL      DEFAULT
           )
NOCOMPRESS 
MONITORING;


CREATE TABLE INSTANCE_NOTE
(
  INSTANCE_NOTE_ID        NUMBER(20),
  INSTANCE_NOTE_OWNER_ID  NUMBER(20)            NOT NULL,
  INSTANCE_NOTE_OWNER     VARCHAR2(30 BYTE)    NOT NULL,
  INSTANCE_NOTE_NAME      VARCHAR2(256 BYTE)    NOT NULL,
  INSTANCE_NOTE_DETAILS   VARCHAR2(4000 BYTE),
  CREATE_DT               DATE                  DEFAULT SYSDATE               NOT NULL,
  CREATE_USR              VARCHAR2(30 BYTE)     DEFAULT USER                  NOT NULL,
  MOD_DT                  DATE,
  MOD_USR                 VARCHAR2(30 BYTE),
  COMMENTS                VARCHAR2(1000 BYTE)
)
TABLESPACE &&data_tbs_name
PCTUSED    0
PCTFREE    10
INITRANS   1
MAXTRANS   255
STORAGE    (
            INITIAL          64K
            MINEXTENTS       1
            MAXEXTENTS       UNLIMITED
            PCTINCREASE      0
            BUFFER_POOL      DEFAULT
           )
NOCOMPRESS 
MONITORING;


CREATE TABLE EVENT_LOG
(
  EVENT_LOG_ID        NUMBER(24)                NOT NULL,
  EVENT_NAME          VARCHAR2(1000 BYTE)       NOT NULL,
  EVENT_LOG_OWNER_ID  NUMBER(20)                NOT NULL,
  EVENT_LOG_OWNER     VARCHAR2(30 BYTE)       NOT NULL,
  EVENT_SOURCE        VARCHAR2(80 BYTE)       NOT NULL,
  CREATE_DT           DATE                      DEFAULT SYSDATE               NOT NULL,
  CREATE_USR          VARCHAR2(30 BYTE)         DEFAULT USER                  NOT NULL,
  MOD_DT              DATE,
  MOD_USR             VARCHAR2(30 BYTE),
  COMMENTS            VARCHAR2(1000 BYTE),
  EVENT_CATEGORY      VARCHAR2(80 BYTE)       NOT NULL,
  STATUS_CD           NUMBER(2)                NOT NULL,
  EVENT_SUB_CATEGORY  VARCHAR2(80 BYTE)
)
TABLESPACE &&data_tbs_name
PCTUSED    0
PCTFREE    10
INITRANS   1
MAXTRANS   255
STORAGE    (
            INITIAL          64K
            MINEXTENTS       1
            MAXEXTENTS       UNLIMITED
            PCTINCREASE      0
            BUFFER_POOL      DEFAULT
           )
NOCOMPRESS 
MONITORING;


CREATE TABLE PACKAGE
(
  PACKAGE_ID      NUMBER(8),
  PACKAGE_NAME    VARCHAR2(80 BYTE),
  SCHEMA_VERSION  NUMBER(6),
  DATA_VERSION    NUMBER(10),
  MOD_DT          DATE,
  GROUP_NAME   	  VARCHAR2(80 BYTE),  
  EXPORTED_IND    NUMBER(1) DEFAULT '0' NOT NULL
)
TABLESPACE &&data_tbs_name
PCTUSED    0
PCTFREE    10
INITRANS   1
MAXTRANS   255
STORAGE    (
            INITIAL          64K
            MINEXTENTS       1
            MAXEXTENTS       2147483645
            PCTINCREASE      0
            BUFFER_POOL      DEFAULT
           )
LOGGING 
NOCOMPRESS 
NOCACHE
NOPARALLEL
MONITORING;

CREATE TABLE PACKAGE_EXTERNAL_EVENTS
(
  PACKAGE_ID  NUMBER(8),
  EXTERNAL_EVENT_ID  NUMBER(8)
)
TABLESPACE &&data_tbs_name
PCTUSED    0
PCTFREE    10
INITRANS   1
MAXTRANS   255
STORAGE    (
            INITIAL          64K
            MINEXTENTS       1
            MAXEXTENTS       2147483645
            PCTINCREASE      0
            BUFFER_POOL      DEFAULT
           )
LOGGING 
NOCOMPRESS 
NOCACHE
NOPARALLEL
MONITORING;


CREATE TABLE PACKAGE_ACTIVITY_IMPLEMENTORS
(
  PACKAGE_ID  NUMBER(8),
  ACTIVITY_IMPLEMENTOR_ID  NUMBER(8)
)
TABLESPACE &&data_tbs_name
PCTUSED    0
PCTFREE    10
INITRANS   1
MAXTRANS   255
STORAGE    (
            INITIAL          64K
            MINEXTENTS       1
            MAXEXTENTS       2147483645
            PCTINCREASE      0
            BUFFER_POOL      DEFAULT
           )
LOGGING 
NOCOMPRESS 
NOCACHE
NOPARALLEL
MONITORING;

CREATE TABLE PACKAGE_RULESETS
(
  PACKAGE_ID  NUMBER(8),
  RULE_SET_ID  NUMBER(8)
)
TABLESPACE &&data_tbs_name
PCTUSED    0
PCTFREE    10
INITRANS   1
MAXTRANS   255
STORAGE    (
            INITIAL          64K
            MINEXTENTS       1
            MAXEXTENTS       2147483645
            PCTINCREASE      0
            BUFFER_POOL      DEFAULT
           )
LOGGING 
NOCOMPRESS 
NOCACHE
NOPARALLEL
MONITORING;

CREATE TABLE DOCUMENT
(
  DOCUMENT_ID		NUMBER(20)					NOT NULL,
  PROCESS_INST_ID   NUMBER(16)                  NOT NULL,
  OWNER_TYPE		VARCHAR2(30 BYTE)			NOT NULL,
  OWNER_ID			NUMBER(20)					NOT NULL,
  DOCUMENT_TYPE     VARCHAR2(80 BYTE),
  CREATE_DT         DATE                        DEFAULT SYSDATE               NOT NULL,
  MODIFY_DT			DATE,
  SEARCH_KEY1		VARCHAR2(256 BYTE),
  SEARCH_KEY2		VARCHAR2(256 BYTE),
  CONTENT			CLOB						NOT NULL
)
TABLESPACE &&data_tbs_name
PCTUSED    0
PCTFREE    10
INITRANS   1
MAXTRANS   255
STORAGE    (
            INITIAL          64K
            MINEXTENTS       1
            MAXEXTENTS       2147483645
            PCTINCREASE      0
            BUFFER_POOL      DEFAULT
           )
LOB (CONTENT)
    STORE AS CONTENT_LOB_SEG (
        TABLESPACE &&lob_tbs_name
        CHUNK 4096
        CACHE
        STORAGE (MINEXTENTS 1)
        INDEX CONTENT_LOB_IDX (
            TABLESPACE &&lob_index_tbs_name
            STORAGE (MAXEXTENTS UNLIMITED)
        )
    )           
LOGGING 
NOCOMPRESS 
NOCACHE
NOPARALLEL
MONITORING;

CREATE TABLE TASK_INST_GRP_MAPP (
  TASK_INSTANCE_ID 			NUMBER(38)	NOT NULL,
  USER_GROUP_ID  			NUMBER(38)	NOT NULL,
  CREATE_DT 		DATE DEFAULT SYSDATE NOT NULL
)
TABLESPACE &&data_tbs_name
PCTUSED    0
PCTFREE    10
INITRANS   1
MAXTRANS   255
STORAGE    (
            INITIAL          64K
            MINEXTENTS       1
            MAXEXTENTS       UNLIMITED
            PCTINCREASE      0
            BUFFER_POOL      DEFAULT
           )
NOCOMPRESS
MONITORING;

CREATE TABLE TASK_INST_INDEX (
  TASK_INSTANCE_ID 			NUMBER(38)	NOT NULL,
  INDEX_KEY					VARCHAR2(64) NOT NULL,
  INDEX_VALUE				VARCHAR2(256)	NOT NULL,
  CREATE_DT 		DATE DEFAULT SYSDATE NOT NULL
)
TABLESPACE &&data_tbs_name
PCTUSED    0
PCTFREE    10
INITRANS   1
MAXTRANS   255
STORAGE    (
            INITIAL          64K
            MINEXTENTS       1
            MAXEXTENTS       UNLIMITED
            PCTINCREASE      0
            BUFFER_POOL      DEFAULT
           )
NOCOMPRESS
MONITORING;

CREATE TABLE RESOURCE_TYPE
(
  RESOURCE_TYPE_NAME    VARCHAR2(32),
  FILE_SUFFIX			VARCHAR2(16),
  IS_BINARY				NUMBER(1),
  RESOURCE_TYPE_DESC	VARCHAR2(512)
)
TABLESPACE &&data_tbs_name
PCTUSED    0
PCTFREE    10
INITRANS   1
MAXTRANS   255
STORAGE    (
            INITIAL          64K
            MINEXTENTS       1
            MAXEXTENTS       UNLIMITED
            PCTINCREASE      0
            BUFFER_POOL      DEFAULT
           )
NOCOMPRESS
MONITORING;

undefine data_tbs_name
undefine lob_tbs_name
undefine lob_index_tbs_name
spool off;
