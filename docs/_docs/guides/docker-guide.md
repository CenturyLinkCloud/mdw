---
permalink: /docs/guides/docker-guide/
title: Docker Guide
---

Run the MDW Docker container.

build:
```
docker build -t mdwcore/mdw .
```

run:
```
docker run -it --rm -p 8080:8080 -v /home/donald/src/mdw6/:/mdw -e JAVA_OPTS='-Dmdw.runtime.env=dev -Dmdw.config.location=/mdw/mdw/config -Xmx1g' mdwcore/mdw
```

publish:
```
docker push mdwcore/mdw
```