Building MDW 6
==============

1 - Edit gradle.properties to set the new build numbers.
    a) Update mdwCommonVersion to set the MDW 5.5 build that this depends on.
    b) Update mdwVersion.  Until we publish a fully-contained 6.0 build, our convention is that 
       the last two digits of the 6.0 build number equate to the MDW 5.5 build that this depends on.
       For example 6.0.025 depends on 5.5.25, and 6.0.036 will depend on 5.5.36.
    c) Update version in mdw-admin/package.json (omit -SNAPSHOT).
    d) Update version in mdw-admin/bower.json (omit -SNAPSHOT).
    e) Update path in mdw-admin/manifest.yaml to the new version.
   
2 - Commit and push these changes to Git.

3 - Run mdw-admin Gradle task getMdwCommon to pull in the desired mdw-schemas and mdw-common jars.
    Commit and push the updated jars to Git (removing the old ones). 
 
3 - Perform the Jenkins build (http://lxdenvmtc143.dev.qintra.com:8181/jenkins):
    - MDW6-Build
    - Review console output for errors.
    
4 - Deploy the new build on the test server:
     - Jenkins build: "MDW6-Deploy"

(After running tests)     
5 - Publish the build from Jenkins (http://lxdenvmtc143.dev.qintra.com:8181/jenkins):
     - Jenkins build: "MDW6-Publish Snapshot" or "MDW6-Publish Formal"
     - Review console output for errors.
     
     TODO: Steps to publish a new buildpack version.