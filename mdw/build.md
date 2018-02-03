## Publishing an mdw6 build

1 - Edit gradle.properties to set the new build numbers.
    - mdwVersion
    
2 - Run Gradle task updateMdwVerInFiles to update these files: 
  - mdw-workflow/.settings/com.centurylink.mdw.plugin.xml
  - mdw-hub/package.json
  - mdw-hub/bower.json
  - RestApiDefinition.java
  - **/.mdw/package.json

3 - Clean Tag (Formal builds only)
  - Go to https://github.com/CenturyLinkCloud/mdw/tags
  - Delete SNAPSHOT release and tag
  - git pull
        
4 - Commit and push all the above changes to Git (normally plugin.xml and gradle.properties).
  - Travis CI will run the build, tests and publish to maven-central or sonatype.
  - Compilation or testing errors will prevent the build from being published.

5 - After success, verify release artifacts are published to Maven Central (https://oss.sonatype.org/#stagingRepositories)
  - Formal build:       http://repo.maven.apache.org/maven2/com/centurylink/mdw/ (10 min)
  - SNAPHOT:            https://oss.sonatype.org/content/repositories/snapshots/com/centurylink/mdw/ 
  - Assets:             http://repo.maven.apache.org/maven2/com/centurylink/mdw/assets/tests-workflow/  (10 min)
  - SNAPSHOT Assets:             https://oss.sonatype.org/content/repositories/snapshots/com/centurylink/mdw/assets/tests-workflow/
  - Buildpack:          https://github.com/CenturyLinkCloud/mdw-buildpack/tree/master/resources/mdw
  - Internal buildpack: https://ne1itcprhas62.ne1.savvis.net/PCF_Buildpacks_PUB_DEV/mdw-buildpack/tree/master/resources/mdw

6 - Run task 1,2 & 4 and commit the files right away for the post-release snapshot (to prevent another commit from auto-publishing).

7 - (Formal builds only) On GitHub:
  - Create a milestone marker for the next build. (https://github.com/CenturyLinkCloud/mdw/milestones/new)
  - Assign any un-delivered issues and pull request for current build's milestone to the next build's milestone.
  - Close this build's milestone in GitHub.
    
8 - Release Notes
  - If you are doing it first time then install ruby (https://github.com/CenturyLinkCloud/mdw#documentation) and do following in root of your workspace dir 
    `gem install github_changelog_generator`
  - Set the CHANGELOG_GITHUB_TOKEN environment variable to your 40 digit token from GitHub
  - Run following command in root of your workspace
  github_changelog_generator --no-pull-request  --filter-by-milestone --future-release '6.0.xx' --exclude-labels designer,internal,wontfix,duplicate,documentation
  - git pull
  - git commit CHANGELOG.md -m "Release notes [skip ci]" 
  - git push (commits and pushes generated CHANGELOG.md to GitHub)
  - Update the new release on GitHub, release name should be 6.0.xx, copy the notes from updated CHANGELOG.md
  - Change release status from pre-release to release
  - Check if mdw-cli-{{version}}.zip and mdw-boot-{{version}}.jar binaries are uploaded, Jenkins publish task should have done that.

9 - Update any support items delivered with this build to Resolved status.
  - Delete any obsolete branches on GitHub that were merged as part of this build.

10a - Update mdw-demo
  - git pull
  - In framework workspace Copy https://ne1itcprhas62.ne1.savvis.net/MDW_DEV/mdw60_internal/blob/master/local.gradle to mdw folder (update mdwDemoDir based on your local setup) 
  - make sure you have curl.exe in your path
  - Run the Gradle task mdw/updateMDWDemo to copy latest framework assets, update gradle.properties, manifest file in mdw-demo workspace
  - Commit and push to git (manifest.yml and mdw.properties should not be committed)
   
10b - Upgrade mdw-demo to new version of mdw by clicking on project properties and selecting new version
  -  Refresh Gradle dependencies
  -  Commit and push new version of com.centurylink.mdw.plugin.xml to git

11 - Publishing to AppFog  
   -  go to root of mdw-demo project (check correct dev/prod manifest.yml is there)
   -  cf login -a https://api.useast.appfog.ctl.io -o MDWF -u manoj.agrawal@centurylink.com
   -  Select a space: Prod for formal, and Dev for snapshots
   -  cf push
  
TODO: Javadocs, buildpack     