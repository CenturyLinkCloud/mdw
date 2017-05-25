Run the following scripts to create a  database on Oracle for VCS based projects:
(scripts location : http://cshare.ad.qintra.com/sites/MDW/Developer Resources/MDW Database/Base Scripts MDW 5.5 Oracle/VCSBasedDB)
1. create_tables.sql	
2. create_indexes.sql
3. add_primary_keys.sql
4. add_other_contraints.sql
5. add_fkeys.sql
6. create_sequences.sql
7. baseline_inserts.sql
	Following script inserts a basic set of reference data into some of the tables created above.
8. seed_users.sql
	NOTE : Edit the seed_users.sql script to add initial MDW users.  At least one of these should
    be granted Site Admin access to enable them to add users through the Administration webapp.
9. (Optional) Run add_solutions_entities.sql if the solutions concept is desired.
   

