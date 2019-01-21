Setting up an MDW database in MySQL:

Run the following scripts in this order:
1. create_tables.sql	
2. create_indexes.sql
3. add_fkeys.sql
4. baseline_inserts.sql
	This script inserts a basic set of reference data into some of the tables created above.
5. seed_users.sql
	NOTE : Edit the seed_users.sql script to add initial MDW users.  At least one of these should
    be granted Site Admin access to enable them to add users through the Administration webapp.  	