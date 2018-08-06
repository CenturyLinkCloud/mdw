## Publishing an mdw6 build

1 - Edit gradle.properties to set the new build numbers.
    - mdwVersion
    - mdwPrevTag
    
2 - Run Gradle task updateMdwVerInFiles to update these files: 
  - mdw-workflow/.settings/com.centurylink.mdw.plugin.xml
  - mdw-hub/package.json
  - RestApiDefinition.java
  - **/.mdw/package.json

3 - (Brand new point-release -- eg: moving from 6.1 to 6.2):
  - Clean out schemaUpgradeQueries in mdw-common/src/META-INF/mdw/db/mysql.json and oracle.json. 

4 - (Formal builds only) Clean Tag
  - Go to https://github.com/CenturyLinkCloud/mdw/tags
  - Delete SNAPSHOT release and tag
  - git pull
  
5 - Commit and push all the above changes to Git (normally plugin.xml and gradle.properties).
  - Travis CI will run the build, tests and publish to maven-central or sonatype.
  - Compilation or testing errors will prevent the build from being published.

6 - After success, verify release artifacts are published to Maven Central (https://oss.sonatype.org/#stagingRepositories)
  - Formal build:       http://repo.maven.apache.org/maven2/com/centurylink/mdw/ (10 min)
  - SNAPSHOT:           https://oss.sonatype.org/content/repositories/snapshots/com/centurylink/mdw/ 
  - Assets:             http://repo.maven.apache.org/maven2/com/centurylink/mdw/assets/tests-workflow/  (15 min)
 
7 - (Formal builds only) On GitHub:
  - Make sure all the closed issues have milestone assigned otherwise they will not be reported in release notes.
  - Create a milestone marker for the next build. (https://github.com/CenturyLinkCloud/mdw/milestones/new)
  - Assign any un-delivered issues and pull request for current build's milestone to the next build's milestone.
  - Close this build's milestone in GitHub.
    
8 - Release Notes
  - If you are doing it first time then install ruby (https://github.com/CenturyLinkCloud/mdw#documentation) and do following in root of your workspace dir 
    `gem install github_changelog_generator`
  - Set the CHANGELOG_GITHUB_TOKEN environment variable to your 40 digit token from GitHub
  - Run following command in root of your workspace
  github_changelog_generator --no-pull-request  --filter-by-milestone --future-release '6.1.xx' --exclude-labels designer,internal,wontfix,duplicate,documentation
  - git pull
  - Review/Update CHANGELOG.md and then: `git commit CHANGELOG.md -m "Release notes [skip ci]"` 
  - git push (pushes generated CHANGELOG.md to GitHub)
  - Update the new release on GitHub, copy the notes from updated CHANGELOG.md

9 - Update any support items delivered with this build to Resolved status.
  - Delete any obsolete branches on GitHub that were merged as part of this build.

10a - Update mdw-demo
  - git pull 
  - In framework workspace Copy https://ne1itcprhas62.ne1.savvis.net/MDW_DEV/mdw60_internal/blob/master/local.gradle to mdw folder (update mdwDemoDir based on your local setup) 
  - Run the Gradle task mdw/updateMDWDemo to copy latest framework assets, update gradle.properties, manifest file in mdw-demo workspace
   
10b - Upgrade mdw-demo to new version of mdw by clicking on project properties and selecting new version
  - Refresh Gradle dependencies
  - Commit and push new version of com.centurylink.mdw.plugin.xml to git
  - Commit and push to git (manifest.yml and cloud package should not be committed)

11 - Run task 1,2 & 4 and commit the files right away for the post-release snapshot (to prevent another commit from auto-publishing).

12 - Create and publish Docker image
    - Log into 143 server and sudo su - mdwapp, then go to directory with cloned Git repo (/app/prod/jack/mdw/mdw).
    - git pull
    - Create docker image with following command:
        docker build --build-arg version=6.1.04 -t mdwcore/mdw:6.1.04 .   (update with actual MDW version)
    - Log into docker using the following command (use your Docker Hub credentials when it prompts you)
        docker login
    - Publish image to Docker repository with command
        docker push mdwcore/mdw:6.1.04   (update with actual MDW version)
        
13 - Internal Assets
   - TODO
