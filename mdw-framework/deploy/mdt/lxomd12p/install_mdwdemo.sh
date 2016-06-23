#!/bin/bash
MDW_VERSION=5.5.10-SNAPSHOT
MDW_REPO=http://archiva.corp.intranet/archiva/repository/snapshots
LIBS_REPO=http://archiva.corp.intranet/archiva/repository/development
PROPS_FILE=/foss/foss-ews/instances/mdwapp/current/mdw/config/mdw.properties

wget ${MDW_REPO}/com/centurylink/mdw/mdw-common/${MDW_VERSION}/mdw-common-${MDW_VERSION}.jar -Omdw-common.jar
wget -N ${LIBS_REPO}/org/eclipse/jgit/org.eclipse.jgit/3.4.1.201406201815-r/org.eclipse.jgit-3.4.1.201406201815-r.jar
wget -N ${LIBS_REPO}/com/jcraft/jsch/0.1.51/jsch-0.1.51.jar

CLASSPATH=./mdw-common.jar:./org.eclipse.jgit-3.4.1.201406201815-r.jar:./jsch-0.1.51.jar
# real-world app would pass credentials from the command-line
java -cp ${CLASSPATH} com.centurylink.mdw.common.utilities.GitImporter ${PROPS_FILE} mdw ldap_0123
