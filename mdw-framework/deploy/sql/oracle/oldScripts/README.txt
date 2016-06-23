This directory contains scripts to set up MDW database from scratch,
as well as scripts for upgrading one MDW version to another.

The scripts prompt for several different inputs. 
Please decide on values for each of these parameters before running the scripts. 
Also, the scripts don't create the tablespaces, so that step
should be done prior to execution as well. 
Example values are in brackets after each substitution variable - p
please feel free to update with your own values and then cut and paste 
as the scripts execute.

data_tbs_name [eos_mdw_data]  - the main data tablespace for the mdw tables.
lob_tbs_name  [eos_mdw_data_lob]  - the tablespace to store the mdw lob segments.
lob_index_tbs_name [eos_mdw_index_lob] - the tablespace to store the lob indexes.
index_tbs_name [eos_mdw_index]  - the tablespace to store normal (non lob) indexes.
default_tbs_name [eos_mdw_data] - default tablespace referenced on create user statement.

update_role_name [eos_mdw_update_role]  - the role which will be granted 
		to the app user and contain full privs (select, insert, update and delete)
read_role_name [eos_mdw_read_role] - the role which will be granted to the app user
		and contain only select privs.

mdw_user_name [eos_mdw] - the user which owns the MDW objects.
mdw_user_pwd  - pwd for above user.

app_user_name [eos_app] - the user which the applications connects through.
app_user_pwd - pwd for above user.

To set up a brand new MDW installation, the following steps are needed.

1. Create table spaces. This needs to be done manually - no scripts here for that.
2. Log in as system and run script RUN_MDW_system.sql
   This creates users and roles, and grant roles to users.
3. Log in as MDW schema owner (mdw_user_name) and run RUN_MDW_DB_Base.sql.
   This scripts creates tables, indexes, constraints, sequences and public synonyms,
   grants read/update accesses to the users.
4. Log in as MDW application user (or schema owner user) and run baseline_inserts.sql.
   This script inserts a small set of reference data into some of the tables
   created above. The step does not have to be run by DBA (application user can do this).
5. Edit the seed_users.sql script to add initial MDW users.  At least one of these should
   be granted Site Admin access to enable them to add users through the Administration webapp.
   Execute the script after customizing.