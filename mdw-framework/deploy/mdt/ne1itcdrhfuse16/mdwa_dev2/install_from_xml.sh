LIBS_REPO=http://archiva.corp.intranet/archiva/repository/development
DESIGNER_VERSION=8.8.0
DESIGNER_UPDATE_SITE=http://lxdenvmtc143.dev.qintra.com:6101/MdwPlugin
SERVER_URL=http://ne1itcdrhfuse16.dev.intranet:8002/MDWHub
ASSET_LOC=/foss/foss-fuse/instances/mdwa_dev2_1/data/mdw-workflow/assets
BASE_URL=${MDW_REPO}/com/centurylink/mdw/assets/base/${MDW_VERSION}/com.centurylink.mdw.base-${MDW_VERSION}.xml
HUB_URL=${MDW_REPO}/com/centurylink/mdw/assets/hub/${MDW_VERSION}/com.centurylink.mdw.hub-${MDW_VERSION}.xml
TESTS_URL=${MDW_REPO}/com/centurylink/mdw/assets/tests/${MDW_VERSION}/com.centurylink.mdw.tests-${MDW_VERSION}.xml
ROUTING_URL=${MDW_REPO}/com/centurylink/mdw/assets/routing/${MDW_VERSION}/com.centurylink.mdw.routing-${MDW_VERSION}.xml
XMLFILE_URL=http://archiva.corp.intranet/archiva/repository/development/com/centurylink/mdwdemo/1.0.03/com.centurylink.mdw.demo.intro-1.0.03.xml


# download needed libs
mkdir mdw
wget ${MDW_REPO}/com/centurylink/mdw/mdw-schemas/${MDW_VERSION}/mdw-schemas-${MDW_VERSION}.jar -Omdw/mdw-schemas.jar
wget ${MDW_REPO}/com/centurylink/mdw/mdw-common/${MDW_VERSION}/mdw-common-${MDW_VERSION}.jar -Omdw/mdw-common.jar
wget ${DESIGNER_UPDATE_SITE}/plugins/com.centurylink.mdw.designer.core_${DESIGNER_VERSION}.jar -Omdw/com.centurylink.mdw.designer.core.jar
wget ${LIBS_REPO}/org/eclipse/jgit/org.eclipse.jgit/3.4.1.201406201815-r/org.eclipse.jgit-3.4.1.201406201815-r.jar -Omdw/org.eclipse.jgit-3.4.1.201406201815-r.jar
wget ${LIBS_REPO}/com/jcraft/jsch/0.1.51/jsch-0.1.51.jar -Omdw/jsch-0.1.51.jar
wget ${LIBS_REPO}/com/qwest/mbeng/mbeng/7.1.0/mbeng-7.1.0.jar -Omdw/mbeng-7.1.0.jar
wget ${LIBS_REPO}//org/apache/xmlbeans/xmlbeans/2.4.0ctl/xmlbeans-2.4.0ctl.jar -Omdw/xmlbeans-2.4.0ctl.jar
wget ${LIBS_REPO}/com/qwest/AccessControl/1.0/AccessControl-1.0.jar -Omdw/AccessControl-1.0.jar
wget ${LIBS_REPO}/log4j/log4j/1.2.15/log4j-1.2.15.jar -Omdw/log4j-1.2.15.jar
wget ${LIBS_REPO}/com/qwest/ct_runtime_api/1.0/ct_runtime_api-1.0.jar -Omdw/ct_runtime_api-1.0.jar
wget ${LIBS_REPO}/com/rsa/jsafe/JsafeJCE/1.0/JsafeJCE-1.0.jar -Omdw/JsafeJCE-1.0.jar
wget ${LIBS_REPO}/com/oracle/ojdbc6/11.2.0.3/ojdbc6-11.2.0.3.jar -Omdw/ojdbc6-11.2.0.3.jar
wget ${LIBS_REPO}/org/json/json/20090211/json-20090211.jar -Omdw/json-20090211.jar
wget ${LIBS_REPO}/commons-codec/commons-codec/1.3/commons-codec-1.3.jar -Omdw/commons-codec-1.3.jar

CLASSPATH=./mdw/mdw-schemas.jar:./mdw/mdw-common.jar:./mdw/com.centurylink.mdw.designer.core.jar:./mdw/org.eclipse.jgit-3.4.1.201406201815-r.jar:./mdw/jsch-0.1.51.jar:./mdw/mbeng-7.1.0.jar:./mdw/xmlbeans-2.4.0ctl.jar:./mdw/AccessControl-1.0.jar:./mdw/log4j-1.2.15.jar:./mdw/ct_runtime_api-1.0.jar:./mdw/JsafeJCE-1.0.jar:./mdw/ojdbc6-11.2.0.3.jar:./mdw/json-20090211.jar:./mdw/commons-codec-1.3.jar
# real-world app would pass credentials from the command-line
mkdir -p ../../data/mdw-workflow/assets
java -Djavax.net.ssl.trustStore=./CenturyLinkQCA.jks -cp ${CLASSPATH} com.centurylink.mdw.designer.Importer mdwapp ldap_012 ${ASSET_LOC} ${BASE_URL} overwrite=true
java -Djavax.net.ssl.trustStore=./CenturyLinkQCA.jks -cp ${CLASSPATH} com.centurylink.mdw.designer.Importer mdwapp ldap_012 ${ASSET_LOC} ${HUB_URL} overwrite=true
java -Djavax.net.ssl.trustStore=./CenturyLinkQCA.jks -cp ${CLASSPATH} com.centurylink.mdw.designer.Importer mdwapp ldap_012 ${ASSET_LOC} ${TESTS_URL} overwrite=true
java -Djavax.net.ssl.trustStore=./CenturyLinkQCA.jks -cp ${CLASSPATH} com.centurylink.mdw.designer.Importer mdwapp ldap_012 ${ASSET_LOC} ${ROUTING_URL} overwrite=true
java -Djavax.net.ssl.trustStore=./CenturyLinkQCA.jks -cp ${CLASSPATH} com.centurylink.mdw.designer.Importer mdwapp ldap_012 ${ASSET_LOC} ${XMLFILE_URL} overwrite=true