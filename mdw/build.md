## Building MDW 6

1 - Edit gradle.properties to set the new build numbers.
    - mdwVersion
    - mdwDesignerVersion (latest published version)
    
2 - Run updateMdwVerInFiles task to update following file 
    mdw-workflow/.settings/com.centurylink.mdw.plugin.xml
    mdw-hub/package.json
    mdw-hub/bower.json
    mdw-hub/manifest.yaml
    RestApiDefinition.java
    
3 - Run mdw/clean and mdw/buildAll task 

4 - Run exportAssetPackages task to update **/.mdw/package.json files
    
5 - On GitHub:
  - Create a milestone marker for the next upcoming build.
  - Assign any un-delivered issues and pull request for this build's milestone to the next build's milestone.
  - Close this build's milestone in GitHub.
    
6 - Commit and push these changes to Git.

5 - Perform the Jenkins build (http://lxdenvmtc143.dev.qintra.com:8181/jenkins):
  - MDW6-Build
  - Review console output for errors.
  
6 - Update mdw-demo
  -  Update mdwVersion in gradle.properties  
  -  Run upgradeMDWVer task to update assets and refresh the project
  -  Commit and push to git (manifest.yml and mdw.properties should not be committed)
  
7  Deploy and Test
  - MDW6-Deploy  (You might have to start the server manually if this task does not do automatically)
  - Login to mdw-hub and run all the test cases (select Stubbing from configure icon)
  - Investigate any failed test cases
  
8a - Tag release (First time)
  - git tag -a v6.0.xx -m 'v6.0.xx'
  - git push origin --tags
  
8b - if doing subsequent SNAPSHOT builds then you need to delete the previous git tag and uploaded binaries and create tag again
  - git pull
  - git tag -d v6.0.xx 
  - git push origin :refs/tags/v6.0.xx
  - log into GitHub to delete uploaded artifacts (mdw-cli and mdw-boot) for the release
  
9 - Publish using Jenkins (http://lxdenvmtc143.dev.qintra.com:8181/jenkins):
  - To publish on internal repo (lxdenvmtc143)- MDW6-Publish-Formal (or -Snapshot)
  - To publish on Maven Central repository by using mdw6-publish-maven-central (for formal and SNAPSHOT builds) 
  - Review console output for errors.

10 - Verify release artifact are published to Maven Central
  - for formal build:  http://repo.maven.apache.org/maven2/com/centurylink/mdw/ (20 min)
  - for SNAPHOT:       https://oss.sonatype.org/content/repositories/snapshots/com/centurylink/mdw/ 

11 - Upgrade mdw-demo to new version of mdw by clicking on project properties and selecting new version
  -  Commit and push new version of com.centurylink.mdw.plugin.xml to git

12 - Release Notes
  - If you are doing it first time then install ruby (https://github.com/CenturyLinkCloud/mdw#documentation) and do following in root of your workspace dir 
    `gem install github_changelog_generator`
  - github_changelog_generator --no-pull-request  --filter-by-milestone --future-release 'v6.0.xx' --exclude-labels designer,internal,wontfix,duplicate,documentation
  - git commit CHANGELOG.md -m "Release notes" (commits and pushes generated CHANGELOG.md to GitHub)
  - Update the new release on GitHub (https://github.com/CenturyLinkCloud/mdw/releases), copy the notes from CHANGELOG.md
  - Change release status from pre-release to release
  - Check if mdw-cli-{{version}}.zip and mdw-{{version}}.jar binaries are uploaded, Jenkins publish task should do that.
  
13 - Update support items delivered with this build to Resolved status.
    
14 - mdw-buildpack
   - Check if the build uploaded new mdw.war at https://github.com/CenturyLinkCloud/mdw-buildpack/tree/master/resources/mdw
    
15 - Publishing to AppFog  
   -  go to root of mdw-demo project
   -  Update and check-in MDW_VERSION in (https://ne1itcprhas62.ne1.savvis.net/MDW_DEV/mdw60_internal/blob/master/assets/com/centurylink/mdw/env/manifest.yml.appfog_dev)
   -  Copy content of above file into mdw-demo/manifest.yml    
   -  cf login -a https://api.useast.appfog.ctl.io -o MDWF -u manoj.agrawal@centurylink.com
   -  Select a space (or press enter to skip): dev
   -  cf push

16 - TODO: Publish NPM package 

17 - Run task 1 to 4 and commit the files to start work on post release SNAPSHOT builds
    