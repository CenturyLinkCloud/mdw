## building mdw6

0 - Edit gradle.properties to set the new build numbers.
    - mdwVersion
    - mdwDesignerVersion (latest published version)
    
0 - Edit mdw-workflow/.settings/com.centurylink.mdw.plugin.xml:
    - mdwFramework

0 - Edit mdw-hub/package.json:
    - version (omit -SNAPSHOT)

0 - Edit mdw-hub/bower.json:
    - version (omit -SNAPSHOT)

0 - Edit mdw-hub/manifest.yaml:
    - path (point to new war version)

0 - Edit RestApiDefinition.java (TODO: parameterize):
    - info annotation

0 - Edit Init.java (TODO: parameterize):
    - mdw-version parameter annotation

0 - Do a local Gradle build and Run exportAssetPackages task to update **/.mdw/package.json files
    
0 - Commit and push these changes to Git.

0 - Run all tests locally (TODO: on build box -- depends on issue #14).

0 - Perform the Jenkins build (http://lxdenvmtc143.dev.qintra.com:8181/jenkins):
  - MDW6-Build
  - Review console output for errors.
  - MDW6-Deploy  (You might have to start the server manually if this task does not do automatically)
  - Login to mdw-hub and run all the test cases (select Stubbing from configure icon)

0 - Publish using Jenkins (http://lxdenvmtc143.dev.qintra.com:8181/jenkins):
  - MDW6-Publish-Formal (or -Snapshot)
  - Publish to Maven Central using mdw6-publish-maven-central (for formal build only) 
  - Follow the steps of closing and releasing the build here [Steps] (http://central.sonatype.org/pages/releasing-the-deployment.html)
  - You should see new build here http://repo.maven.apache.org/maven2/com/centurylink/mdw/
  - Review console output for errors.

0 - On GitHub:
  - Close any open issues delivered with this build.
  - Create a milestone marker for the next upcoming build.
  - Assign any undelivered issues for this build's milestone to the next build's milestone.
  - Close this build's milestone in GitHub.
  
0 - Release Notes
  - If you are doing it first time then install ruby (https://github.com/CenturyLinkCloud/mdw#documentation) and do following in root of your workspace dir 
    `gem install github_changelog_generator`
  - github_changelog_generator --exclude-labels designer,internal,wontfix,duplicate,documentation --exclude-tags-regex  [v9.*] --no-pull-request --future-release v6.0.03
  - commit and push generated CHANGELOG.md to GitHub 
  - git commit CHANGELOG.md -m "Release notes"
  - Create the release on GitHub, copy the notes from CHANGELOG.md

0 - Update support items delivered with this build to Resolved status.

0 - Update mdw-demo
  - Update assets
  -  Remove all the folders under com/centurylink/mdw except demo
  -  Copy assets from mdw-workflow to mdw-demo
  -  Update MDW_VERSION in manifest.yml and manifest.yml.prod
  -  Update mdwVersion in gradle.properties
  -  Update version in com.centurylink.mdw.plugin.xml
  -  Run test cases from mdw-hub in mdw-demo environment
  -  Commit and push to git 
    
0 - mdw-buildpack
   - clone https://github.com/mdw-dev/mdw-buildpack.git
   - replace mdw*.war with one from latest published war from http://repo.maven.apache.org/maven2/com/centurylink/mdw/mdw/
   - commit and push  
    
0 - Publishing to AppFog  
   -  go to root of mdw-demo
   -  cf login -a https://api.useast.appfog.ctl.io -o MDWF -u manoj.agrawal@centurylink.com
   -  cf push

0 - TODO: Publish NPM package  
    