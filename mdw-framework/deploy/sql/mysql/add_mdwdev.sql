-- script to populate the database with initial user(s) with Site Admin privileges
insert into user_info 
(cuid, create_usr, name)
values ('mdwdev', 'BASELINE5.5', 'MDW Developer'); 


-- add me to Site Admin group
insert into user_group_mapping
(user_info_id, user_group_id,create_usr)
values ((select user_info_id from user_info where cuid = 'mdwdev' and end_date is null), 
        (select user_group_id from user_group where group_name = 'Site Admin'),'MDW');

-- add User Admin role to my Site Admin group
insert into user_role_mapping
( user_role_mapping_owner, user_role_mapping_owner_id, user_role_id, create_usr)
values ('USER_GROUP_MAP',
        (select user_group_mapping_id from user_group_mapping ugm, user_group ug, user_info ui
         where ugm.user_group_id = ug.user_group_id
         and ui.user_info_id = ugm.user_info_id
         and ui.cuid = 'mdwdev'
         and ui.end_date is null
         and ug.group_name = 'Site Admin'),
        (select user_role_id from user_role where user_role_name = 'User Admin'),'MDW');
        
-- add me to Process Design role for Common group
insert into user_role_mapping
(user_role_mapping_owner, user_role_mapping_owner_id, user_role_id, create_usr)
values ('USER', (select user_info_id from user_info where cuid = 'mdwdev' and end_date is null), 
        (select user_role_id from user_role where user_role_name = 'Process Design'),'MDW');
        
-- add me to Process Execution role for Common group
insert into user_role_mapping
(user_role_mapping_owner, user_role_mapping_owner_id, user_role_id, create_usr)
values ('USER', (select user_info_id from user_info where cuid = 'mdwdev' and end_date is null), 
        (select user_role_id from user_role where user_role_name = 'Process Execution'),'MDW');
        
commit;        