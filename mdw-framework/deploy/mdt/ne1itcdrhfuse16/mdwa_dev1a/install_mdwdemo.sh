#!/bin/bash
MDW_VERSION=5.5.24
LIBS_REPO=http://archiva.corp.intranet/archiva/repository/development
MDW_REPO=http://mdwapp:ldap_012@lxomavmpc110.qintra.com:8081/nexus/content/repositories/mdw
#MDW_REPO=http://mdwapp:ldap_012@lxomavmpc110.qintra.com:8081/nexus/content/repositories/snapshots
PROPS_FILE=/foss/foss-fuse/instances/mdwa_dev1_1a/etc/properties/com.centurylink.mdw.cfg

wget ${MDW_REPO}/com/centurylink/mdw/mdw-common/${MDW_VERSION}/mdw-common-${MDW_VERSION}.jar -Ojars/mdw-common.jar
wget ${MDW_REPO}/com/centurylink/mdw/mdw-schemas/${MDW_VERSION}/mdw-schemas-${MDW_VERSION}.jar -Ojars/mdw-schemas.jar
wget -N ${LIBS_REPO}/org/eclipse/jgit/org.eclipse.jgit/3.4.1.201406201815-r/org.eclipse.jgit-3.4.1.201406201815-r.jar -Pjars
wget -N ${LIBS_REPO}/com/jcraft/jsch/0.1.51/jsch-0.1.51.jar -Pjars
wget -N ${LIBS_REPO}/org/apache/xmlbeans/xmlbeans/2.4.0/xmlbeans-2.4.0.jar -Pjars

CLASSPATH=./jars/mdw-common.jar:./jars/mdw-schemas.jar:./jars/org.eclipse.jgit-3.4.1.201406201815-r.jar:./jars/jsch-0.1.51.jar:./jars/xmlbeans-2.4.0.jar
# real-world app would pass credentials from the command-line
java -cp ${CLASSPATH} com.centurylink.mdw.common.utilities.GitImporter ${PROPS_FILE} mdw ldap_0123

