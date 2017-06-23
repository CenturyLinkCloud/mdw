## building mdw6

1 - Edit gradle.properties to set the new build numbers.
    - mdwVersion
    - mdwDesignerVersion (latest published version)
    
2 - Edit mdw-workflow/.settings/com.centurylink.mdw.plugin.xml:
    - mdwFramework

3 - Edit mdw-hub/package.json:
    - version (omit -SNAPSHOT)

4 - Edit mdw-hub/bower.json:
    - version (omit -SNAPSHOT)

5 - Edit mdw-hub/manifest.yaml:
    - path (point to new war version)

6 - Do a local Gradle build and Run exportAssetPackages task to update **/.mdw/package.json files

7 - Run updateRestApiDefinition task to update RestApiDefinition.java
    
8 - On GitHub:
  - Close any open issues delivered with this build.
  - Create a milestone marker for the next upcoming build.
  - Assign any un-delivered issues for this build's milestone to the next build's milestone.
  - Close this build's milestone in GitHub.
    
9 - Commit and push these changes to Git.

10 - Perform the Jenkins build (http://lxdenvmtc143.dev.qintra.com:8181/jenkins):
  - MDW6-Build
  - Review console output for errors.
  
11 - Update mdw-demo
  -  Update assets
  -  Copy and overwrite all the folders under com/centurylink/mdw 
  -  Copy assets from mdw-workflow to mdw-demo
  -  Update MDW_VERSION in manifest.yml
  -  Update mdwVersion in gradle.properties  
  -  Update mdw.version in mdw.yaml
  -  Update version in com.centurylink.mdw.plugin.xml
  -  Commit and push to git 
  
12  Deploy
  - MDW6-Deploy  (You might have to start the server manually if this task does not do automatically)
  - Login to mdw-hub and run all the test cases (select Stubbing from configure icon)

13 - Publish using Jenkins (http://lxdenvmtc143.dev.qintra.com:8181/jenkins):
  - MDW6-Publish-Formal (or -Snapshot)
  - Publish to Maven Central staging repository by using mdw6-publish-maven-central (for formal build only) 
  - Review console output for errors.

14 - Publish to Mavan Central
  - login to https://oss.sonatype.org/ (Let Manoj create a ticket to allow you to publish mdw)
  - Select the staging repositories
  - Search for comcenturylink
  - After you deployment the repository will be in an Open status,  press the Close button above the list
  - Wait for 2 min to close, verify by refreshing the screen
  - Once you have successfully closed, you can release it by pressing the Release button
  - Once done check this URL https://oss.sonatype.org/service/local/repositories/releases/content/com/centurylink/mdw/mdw-common/6.0.04/
  - For detailed explanation on the steps of closing and releasing the build go here  [Steps] (http://central.sonatype.org/pages/releasing-the-deployment.html)
  - After completing the above step you should see new build here http://repo.maven.apache.org/maven2/com/centurylink/mdw/ (20 min)

15 - Tag release  
  - git tag -a v6.0.04 -m 'v6.0.04'
  - git push origin --tags
  
16 - Release Notes
  - If you are doing it first time then install ruby (https://github.com/CenturyLinkCloud/mdw#documentation) and do following in root of your workspace dir 
    `gem install github_changelog_generator`
  - github_changelog_generator --no-pull-request  --filter-by-milestone --future-release 'v6.0.04' --exclude-labels designer,internal,wontfix,duplicate,documentation
  - commit and push generated CHANGELOG.md to GitHub 
  - git commit CHANGELOG.md -m "Release notes"
  - Create new release on GitHub (https://github.com/CenturyLinkCloud/mdw/releases/new), copy the notes from CHANGELOG.md
  - Include mdw-cli-{{version}}.zip and mdw-{{version}}.jar binaries with the release.

17 - Update support items delivered with this build to Resolved status.
    
18 - mdw-buildpack
   - clone https://github.com/mdw-dev/mdw-buildpack.git
   - replace mdw*.war (mdw-buildpack/resources/mdw) with one from latest published war from http://repo.maven.apache.org/maven2/com/centurylink/mdw/mdw/
   - commit and push  
    
19 - Publishing to AppFog  
   -  go to root of mdw-demo
   -  copy the https://ne1itcprhas62.ne1.savvis.net/MDW_DEV/mdw60_internal/blob/master/assets/com/centurylink/mdw/env/manifest.yml.appfog_prod file  to mdw-demo project
   -  cf login -a https://api.useast.appfog.ctl.io -o MDWF -u manoj.agrawal@centurylink.com
   -  Select a space (or press enter to skip):Prod
   -  cf push

20 - TODO: Publish NPM package  
    