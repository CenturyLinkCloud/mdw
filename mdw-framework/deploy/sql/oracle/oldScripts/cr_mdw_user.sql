spool cr_mdw_user
undefine default_tbs_name
undefine mdw_user_name
undefine mdw_user_pwd
create user &&mdw_user_name identified by &&mdw_user_pwd
temporary tablespace temp default tablespace &&default_tbs_name;

grant RESOURCE,SELECT_CATALOG_ROLE, CONNECT to &&mdw_user_name;

grant CREATE PUBLIC SYNONYM,UNLIMITED TABLESPACE to &&mdw_user_name;
undefine default_tbs_name
undefine mdw_user_name
undefine mdw_user_pwd
spool off
