spool grants_to_user.lst;
undefine read_role_name
undefine update_role_name
undefine app_user_name

GRANT &&read_role_name TO &&app_user_name;
GRANT &&update_role_name TO &&app_user_name;
undefine read_role_name
undefine update_role_name
undefine app_user_name

spool off;
