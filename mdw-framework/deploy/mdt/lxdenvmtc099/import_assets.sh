#!/bin/bash
# TODO add imports of other desired assets from XML

# TODO: change MDW_VERSION, DESIGNER_VERSION, MDW_REPO, MDW_UPDATE_SITE to non-snapshot/non-preview
MDW_VERSION=5.5.12-SNAPSHOT
DESIGNER_VERSION=8.4.9
MDW_REPO=http://archiva.corp.intranet/archiva/repository/snapshots
DESIGNER_UPDATE_SITE=http://lxdenvmtc143.dev.qintra.com:6101/MdwPluginPreview
LIBS_REPO=http://archiva.corp.intranet/archiva/repository/development
PROPS_FILE=/prod/ecom2/local/apps/jboss-fuse-6.1.0.redhat-139/instances/mdwdemo/etc/com.centurylink.mdw.cfg
ASSET_LOC=/prod/ecom2/local/apps/jboss-fuse-6.1.0.redhat-139/instances/mdwdemo/mdw-workflow/assets
SERVER_URL=http://lxdenvmtc099.dev.qintra.com:10001/MDWWeb

# remove old assets
rm -rf mdw-workflow

# download needed libs
wget ${MDW_REPO}/com/centurylink/mdw/mdw-schemas/${MDW_VERSION}/mdw-schemas-${MDW_VERSION}.jar -Omdw/mdw-schemas.jar
wget ${MDW_REPO}/com/centurylink/mdw/mdw-common/${MDW_VERSION}/mdw-common-${MDW_VERSION}.jar -Omdw/mdw-common.jar
wget ${DESIGNER_UPDATE_SITE}/plugins/com.centurylink.mdw.designer.core_${DESIGNER_VERSION}.jar -Omdw/com.centurylink.mdw.designer.core.jar
wget ${LIBS_REPO}/org/eclipse/jgit/org.eclipse.jgit/3.4.1.201406201815-r/org.eclipse.jgit-3.4.1.201406201815-r.jar -Omdw/org.eclipse.jgit-3.4.1.201406201815-r.jar
wget ${LIBS_REPO}/com/jcraft/jsch/0.1.51/jsch-0.1.51.jar -Omdw/jsch-0.1.51.jar
wget ${LIBS_REPO}/com/qwest/mbeng/mbeng/7.1.0/mbeng-7.1.0.jar -Omdw/mbeng-7.1.0.jar
wget ${LIBS_REPO}/org/apache/xmlbeans/xmlbeans/2.4.0/xmlbeans-2.4.0.jar -Omdw/xmlbeans-2.4.0.jar
wget ${LIBS_REPO}/com/qwest/AccessControl/1.0/AccessControl-1.0.jar -Omdw/AccessControl-1.0.jar
wget ${LIBS_REPO}/log4j/log4j/1.2.15/log4j-1.2.15.jar -Omdw/log4j-1.2.15.jar
wget ${LIBS_REPO}/com/qwest/ct_runtime_api/1.0/ct_runtime_api-1.0.jar -Omdw/ct_runtime_api-1.0.jar
wget ${LIBS_REPO}/com/rsa/jsafe/JsafeJCE/1.0/JsafeJCE-1.0.jar -Omdw/JsafeJCE-1.0.jar
wget ${LIBS_REPO}/com/oracle/ojdbc6/11.2.0.3/ojdbc6-11.2.0.3.jar -Omdw/ojdbc6-11.2.0.3.jar
wget ${LIBS_REPO}/org/json/json/20090211/json-20090211.jar -Omdw/json-20090211.jar

CLASSPATH=./mdw/mdw-schemas.jar:./mdw/mdw-common.jar:./mdw/com.centurylink.mdw.designer.core.jar:./mdw/org.eclipse.jgit-3.4.1.201406201815-r.jar:./mdw/jsch-0.1.51.jar:./mdw/mbeng-7.1.0.jar:./mdw/xmlbeans-2.4.0.jar:./mdw/AccessControl-1.0.jar:./mdw/log4j-1.2.15.jar:./mdw/ct_runtime_api-1.0.jar:./mdw/JsafeJCE-1.0.jar:./mdw/ojdbc6-11.2.0.3.jar:./mdw/json-20090211.jar

# clone base, hub, tests using GitImporter
java -cp ${CLASSPATH} com.centurylink.mdw.common.utilities.GitImporter ${PROPS_FILE} mdw ldap_0123