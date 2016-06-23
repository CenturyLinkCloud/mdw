#!/bin/bash
MDW_VERSION=5.5.25-SNAPSHOT
MDW_ADMIN_VERSION=6.0.02-SNAPSHOT
#MDW_REPO=http://mdwapp:ldap_012@lxomavmpc110.qintra.com:8081/nexus/content/repositories/mdw
MDW_REPO=http://mdwapp:ldap_012@lxomavmpc110.qintra.com:8081/nexus/content/repositories/snapshots

# install the mdw war files
rm -rf ../apps/mdw
curl ${MDW_REPO}/com/centurylink/mdw/mdw/${MDW_VERSION}/mdw-${MDW_VERSION}.war -o ../apps/mdw.war
rm -rf ../apps/mdw-admin
curl ${MDW_REPO}/com/centurylink/mdw/mdw-admin/${MDW_ADMIN_VERSION}/mdw-admin-${MDW_ADMIN_VERSION}.war
 -o ../apps/mdw-admin.war
