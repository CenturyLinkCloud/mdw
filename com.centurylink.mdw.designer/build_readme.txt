Steps for Building and Publishing MDW Designer Plug-In:
-------------------------------------------------------

Export to HTML from the tutorial docx files in mdw-hub/web/doc/tutorials (if changed).
These should end up under com.centurylink.mdw.designer.ui/help/doc/tutorials so they're available in the plug-in.

Build the latest framework code:
 - Run the mdw-framework/buildAll Gradle task.
 
Set the target platform to be the minimal supported Eclipse version and JDK.
  (Window > Preferences > Plug-In Development > Target Platform > eclipse_4.4.1)

Refresh designer projects and then Ctrl-B (to make sure target platform is used for compilation)

In com.centurylink.mdw.designer:
 - Update gradle.properties: 
     mdwDesignerVersion
 - Run the Gradle build task updateDesignerVersion
 - Run the Gradle build task buildFeature
   
Upload the following files to /prod/ecom2/local/apps/MdwPlugin(or Preview) on lxdenvmtc143.dev.qintra.com:
  com.centurylink.mdw.designer.core/updateSite:
    plugins/com.centurylink.mdw.designer.core_X.X.X.jar
  com.centurylink.mdw.reports/updateSite:
    plugins/com.centurylink.mdw.reports_X.X.X.jar
  com.centurylink.mdw.designer.ui/updateSite:
    site.xml
    plugins/com.centurylink.mdw.designer.ui_X.X.X.jar
    features/com.centurylink.mdw.designer.feature_X.X.X.jar
(Other plugins required in com.centurylink.mdw.designer.feature/feature.xml should already be present.
 If versions have changed, the newer versions may need to be uploaded (esp. cucumber.eclipse).
    
Log into the server (as your CUID) and chmod -R a+rwx /prod/ecom2/local/apps/MdwPlugin/* (or MdwPluginPreview/*)

Test updating Eclipse (Luna/Mars) to the new build.
  Update Site URL: http://lxdenvmtc143.dev.qintra.com:6101/MdwPlugin
               or: http://lxdenvmtc143.dev.qintra.com:6101/MdwPluginPreview

Manually upload (TODO: automate) the designer core jar to the MDW Maven repo: (TODO: automate) 
http://lxdenvmtc143.dev.qintra.com:7021/maven/repository

NOTE: Going forward we will only deliver an RCP build when there is a pressing need.
Build com.centurylink.mdw.designer.rcp according to the instructions in its build_readme.txt.

Check in the MANIFEST and XML files that were automatically updated with the new Designer version.

In Eclipse, create a Dimensions Baseline for the build:
   - Switch to Serena Perspective 
   - Right-click on the MDWA:MDWA_ECLIPSE project under the MDWA:MDWA_ECLIPSE container
   - Select New > Tip Baseline
   - Product=MDWA, Type=SNAPSHOT, Baseline ID=MDWA_ECLIPSE_V9_x_x (xx is the build number)
   - Click Next, then Finish.
   
Publish Release Notes to the MDW Users mailing list.
  View the revision history by querying the scm5 db:
    select id.originator, ih.remark, id.revised_date, wi.dir_fullpath, wi.filename
    from pcms_item_data id,
    pcms_item_history ih,
    pcms_workset_info wf,
    pcms_workset_items wi
    where id.item_uid=ih.item_uid
    and id.product_id=upper('MDWA')
    and wf.workset_uid=wi.workset_uid
    and id.product_id=wf.product_id
    and wi.item_uid=id.item_uid
    and wf.workset_name=upper('MDWA_ECLIPSE')
    and id.create_date > to_Date ('2015-05-27', 'YYYY-MM-DD')
    order by remark, id.revised_date desc
  Use this information to produce the release notes email.
  
Upload the release notes email to the SharePoint site.

Update support items delivered with this build to Resolved status.

