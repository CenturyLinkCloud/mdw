insert into user_group_mapping
(user_info_id, user_group_id, create_usr)
values ((select user_info_id from user_info where cuid = ? and end_date is null), 
    (select user_group_id from user_group where group_name = ?), 'MDW');