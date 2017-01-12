#!/bin/sh

JAVA_HOME=/prod/ecom2/local/apps/java/jdk1.8.0_45

CATALINA_HOME=/prod/ecom2/local/apps/tomcat/apache-tomcat-7.0.40
CATALINA_BASE=/prod/ecom2/local/apps/tomcat/tomcat7-mdw6

CFG_DIR=/prod/ecom2/local/apps/tomcat/tomcat7-mdw6/conf

JAVA_OPTS="${JAVA_OPTS} -server -Dmdw.filepanel.root.dirs=/prod/ecom2/local/apps/tomcat/tomcat7-mdw6 -Dmdw.runtime.env=int -Dmdw.config.location=${CFG_DIR} -Dmdw.logger.impl=org.apache.log4j.Logger -Dmdw.service.api.open=true -Dlog4j.configuration=file:///prod/ecom2/local/apps/tomcat/tomcat7-mdw6/conf/log4j.properties -Djava.util.logging.config.file=${CFG_DIR}/logging.properties -Djavax.net.ssl.trustStore=${CFG_DIR}/trust.jks -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=8950 -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false -Xms256m -Xmx2048m -XX:MaxPermSize=256m -XX:+UseParallelGC -XX:+UseParallelOldGC -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=${CATALINA_BASE}/logs -XX:ErrorFile=${CATALINA_BASE}/logs/hs_err_pid%p.log -XX:+PrintGCDetails -Xloggc:${CATALINA_BASE}/logs/gclogs.log -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=8951"

CATALINA_PID=${CATALINA_BASE}/logs/mdw.pid

export JAVA_HOME CATALINA_HOME CATALINA_BASE JAVA_OPTS CATALINA_PID

# need to avoid Jenkins killing spawned tomcat
export BUILD_ID=dontKillMe

rm $CATALINA_BASE/logs/*.log
rm $CATALINA_BASE/logs/*.out

$CATALINA_HOME/bin/catalina.sh start &
