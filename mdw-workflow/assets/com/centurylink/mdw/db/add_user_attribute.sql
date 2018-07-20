insert into attribute
(attribute_owner, attribute_owner_id, attribute_name, attribute_value, create_usr, create_dt)
values ('USER', (select user_info_id from user_info where cuid = ? and end_date is null), ?, ?, 'MDW', now());