Configuration for filepanel:

1. Global configuration (all servers share cross-mounted view):
  - mdw.properties:
    ```
    mdw.filepanel.root.dirs=/var/lib/tomcat8/logs,/var/lib/tomcat8/mdw/config
    ```
  - mdw.yaml:
    ```yaml
    filepanel:
      root.dirs:
        - /var/lib/tomcat8/logs
        - /var/lib/tomcat8/mdw/config
      exclude.patterns:
        - '**/boring' 
        - '**/*.secret'    
    ```
    
2. Per-server configuration (requires mdw.yaml)
  - mdw.yaml:
    ```yaml
    servers:
      mine.example.com: # two instances on this server, but one filepanel config
        ports:
          - 8080
          - 8181
        filepanel:
          root.dirs:
            - /var/lib/tomcat8/logs
            - /var/lib/tomcat8/mdw/config
            - /var/lib/something/special
      yours.example.com:
        ports:
          - 8080
        filepanel:
          root.dirs:
            - /var/lib/tomcat8/logs
            - /var/lib/tomcat8/mdw/config
    
    ```

3. Kubernetes
  - Requires asset package com.centurylink.mdw.kubernetes
  - Environment variables:
    - K8S_NAMESPACE = Kubernetes namespace
    - K8S_SERVICE_TOKEN = Secret service token with permissions to view pods in $K8S_NAMESPACE
      Example from deployment.yaml:
      ```yaml
        env:
        - name: K8S_SERVICE_TOKEN
          valueFrom:
            secretKeyRef:
              name: my-service-token-name
              key: token
      ```
  - mdw.yaml:
  ```yaml
  # (no servers section)

  filepanel:
    root.dirs:
      - /opt/mdw/config
      - /opt/mdw/logs
  ```
  - One log per pod will be visible in FilePanel if configured correctly.
  - TODO:
    - allow overriding rolling 1GB size limit
    - configurable location (default=/opt/mdw/logs)
