spool upgrade_55_to_vcs_assets.lst;

-- increase id sizes to accommodate SHA hash values
alter table process_instance modify (process_id number(16));
alter table task_instance modify (task_id number(16));
-- remove foreign key constraint 
alter table task_instance drop constraint task_id_fk;
alter table attribute modify (attribute_owner_id number(16));

commit;

spool off;