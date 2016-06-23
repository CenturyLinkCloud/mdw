This is continuation of installation instructions for MDW in Fuse Cloud Server mdwa_dev1
 ==========================================================================================
Import Assets:
mkdir /foss/foss-fuse/instances/mdwa_dev1_1a/bin/install
modify and copy over install_mdwdemo.sh in /foss/foss-fuse/instances/mdwa_dev1_1a/bin/install

The script downloads all the required jars from Archiva.
Recommended approach would be that you download base, hub and artis from Archive rather than using app teams checked-in their own version. 
You can use -h for example usage.
Please take the script and modify as you want. Please parameterizing according to your needs. 
You would need to customize ASSET_LOC, and ARTIS_URL, and then the XMLFILE_URL would point to the location of the VCS process defs you exported during our build.
Script is located at NE1ITCDRHFUSE16:/foss/foss-fuse/instances/mdwa_dev1_1a/bin/install

run install_mdwdemo.sh
restart server

===========================================================================================
http://ne1itcdrhfuse16.dev.intranet:8003/MDWHub
http://ne1itcdrhfuse16.dev.intranet:8003/MDWHub/Services/GetAppSummary
ssh -q -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -p 10003 aa56486@ne1itcdrhfuse16.dev.intranet
