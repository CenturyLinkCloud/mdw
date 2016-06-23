-- script to populate the database with initial user(s) with Site Admin privileges
insert into user_info 
(user_info_id, cuid, create_usr, name)
values (MDW_COMMON_ID_SEQ.nextVal, 'dxoakes', 'BASELINE5.2', 'Donald Oakes'); 

insert into user_info 
(user_info_id, cuid, create_usr, name)
values (MDW_COMMON_ID_SEQ.nextVal, 'aa56486', 'BASELINE5.2', 'Manoj Agrawal'); 

insert into user_info 
(user_info_id, cuid, create_usr, name)
values (MDW_COMMON_ID_SEQ.nextVal, 'aa72442', 'BASELINE5.2', 'Dhanalakshmi Vemulapalli'); 

-- add me to Site Admin group
insert into user_group_mapping
(user_group_mapping_id, user_info_id, user_group_id)
values (MDW_COMMON_ID_SEQ.nextVal, 
        (select user_info_id from user_info where cuid = 'dxoakes' and end_date is null), 
        (select user_group_id from user_group where group_name = 'Site Admin'));

insert into user_group_mapping
(user_group_mapping_id, user_info_id, user_group_id)
values (MDW_COMMON_ID_SEQ.nextVal, 
        (select user_info_id from user_info where cuid = 'aa56486' and end_date is null), 
        (select user_group_id from user_group where group_name = 'Site Admin'));
        
insert into user_group_mapping
(user_group_mapping_id, user_info_id, user_group_id)
values (MDW_COMMON_ID_SEQ.nextVal, 
        (select user_info_id from user_info where cuid = 'aa72442' and end_date is null), 
        (select user_group_id from user_group where group_name = 'Site Admin'));
        
-- add User Admin role to my Site Admin group
insert into user_role_mapping
(user_role_mapping_id, user_role_mapping_owner, user_role_mapping_owner_id, user_role_id)
values (MDW_COMMON_ID_SEQ.nextVal,
        'USER_GROUP_MAP',
        (select user_group_mapping_id from user_group_mapping ugm, user_group ug, user_info ui
         where ugm.user_group_id = ug.user_group_id
         and ui.user_info_id = ugm.user_info_id
         and ui.cuid = 'dxoakes'
         and ui.end_date is null
         and ug.group_name = 'Site Admin'),
        (select user_role_id from user_role where user_role_name = 'User Admin'));

insert into user_role_mapping
(user_role_mapping_id, user_role_mapping_owner, user_role_mapping_owner_id, user_role_id)
values (MDW_COMMON_ID_SEQ.nextVal,
        'USER_GROUP_MAP',
        (select user_group_mapping_id from user_group_mapping ugm, user_group ug, user_info ui
         where ugm.user_group_id = ug.user_group_id
         and ui.user_info_id = ugm.user_info_id
         and ui.cuid = 'aa56486'
         and ui.end_date is null
         and ug.group_name = 'Site Admin'),
        (select user_role_id from user_role where user_role_name = 'User Admin'));
        
        
insert into user_role_mapping
(user_role_mapping_id, user_role_mapping_owner, user_role_mapping_owner_id, user_role_id)
values (MDW_COMMON_ID_SEQ.nextVal,
        'USER_GROUP_MAP',
        (select user_group_mapping_id from user_group_mapping ugm, user_group ug, user_info ui
         where ugm.user_group_id = ug.user_group_id
         and ui.user_info_id = ugm.user_info_id
         and ui.cuid = 'aa72442'
         and ui.end_date is null
         and ug.group_name = 'Site Admin'),
        (select user_role_id from user_role where user_role_name = 'User Admin'));
        
-- add me to Process Design role for Common group
insert into user_role_mapping
(user_role_mapping_id, user_role_mapping_owner, user_role_mapping_owner_id, user_role_id)
values (MDW_COMMON_ID_SEQ.nextVal, 'USER', (select user_info_id from user_info where cuid = 'dxoakes' and end_date is null), 
        (select user_role_id from user_role where user_role_name = 'Process Design'));

insert into user_role_mapping
(user_role_mapping_id, user_role_mapping_owner, user_role_mapping_owner_id, user_role_id)
values (MDW_COMMON_ID_SEQ.nextVal, 'USER', (select user_info_id from user_info where cuid = 'aa56486' and end_date is null), 
        (select user_role_id from user_role where user_role_name = 'Process Design'));

insert into user_role_mapping
(user_role_mapping_id, user_role_mapping_owner, user_role_mapping_owner_id, user_role_id)
values (MDW_COMMON_ID_SEQ.nextVal, 'USER', (select user_info_id from user_info where cuid = 'aa72442' and end_date is null), 
        (select user_role_id from user_role where user_role_name = 'Process Design'));
        
-- add me to Process Execution role for Common group
insert into user_role_mapping
(user_role_mapping_id, user_role_mapping_owner, user_role_mapping_owner_id, user_role_id)
values (MDW_COMMON_ID_SEQ.nextVal, 'USER', (select user_info_id from user_info where cuid = 'dxoakes' and end_date is null), 
        (select user_role_id from user_role where user_role_name = 'Process Execution'));        

insert into user_role_mapping
(user_role_mapping_id, user_role_mapping_owner, user_role_mapping_owner_id, user_role_id)
values (MDW_COMMON_ID_SEQ.nextVal, 'USER', (select user_info_id from user_info where cuid = 'aa56486' and end_date is null), 
        (select user_role_id from user_role where user_role_name = 'Process Execution'));        

insert into user_role_mapping
(user_role_mapping_id, user_role_mapping_owner, user_role_mapping_owner_id, user_role_id)
values (MDW_COMMON_ID_SEQ.nextVal, 'USER', (select user_info_id from user_info where cuid = 'aa72442' and end_date is null), 
        (select user_role_id from user_role where user_role_name = 'Process Execution'));        
          
commit;        