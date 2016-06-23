These are the steps to install MDW in EWS Cloud Server 
===========================================================================================
To get access you need to do WSS request to get access to Jump server lxomavd171  using following document (takes 1 hr after manager approval) 
http://cshare.ad.qintra.com/sites/EAO/fossops/EWS%20Getting%20Started%20for%20Developers/FOSS_IAAS_EWS_Guide_ForDevelopers.htm#Access
Once access approved request sudo access :  sudo su – mdwweblk to ne1itcdrhews10.dev.intranet using above document (takes 1 day after manager approval)
===========================================================================================
Once sudo access  is approved:
Use putty to login to lxomavd171 with the password sent in the email above.
Then do following
sudo su – mdwweblk
Then connect to MDW server via: 
ssh -o ServerAliveInterval=5 -o ServerAliveCountMax=1 ne1itcdrhews10.dev.intranet
===========================================================================================
type alias
It is always a good idea to backup your instance dir before installing mdw so you can refer back to it if something goes wrong. 
Please refer to backup.sh here for sample example. mdwweblk@NE1ITCDRHEWS10:/home/mdwweblk/backup
===========================================================================================
Create following dir
/foss/foss-ews/instances/mdwews-dev1/current/CenturyLink/install
Create following dir (mdwdemo is an example here, you can name dir anything you want, it is referred in mdw.properties file)
/foss/foss-ews/instances/mdwews-dev1/current/CenturyLink/mdwdemo/workflow/assets
copy install_mdw.sh into install folder
Update mdw version in install_mdw.sh
Update MDW_REPO for Nexus URL based on formal build vs SNAPSHOT build
stop the server
run install_mdw.sh
This downloads new mdw.war and mdwadmin.war and deletes old ones (application teams may need to deploy it from dimensions)
Now run install/install_mdwdemo.sh
This will download mdwdemo assets from Git Repo (App teams might have their own repo so update the script accordingly)
===========================================================================================
updates  /foss/foss-ews/instances/mdwews-dev1/current/CenturyLink/config/mdw.properties file 
(Below one is sample from mdw setup, each app will have their own mdw.properties)
## Main MDW URLs
mdw.hub.url=https://lxdenvmtc144.dev.qintra.com:50911/mdw
mdw.services.url=http://ne1itcdrhews10.dev.intranet:12081/mdw
mdw.task.manager.url=https://lxdenvmtc144.dev.qintra.com:50913/mdwadmin
mdw.reports.url=https://lxdenvmtc144.dev.qintra.com:50912/mdwreports/reportsList

mdw.asset.location=/foss/foss-ews/instances/mdwews-dev1/current/CenturyLink/mdwdemo/assets
mdw.git.local.path=/foss/foss-ews/instances/mdwews-dev1/current/CenturyLink/mdwdemo
mdw.git.remote.url=https://8.22.8.164/mdw/mdwdemo.git
mdw.git.branch=master

mdw.server.list=ne1itcdrhews10.dev.intranet:12081
===========================================================================================
Updates to /foss/foss-ews/instances/mdwews-dev1/current/conf/mdwews-dev1.conf

CATALINA_OPTS="${CATALINA_OPTS} -DruntimeEnv=cloudDev"
CFG_DIR=$CATALINA_BASE/CenturyLink/config
CATALINA_OPTS="${CATALINA_OPTS} -Dmdw.config.location=$CFG_DIR"
CATALINA_OPTS="${CATALINA_OPTS} -Dmdw.logger.impl=org.apache.log4j.Logger"
CATALINA_OPTS="${CATALINA_OPTS} -Dlog4j.configuration=file://$CFG_DIR/log4j.properties"
CATALINA_OPTS="${CATALINA_OPTS} -Djava.util.logging.config.file=$CFG_DIR/logging.properties"
CATALINA_OPTS="${CATALINA_OPTS} -Dcom.qwest.appsec.CTAPPFilterConfigFilePath=$CFG_DIR/CTAPPFilter.config"
CATALINA_OPTS="${CATALINA_OPTS} -Dcom.qwest.appsec.CTECOMFilterConfigFilePath=$CFG_DIR/CTECOMFilter.config"
===========================================================================================
Updates to server.xml to configure apache Connector
   <Executor
        name="tomcatThreadPool"
        namePrefix="catalina-exec-"
        minSpareThreads="10"
        maxThreads="150"/>
    <Connector
        port="12081"
        redirectPort="12083"
        executor="tomcatThreadPool"
        connectionTimeout="20000"
        maxHttpHeaderSize="8192"
        proxyName="lxdenvmtc144.dev.qintra.com"
        scheme="https"
        secure="true">  
===========================================================================================
How to check in config files in dimensions:
scp mdwews-dev1.conf server.xml  aa56486@lxomd12p:/tmp/mdw (you can also use jump server for this purpose)
once all files are on lxomd12p then you can use sftp (FileZilla) to transfer these files to local desktop to check-in the files
===========================================================================================
ClearTrust Setup
Edit conf/CTECOMFilter.config and either comment out excludedURLs or remove it from the file:
#excludedURLs=*.gif,*.jpg,*.css,*.js,*.png
CTAPPFilter.config is now delivered as a build artifact. Please copy that in your CenturyLink/config dir
Work with Web Eng team
It is advisable to request a access to sudo from lxomavd171 jump server:
sudo su - weblk 
===========================================================================================
It is always a good idea to backup your instance dir after a major change in configuration
Please refer to backup.sh here for sample example. mdwweblk@NE1ITCDRHEWS10:/home/mdwweblk/backup
===========================================================================================
IBM MQ Series installation
Instructions for referencing the IBM MQ Series jars in Tomcat
http://cshare.ad.qintra.com/sites/MDW/Wiki%20Pages/How%20to%20Get%20Started%20with%20IBM%20MQ%20client.aspx
===========================================================================================
Some other helpful commands
scp mdw.properties aa56486@lxomavd171:/tmp/mdw
scp aa56486@lxomavd171:/foss/foss-ews/instances/mdwapp/current/mdw/config/* .
scp -v -r aa56486@lxomavd171:/foss/foss-ews/instances/mdwapp/current/mdw/assets/* .
===========================================================================================
To start Tomcat debug:
uncomment following line in mdwews-dev1.conf
#JAVA_OPTS="${JAVA_OPTS} -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=9500"
and restart the server
===========================================================================================
MDW EWS Info
http://cshare.ad.qintra.com/sites/EAO/fossdev/Lists/FDE%20Jump/DispForm.aspx?ID=267&Source=http%3A%2F%2Fcshare%2Ead%2Eqintra%2Ecom%2Fsites%2FEAO%2Ffossdev%2FLists%2FFDE%2520Jump%2FAbbreviated%2Easpx&ContentTypeId=0x0100179517604679CE488CBA738803DAD5D6
===========================================================================================

http://ne1itcdrhews10.dev.intranet:12081/mdw/Services/GetAppSummary
https://mdwews.dev.intranet:40223/mdw
==================================================================================
Log4J setup:-
/foss/foss-ews/instances/mdwews-dev1/current/conf/mdwews-dev1.conf
CATALINA_OPTS="${CATALINA_OPTS} -Dmdw.logger.impl=org.apache.log4j.Logger"
CATALINA_OPTS="${CATALINA_OPTS} -Dlog4j.configuration=file://$CFG_DIR/log4j.properties"
====================================================================================
Tibco setup 
/foss/foss-ews/instances/mdwews-dev1/current/conf/mdwews-dev1.conf
CLASSPATH=/opt/tib/MIDD50/tibrv/lib/tibrvj.jar
PATH=$PATH:/opt/tib/MIDD50/tibrv/bin;
export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:/opt/tib/MIDD50/tibrv/lib
==================================================================================
