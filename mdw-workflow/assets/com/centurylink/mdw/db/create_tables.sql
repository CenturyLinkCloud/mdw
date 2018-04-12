CREATE TABLE WORK_TRANSITION_INSTANCE
(
  WORK_TRANS_INST_ID  BIGINT         PRIMARY KEY auto_increment,
  WORK_TRANS_ID       INT            NOT NULL,
  PROCESS_INST_ID     BIGINT         NOT NULL,
  STATUS_CD           TINYINT        NOT NULL,
  START_DT            DATETIME(6),
  END_DT              DATETIME(6),
  CREATE_DT           DATETIME(6)    NOT NULL,
  CREATE_USR          VARCHAR(30)    NOT NULL,
  MOD_DT              TIMESTAMP(6),
  MOD_USR             VARCHAR(30),
  COMMENTS            VARCHAR(1000), 
  DEST_INST_ID        BIGINT
) auto_increment=10000;

CREATE TABLE USER_GROUP_MAPPING
(
  USER_GROUP_MAPPING_ID  INT            PRIMARY KEY auto_increment,
  USER_INFO_ID           INT            NOT NULL,
  USER_GROUP_ID          INT            NOT NULL,
  CREATE_DT              DATETIME       NOT NULL,
  CREATE_USR             VARCHAR(30)    NOT NULL,
  MOD_DT                 TIMESTAMP,
  MOD_USR                VARCHAR(30),
  COMMENTS               VARCHAR(1000)
) auto_increment=1000;

CREATE TABLE USER_ROLE
(
  USER_ROLE_ID    INT               PRIMARY KEY auto_increment,
  USER_ROLE_NAME  VARCHAR(80)       NOT NULL,
  CREATE_DT       DATETIME          NOT NULL,
  CREATE_USR      VARCHAR(30)       NOT NULL,
  MOD_DT          TIMESTAMP,
  MOD_USR         VARCHAR(30),
  COMMENTS        VARCHAR(1000)
) auto_increment=1000;

CREATE TABLE USER_INFO
(
  USER_INFO_ID  INT                 PRIMARY KEY auto_increment,
  CUID          VARCHAR(128)        NOT NULL,
  CREATE_DT     DATETIME            NOT NULL,
  CREATE_USR    VARCHAR(30)         NOT NULL,
  MOD_DT        TIMESTAMP,
  MOD_USR       VARCHAR(30),  
  END_DATE      DATETIME,
  NAME          VARCHAR(30),
  COMMENTS      VARCHAR(1000)
) auto_increment=1000;

CREATE TABLE TASK_INSTANCE
(
  TASK_INSTANCE_ID              BIGINT       PRIMARY KEY auto_increment,
  TASK_ID                       BIGINT       NOT NULL,
  TASK_INSTANCE_STATUS          TINYINT      NOT NULL,
  TASK_INSTANCE_OWNER           VARCHAR(30)  NOT NULL,
  TASK_INSTANCE_OWNER_ID        BIGINT       NOT NULL,
  TASK_CLAIM_USER_ID            INT,
  CREATE_DT                     DATETIME(6)  NOT NULL,
  CREATE_USR                    VARCHAR(30)  NOT NULL,
  MOD_DT                        TIMESTAMP(6),
  MOD_USR                       VARCHAR(30),
  COMMENTS                      VARCHAR(1000),
  TASK_START_DT                 DATETIME(6),
  TASK_END_DT                   DATETIME(6),
  TASK_INSTANCE_STATE           TINYINT      DEFAULT 1 NOT NULL,
  TASK_INSTANCE_REFERRED_AS     VARCHAR(500),
  TASK_INST_SECONDARY_OWNER     VARCHAR(30),
  TASK_INST_SECONDARY_OWNER_ID  BIGINT,
  DUE_DATE                      DATETIME,
  PRIORITY                      TINYINT,
  MASTER_REQUEST_ID             VARCHAR(128),
  TASK_TITLE                    VARCHAR(512)
) auto_increment=10000;

CREATE TABLE VARIABLE_INSTANCE
(
  VARIABLE_INST_ID  BIGINT              PRIMARY KEY auto_increment,
  VARIABLE_ID       INT                 NOT NULL,
  PROCESS_INST_ID   BIGINT              NOT NULL,
  CREATE_DT         DATETIME(6)         NOT NULL,
  CREATE_USR        VARCHAR(30)         NOT NULL,
  MOD_DT            TIMESTAMP(6),
  MOD_USR           VARCHAR(30),
  COMMENTS          VARCHAR(1000),
  VARIABLE_VALUE    VARCHAR(4000),
  VARIABLE_NAME     VARCHAR(80),
  VARIABLE_TYPE_ID  SMALLINT
) auto_increment=10000;


CREATE TABLE USER_GROUP
(
  USER_GROUP_ID   INT                PRIMARY KEY auto_increment,
  GROUP_NAME      VARCHAR(80)        NOT NULL,
  CREATE_DT       DATETIME           NOT NULL,
  CREATE_USR      VARCHAR(30)        NOT NULL,
  MOD_DT          TIMESTAMP,
  MOD_USR         VARCHAR(30),  
  END_DATE        DATETIME,
  PARENT_GROUP_ID INT,
  COMMENTS        VARCHAR(1000)
) auto_increment=1000;


CREATE TABLE USER_ROLE_MAPPING
(
  USER_ROLE_MAPPING_ID        INT            PRIMARY KEY auto_increment,
  USER_ROLE_MAPPING_OWNER     VARCHAR(16)    NOT NULL,
  USER_ROLE_MAPPING_OWNER_ID  INT            NOT NULL,
  CREATE_DT                   DATETIME       NOT NULL,
  CREATE_USR                  VARCHAR(30)    NOT NULL,
  MOD_DT                      TIMESTAMP,
  MOD_USR                     VARCHAR(30),
  COMMENTS                    VARCHAR(1000),
  USER_ROLE_ID                INT            NOT NULL
) auto_increment=1000;

CREATE TABLE ATTACHMENT
(
  ATTACHMENT_ID            BIGINT         PRIMARY KEY auto_increment,
  ATTACHMENT_OWNER         VARCHAR(30)    NOT NULL,
  ATTACHMENT_OWNER_ID      BIGINT         NOT NULL,
  ATTACHMENT_NAME          VARCHAR(1000)  NOT NULL,
  ATTACHMENT_LOCATION      VARCHAR(1000)  NOT NULL,
  CREATE_DT                DATETIME       NOT NULL,
  CREATE_USR               VARCHAR(100)   NOT NULL,
  MOD_DT                   TIMESTAMP,
  MOD_USR                  VARCHAR(100),
  ATTACHMENT_CONTENT_TYPE  VARCHAR(1000)
) auto_increment=10000;

CREATE TABLE ACTIVITY_INSTANCE
(
  ACTIVITY_INSTANCE_ID  BIGINT        PRIMARY KEY auto_increment,
  ACTIVITY_ID           INT           NOT NULL,
  PROCESS_INSTANCE_ID   BIGINT        NOT NULL,
  STATUS_CD             TINYINT       NOT NULL,
  START_DT              DATETIME(6),
  END_DT                DATETIME(6),
  CREATE_DT             DATETIME(6)   NOT NULL,
  CREATE_USR            VARCHAR(30)   NOT NULL,
  MOD_DT                TIMESTAMP(6),
  MOD_USR               VARCHAR(30),
  COMMENTS              VARCHAR(1000),
  STATUS_MESSAGE        VARCHAR(4000),
  COMPCODE              VARCHAR(80),  
  ENGINE_ID             VARCHAR(8)
) auto_increment=10000;

CREATE TABLE PROCESS_INSTANCE
(
  PROCESS_INSTANCE_ID  BIGINT         PRIMARY KEY auto_increment,
  PROCESS_ID           BIGINT         NOT NULL,
  OWNER                VARCHAR(30)    NOT NULL,
  OWNER_ID             BIGINT         NOT NULL,
  SECONDARY_OWNER      VARCHAR(30),
  SECONDARY_OWNER_ID   BIGINT,
  STATUS_CD            TINYINT        NOT NULL,
  START_DT             DATETIME(6),
  END_DT               DATETIME(6),
  CREATE_DT            DATETIME(6)    NOT NULL,
  CREATE_USR           VARCHAR(30)    NOT NULL,
  MOD_DT               TIMESTAMP(6),
  MOD_USR              VARCHAR(30),
  COMMENTS             VARCHAR(1000),
  MASTER_REQUEST_ID    VARCHAR(80),
  COMPCODE             VARCHAR(80)
) auto_increment=10000;

CREATE TABLE ATTRIBUTE
(
  ATTRIBUTE_ID        INT              PRIMARY KEY auto_increment,
  ATTRIBUTE_OWNER     VARCHAR(30)      NOT NULL,
  ATTRIBUTE_OWNER_ID  BIGINT           NOT NULL,
  ATTRIBUTE_NAME      VARCHAR(1000)    NOT NULL,
  ATTRIBUTE_VALUE     VARCHAR(4000),
  CREATE_DT           DATETIME         NOT NULL,
  CREATE_USR          VARCHAR(30)      NOT NULL,
  MOD_DT              TIMESTAMP,
  MOD_USR             VARCHAR(30),
  COMMENTS            VARCHAR(1000)
) auto_increment=1000;


CREATE TABLE EVENT_WAIT_INSTANCE
(
  EVENT_WAIT_INSTANCE_ID        BIGINT        PRIMARY KEY auto_increment,
  EVENT_NAME                    VARCHAR(512)  NOT NULL,
  EVENT_WAIT_INSTANCE_OWNER_ID  BIGINT        NOT NULL,
  EVENT_WAIT_INSTANCE_OWNER     VARCHAR(30)   NOT NULL,
  EVENT_SOURCE                  VARCHAR(80)   NOT NULL,
  WORK_TRANS_INSTANCE_ID        BIGINT        NOT NULL,
  WAKE_UP_EVENT                 VARCHAR(30)   NOT NULL,
  STATUS_CD                     TINYINT       NOT NULL,
  CREATE_DT                     DATETIME(6)   NOT NULL,
  CREATE_USR                    VARCHAR(30)   NOT NULL,
  MOD_DT                        TIMESTAMP(6),
  MOD_USR                       VARCHAR(30),
  COMMENTS                      VARCHAR(1000)
) auto_increment=10000;

CREATE TABLE EVENT_INSTANCE
(
  EVENT_NAME                    VARCHAR(512),
  DOCUMENT_ID                   BIGINT,
  STATUS_CD                     SMALLINT       NOT NULL,
  CREATE_DT                     DATETIME(6)    NOT NULL,
  CONSUME_DT                    DATETIME(6),
  PRESERVE_INTERVAL             INT,
  AUXDATA                       VARCHAR(4000),
  REFERENCE                     VARCHAR(1000),
  COMMENTS                      VARCHAR(1000),
  PRIMARY KEY(EVENT_NAME(255))
);

CREATE TABLE INSTANCE_NOTE
(
  INSTANCE_NOTE_ID        BIGINT          PRIMARY KEY auto_increment,
  INSTANCE_NOTE_OWNER_ID  BIGINT          NOT NULL,
  INSTANCE_NOTE_OWNER     VARCHAR(30)     NOT NULL,
  INSTANCE_NOTE_NAME      VARCHAR(256),
  INSTANCE_NOTE_DETAILS   VARCHAR(4000),
  CREATE_DT               DATETIME        NOT NULL,
  CREATE_USR              VARCHAR(100)    NOT NULL,
  MOD_DT                  TIMESTAMP,
  MOD_USR                 VARCHAR(100)
) auto_increment=10000;


CREATE TABLE EVENT_LOG
(
  EVENT_LOG_ID        BIGINT           PRIMARY KEY auto_increment,
  EVENT_NAME          VARCHAR(512)     NOT NULL,
  EVENT_LOG_OWNER_ID  BIGINT           NOT NULL,
  EVENT_LOG_OWNER     VARCHAR(30)      NOT NULL,
  EVENT_SOURCE        VARCHAR(80)      NOT NULL,
  CREATE_DT           DATETIME(6)      NOT NULL,
  CREATE_USR          VARCHAR(30)      NOT NULL,
  MOD_DT              TIMESTAMP(6),
  MOD_USR             VARCHAR(30),
  COMMENTS            VARCHAR(1000),
  EVENT_CATEGORY      VARCHAR(80)      NOT NULL,
  STATUS_CD           TINYINT          NOT NULL,
  EVENT_SUB_CATEGORY  VARCHAR(80)
) auto_increment=10000;

CREATE TABLE DOCUMENT
(
  DOCUMENT_ID         BIGINT          PRIMARY KEY auto_increment,
  DOCUMENT_TYPE       VARCHAR(80),
  OWNER_TYPE          VARCHAR(30)     NOT NULL,
  OWNER_ID            BIGINT          NOT NULL,
  CREATE_DT           DATETIME(6)     NOT NULL,
  MODIFY_DT           TIMESTAMP(6),
  STATUS_CODE         SMALLINT,
  STATUS_MESSAGE      VARCHAR(1000),
  PATH                VARCHAR(1000)
) auto_increment=10000;

-- not used when mongodb is present
CREATE TABLE DOCUMENT_CONTENT
(
  DOCUMENT_ID         BIGINT,
  CONTENT             MEDIUMTEXT      NOT NULL  
);

CREATE TABLE TASK_INST_GRP_MAPP (
  TASK_INSTANCE_ID    BIGINT                    NOT NULL,
  USER_GROUP_ID       INT                       NOT NULL,
  CREATE_DT           DATETIME                  NOT NULL,
  PRIMARY KEY (TASK_INSTANCE_ID,USER_GROUP_ID)
);

CREATE TABLE INSTANCE_INDEX (
  INSTANCE_ID        BIGINT                    NOT NULL,
  OWNER_TYPE         VARCHAR(30)               NOT NULL,
  INDEX_KEY          VARCHAR(64)               NOT NULL,
  INDEX_VALUE        VARCHAR(256)              NOT NULL,
  CREATE_DT          DATETIME                  NOT NULL,
  PRIMARY KEY (INSTANCE_ID,OWNER_TYPE,INDEX_KEY)
);

CREATE TABLE ASSET_REF (
  DEFINITION_ID       BIGINT                    NOT NULL,
  NAME                VARCHAR(512)              NOT NULL,
  REF                 VARCHAR(64)               NOT NULL,
  ARCHIVE_DT          TIMESTAMP                 NOT NULL,
  PRIMARY KEY (DEFINITION_ID)
);

CREATE TABLE SOLUTION
(
  SOLUTION_ID    BIGINT            PRIMARY KEY auto_increment,
  ID             VARCHAR(128)      NOT NULL, -- TODO: unique constraint
  NAME           VARCHAR(1024)     NOT NULL,
  OWNER_TYPE     VARCHAR(128)      NOT NULL,
  OWNER_ID       VARCHAR(128)      NOT NULL,  
  CREATE_DT      DATETIME          NOT NULL,
  CREATE_USR     VARCHAR(30)       NOT NULL,
  MOD_DT         TIMESTAMP,
  MOD_USR        VARCHAR(30),
  COMMENTS       VARCHAR(1024)
) auto_increment=1000;

CREATE TABLE SOLUTION_MAP
(
  SOLUTION_ID    BIGINT            NOT NULL,
  MEMBER_TYPE    VARCHAR(128)      NOT NULL,
  MEMBER_ID      VARCHAR(128)      NOT NULL,
  CREATE_DT      DATETIME          NOT NULL,
  CREATE_USR     VARCHAR(30)       NOT NULL,
  MOD_DT         TIMESTAMP,
  MOD_USR        VARCHAR(30),
  COMMENTS       VARCHAR(1024)
);

CREATE TABLE VALUE
(
  NAME            VARCHAR(1024)    NOT NULL,
  VALUE           VARCHAR(2048)    NOT NULL,
  OWNER_TYPE      VARCHAR(128)     NOT NULL,
  OWNER_ID        VARCHAR(128)     NOT NULL,
  CREATE_DT       DATETIME         NOT NULL,
  CREATE_USR      VARCHAR(30)      NOT NULL,
  MOD_DT          TIMESTAMP,
  MOD_USR         VARCHAR(30),
  COMMENTS        VARCHAR(1024)
);

ALTER TABLE SOLUTION_MAP ADD  
(   
  CONSTRAINT SOLUTION_MAP_MEMBER PRIMARY KEY (SOLUTION_ID,MEMBER_TYPE,MEMBER_ID)
);

ALTER TABLE SOLUTION ADD  (   
  CONSTRAINT SOL_FRIENDLY_ID UNIQUE KEY (ID)  
);

ALTER TABLE VALUE ADD  
(   
  CONSTRAINT value_primary_key PRIMARY KEY (Name(100),Owner_type,owner_id)
);