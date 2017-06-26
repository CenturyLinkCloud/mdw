insert into user_role_mapping
(user_role_mapping_owner, user_role_mapping_owner_id, user_role_id, create_usr)
values ('USER_GROUP_MAP',
    (select user_group_mapping_id from user_group_mapping ugm, user_group ug, user_info ui
     where ugm.user_group_id = ug.user_group_id
     and ui.user_info_id = ugm.user_info_id
     and ui.cuid = ?
     and ui.end_date is null
     and ug.group_name = ?),
     (select user_role_id from user_role where user_role_name = ?),'MDW');