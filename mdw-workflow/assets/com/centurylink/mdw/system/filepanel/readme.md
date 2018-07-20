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