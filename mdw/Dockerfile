FROM tomcat:8.5

# RUN apk --update add openjdk8
RUN apt-get update
RUN apt-get install -y openjdk-8-jdk

RUN ["rm", "-fr", "/usr/local/tomcat/webapps/ROOT"]
COPY ./deploy/app/mdw-6.0.10-SNAPSHOT.war /usr/local/tomcat/webapps/mdw.war
COPY ./tomcat/context.xml /usr/local/tomcat/conf
COPY ./tomcat/server.xml /usr/local/tomcat/conf

# TODO embedded db support
EXPOSE 3306
EXPOSE 8009

CMD ["catalina.sh", "run"]

# assets


