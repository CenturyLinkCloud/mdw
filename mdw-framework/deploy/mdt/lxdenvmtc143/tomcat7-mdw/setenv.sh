JAVA_HOME=/prod/ecom2/local/apps/java/jdk1.7.0_79
CATALINA_PID="$CATALINA_BASE/tomcat.pid"
CLASSPATH=${CLASSPATH}:/opt/mqm/java/lib/com.ibm.mq.jar:/opt/mqm/java/lib/com.ibm.mq.jmqi.jar:/opt/mqm/java/lib/com.ibm.mq.commonservices.jar:/opt/mqm/java/lib/com.ibm.mq.headers.jar:/opt/tib/MIDD50/tibrv/lib/tibrvj.jar;

PATH=$PATH:/opt/tib/MIDD50/tibrv/bin;

CATALINA_OPTS="${CATALINA_OPTS} -Dmdw.logger.impl=org.apache.log4j.Logger"

LD_LIBRARY_PATH=$LD_LIBRARY_PATH:$CATALINA_HOME/lib
export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:/opt/tib/MIDD50/tibrv/lib

