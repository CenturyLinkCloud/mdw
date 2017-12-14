FROM tomcat:8.5

# RUN apk --update add openjdk8
RUN apt-get update
RUN apt-get install -y openjdk-8-jdk

RUN ["rm", "-fr", "/usr/local/tomcat/webapps/ROOT"]
COPY ./mdw/deploy/app/mdw-6.0.10-SNAPSHOT.war /usr/local/tomcat/webapps/mdw.war

# TODO embedded db support
EXPOSE 3306

CMD ["catalina.sh", "run"]

# assets


