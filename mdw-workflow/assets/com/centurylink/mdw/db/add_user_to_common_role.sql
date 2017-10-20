insert into user_role_mapping
(user_role_mapping_owner, user_role_mapping_owner_id, user_role_id, create_usr, create_dt)
values ('USER', (select user_info_id from user_info where cuid = ? and end_date is null), 
    (select user_role_id from user_role where user_role_name = ?), 'MDW', now());