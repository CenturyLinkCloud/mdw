Run the following scripts to create a streamlined local development database on Oracle XE:
(scripts location : http://cshare.ad.qintra.com/sites/MDW/Developer%20Resources/MDW%20Database/Base%20Scripts%20MDW%205.5%20Oracle/RDBMS)
1. create_tables.sql	
2. create_indexes.sql
3. add_primary_keys.sql
4. add_other_contraints.sql
5. add_fkeys.sql
6. create_sequences.sql
7. baseline_inserts.sql
	This script inserts a basic set of reference data into some of the tables created above.
8. seed_users.sql
	NOTE : Edit the seed_users.sql script to add initial MDW users.  At least one of these should
    be granted Site Admin access to enable them to add users through the Administration webapp.
9. (Optional) Run add_solutions_entities.sql if the solutions concept is desired.    