#!/bin/bash
MDW_VERSION=5.5.17
LIBS_REPO=http://archiva.corp.intranet/archiva/repository/development
MDW_REPO=http://mdwapp:ldap_012@lxomavmpc110.qintra.com:8081/nexus/content/repositories/mdw
#MDW_REPO=http://archiva.corp.intranet/archiva/repository/snapshots
PROPS_FILE=/foss/foss-fuse/instances/mdwa_dev1_1/etc/properties/com.centurylink.mdw.cfg

wget ${MDW_REPO}/com/centurylink/mdw/mdw-common/${MDW_VERSION}/mdw-common-${MDW_VERSION}.jar -Ojars/mdw-common.jar
wget -N ${LIBS_REPO}/org/eclipse/jgit/org.eclipse.jgit/3.4.1.201406201815-r/org.eclipse.jgit-3.4.1.201406201815-r.jar -Pjars
wget -N ${LIBS_REPO}/com/jcraft/jsch/0.1.51/jsch-0.1.51.jar -Pjars

CLASSPATH=./jars/mdw-common.jar:./jars/org.eclipse.jgit-3.4.1.201406201815-r.jar:./jars/jsch-0.1.51.jar
# real-world app would pass credentials from the command-line
java -cp ${CLASSPATH} com.centurylink.mdw.common.utilities.GitImporter ${PROPS_FILE} mdw ldap_0123
