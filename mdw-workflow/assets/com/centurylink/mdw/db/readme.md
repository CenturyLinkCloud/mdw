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

In this example, the embedded db *mdw* will be created with the specified access credentials.
This database will be active on port 3308 whenever the server is running.

In a clustered environment, a single server instance is identified as the db server by the
`mdw.db.embedded.server` property value.  Other instances in `mdw.server.list` will not start
their own embedded db, but rather will connect through JDBC.  **Never** stipulate `localhost`
as the embedded db or in the server list except for local development on your PC. 

## Seed Users
To effectively use MDW you'll want to create a user or users and grant them appropriate roles.
This is needed to perform actions such as executing workflow processes, actioning manual tasks, etc.
You can add users in MDWHub, but this presents a chicken-and-egg dilemma since you'll need to be
a user yourself with the Admin role.  To insert yourself as a user, add your information to a file
called seed_users.json located in the config directory specified by system property
`-Dmdw.config.location`.  The contents of seed_users.json should look like this:

```json
{
  "users":  [
    {
      "name": "Joe Dev",
      "id": "jdev",
      "attributes": {
        "Email": "jdev@example.com"
      },      
      "groups": [
        "Developers",
        "Site Admin"
      ],
      "roles": [
        "User Admin",
        "Process Execution",
        "Process Design",
        "Task Execution"
      ]            
    }
  ]
}
```
This gives Joe permission to perform key actions for the Common group.

**Important:** The seed_users.json file must be in place before the first time you start your
server with embedded db enabled.  Otherwise this file is ignored as the database has already
been created and initialized.  If you want to reseed your database with different users, the
only way to do this is to stop the server and delete the database from your file system.  By
default the database directory is called *data* and resides as a sibling to the asset location
specified by `mdw.asset.location` in mdw.properties.  Deleting this directory completely
removes all MDW runtime data as well as all users.

## Extensibility
Alongside the MDW database you can create your own custom db to store app-specific data.
Before pursuing this option, you should give due consideration to whether MDW runtime values
might be more suited to meet your requirement.  Runtime values take the form of process
variables and task data, and provide a very flexible mechanism for associating any type of
application data with your workflow.  For detailed documentation on how to use MDW runtime
values, check out the MDW online help **Workflow Design** section (especially the *Processes*
and *Variables* topics).

If you decide that you really do need to maintain a custom db, then you'll need to create an
asset package dedicated to this purpose.  This is where you'll keep the SQL scripts for creating
your tables and populating any required reference data.  In this package you also need a Dynamic
Java class that implements EmbeddedDbExtension.  Provided that you add the @RegisteredService
annotation as illustrated below, the *create* method of your implementation will be invoked by
MDW when it first creates the db.  **Note**: this is only invoked when the data directory described
above is empty or missing.

## Source Fork on GitHub:
https://github.com/mdw-dev/MariaDB4j


