insert into USER_GROUP_MAPPING
(user_info_id, user_group_id, create_usr, create_dt)
values ((select user_info_id from USER_INFO where cuid = ? and end_date is null),
    (select user_group_id from USER_GROUP where group_name = ?), 'MDW', now());