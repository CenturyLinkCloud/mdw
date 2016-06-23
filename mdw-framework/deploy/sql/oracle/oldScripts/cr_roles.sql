spool cr_roles.lst;
undefine &&read_role_name
undefine &&update_role_name

create role &&read_role_name;
create role &&update_role_name;

undefine &&read_role_name
undefine &&update_role_name

spool off;
