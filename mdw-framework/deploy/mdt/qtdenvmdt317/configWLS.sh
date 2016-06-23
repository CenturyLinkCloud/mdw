export WL_HOME="/opt/bea/wlserver_10.3"
export DOMAIN_HOME="/home/wlsmdt/domains/mdwDomain"
export WEBLOGIC_USER="weblogic"
export WEBLOGIC_PASSWORD="weblogic"
export ADMIN_URL="t3://localhost:7001"

# first-time setup still requires manual edits in domains/bin/setDomainEnv.sh:
# JAVA_OPTION, CLASSPATH

INSTALL_DIR=$PWD


. ${WL_HOME}/server/bin/setWLSEnv.sh
. ${DOMAIN_HOME}/bin/setDomainEnv.sh

echo DOMAIN_HOME: ${DOMAIN_HOME}

cd $INSTALL_DIR
ant buildServices

java weblogic.WLST ${DOMAIN_HOME}/services.py ${WEBLOGIC_USER} ${WEBLOGIC_PASSWORD} ${ADMIN_URL}
java weblogic.WLST ${DOMAIN_HOME}/datasource.py ${WEBLOGIC_USER} ${WEBLOGIC_PASSWORD} ${ADMIN_URL}
