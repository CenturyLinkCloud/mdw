# Embedded DB

## Purpose
MDW uses a relational database for persisting runtime state.
The most convenient way to provision this is to import and enable
the com.centurylink.mdw.db asset package.  This provides an embedded MariaDB
instance that's automatically created the first time you start your server.

## Setup
After importing this package, to enable MDW's embedded db, set the following value in mdw.properties
(this example is for a local development Tomcat instance running on port 8080):
```
mdw.db.embedded.server=localhost:8080
```
 
Then set your db connection info in mdw.properties to use the embedded MariaDB on a port of your 
choosing:
```
mdw.database.driver=org.mariadb.jdbc.Driver  
mdw.database.url=jdbc:mariadb://localhost:3308/mdw  
mdw.database.username=mdw  
mdw.database.password=mdw
```

In this example, on first startup 