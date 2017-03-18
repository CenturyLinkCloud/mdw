Building MDW 6
==============

1 - Edit gradle.properties to set the new build numbers.
    a) Update mdwVersion.
    b) Update mdwDesignerVersion to latest published version.
    
2 - Edit mdw-hub/package.json:
    - Update version (omit -SNAPSHOT)

3 - Edit mdw-hub/bower.json:
    - Update version (omit -SNAPSHOT)

4 - Edit mdw-hub/manifest.yaml:
    - Set path to point to new war version
   
5 - Commit and push these changes to Git.

6 - Perform the Jenkins build (http://lxdenvmtc143.dev.qintra.com:8181/jenkins):
    - MDW6-Build
    - Review console output for errors.
    