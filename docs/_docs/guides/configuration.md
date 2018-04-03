---
permalink: /docs/guides/configuration/
title: MDW Configuration
---

## mdw.yaml
  - To convert from old-style mdw.properties to mdw.yaml, run the [CLI](../../getting-started/cli/) command `mdw convert`.
  - Here's an annotated example describing MDW's configuration options:   
    (**Please** do not use this as a starting point as it contains inappropriate/non-default values for illustration.  
      Instead run the `mdw init` CLI command to generate from the latest template).
    ```yaml
    application:
      name: my-project  # required
      
    # clustered server instances (required -- at least one)
    servers:
      localhost:
        ports:
        - 8080
    
    # container settings (leave these properties as shown except for unusual scenarios)    
    container:
      datasource_provider: Tomcat  # required
      messenger: jms  # required
      jms_provider: ActiveMQ  # required 
      threadpool_provider: MDW  # required
    
    # database connection information (below is typical for embedded db)
    database:
      driver: org.mariadb.jdbc.Driver  # required
      url: jdbc:mariadb://localhost:3308/mdw  # required
      username: mdw  # required
      password: mdw  # required
      poolsize: 10  # default=5
      poolMaxIdle: 3  # default=5
      validationQuery: select 1 from dual  # required
      # save datetime/timestamp using microsecond precision
      microsecond.precision: true  # default=false
      # log all queries and timings 
      trace: false  # default=false
    
    # embedded database
    db.embedded.server: localhost:8080
    db.base.location: ../data  # default=assetLoc + "/../data/db"
    db.data.location: ../data/mdw  # default=assetLoc + "/../data/mdw"
    
    # optional mongodb
    mongodb:
      host: localhost
      port: 27017
    
    # asset location info (use absolute paths when running thru eclipse wtp) 
    asset:
      location: assets  # required
    
    # git repository
    git:
      local.path: .  # required
      remote.url: https://github.com/CenturyLinkCloud/mdw-demo.git  # required
      branch: master  # required
      auto.pull: false  # default=false
      user: anonymous
      password: onlyifrequired
      trusted.host: mytrustedhost.com  # blindly trust https certificate
    
    temp.dir: mdw/.temp  # default=mdw/.temp
    attachments.dir: mdw/attach  # default=mdw/attachments
    
    hub.url: http://localhost:8080/mdw  # required
    services.url: http://localhost:8080/mdw  # required
    websocket.url: none # 'none' to use polling; otherwise don't set
    discovery.url: http://repo.maven.apache.org/maven2  # default=http://repo.maven.apache.org/maven2
    docs.url: https://centurylinkcloud.github.io/mdw/docs  # default=https://centurylinkcloud.github.io/mdw/docs
    
    # for SimpleLogger (see also mdw.log4j.properties)
    logging:
      level: DEBUG  # default=INFO
      file: mdw.log  # DEPRECATED: use -Dmdw.log.location
    
    # scripting support (custom executors should implement ScriptExecutor)
    script:
      executors:
        groovy: com.centurylink.mdw.script.GroovyExecutor
        javascript: com.centurylink.mdw.script.JavaScriptExecutor
    
    # JavaMail notifications
    mail:
      smtp.host: mymailhost
      smtp.port: 25
      connection.timeout: 10000
      smtp.timeout: 10000
      user: smtpuser
      password: smtppass
    
    # for LDAP adapter activity
    ldap:
      host: ldap.myorg.com
      port: 1636
      base.dn: dc=ad,dc=intra,dc=com
    
    # process cleanup scheduled job registration    
    timer.task:
      ProcessCleanup: # run daily at 2:30 am
      TimerClass: com.centurylink.mdw.timer.cleanup.ProcessCleanup
      Schedule: 30 2 * * ? *
      RuntimeCleanupScript: Cleanup-Runtime.sql
      ProcessExpirationAgeInDays: 180
      ExternalEventExpirationAgeInDays: 180
      MaximumProcessExpiration: 10000
      CommitInterval: 10000
    
    timer.InitialDelay: 120  # (seconds) default=120
    timer.CheckInterval: 60  # (seconds) default=60
    timer.ThresholdForDelay: 30  # (minutes) default=60
    
    # https://github.com/CenturyLinkCloud/mdw/blob/master/mdw-workflow/assets/com/centurylink/mdw/system/filepanel/readme.md
    filepanel:
      root.dirs: ./logs,./config
      exclude.patterns: **/temp/*
      masked.lines: 'password:'  # default=mdw.database.password=,LDAP-AppPassword=,password:
      
    # https://centurylinkcloud.github.io/mdw/docs/guides/tuning/
    threadpool:
      max_threads: 10  # default=10
      core_threads: 5  # default=((max_threads/2)>50?50:(max_threads/2))
      queue_size: 20  # default=(max_threads>100?100:20)
      keep_alive: 300  # default=300
      termination_timeout: 120  # default=120
      # prefix, add worker_name.min_threads/max_threads
      worker: my-worker 
      
    jms:
      listener:
        poll.interval: 10  # (ms) default=5
        receive.timeout: 180  # (ms) default=300
        
    java:
      compiler.options: '-deprecation -nowarn'
      compiler.classpath: '/opt/important/impl.jar'
      library.path: '/opt/custom:/opt/other'
      runtime.classpath: '/opt/important/impl.jar'
      
    services:
      http.basic.auth: true  # default=false
        
    activity:
      resume.delay: 3  # (seconds) default=2
      active.max.retry: 10  # default=5
      
    process:
      launch.delay: 3  # (seconds) default=2
   
    # https://centurylinkcloud.github.io/mdw/docs/guides/tuning/#performance-levels
    performance:
      level.service: 5  # default=3
      level.regular: 5  # default=3
      
    engine:
      use.transaction: false  # default=false
      
    transaction:
      retry.interval: 500  # (ms) default=1000
      retry.max: 5  # default=3
    
    internal.event:
      consume.retry.sleep: 3  # (seconds) default=2
      dev.cleanup: true  # default=true
      
    scheduled.events:
      max.batch.size: 500  # default=1000
      memory.range: 2880  # (minutes) default=1440
      
    unscheduled.events
      check.delay: 120  # (seconds) default=90
      check.interval: 180  # (seconds) default=300
      max.batch.size: 500  # default=1000
      # (WARNING: Never set lower than mdw.timer.ThresholdForDelay which is in minutes)
      min.age: 3600  # default=3600, min=300
 
    hub:
      override.package: my-hub  # default=mdw-hub
      action.definition: my-task-actions.xml  # default=mdw-task-actions.xml 

    translator:
      xmlbeans.load.options:  # for XmlOptions.put()
      xmlbeans.save.options:  # for XmlOptions.put()
      
    test:
      results.location: ./mytestresults  # default=git/local.path+"/testResults"
      summary.file: myfile.json  # default=mdw-function-test-results.json
    
    routing.servers:
      localhost:
        ports:
        - 8080

    requestrouting:
      enabled: true  # default=false
      https.enabled: true  # default=false
      default.strategy: com.centurylink.mdw.routing.LoadBasedRoutingStrategy
      active.server.interval: 20  # (seconds) default=15
      timeout: 180  # (seconds) default=300
      
    ```
    
## access.yaml
  - Controls access to MDWHub and the services API
  - Here's an annotated example:
  
    ```yaml
    # If upstreamHosts is populated then access is restricted to this list
    # unless running in dev mode (-Dmdw.runtime.env=dev).
    upstreamHosts:
      - 127.0.0.1
      - 0:0:0:0:0:0:0:1
      
    # Auth methods supported include ct (ClearTrust Web Agent) 
    # or oauth (OAuth2).  Default is oauth.
    authMethod: ct
    
    # This is the header we trust to specify the authenticated user id.
    # NOTE: This is only secure when upstreamHosts is enforced.
    authUserHeader: ct-remote-user
    
    # Allows access to all UI functions and Service APIs permitted for this
    # user without authenticating.  Requires "-Dmdw.runtime.env=dev" system property.
    devUser: mdwapp
    
    # Allows access to all Service APIs permitted for this user without
    # authenticating.  Useful when UI access is through login, but service access
    # should be wide open.  Requires "-Dmdw.service.api.open=true" system property.
    # serviceUser: mdwapp
    
    # Auth exclusions are patterns that can be accessed directly
    # without authentication even when running in non-dev mode. 
    # (upstreamHosts is still enforced if specified). 
    authExclusions:
      - '/login'
      - '/error'
      - '/offline'
      - '/sysInfo'
      - '/images/*'
      - '/css/*'
      - '/doc/*'
      - '/javadoc/*'
      - '/filepanel/*'
      - '/services/*'
      - '/Services/*'
      - '/api/*'
      - '/asset/*'
      
    # Headers appended to all HTTP servlet responses.
    responseHeaders:
      X-UA-Compatible: IE=Edge
      
    # Session timeout in seconds (if not specified, container will govern).
    sessionTimeout: 3600
    
    # Allowing any authenticated user means even those not in mdw's db.
    allowAnyAuthenticatedUser: false
    
    # Special options for logging
    loggingOptions:
      logResponseTimes: false
      logHeaders: false
      logParameters: false    
    ```

## seed_users.json
  - With [Embedded DB](https://github.com/CenturyLinkCloud/mdw/blob/master/mdw-workflow/assets/com/centurylink/mdw/db/readme.md), 
    designated users are inserted when db is created (no effect once db exists -- delete data directory to recreate with different users).
  - Here's an example:
    ```json
    {
      "users":  [
        {
          "name": "MDW Application",
          "id": "mdwapp",
          "attributes": {
            "Email": "mdwapp@centurylink.com"
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
    
