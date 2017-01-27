Steps for Building and Publishing MDW Designer Plug-In:
-------------------------------------------------------

In com.centurylink.mdw.designer:
 - Update mdwDesignerVersion

(If changed) Export tutorial docx files to HTML in MDW 5.5 mdw-hub/web/doc/tutorials.
In com.centurylink.mdw.designer.ui:
 - Run the Gradle build task copyTutorialDocs (copies into com.centurylink.mdw.designer.ui/help/doc/tutorials)

Commit and push these changes to Git remote

Perform Jenkins Builds (http://lxdenvmtc143.dev.qintra.com:8181/jenkins):
 - MDW55 - Build
 - Designer Build
 - Designer Publish (or Designer Preview)
    
Upload any needed files to /prod/ecom2/local/apps/MdwPlugin(or Preview) on lxdenvmtc143.dev.qintra.com:
(Other plugins required in com.centurylink.mdw.designer.feature/feature.xml should already be present.
 If versions have changed, the newer versions may need to be uploaded (esp. cucumber.eclipse).
    
Test updating Eclipse (Mars/Neon) to the new build.
  Update Site URL: http://lxdenvmtc143.dev.qintra.com:6101/MdwPlugin
               or: http://lxdenvmtc143.dev.qintra.com:6101/MdwPluginPreview

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

