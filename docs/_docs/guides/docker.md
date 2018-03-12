---
permalink: /docs/guides/docker/
title: Docker Guide
---

Run the MDW Docker container.

build:
```
docker build -t mdwcore/mdw .
```

install:
```
docker pull mdwcore/mdw
```

run:
```
docker run -d -it --rm -p 8080:8080 -p 8009:8009 -v /home/ubuntu/workspaces/mdw-demo/:/mdw-demo -e JAVA_OPTS='-Dmdw.runtime.env=dev -Dmdw.config.location=/mdw-demo/config -Xmx1g' mdwcore/mdw
```

logs:
```
docker logs --follow <container_id>
```

publish:
```
docker push mdwcore/mdw
```

show all images:
```
docker image ls
```
**TODO: tomcat configuration (context.xml, 