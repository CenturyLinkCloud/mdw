## Building MDW 6

1 - Edit gradle.properties to set the new build numbers.
    - mdwVersion
    - mdwDesignerVersion (latest published version)
    
2 - Run updateMdwVerInFiles task to update following file 
    mdw-workflow/.settings/com.centurylink.mdw.plugin.xml
    mdw-hub/package.json
    mdw-hub/bower.json
    RestApiDefinition.java
    
3 - Run mdw/clean and mdw/buildAll task 

4 - Run exportAssetPackages task to update **/.mdw/package.json files
    
5 - On GitHub:
  - Create a milestone marker for the next build.
  - Assign any un-delivered issues and pull request for current build's milestone to the next build's milestone.
  - Close this build's milestone in GitHub.
    
5b - if doing formal build then delete SNAPSHOT release and tags
  - git pull
  - git tag -d v6.0.xx-SNAPSHOT 
  - git push origin :refs/tags/v6.0.xx-SNAPSHOT
  - Delete the Draft of SNAPSHOT from GitHub
     
6 - Commit and push these changes to Git.

5 - Perform the Jenkins build (http://lxdenvmtc143.dev.qintra.com:8181/jenkins):
  - MDW6-Build
  - Review console output for errors.
  
6 - Update mdw-demo
  - git pull
  - Update mdwVersion and mdwDesignerVersion in gradle.properties
  - Copy latest manifest.yml.appfog_dev (for snapshots) or manifest.yml.appfog_prod over manifest.yml
  - Update MDW_VERSION in manifest.yml  
  - Run the copyAssets task to bring over the latest framework assets
  - Commit and push to git (manifest.yml and mdw.properties should not be committed)
  
7 - Deploy and Test
  - MDW6-Deploy  (You might have to start the server manually if this task does not do automatically)
  - Login to mdw-hub and run all the test cases (select Stubbing from configure icon)
  - Investigate any failed test cases
  
8 - Tag release (First time)
  - git tag -a v6.0.xx -m 'v6.0.xx'
  - git push origin --tags
   
9 - Publish using Jenkins (http://lxdenvmtc143.dev.qintra.com:8181/jenkins):
  - To publish on internal repo (lxdenvmtc143)- mdw6-publish-maven-internal (or -SNAPSHOT) (always run this job, this updates buildpacks too)
  - To publish on Maven Central repository by using mdw6-publish-maven-central (or -SNAPSHOT) (optional job)
  - Review console output for errors.

10 - Verify release artifact are published to Maven Central (https://oss.sonatype.org/#stagingRepositories)
  - Formal build:       http://repo.maven.apache.org/maven2/com/centurylink/mdw/ (20 min)
  - SNAPHOT:            https://oss.sonatype.org/content/repositories/snapshots/com/centurylink/mdw/ 
  - Buildpack:          https://github.com/CenturyLinkCloud/mdw-buildpack/tree/master/resources/mdw
  - Internal buildpack: https://ne1itcprhas62.ne1.savvis.net/PCF_Buildpacks_PUB_DEV/mdw-buildpack/tree/master/resources/mdw

11 - Upgrade mdw-demo to new version of mdw by clicking on project properties and selecting new version
  -  Refresh Gradle dependencies
  -  Commit and push new version of com.centurylink.mdw.plugin.xml to git

12 - Release Notes
  - If you are doing it first time then install ruby (https://github.com/CenturyLinkCloud/mdw#documentation) and do following in root of your workspace dir 
    `gem install github_changelog_generator`
    Set the CHANGELOG_GITHUB_TOKEN environment variable to your 40 digit token
  - github_changelog_generator --no-pull-request  --filter-by-milestone --future-release '6.0.xx' --exclude-labels designer,internal,wontfix,duplicate,documentation
  - git pull
  - git commit CHANGELOG.md -m "Release notes" (commits and pushes generated CHANGELOG.md to GitHub)
  - Update the new release on GitHub, release name should be 6.0.xx, copy the notes from updated CHANGELOG.md
  - Change release status from pre-release to release
  - Check if mdw-cli-{{version}}.zip and mdw-{{version}}.jar binaries are uploaded, Jenkins publish task should have done that.
  
13 - Update support items delivered with this build to Resolved status.

14 - Publishing to AppFog  
   -  go to root of mdw-demo project (check correct dev/prod manifest.yml is there)
   -  cf login -a https://api.useast.appfog.ctl.io -o MDWF -u manoj.agrawal@centurylink.com
   -  Select a space (or press enter to skip): Prod (Dev for snapshots)
   -  cf push

15 - TODO: Publish NPM package 

16 - Run task 1 to 4 and commit the files to start work on post release SNAPSHOT builds
    