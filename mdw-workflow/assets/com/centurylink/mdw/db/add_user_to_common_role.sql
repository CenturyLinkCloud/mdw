insert into USER_ROLE_MAPPING
(user_role_mapping_owner, user_role_mapping_owner_id, user_role_id, create_usr, create_dt)
values ('USER', (select user_info_id from USER_INFO where cuid = ? and end_date is null),
    (select user_role_id from USER_ROLE where user_role_name = ?), 'MDW', now());