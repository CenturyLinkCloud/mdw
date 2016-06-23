This is installation instructions for MDW in Fuse Cloud Server 
===========================================================================================
To get access you need to do WSS request to get access to Jump server lxomavd171  using following document (takes 1 hr after manager approval) 
http://cshare.ad.qintra.com/sites/EAO/fossops/EWS%20Getting%20Started%20for%20Developers/FOSS_IAAS_EWS_Guide_ForDevelopers.htm#Access
Once access approved request sudo access :  sudo su – mdwflkc to ne1itcdrhfuse16.dev.intranet using above document (takes 1 day after manager approval)
===========================================================================================
Once sudo access  is approved:
Use putty to login to lxomavd171 with the password sent in the email above.
Then do following
sudo su – mdwflkc
Then connect to MDW server via: 
ssh -o ServerAliveInterval=5 -o ServerAliveCountMax=1 ne1itcdrhfuse16.dev.intranet
===========================================================================================
Steps to create DB
Follow the steps in readme.txt - framework/deploy/sql/oracle/RDBMS
===========================================================================================
type alias
It is always a good idea to backup your instance dir before installing mdw so you can refer back to it if something goes wrong. 
Please refer to backup.sh here for sample example. mdwflkc@NE1ITCDRHFUSE16:/home/mdwflkc/backup
===========================================================================================
go to /foss/foss-fuse/instances/mdwa_dev1_1/etc
In file "config.properties" around line 206 update following to include properties folder for 
cfg files to be picked up. It could already be there, since Web Engg team started including 
this property in basic install)
felix.fileinstall.dir    = ${karaf.etc},${karaf.etc}/properties   
===========================================================================================
copy com.centurylink.mdw.cfg to  /foss/foss-fuse/instances/mdwa_dev1_1/etc/properties
update the config file based on the information provided by Web Eng team

go to /foss/foss-fuse/instances/mdwa_dev1_1/etc
Add following repos in org.ops4j.pax.url.mvn.cfg

    http://mdwapp:ldap_012@lxomavmpc110.qintra.com:8081/nexus/content/repositories/mdw, \
    http://mdwapp:ldap_012@lxomavmpc110.qintra.com:8081/nexus/content/groups/public, \
    http://mdwapp:ldap_012@lxomavmpc110.qintra.com:8081/nexus/content/repositories/snapshots@snapshots, \

App teams need to get their own Nexus id by contacting Gajaananan, Joe. 
Teams should ask Joe to create a Nexus id so they can install mdw and other bundles in Fuse env 
mdwapp id is for internal use for MDW Framework team. 
===========================================================================================
start the instance
/foss/foss-fuse/instances/mdwa_dev1_1/bin/start

login to karaf using ldap credentials
ssh -q -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -p 10001 aa56486@ne1itcdrhfuse16.dev.intranet

addUrl mvn:org.apache.activemq/activemq-karaf/5.9.0.redhat-610379/xml/features
features:install activemq-blueprint
JBossFuse:aa56486@mdwa_dev2_1> list |grep activemq-blueprint
[ 134] [Resolved   ] [            ] [   50] activemq-blueprint (5.9.0.redhat-610394), Hosts: 129

addurl mvn:org.jboss.fuse/jboss-fuse/6.1.0.redhat-379/xml/features
addUrl mvn:com.centurylink.mdw/mdw/5.5.15-SNAPSHOT/xml/dependencies

features:install mdw-fuse-dependencies
if you get error then try installing in following fashion:
            features:install mdw-dependencies
            features:install mdw-fuse-dependencies
            
/foss/foss-fuse/instances/mdwa_dev1_1/bin/stop

Now deploy the MDW example ActiveMQ broker configuration by right-clicking on the following link and selecting "Save target as…" to download this file into the deploy directory of your Fuse instance:
http://cshare.ad.qintra.com/sites/MDW/Developer%20Resources/Environment/Fuse/ActiveMQ/activemq-blueprint.xml
/foss/foss-fuse/instances/mdwa_dev1_1/deploy/activemq-blueprint.xml

Update the ports in activemq-blueprint.xml -- This step should be same as in ServiceMix
<!-- substitute your ServiceMix instance name for mdwdev1, and use a unique value for the port -->
Here is the example of mdw dev 1 instance on ne1itcdrhews16
<managementContext connectorPort="12345" jmxDomainName="mdwa_dev1_1x.jms" createConnector="false"/>
<transportConnector name="openwire" uri="tcp://0.0.0.0:61618"/>
This does not have any link to property file if app teams are not using BAM. Also these ports should be unique for each instance on the same servere.
        
/foss/foss-fuse/instances/mdwa_dev1_1/bin/start
Send the info of your new environment to MDW Team to update Config Manager
===========================================================================================
login to karaf console
addUrl mvn:com.centurylink.mdw/mdw/5.5.15-SNAPSHOT/xml/features
features:install mdw
===========================================================================================
Tibco setup in setenv(/foss/foss-fuse/instances/mdwa_dev1_1/bin/setenv):
------------------------------------------------------------------------
PATH=$PATH:/opt/tib/MIDD50/tibrv/bin;
export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:/opt/tib/MIDD50/tibrv/lib;

install tibrvj.jar from karaf
install wrap:file:/opt/tib/MIDD50/tibrv/lib/tibrvj.jar
===========================================================================================
Import VCS Assets:
------------------------------------------------------------------------
mkdir /foss/foss-fuse/instances/mdwa_dev1_1/bin/install
modify and copy over install_mdwdemo.sh in /foss/foss-fuse/instances/mdwa_dev1_1/bin/install
run install_mdwdemo.sh
restart server
Add a Remote Project in eclipse
Discover and the packages and import
===========================================================================================
How to check in config files in dimensions
scp mdwews-dev1.conf server.xml  aa56486@lxomd12p:/tmp/mdw
once all files are on lxomd12p then you can use sftp (FileZilla) to transfer these files to local desktop to check-in the files
===========================================================================================
Some other helpful commands
scp log4j.properties aa56486@lxomavd171:/tmp/mdw (then use fileZilla/winscp  to transfer using sftp)
scp aa56486@lxomavd171:/foss/foss-ews/instances/mdwapp/current/mdw/config/* .
scp -v -r aa56486@lxomavd171:/foss/foss-ews/instances/mdwapp/current/mdw/assets/* .
To test MDW in linux local browser
xdg-open http://localhost:8001/MDWHub
Nexus Repo: http://lxomavmpc110.qintra.com:8081/nexus/#welcome (mdwapp/ldap_012)
Read Nexus Doc http://books.sonatype.com/nexus-book/reference/concepts.html
===========================================================================================
IBM MQ Series installation
--------------------------
Instructions for installing the IBM MQ Series bundle is here
http://cshare.ad.qintra.com/sites/MDW/Wiki%20Pages/How%20to%20Get%20Started%20with%20IBM%20MQ%20client.aspx
===========================================================================================
MDW Fuse Information provided by Web Eng team
http://cshare.ad.qintra.com/sites/EAO/fossdev/Lists/FDE%20Jump/DispForm.aspx?ID=263&Source=http%3A%2F%2Fcshare%2Ead%2Eqintra%2Ecom%2Fsites%2FEAO%2Ffossdev%2FLists%2FFDE%2520Jump%2FAbbreviated%2Easpx&ContentTypeId=0x0100179517604679CE488CBA738803DAD5D6
===========================================================================================
Fuse Documentation
http://cshare.ad.qintra.com/sites/EAO/fossops/FUSE%20Getting%20Started%20for%20Developers/Forms/AllItems.aspx
===========================================================================================
http://ne1itcdrhfuse16.dev.intranet:8001/MDWHub
http://mdwa-dev1.dev.qintra.com:30241/MDWHub
http://ne1itcdrhfuse16.dev.intranet:8001/MDWHub/Services/GetAppSummary
===========================================================================================
How to look at Apache configuration:
------------------------------------
Request a sudo access on lxomavd171 (sudo -l   will give you all available sudo commands to you)
sudo su - weblk
ssh qtdenvmdt066.dev.qintra.com   (get this info from Jump Page of MDW Fuse)
ps -ef| grep  mdwa-dev1-30241-40229 (get this info from Jump Page of MDW Fuse, this will give you Apache directory location)
cd /opt/apache/qwest/websites/mdwa-dev1-30241-40229
===========================================================================================
Enabling cleartrust webagent authentication
-------------------------------------------
 - place these two files from mdw-web/META-INF/mdw into the ServiceMix instance etc directory:
     CTAPPFilter.config
     CTECOMFilter.config
 - comment out this line in /foss/foss-fuse/instances/mdwa_dev1_1/etc/CTAPPFilter.config:
     #useLdapFilter=true
 - add to /foss/foss-fuse/instances/mdwa_dev1_1/etc/system.properties:
     com.qwest.appsec.CTECOMFilterConfigFilePath=etc/CTECOMFilter.config
     com.qwest.appsec.CTAPPFilterConfigFilePath=etc/CTAPPFilter.config
     change runtimeEnv to something other than "dev"
 - in /foss/foss-fuse/instances/mdwa_dev1_1/etc/properties/com.centurylink.mdw.cfg
     change MDWFramework.TaskManagerWeb-task.manager.url and MDWFramework.MDWDesigner-helpers.url to point to the proxy
===========================================================================================
Add following property in system.properties to read files like *.map or any other config 
mdw.config.location=etc/properties
===========================================================================================
How to add Git Repo in Eclipse
------------------------------
Please follow these steps 
   i.      In Eclipse Got to Windows-->Preferences 
   ii.      Team-->Git-->Configuration
Add Entry following
1.      Key = http.sslVerify
2.      Value=false
 
Detail instructions can be found at http://cshare.ad.qintra.com/sites/MDW/Developer%20Resources/Tutorials/MdwCamelCookbook.html#_Toc417395950
===========================================================================================
How to upgrade MDW version 
--------------------------
login to karaf using ldap credentials
ssh -q -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -p 10001 aa56486@ne1itcdrhfuse16.dev.intranet
uninstall mdw
removeurl mvn:com.centurylink.mdw/mdw/5.5.16-SNAPSHOT/xml/features
removeurl mvn:com.centurylink.mdw/mdw/5.5.16-SNAPSHOT/xml/dependencies
shutdown
/foss/foss-fuse/instances/mdwa_dev1_1/bin/start
ssh -q -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -p 10001 aa56486@ne1itcdrhfuse16.dev.intranet
addUrl mvn:com.centurylink.mdw/mdw/5.5.17/xml/dependencies
addUrl mvn:com.centurylink.mdw/mdw/5.5.17/xml/features
features:install mdw-fuse-dependencies
shutdown
/foss/foss-fuse/instances/mdwa_dev1_1/bin/start
ssh -q -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -p 10001 aa56486@ne1itcdrhfuse16.dev.intranet
features:install mdw
admin list