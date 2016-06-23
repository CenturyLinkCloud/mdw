spool create_tablespaces
create tablespace eos_mdw_data 
datafile '/db/eospl2/vol01/oradata/eos_mdw_data01.dbf' size 5000m
extent management local segment space management auto;
create tablespace eos_mdw_index 
datafile '/db/eospl2/vol01/oradata/eos_mdw_index01.dbf' size 1000m
extent management local segment space management auto;
create tablespace eos_mdw_data_lob
datafile '/db/eospl2/vol01/oradata/eos_mdw_data_lob01.dbf' size 2000m
extent management local segment space management auto;
create tablespace eos_mdw_index_lob
datafile '/db/eospl2/vol01/oradata/eos_mdw_index_lob01.dbf' size 2000m
extent management local segment space management auto;
spool off
