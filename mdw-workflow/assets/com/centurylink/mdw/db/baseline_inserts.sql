insert into ATTRIBUTE (ATTRIBUTE_OWNER,ATTRIBUTE_OWNER_ID,ATTRIBUTE_NAME,ATTRIBUTE_VALUE,CREATE_DT,CREATE_USR)
    values ('SYSTEM',0,'mdw.database.version','5005',now(),'MDWDEV');
       
INSERT INTO USER_ROLE (USER_ROLE_NAME,CREATE_USR,COMMENTS, CREATE_DT) 
    values ('Process Design', 'BASELINE5.5', 'define processes', now()); 
INSERT INTO USER_ROLE (USER_ROLE_NAME,CREATE_USR,COMMENTS, CREATE_DT) 
    values ('Process Execution', 'BASELINE5.5', 'execute processes', now());
INSERT INTO USER_ROLE (USER_ROLE_NAME,CREATE_USR,COMMENTS, CREATE_DT) 
    values ('User Admin', 'BASELINE5.5', 'manage users, groups and roles', now());
INSERT INTO USER_ROLE (USER_ROLE_NAME,CREATE_USR,COMMENTS, CREATE_DT) 
    values ('Task Execution', 'BASELINE5.5', 'perform manual tasks', now());
INSERT INTO USER_ROLE (USER_ROLE_NAME,CREATE_USR,COMMENTS, CREATE_DT) 
    values ('Task Admin', 'BASELINE5.5', 'create and assign tasks', now());
    
INSERT INTO USER_GROUP (GROUP_NAME,CREATE_USR,COMMENTS, CREATE_DT)
    values ('MDW Support','BASELINE5.5','MDW Support Group', now());
INSERT INTO USER_GROUP (GROUP_NAME,CREATE_USR,COMMENTS, CREATE_DT)
    values ('Developers','BASELINE5.5','Developers Group', now());
INSERT INTO USER_GROUP (GROUP_NAME,CREATE_USR,COMMENTS, CREATE_DT)
    values ('Site Admin','BASELINE5.5','Site Administrators', now());
commit;
