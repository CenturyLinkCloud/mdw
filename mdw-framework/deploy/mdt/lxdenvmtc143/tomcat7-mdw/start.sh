#!/bin/sh

#JAVA_HOME=/prod/ecom2/local/apps/java/jdk1.6.0_45
JAVA_HOME=/prod/ecom2/local/apps/java/jdk1.7.0_79

CATALINA_HOME=/prod/ecom2/local/apps/tomcat/apache-tomcat-7.0.40
CATALINA_BASE=/prod/ecom2/local/apps/tomcat/tomcat7-mdw

CFG_DIR=/prod/ecom2/local/apps/tomcat/tomcat7-mdw/conf

JAVA_OPTS="${JAVA_OPTS} -server -DruntimeEnv=int -Dmdw.config.location=${CFG_DIR} -Dmdw.logger.impl=org.apache.log4j.Logger -Dlog4j.configuration=file:///prod/ecom2/local/apps/tomcat/tomcat7-mdw/conf/log4j.properties -Djava.util.logging.config.file=${CFG_DIR}/logging.properties -Djavax.net.ssl.trustStore=${CFG_DIR}/CenturyLinkQCA.jks -Dcom.qwest.appsec.CTAPPFilterConfigFilePath=${CFG_DIR}/CTAPPFilter.config -Dcom.qwest.appsec.CTECOMFilterConfigFilePath=${CFG_DIR}/CTECOMFilter.config -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=8944 -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false -Xms256m -Xmx1024m -XX:MaxPermSize=256m -XX:+UseParallelGC -XX:+UseParallelOldGC -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=${CATALINA_BASE}/logs -XX:ErrorFile=${CATALINA_BASE}/logs/hs_err_pid%p.log -XX:+PrintGCDetails -Xloggc:${CATALINA_BASE}/logs/gclogs.log -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=8915"

CATALINA_PID=${CATALINA_BASE}/logs/mdw.pid

export JAVA_HOME CATALINA_HOME CATALINA_BASE JAVA_OPTS CATALINA_PID

# need to avoid Jenkins killing spawned tomcat
export BUILD_ID=dontKillMe

rm $CATALINA_BASE/logs/*.log
rm $CATALINA_BASE/logs/*.out

$CATALINA_HOME/bin/catalina.sh start &
