spool upgrade_55_to_60.lst;

ALTER TABLE DOCUMENT DROP COLUMN PROCESS_INST_ID;
ALTER TABLE DOCUMENT ADD (STATUS_CODE NUMBER(4), STATUS_MESSAGE VARCHAR2(1000));

UPDATE DOCUMENT set OWNER_TYPE = 'ADAPTER_REQUEST' where OWNER_TYPE = 'ADAPTOR_REQUEST';
UPDATE DOCUMENT set OWNER_TYPE = 'ADAPTER_RESPONSE' where OWNER_TYPE = 'ADAPTOR_RESPONSE';

-- not used when mongodb is present - except for pre-existing coming from 5.5
CREATE TABLE DOCUMENT_CONTENT
(
  DOCUMENT_ID         NUMBER(20)		  PRIMARY KEY,
  CONTENT             CLOB      NOT NULL  
);
ALTER TABLE DOCUMENT_CONTENT ADD (
  CONSTRAINT DOCCONTENT_DOCUMENT_FK 
  FOREIGN KEY (DOCUMENT_ID) 
  REFERENCES DOCUMENT (DOCUMENT_ID));
CREATE INDEX DOCCONTENT_DOCUMENT_FK ON DOCUMENT_CONTENT
(DOCUMENT_ID);

-- move (or copy from DOCUMENT.CONTENT to DOCUMENT_CONTENT.CONTENT)
INSERT INTO DOCUMENT_CONTENT (DOCUMENT_ID, CONTENT) SELECT DOCUMENT_ID, CONTENT FROM DOCUMENT;

-- If the above statement fails due to very large number of rows, perform the below statement multiple times until all rows have been copied

-- INSERT INTO DOCUMENT_CONTENT (DOCUMENT_ID, CONTENT) SELECT DOCUMENT_ID, CONTENT FROM DOCUMENT AS d
-- JOIN (SELECT COALESCE(MAX(DOCUMENT_ID), 0) AS offset FROM DOCUMENT_CONTENT) AS dc
-- ON  d.DOCUMENT_ID > dc.offset
-- ORDER BY d.DOCUMENT_ID LIMIT 100000;

-- Alternatively, do the below to create stored procedure to repeat statement above automatically.  
-- This works as is when using Quantum plugin in Eclipse.
-- If using native mySQL client shell, remove the '\' before each ';' and add a statement to change delimeter before and after the code block
-- "delimiter ;;" (before) and "delimiter ;" (after) and then add the extra ; (so you have ;; at end of each line)

-- DROP PROCEDURE IF EXISTS copyDocumentContent;  
-- CREATE PROCEDURE copyDocumentContent(p1 INT)  
-- BEGIN  
-- DECLARE d_count INT\;  
-- DECLARE dc_count INT DEFAULT 0\;  
-- SELECT COUNT(DOCUMENT_ID) INTO d_count FROM DOCUMENT\;  
-- REPEAT  
-- INSERT INTO DOCUMENT_CONTENT (DOCUMENT_ID, CONTENT) SELECT DOCUMENT_ID, CONTENT FROM DOCUMENT AS d   
-- JOIN (SELECT COALESCE(MAX(DOCUMENT_ID), 0) AS offset FROM DOCUMENT_CONTENT) AS dc   
-- ON d.DOCUMENT_ID > dc.offset   
-- ORDER BY d.DOCUMENT_ID LIMIT p1\;
-- SELECT COUNT(DOCUMENT_ID) INTO dc_count FROM DOCUMENT_CONTENT\;
-- UNTIL dc_count >= d_count END REPEAT\;
-- END;       
-- CALL copyDocumentContent(100000);

-- iff the copy was successful, drop the CONTENT column from DOCUMENT table as a manual step.   
-- Make sure you validate row counts from both tables BEFORE dropping the CONTENT column from DOCUMENT

-- ALTER TABLE DOCUMENT DROP COLUMN CONTENT;

-- solutions
CREATE TABLE SOLUTION
(
  SOLUTION_ID    NUMBER(20)        NOT NULL,
  ID             VARCHAR2(128)     NOT NULL, -- TODO: unique constraint
  NAME           VARCHAR2(1024)    NOT NULL,
  OWNER_TYPE     VARCHAR2(128)     NOT NULL,
  OWNER_ID       VARCHAR2(128)     NOT NULL,  
  CREATE_DT      DATE              DEFAULT SYSDATE,
  CREATE_USR     VARCHAR2(30)      DEFAULT USER,
  MOD_DT         DATE,
  MOD_USR        VARCHAR2(30),
  COMMENTS       VARCHAR2(1024)
);

CREATE TABLE SOLUTION_MAP
(
  SOLUTION_ID    NUMBER(20)        NOT NULL,
  MEMBER_TYPE    VARCHAR2(128)     NOT NULL,
  MEMBER_ID      VARCHAR2(128)     NOT NULL,
  CREATE_DT      DATE              NOT NULL,
  CREATE_USR     VARCHAR(30)       NOT NULL,
  MOD_DT         DATE,
  MOD_USR        VARCHAR2(30),
  COMMENTS       VARCHAR2(1024)
);

CREATE TABLE VALUE
(
  NAME            VARCHAR(1024)    NOT NULL,
  VALUE           VARCHAR(2048)    NOT NULL,
  OWNER_TYPE      VARCHAR(128)     NOT NULL,
  OWNER_ID        VARCHAR(128)     NOT NULL,
  CREATE_DT       TIMESTAMP        NOT NULL,
  CREATE_USR      VARCHAR(30)      NOT NULL,
  MOD_DT          DATETIME,
  MOD_USR         VARCHAR(30),
  COMMENTS        VARCHAR(1024)
);

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

ALTER TABLE SOLUTION ADD  (   
   UNIQUE(ID) 
);

CREATE INDEX SOLUTION_MAP_IDX
ON SOLUTION_MAP (SOLUTION_ID);

ALTER TABLE SOLUTION_MAP ADD (
  CONSTRAINT SOLUTION_MAP_FK 
  FOREIGN KEY (SOLUTION_ID) 
  REFERENCES SOLUTION(SOLUTION_ID)
);

spool off;