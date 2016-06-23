#!/bin/sh

JAVA_HOME=/prod/ecom2/local/apps/java/jdk1.6.0_45

CATALINA_HOME=/prod/ecom2/local/apps/tomcat/apache-tomcat-7.0.40
CATALINA_BASE=/prod/ecom2/local/apps/tomcat/tomcat7-anthill

JAVA_OPTS="${JAVA_OPTS} -server -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=7994 -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false -Xms128m -Xmx512m -XX:MaxPermSize=128m -XX:+UseParallelGC -XX:+UseParallelOldGC -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=${CATALINA_BASE}/logs -XX:ErrorFile=${CATALINA_BASE}/logs/hs_err_pid%p.log -XX:+PrintGCDetails -Xloggc:${CATALINA_BASE}/logs/gclogs.log -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=7995"

CATALINA_PID=${CATALINA_BASE}/logs/mdw.pid

export JAVA_HOME CATALINA_HOME CATALINA_BASE JAVA_OPTS CATALINA_PID

# need to avoid Jenkins killing spawned tomcat
export BUILD_ID=dontKillMe

rm $CATALINA_BASE/logs/*.log
rm $CATALINA_BASE/logs/*.out

$CATALINA_HOME/bin/catalina.sh start &
