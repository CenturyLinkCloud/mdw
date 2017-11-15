## Building MDW 6

1 - Edit gradle.properties to set the new build numbers.
    - mdwVersion
    - mdwDesignerVersion (latest published version)
    
2 - Run updateMdwVerInFiles task to update following file 
  - mdw-workflow/.settings/com.centurylink.mdw.plugin.xml
  - mdw-hub/package.json
  - mdw-hub/bower.json
  - RestApiDefinition.java
  - **/.mdw/package.json
        
3 - On GitHub:
  - Create a milestone marker for the next build. (https://github.com/CenturyLinkCloud/mdw/milestones/new)
  - Assign any un-delivered issues and pull request for current build's milestone to the next build's milestone.
  - Close this build's milestone in GitHub.
    
4 - if doing formal build then delete SNAPSHOT release and tags
  - git pull
  - git tag -d v6.0.xx-SNAPSHOT 
  - git push origin :refs/tags/v6.0.xx-SNAPSHOT
  - Delete the Draft of SNAPSHOT from GitHub
     
5 - Commit and push all the above changes to Git.

6 - Perform the Jenkins build (http://lxdenvmtc143.dev.qintra.com:8181/jenkins):
  - MDW6-Build
  - Review console output for errors.
  
7 - Update mdw-demo
  - git pull
  - In framework workspace Copy https://ne1itcprhas62.ne1.savvis.net/MDW_DEV/mdw60_internal/blob/master/local.gradle to mdw folder (update mdwDemoDir based on your local setup)
  - Run the Gradle task mdw/updateMDWDemoWorkspace to copy latest framework assets, update gradle.properties, manifest file in mdw-demo workspace
  - Commit and push to git (manifest.yml and mdw.properties should not be committed)
  
8 - Deploy and Test
  - MDW6-Deploy  (You might have to start the server manually if this task does not do automatically)
  - Login to mdw-hub, select Stubbing from configure icon and run all the test cases.
  - Investigate any failed test cases
  
9 - Tag release
  - git tag -a v6.0.xx -m 'v6.0.xx'
  - git push origin --tags
   
10 - Publish using Jenkins (http://lxdenvmtc143.dev.qintra.com:8181/jenkins):
  - Use mdw6-publish-maven-central to publish mdw bineries to Maven Central repository(or -SNAPSHOT) 
  - Use mdw6-publish-assets-maven-central to publish assets to Maven Central repository(or -SNAPSHOT) 
  - Review console output for errors.

11 - Verify release artifact are published to Maven Central (https://oss.sonatype.org/#stagingRepositories)
  - Formal build:       http://repo.maven.apache.org/maven2/com/centurylink/mdw/ (20 min)
  - SNAPHOT:            https://oss.sonatype.org/content/repositories/snapshots/com/centurylink/mdw/ 
  - Assets:             http://repo.maven.apache.org/maven2/com/centurylink/mdw/assets/tests-workflow/
  - SNAPSHOT Assets:             https://oss.sonatype.org/content/repositories/snapshots/com/centurylink/mdw/assets/tests-workflow/
  - Buildpack:          https://github.com/CenturyLinkCloud/mdw-buildpack/tree/master/resources/mdw
  - Internal buildpack: https://ne1itcprhas62.ne1.savvis.net/PCF_Buildpacks_PUB_DEV/mdw-buildpack/tree/master/resources/mdw

12 - Upgrade mdw-demo to new version of mdw by clicking on project properties and selecting new version
  -  Refresh Gradle dependencies
  -  Commit and push new version of com.centurylink.mdw.plugin.xml to git

13 - Release Notes
  - If you are doing it first time then install ruby (https://github.com/CenturyLinkCloud/mdw#documentation) and do following in root of your workspace dir 
    `gem install github_changelog_generator`
  - Set the CHANGELOG_GITHUB_TOKEN environment variable to your 40 digit token
  - Run following command in root of your workspace
  github_changelog_generator --no-pull-request  --filter-by-milestone --future-release '6.0.xx' --exclude-labels designer,internal,wontfix,duplicate,documentation
  - git pull
  - git commit CHANGELOG.md -m "Release notes" 
  - git push (commits and pushes generated CHANGELOG.md to GitHub)
  - Update the new release on GitHub, release name should be 6.0.xx, copy the notes from updated CHANGELOG.md
  - Change release status from pre-release to release
  - Check if mdw-cli-{{version}}.zip and mdw-boot-{{version}}.jar binaries are uploaded, Jenkins publish task should have done that.

14 - Publishing to AppFog  
   -  go to root of mdw-demo project (check correct dev/prod manifest.yml is there)
   -  cf login -a https://api.useast.appfog.ctl.io -o MDWF -u manoj.agrawal@centurylink.com
   -  Select a space (or press enter to skip): Prod (Dev for snapshots)
   -  cf push
  
15 - Update support items delivered with this build to Resolved status.
   - Delete any obsolete branches on GitHub that were merged as part of this build.

16 - TODO: Publish NPM package 

17 - Run task 1,2 & 5 and commit the files to start work on post release SNAPSHOT builds
    