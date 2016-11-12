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
The database will be active on port 3308 whenever the server is running.

## Seed Users
To effectively use MDW you'll want to create a user or users and grant them appropriate roles.
This is needed to perform actions such as executing workflow processes, actioning manual tasks, etc.
You can add users in MDWHub, but this presents a chicken-and-egg dilemma since you'll need to be
a user yourself with the Admin role.  To insert yourself as a users, add your information to a file
called seed_users.json located in the config directory specified by system property
`-Dmdw.config.location`.  The contents of seed_users.json should look like this:

```json
{
  "users":  [
    {
      "name": "Joe Developer",
      "cuid": "jd123",
      "attributes": {
        "emailAddress": "joe.developer@centurylink.com"
      },          
      "groups": [
        "Common",
        "MDW Support",
        "Site Admin"
      ],
      "Common": [
        "Process Design",
        "Process Execution",
        "Task Execution"
      ],
      "Site Admin": [
        "User Admin"
      ]            
    }
  ]
}
```
This gives Joe permission to perform key actions for the Common group, and to add users
under any group.

**Important:** The seed_users.json file must be in place before the first time you start your
server with embedded db enabled.  Otherwise this file is ignored as the database has already
been created and initialized.  If you want to reseed your database with different users, the
only way to do this is to stop the server and delete the database from your file system.  By
default the database directory is called *data* and resides as a sibling to the asset location
specified by `mdw.asset.location` in mdw.properties.
