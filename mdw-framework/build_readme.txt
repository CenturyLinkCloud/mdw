Steps for Building and Publishing a New MDW Release:
----------------------------------------------------

1 - Run the regression tests locally on Fuse since on lxdenvmtc143.dev.qintra.com they're executed only on Tomcat.     
      
2 - Increment the build version in the MDW codebase:
     - Update mdwVersion in:
        - mdw-framework/gradle.properties
        - com.centurylink.mdw.designer/gradle.properties
        - mdw-listeners/com/centurylink/mdw/service/api/RestApiDefinition (TODO: better way)
     - Update mdwDesignerVersion in:
        - mdw-framework/gradle.properties -- set to latest version published here:
          http://archiva.corp.intranet/archiva/repository/mdw/com/centurylink/mdw/mdw-designer-core/
     - Update mdwFramework version attribute in:
        - mdw-workflow/.settings/com.centurylink.mdw.plugin.xml
     - Confirm that the correct build version is indicated in mdw-workflow/assets/**/.mdw/package.xml. 
       If not: run exportAssetPackages to automatically update these files with ${mdwVersion}. 
     - Check these changes in to Dimensions

3 - (If Designer version incremented) Update mdw-framework projects to the latest Designer codebase:
     - In mdw-framework/build.gradle run task updateDesignerCoreJar to download locally into
       mdw-workflow/assets/com/centurylink/mdw/testing, mdw-web/web/WEB-INF/lib, and mdw-hub/web/WEB-INF/lib.
       (Note: This may require shutting down Eclipse or at least the gradle daemon and manually removing the core jar if Windows has the file locked).
       - Delete the old mdw-web and mdw-hub core jars.
       - Update the core jar version in mdw-workflow/assets/com/centurylink/mdw/testing/.mdw/versions to match mdwDesignerVersion.
     - In mdw-framework/build.gradle run task updateDesignerReportsJar to download locally into mdw-hub (change to non-readonly to avoid failure)
       - Refresh mdw-hub in Eclipse and delete any old update designer reports jars under WEB-INF/lib.
     - Check in these updates.
     
4 - Perform the Jenkins build (http://lxdenvmtc143.dev.qintra.com:8181/jenkins):
     - MDW55 - Build
     - Review console output for errors.

4a - Perform the MDW 6 build according to the instructions in mdw/build_readme.txt.
     - This will be needed for the test deployment in step 5.
     - Execute MDW6-Deploy to copy to the Tomcat webapps dir.     

5 - Deploy the new build on the test server:
     - Jenkins build: "MDW55 - Deploy"
       - This deploys to Tomcat on lxdenvmtc143, stopping and restarting the server 
         (see http://cshare.ad.qintra.com/sites/MDW/Developer%20Resources/Environment/MDW%20Environments%20Cheat%20Sheet.txt)
       - Check sysInfo for correct deployed version: http://lxdenvmtc143.dev.qintra.com:8989/mdw/sysInfo.jsf

6 - Execute the regression test suite:
     - Jenkins build: "MDW55 - Test"
     - When complete check results here: http://lxdenvmtc143.dev.qintra.com:8181/testResults/mdw-5.5.XX.html

7 - Publish the build from Jenkins (http://lxdenvmtc143.dev.qintra.com:8181/jenkins):
     - Jenkins build: "MDW55 - Publish Snapshot" or "MDW55 - Publish Formal"
     - Review console output for errors.
     (Do the same for MDW 6 as described in mdw/build_readme.txt)

8 - In Eclipse, create a Dimensions Baseline for the build:
     - Switch to Serena Perspective 
     - Right-click on the MDWA:MDWA project under the MDWA:MDWA container
     - Select New > Tip Baseline
     - Product=MDWA, Type=SNAPSHOT, Baseline ID=MDWA_V55_xx (xx is the build number)
     - Click Next, then Finish.
          
9 - Publish Release Notes to the MDW Users mailing list.
     - View the revision history by querying the scm5 db with the query shown below
                select id.originator, ih.remark, id.revised_date, wi.dir_fullpath, wi.filename
                from pcms_item_data id, pcms_item_history ih, pcms_workset_info wf, pcms_workset_items wi
                where id.item_uid=ih.item_uid
                and id.product_id=upper('MDWA') and wf.workset_uid=wi.workset_uid
                and id.product_id=wf.product_id and wi.item_uid=id.item_uid
                and wf.workset_name=upper('MDWA') and id.create_date > to_Date ('2014-05-17', 'YYYY-MM-DD') /*Last build date*/
                order by remark, id.revised_date desc
     - Use this information to produce the release notes email
     - Upload the release notes email to the SharePoint site
       (http://cshare.ad.qintra.com/sites/MDW/Releases/Release%20Notes/MDW%20Framework%205.5)
     -Update MDW Support site if there are any open issues related to new build   
     

Optional Fuse Installation:

1 - Install VCS assets for the new build on ne1itcdrhfuse16: mdwa_dev2_1:
     - Use putty to login to lxomavd171
     - sudo su – mdwflkc
     - Then connect to MDW server via: ssh -o ServerAliveInterval=5 -o ServerAliveCountMax=1 ne1itcdrhfuse16.dev.intranet
     - cd /foss/foss-fuse/instances/mdwa_dev2_1/bin/install
     - Update MDW_VERSION, DESIGNER_VERSION in ./install_workflow.sh and run it

2 - Install bundles for the new build in karaf:        
     - ssh -q -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -p 10002 <replace with your CUID>@ne1itcdrhfuse16.dev.intranet
     - If dependencies changed:
        - features:uninstall mdw-fuse-dependencies
        - features:removeurl mvn:com.centurylink.mdw/mdw/5.5.xx/xml/dependencies
        - features:addurl mvn:com.centurylink.mdw/mdw/5.5.xx/xml/dependencies
        - features:install mdw-fuse-dependencies
     - Uninstall the old MDW features (mdw-blv mdw-camel mdw-jsf2 mdw-reports mdw)
        - features:uninstall <feature_name>
     - Update the MDW feature URL:
        - features:removeurl mvn:com.centurylink.mdw/mdw/5.5.xx/xml/features
        - features:addurl mvn:com.centurylink.mdw/mdw/5.5.xx/xml/features
     - Install the MDW features (mdw mdw-camel mdw-jsf2 mdw-reports mdw-blv)
        - features:install <feature_name>
   
3 - Restart the mdwa_dev2_1 instance on ne1itcdrhfuse16: mdwa_dev2_1:
     - cd /foss/foss-fuse/instances/mdwa_dev2_1/bin
     - /foss/foss-fuse/instances/mdwa_dev2_1/bin/stop
     - /foss/foss-fuse/instances/mdwa_dev2_1/bin/start
     