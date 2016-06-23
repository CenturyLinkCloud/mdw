#!/bin/sh

JAVA_HOME=/prod/ecom2/local/apps/java/jdk1.6.0_45

CATALINA_HOME=/prod/ecom2/local/apps/tomcat/apache-tomcat-7.0.40
CATALINA_BASE=/prod/ecom2/local/apps/tomcat/tomcat7-mdwDiscovery

CATALINA_PID=${CATALINA_BASE}/logs/mdw.pid

export JAVA_HOME CATALINA_HOME CATALINA_BASE CATALINA_PID

$CATALINA_HOME/bin/catalina.sh stop
