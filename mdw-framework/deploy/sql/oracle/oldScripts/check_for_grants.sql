select table_name from user_tables ut where not exists 
(select 'x' from all_tab_privs atp where grantee = 'EOS_MDW_READ_ROLE'
and atp.table_name = ut.table_name);
select table_name from user_tables ut where not exists 
(select 'x' from all_tab_privs atp where grantee = 'EOS_MDW_UPDATE_ROLE'
and atp.table_name = ut.table_name);
select table_name from user_tables ut where not exists 
(select 'x' from all_synonyms asy 
where asy.table_name = ut.table_name);
