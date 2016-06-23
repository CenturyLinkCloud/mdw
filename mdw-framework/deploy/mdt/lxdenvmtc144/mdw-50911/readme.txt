This is for MDW EWS Cloud Apache
Steps to create a Apache Clear Trust Setup


netstat -lnt| awk '$6 == "LISTEN" && $4 ~ ".50911"'
netstat -lnt| awk '$6 == "LISTEN" && $4 ~ ".50912"'
netstat -lnt| awk '$6 == "LISTEN" && $4 ~ ".50913"'


Clone  one of the existing Apache's based on mdw or MDWHub context. Use above ports in the name
Remove everything under logs dir 
cd  /opt/apache/mdw-hub-50911/server/logs 


Make sure you have free ports avaiable using this:


Replace %s/mdw-50808/mdw-50911/g under conf
ssl.sh %s/50808/50911/g


under conf/extras
change following in proxy.conf (ip/port is the remote server ip/port)

ProxyPass        /mdw  http://148.156.3.38:12081/mdw
ProxyPassReverse /mdw  http://148.156.3.38:12081/mdw


This is what you need to do on remote server

in server.xml
   <Executor
        name="tomcatThreadPool"
        namePrefix="catalina-exec-"
        minSpareThreads="10"
        maxThreads="150"/>
    <Connector
        port="12081"
        redirectPort="12083"
        executor="tomcatThreadPool"
        connectionTimeout="20000"
        maxHttpHeaderSize="8192"
        proxyName="lxdenvmtc144.dev.qintra.com"
        scheme="https"
        secure="true">
    

