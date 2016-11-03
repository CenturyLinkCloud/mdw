insert into ATTRIBUTE (ATTRIBUTE_OWNER,ATTRIBUTE_OWNER_ID,ATTRIBUTE_NAME,ATTRIBUTE_VALUE,CREATE_DT,CREATE_USR)
    values ('SYSTEM',0,'mdw.database.version','5005',now(),'MDWDEV');
       
INSERT INTO USER_ROLE (USER_ROLE_NAME,CREATE_USR,COMMENTS) 
    values ('Process Design', 'BASELINE5.5', 'define processes'); 
INSERT INTO USER_ROLE (USER_ROLE_NAME,CREATE_USR,COMMENTS) 
    values ('Process Execution', 'BASELINE5.5', 'execute processes');
INSERT INTO USER_ROLE (USER_ROLE_NAME,CREATE_USR,COMMENTS) 
    values ('User Admin', 'BASELINE5.5', 'manage users, groups and roles');
INSERT INTO USER_ROLE (USER_ROLE_NAME,CREATE_USR,COMMENTS) 
    values ('Task Execution', 'BASELINE5.5', 'perform manual tasks');
INSERT INTO USER_ROLE (USER_ROLE_NAME,CREATE_USR,COMMENTS) 
    values ('Supervisor', 'BASELINE5.5', 'reassign tasks, acting as a group member');
    
insert into USER_GROUP (GROUP_NAME,CREATE_USR,COMMENTS)
    values ('MDW Support','BASELINE5.5','MDW Support Group');
INSERT INTO USER_GROUP (GROUP_NAME,CREATE_USR,COMMENTS)
    values ('Site Admin','BASELINE5.5','site administrator');
commit;
