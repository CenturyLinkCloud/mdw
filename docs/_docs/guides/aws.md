---
permalink: /docs/guides/aws/
title: AWS Setup Guide
---

Instructions for installing and accessing MDW on an Amazon Web Services EC2 instance.
Note: This guide specifically addresses the Ubuntu Linux distribution.

## Install Apache
```
sudo apt install apache2
```

## Install Tomcat and enable autostart
```
sudo apt install tomcat8
sudo systemctl enable tomcat8
```

## Install [mod-proxy-http](https://httpd.apache.org/docs/2.4/mod/mod_proxy_http.html)
```
sudo a2enmod proxy_http
```

## Configure Apache
### /etc/apache2/apache2.conf (enable CORS access)
```
todo: excerpt
```
### /etc/apache2/mods-enabled/proxy.conf
```
todo: excerpt
```

## Preconfigure MDW
TODO

## Install MDW
```
sudo mdw install --webapps-dir=/var/lib/tomcat8/webapps --mdw-version=6.1.xx
```

