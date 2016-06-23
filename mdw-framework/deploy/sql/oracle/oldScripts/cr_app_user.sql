spool cr_app_user
undefine default_tbs_name
undefine app_user_name
undefine app_user_pwd
create user &&app_user_name identified by &&app_user_pwd
temporary tablespace temp default tablespace &&default_tbs_name;

grant CONNECT to &&app_user_name;
undefine default_tbs_name
undefine app_user_name
undefine app_user_pwd

spool off
