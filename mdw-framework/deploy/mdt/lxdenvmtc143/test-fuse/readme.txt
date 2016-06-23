wget --user=xxxxx --password=xxxxxx --no-check-certificate --no-cookies https://access.cdn.redhat.com/content/origin/files/sha256/d4/d4222f5ed50d5d465bd105f5f2dbaee3ad0adda672bba7a18b0e262ef57458a5/jboss-fuse-full-6.1.0.redhat-379.zip?_auth_=1433887702_5cc88221e5e8d84af5ccddbded6fcd9d
netstat -a |grep 8585
replace 8181 following file with 8585
jetty.xml 
org.ops4j.pax.web.cfg
system.properties
/bin/fuse
admin:create test-fuse

shutdown

in the {FUSE}/instances/myinstance/etc directory:

org.apache.karaf.features.cfg change 
  replace with the version from ${FUSE}/etc

org.apache.karaf.shell.cfg
  (add) sshIdleTimeout=18000000
 
Change sshPort=8222 (available port)
  
users.properties
    (add) karaf=karaf,admin
    (note: adding this in the root instance will avoid startup warnings)
    
Add the following lines to INSTANCE_HOME/etc/system.properties:

runtimeEnv=dev

3.      Add the MDW and Archiva repositories to org.ops4j.pax.url.mvn.repositories in INSTANCE_HOME/etc/org.ops4j.pax.url.mvn.cfg:

(Note: Add these repository URLs before any existing URLs, and be careful to include the appropriate commas and line continuation backslashes)

http://archiva.corp.intranet/archiva/repository/mdw, \

http://archiva.corp.intranet/archiva/repository/development, \

http://archiva.corp.intranet/archiva/repository/snapshots@snapshots, \

./karaf
addUrl mvn:org.apache.activemq/activemq-karaf/5.9.0.redhat-610379/xml/features

features:install activemq-blueprint
features:install mdw-dependencies
restart
features:install mdw-fuse-dependencies
shutdown
cd /prod/ecom2/local/apps/jboss-fuse-6.1.0.redhat-379/instances/test-fuse/deploy
cp /tmp/mdw/mdw-activemq-blueprint.xml .
update ports in mdw-activemq-blueprint.xml
data/install
update install_mdwworkflow.sh
update mdw.cfg file
update start to read from setenv
create setenv

Update Config Manager
Update MDW Environment Cheat


http://lxdenvmtc143.dev.qintra.com:8585/MDWHub/system/filepanel/index.jsf
http://lxdenvmtc143.dev.qintra.com:8585/MDWHub/Services/GetAppSummary

    