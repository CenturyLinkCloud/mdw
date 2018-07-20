-- script to populate the database with initial user(s) with Site Admin privileges
insert into user_info 
(cuid, create_usr, name, create_dt)
values ('mdwapp', 'mdw6', 'MDW Application', now()); 

insert into user_info 
(cuid, create_usr, name, create_dt)
values ('dxoakes', 'mdw6', 'Donald Oakes', now()); 

insert into user_info 
(cuid, create_usr, name, create_dt)
values ('aa56486', 'mdw6', 'Manoj Agrawal', now()); 

insert into user_info 
(cuid, create_usr, name, create_dt)
values ('jbrojde', 'mdw6', 'Jack Brojde', now());

-- add me to Site Admin group
insert into user_group_mapping
(user_info_id, user_group_id,create_usr, create_dt)
values ((select user_info_id from user_info where cuid = 'dxoakes' and end_date is null), 
        (select user_group_id from user_group where group_name = 'Site Admin'),'MDW', now());

insert into user_group_mapping
(user_info_id, user_group_id,create_usr, create_dt)
values ((select user_info_id from user_info where cuid = 'aa56486' and end_date is null), 
        (select user_group_id from user_group where group_name = 'Site Admin'),'MDW', now());

insert into user_group_mapping
(user_info_id, user_group_id,create_usr, create_dt)
values ((select user_info_id from user_info where cuid = 'jbrojde' and end_date is null), 
        (select user_group_id from user_group where group_name = 'Site Admin'),'MDW', now());

-- add me to MDW Support group
insert into user_group_mapping
(user_info_id, user_group_id,create_usr, create_dt)
values ((select user_info_id from user_info where cuid = 'mdwapp' and end_date is null), 
        (select user_group_id from user_group where group_name = 'MDW Support'),'MDW', now());

insert into user_group_mapping
(user_info_id, user_group_id,create_usr, create_dt)
values ((select user_info_id from user_info where cuid = 'dxoakes' and end_date is null), 
        (select user_group_id from user_group where group_name = 'MDW Support'),'MDW', now());

insert into user_group_mapping
(user_info_id, user_group_id,create_usr, create_dt)
values ((select user_info_id from user_info where cuid = 'aa56486' and end_date is null), 
        (select user_group_id from user_group where group_name = 'MDW Support'),'MDW', now());

insert into user_group_mapping
(user_info_id, user_group_id,create_usr, create_dt)
values ((select user_info_id from user_info where cuid = 'jbrojde' and end_date is null), 
        (select user_group_id from user_group where group_name = 'MDW Support'),'MDW', now());
        
-- add User Admin role to my Site Admin group
insert into user_role_mapping
(user_role_mapping_owner, user_role_mapping_owner_id, user_role_id, create_usr, create_dt)
values ('USER_GROUP_MAP',
        (select user_group_mapping_id from user_group_mapping ugm, user_group ug, user_info ui
         where ugm.user_group_id = ug.user_group_id
         and ui.user_info_id = ugm.user_info_id
         and ui.cuid = 'dxoakes'
         and ui.end_date is null
         and ug.group_name = 'Site Admin'),
        (select user_role_id from user_role where user_role_name = 'User Admin'),'MDW', now());

insert into user_role_mapping
(user_role_mapping_owner, user_role_mapping_owner_id, user_role_id, create_usr, create_dt)
values ('USER_GROUP_MAP',
        (select user_group_mapping_id from user_group_mapping ugm, user_group ug, user_info ui
         where ugm.user_group_id = ug.user_group_id
         and ui.user_info_id = ugm.user_info_id
         and ui.cuid = 'aa56486'
         and ui.end_date is null
         and ug.group_name = 'Site Admin'),
        (select user_role_id from user_role where user_role_name = 'User Admin'),'MDW', now());
        
insert into user_role_mapping
(user_role_mapping_owner, user_role_mapping_owner_id, user_role_id, create_usr, create_dt)
values ('USER_GROUP_MAP',
        (select user_group_mapping_id from user_group_mapping ugm, user_group ug, user_info ui
         where ugm.user_group_id = ug.user_group_id
         and ui.user_info_id = ugm.user_info_id
         and ui.cuid = 'jbrojde'
         and ui.end_date is null
         and ug.group_name = 'Site Admin'),
        (select user_role_id from user_role where user_role_name = 'User Admin'),'MDW', now());
        
-- add me to Process Design role for Common group
insert into user_role_mapping
(user_role_mapping_owner, user_role_mapping_owner_id, user_role_id, create_usr, create_dt)
values ('USER', (select user_info_id from user_info where cuid = 'mdwapp' and end_date is null), 
        (select user_role_id from user_role where user_role_name = 'Process Design'),'MDW', now());

insert into user_role_mapping
(user_role_mapping_owner, user_role_mapping_owner_id, user_role_id, create_usr, create_dt)
values ('USER', (select user_info_id from user_info where cuid = 'dxoakes' and end_date is null), 
        (select user_role_id from user_role where user_role_name = 'Process Design'),'MDW', now());

insert into user_role_mapping
(user_role_mapping_owner, user_role_mapping_owner_id, user_role_id, create_usr, create_dt)
values ('USER', (select user_info_id from user_info where cuid = 'aa56486' and end_date is null), 
        (select user_role_id from user_role where user_role_name = 'Process Design'),'MDW', now());

insert into user_role_mapping
(user_role_mapping_owner, user_role_mapping_owner_id, user_role_id, create_usr, create_dt)
values ('USER', (select user_info_id from user_info where cuid = 'jbrojde' and end_date is null), 
        (select user_role_id from user_role where user_role_name = 'Process Design'),'MDW', now());
        
-- add me to Process Execution role for Common group
insert into user_role_mapping
(user_role_mapping_owner, user_role_mapping_owner_id, user_role_id, create_usr, create_dt)
values ('USER', (select user_info_id from user_info where cuid = 'mdwapp' and end_date is null), 
        (select user_role_id from user_role where user_role_name = 'Process Execution'),'MDW', now());

insert into user_role_mapping
(user_role_mapping_owner, user_role_mapping_owner_id, user_role_id, create_usr, create_dt)
values ('USER', (select user_info_id from user_info where cuid = 'dxoakes' and end_date is null), 
        (select user_role_id from user_role where user_role_name = 'Process Execution'),'MDW', now());

insert into user_role_mapping
(user_role_mapping_owner, user_role_mapping_owner_id, user_role_id, create_usr, create_dt)
values ('USER', (select user_info_id from user_info where cuid = 'aa56486' and end_date is null), 
        (select user_role_id from user_role where user_role_name = 'Process Execution'),'MDW', now());
        
insert into user_role_mapping
(user_role_mapping_owner, user_role_mapping_owner_id, user_role_id, create_usr, create_dt)
values ('USER', (select user_info_id from user_info where cuid = 'jbrojde' and end_date is null), 
        (select user_role_id from user_role where user_role_name = 'Process Execution'),'MDW', now());
        
commit;        