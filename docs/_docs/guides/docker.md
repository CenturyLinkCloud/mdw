---
permalink: /docs/guides/docker/
title: Docker Guide
---

Starting with MDW 6.1.01, you can run MDW using the Dockerized build of MDW.  The first thing you will need to do is install Docker on your target system.  After that, you will need to copy your applications's config files (mdw.yaml and access.yaml) to a directory on your target system, which will be mounted to the Docker container at runtime.  When the containers starts, it will read your mdw.yaml properties to determie the location of your application's Git repository and clone it.  If you want to preserve your assets across Docker containers, you will also want to make a new empty directory on your target system where the container will create your assets (and embedded DB if running with an embedded DB).  This new directory will also be mounted to the Docker container at runtime.  

In order to preserve correct file ownership on host for files created by the application running inside the container, you will want to specify the numeric user (you can find the GID by looking in /etc/passwd file) that owns the data directory where the assets will be persisted on your "docker run" command. As a general rule, you will want to specify relative paths instead of absolute paths in your mdw.yaml.   

Following is an example of how to run a Docker container using a specific MDW version (6.1.01), while specifying the MDW config files location on host (/myapp/config/) and the location where we want the assets to be persisted (/myapp/data/).  It also specifies the user GID to use (3543) so that the assets (checked-out from Git) are created as a user that exists outside the container.
  
install:   (This step is optional since Docker run will automatically retrieve the image from the Docker repository if needed)
```
docker pull mdwcore/mdw:6.1.01   (If you do not specify a version, it will default to use the LATEST image)
```

run:
```
docker run -d --rm --user 3543 -p 8080:8080 -p 8009:8009 -p 3308:3308 -v /myapp/config/:/usr/local/tomcat/config -v /myapp/data/:/usr/local/tomcat/mdw 
-e JAVA_OPTS='-Dmdw.runtime.env=dev -Dmdw.config.location=/usr/local/tomcat/config -Xmx2g' mdwcore/mdw:6.1.01 (If you do not specify a version, it will default to use the LATEST image)
```

logs:
```
docker logs --follow <container_id>
```

show images:
```
docker image ls
```

show containers:
```
docker container ls
```