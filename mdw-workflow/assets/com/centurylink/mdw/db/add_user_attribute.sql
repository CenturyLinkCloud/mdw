insert into ATTRIBUTE
(attribute_owner, attribute_owner_id, attribute_name, attribute_value, create_usr, create_dt)
values ('USER', (select user_info_id from USER_INFO where cuid = ? and end_date is null), ?, ?, 'MDW', now());