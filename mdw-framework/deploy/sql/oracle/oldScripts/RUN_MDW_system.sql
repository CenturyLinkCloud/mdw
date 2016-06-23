-- Run this script logged in as system
@cr_mdw_user.sql
commit;
@cr_app_user.sql
commit;
@cr_roles.sql
commit;
@grants_to_user.sql
commit;
spool off;

