Continued from mdwa_dev1/readme.txt
This is additional installation instructions for VCS assets:
===========================================================================================
mkdir /foss/foss-fuse/instances/mdwa_dev2_1/bin/install
modify and copy over import_from_xml.sh in /foss/foss-fuse/instances/mdwa_dev2_1/bin/install
run import_from_xml.sh
===========================================================================================
Some useful links
ssh -q -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -p 10002 aa56486@ne1itcdrhfuse16.dev.intranet
http://ne1itcdrhfuse16.dev.intranet:8002/MDWHub
http://mdwa-dev2.dev.qintra.com:30241/MDWHub
http://mdwa-dev2.dev.qintra.com:30241/MDWHub/Services/GetAppSummary
Nexus Repo: http://lxomavmpc110.qintra.com:8081/nexus/#welcome (mdwapp/ldap_012)
===========================================================================================
Tibco setup in setenv(/foss/appl/jboss-fuse/current/instances/mdwa_dev2_1/bin/setenv):
-------------------------------------------------------------------------------------------
PATH=$PATH:/opt/tib/MIDD50/tibrv/bin;
export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:/opt/tib/MIDD50/tibrv/lib;

install tibrvj.jar from karaf
install wrap:file:/opt/tib/MIDD50/tibrv/lib/tibrvj.jar
===========================================================================================