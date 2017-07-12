spool baseline_inserts.lst;
insert into ATTRIBUTE (ATTRIBUTE_ID,ATTRIBUTE_OWNER,ATTRIBUTE_OWNER_ID,ATTRIBUTE_NAME,ATTRIBUTE_VALUE,CREATE_DT,CREATE_USR)
    values (MDW_COMMON_ID_SEQ.NEXTVAL,'SYSTEM',0,'mdw.database.version','5005',sysdate,'MDWDEV');

INSERT INTO USER_ROLE (USER_ROLE_ID,USER_ROLE_NAME,CREATE_USR,COMMENTS) 
    values (6,'Supervisor', 'BASELINE5.5', 'reassign tasks, acting as a group member');
INSERT INTO USER_ROLE (USER_ROLE_ID,USER_ROLE_NAME,CREATE_USR,COMMENTS) 
    values (7,'Process Design', 'BASELINE5.5', 'define processes'); 
INSERT INTO USER_ROLE (USER_ROLE_ID,USER_ROLE_NAME,CREATE_USR,COMMENTS) 
    values (8,'Process Execution', 'BASELINE5.5', 'execute processes');
INSERT INTO USER_ROLE (USER_ROLE_ID,USER_ROLE_NAME,CREATE_USR,COMMENTS) 
    values (9,'User Admin', 'BASELINE5.5', 'manage users, groups and roles');
INSERT INTO USER_ROLE (USER_ROLE_ID,USER_ROLE_NAME,CREATE_USR,COMMENTS) 
    values (10,'Task Execution', 'BASELINE5.5','perform manual tasks');
       
insert into USER_GROUP (USER_GROUP_ID,GROUP_NAME,CREATE_USR,COMMENTS)
  values (1,'MDW Support','BASELINE5.5','MDW Support Group');
INSERT INTO USER_GROUP (USER_GROUP_ID,GROUP_NAME,CREATE_USR,COMMENTS)
     values (2,'Site Admin','BASELINE5.5','site administrator');

commit;

spool off;