# Steps for Building and Publishing MDW Designer Plug-In:

In com.centurylink.mdw.designer:
 - Update mdwDesignerVersion in gradle.properties

(If changed) Export tutorial docx files to HTML in MDW 5.5 mdw-hub/web/doc/tutorials.
In com.centurylink.mdw.designer.ui:
 - Run the Gradle build task copyTutorialDocs (copies into com.centurylink.mdw.designer.ui/help/doc/tutorials)

Commit and push these changes to Git remote

Perform Jenkins Builds (http://lxdenvmtc143.dev.qintra.com:8181/jenkins):
 - MDW55 - Build
 - Designer Build
 - Designer Publish (or Designer Preview)

Publish to the Update Site on GitHub:
 - Download the updateSite artifacts from the Jenkins workspace:
   http://lxdenvmtc143.dev.qintra.com:8181/jenkins/job/Designer%20Build/ws/com.centurylink.mdw.designer.ui/updateSite/
 - Commit them to mdw6/docs/designer/updateSite on GitHub (and remove any old ones).    
 - Other plugins required in com.centurylink.mdw.designer.feature/feature.xml should already be present.
   If versions have changed, the newer versions may need to be uploaded (esp. cucumber.eclipse).
    
Test updating Eclipse (Mars/Neon) to the new build.
  Update Site URL: http://centurylinkcloud.github.io/mdw/designer/updateSite

(If RCP is to be included in this build)
Build com.centurylink.mdw.designer.rcp according to the instructions in its build.md.
  
On GitHub:
  - Close any open issues delivered with this build.
  - Create a milestone marker for the next upcoming build.
  - Assign any undelivered issues for this build's milestone to the next build's milestone.
  - Close this build's milestone in GitHub. 

Release Notes
  - If you are doing it first time then install rubu (https://github.com/CenturyLinkCloud/mdw#documentation) and do following in root of your workspace dir 
  - github_changelog_generator --include-labels designer --exclude-tags-regex [v6.0.*] --exclude-labels internal,wontfix,duplicate,documentation --no-pull-request  --output Designer_CHANGELOG.md --future-release v9.1.x
  - commit and push generated Designer_CHANGELOG.md to GitHub 
  - git commit Designer_CHANGELOG.md -m "Designer Release notes"
  - Create the release on GitHub, copy the notes from Designer_CHANGELOG.md
  
Update support items delivered with this build to Resolved status.

