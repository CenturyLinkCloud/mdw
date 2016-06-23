spool add_other_constraints.lst;

ALTER TABLE USER_GROUP add (
  CONSTRAINT USERGROUP_GROUPNAME UNIQUE(GROUP_NAME));

spool off;
