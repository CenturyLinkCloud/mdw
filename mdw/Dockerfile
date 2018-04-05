FROM tomcat:8.5

# Used for building MDW-version-specific images 
ARG version

# Install JDK 8 plus library needed for embedded MariaDB
RUN apt-get update
RUN apt-get install -y openjdk-8-jdk
RUN apt-get install -y libncurses5

ENV LD_LIBRARY_PATH ${LD_LIBRARY_PATH:+$LD_LIBRARY_PATH:}/usr/lib64:/usr/lib

# Remove default Tomcat webapp  
RUN ["rm", "-fr", "/usr/local/tomcat/webapps/ROOT"]

# Download correct version of MDW WAR and place it in Tomcat webapps directory 
RUN set -eux; \
wget -O mdw.war http://github.com/CenturyLinkCloud/mdw/releases/download/$version/mdw-$version.war; \
mv mdw.war /usr/local/tomcat/webapps/;

# Provide context and server XML files from MDW's Git Repo (Part of Docker Daemon Context)
COPY ./tomcat/context.xml /usr/local/tomcat/conf
COPY ./tomcat/server.xml /usr/local/tomcat/conf

# Create directory for runtime Bind Mount to provide config files from host to Docker container
RUN mkdir /usr/local/tomcat/config

# Create directory for Volume Mount to persist data created within container on host filesystem
RUN mkdir /usr/local/tomcat/mdw

# Create new user mdw inside container and change ownership from root to mdw / open permissions too for any user inside container
RUN useradd -g root -ms /bin/bash mdw
RUN chown -R mdw /usr/local/tomcat
RUN chmod -R 777 /usr/local/tomcat

VOLUME /usr/local/tomcat/mdw

# Set working directory to mdw directory
WORKDIR /usr/local/tomcat/mdw

# Expose container ports to host (8080 is already exposed by Tomcat's image
EXPOSE 3308
EXPOSE 8009

# Change user from root to mdw
USER mdw

# Launch Tomcat
CMD ["catalina.sh", "run"]
