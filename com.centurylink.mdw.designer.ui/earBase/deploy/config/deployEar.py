user='@WEBLOGIC_USER@'
password='@WEBLOGIC_PASSWORD@'
server_host='@SERVER_HOST@'
server_port='@SERVER_PORT@'

ear_dir='@APP_DIR@/.metadata/.plugins/org.eclipse.core.resources/.projects/@EAR_NAME@/beadep/@DOMAIN_NAME@/@EAR_NAME@/split_src'

# Deploy the MDW EAR
connect(user, password, 't3://'+server_host+':'+server_port)
deploy(appName='@EAR_NAME@',path=ear_dir)
disconnect()
