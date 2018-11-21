---
permalink: /docs/guides/kubernetes/
title: Kubernetes
---

Deploying MDW on Kubernetes is best accomplished by using the [Pure Spring Boot](../spring-boot/#2-mdw-as-a-spring-boot-dependency) approach 
and creating a [Docker image](https://docs.docker.com/get-started/) from your boot jar.  The major steps involved are outlined in this guide.
These steps assume you already have a Kubernetes cluster, and the kubectl command-line tool installed and configured to communicate with your cluster.
Refer to the [Kubernetes documentation](https://kubernetes.io/docs/user-journeys/users/application-developer/foundational/) for help getting started with a cluster.

## Sections in This Guide
  1. [Build a Spring Boot Jar](#1-build-a-spring-boot-jar)
  2. [Stipulate your Docker Image](#2-stipulate-your-docker-image)
  3. [Create a Config Map](#3-create-a-config-map)
  4. [Create a Pod](#4-create-a-pod)
  5. [Filepanel](#5-filepanel)

## 1. Build a Spring Boot Jar
  - The [mdw-demo](https://github.com/CenturyLinkCloud/mdw-demo) project has a build.gradle file that illustrates how to include mdw-spring-boot
    as a dependency of your app:
    ```gradle
    dependencies {
        compile group: 'com.centurylink.mdw', name: 'mdw-spring-boot', version: mdwVersion
        compile group: 'org.springframework.boot', name: 'spring-boot-starter-web', version: springBootVersion

        // asset package dependencies
        compileOnly fileTree(dir: "${assetLoc}", includes: ["**/*.jar"])
    }
    ```
  - Further explanation is available in the [Spring Boot Guide](../spring-boot/).

## 2. Stipulate your Docker Image
  - Here's an example Dockerfile:
    ```dockerfile
    FROM java8
    ADD build/libs/mdw-demo-2.0.1.jar mdw-demo.jar
    EXPOSE 8080
    EXPOSE 8009
    # embedded db
    EXPOSE 3308/tcp
    ENTRYPOINT ["java","-Xmx1024m","-Dmdw.runtime.env=dev","-Dmdw.config.location=/etc/mdw","-jar","/mdw-demo.jar","--spring.config.location=file:///etc/mdw/spring/application.yml"]
    ```
  - The `-Dmdw.config.location` system property points to the configMap described below.

## 3. Create a Config Map
  - Here's the command to create a config map from ./config and ./config/spring directories.
    ```
    kubectl create configmap mdw-config --from-file=config --from-file=config/spring/
    ```

## 4. Create a Pod
  - This example deployment yaml illustrates how you can configure a pod for MDW:
    ```yaml
    apiVersion: extensions/v1beta1
    kind: Deployment
    metadata:
      name: mdw-demo-dev
      namespace: mdwa-dev
    spec:
      replicas: 1
      revisionHistoryLimit: 0
      template:
        metadata:
          labels:
            app: mdw-demo
        spec:
          containers:
          - name: mdw-demo-dev
            image: mdw-demo-image
            imagePullPolicy: Always
            ports:
            - containerPort: 8080
              protocol: TCP
            resources:
              limits:
                memory: 2048Mi
                cpu: 2
            livenessProbe:
              httpGet:
                path: /mdw
                port: 8080
              initialDelaySeconds: 300
              timeoutSeconds: 10
              periodSeconds: 120
              failureThreshold: 3
            env:
            - name: SPRING_PROFILES_ACTIVE
              value: kube-mdw-demo-dev
            volumeMounts:
            - name: config-volume
              mountPath: /etc/mdw
            - name: mdw-volume
              mountPath: /usr/local/mdw/
            - name: data-volume
              mountPath: /var/lib/mdw/
          volumes:
            - name: config-volume
              configMap:
                name: mdw-config
                items:
                - key: mdw.yaml
                  path: mdw.yaml
                - key: access.yaml
                  path: access.yaml
                - key: seed_users.json
                  path: seed_users.json
                - key: application.yml
                  path: spring/application.yml
                - key: application-context.xml
                  path: spring/application-context.xml
            - name: mdw-volume
              emptyDir: {}
            - name: data-volume
              emptyDir: {}
    ```
  - Note how the config map from step 3 is referenced in the volumeMounts and volume sections.

## 5. FilePanel
  - To set up FilePanel log viewing for MDW on Kubernetes, follow the steps under the Kubernetes section here:
    https://github.com/CenturyLinkCloud/mdw/blob/master/mdw-workflow/assets/com/centurylink/mdw/system/filepanel/readme.md

