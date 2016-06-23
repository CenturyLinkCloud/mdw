-- Run this script logged in as $MDW_USER user
@create_tables.sql
commit;
@create_indexes.sql
commit;
@add_primary_keys.sql
commit;
@add_other_constraints.sql
commit;
@add_fkeys.sql
commit;
@create_sequences.sql
commit;
@create_synonyms.sql
commit;
@grants_to_update_role.sql
commit;
@grants_to_read_role.sql
commit;
 
spool off;
