## Publishing an mdw6 build

### TODO: avoid signing snapshots?

1. Edit these files to set the new build number:
  - mdw/gradle.properties:
    - mdwVersion
    - mdwPrevTag
  - mdw/project.yaml:
    - mdw.version

2. Run Gradle task updateMdwVerInFiles to update these files:
  - mdw-hub/package.json
  - RestApiDefinition.java
  - all package.yaml files

3. Comment/uncomment these CLI tests:
  - mdw/cli/tests/quickstart.bats (line 52 -- skip for formal/1st shapshot build, add back for second/subsequent snapshots)
  - mdw/cli/tests/convert.bats (line 18 -- skip for formal/1st snapshot builds, add back for second/subsequent snapshots)
      - comment/uncomment skip (formal)
        TODO: better way of handling -- this is because mdw.version is something that hasn't been published yet (formal and first snapshot)

4. (Brand new point-release -- eg: moving from 6.1 to 6.2):
  - Clean out schemaUpgradeQueries in mdw-common/src/META-INF/mdw/db/mysql.json and oracle.json.

5. (Formal builds only) Clean Tag
  - Open browser to https://github.com/CenturyLinkCloud/mdw/tags
  - Delete SNAPSHOT release and tag
  - git pull

6. Commit and push all the above changes to Git (normally gradle.properties, project.yaml and maybe CLI tests).
  - Travis CI will run the build, tests and publish to maven-central or sonatype.
  - Compilation or testing errors will prevent the build from being published.

7. After success:
  Manually close/release from [Nexus Repository Manager](https://oss.sonatype.org/#welcome) (don't want to automating this)
  Verify repository contains artifacts:
  7a. (Formal Build)
    - Repository: http://repo.maven.apache.org/maven2/com/centurylink/mdw/ (20-30 min)
  7b. (Snapshot Build)
    - Snapshot repo: https://oss.sonatype.org/content/repositories/snapshots/com/centurylink/mdw/

8. (Formal builds only) On GitHub:
  - Make sure all the closed issues have the current milestone assigned; otherwise they will not be included in release notes.
  - Create a milestone marker for the next build. (https://github.com/CenturyLinkCloud/mdw/milestones/new)
  - Assign any un-delivered issues and pull request for current build's milestone to the next build's milestone.
  - Close this build's milestone in GitHub.

9. Release Notes
  - If you are doing it first time then install ruby (https://github.com/CenturyLinkCloud/mdw#documentation) and do following in root of your workspace dir
    `gem install github_changelog_generator`
  - Set the CHANGELOG_GITHUB_TOKEN environment variable to your 40 digit token from GitHub
  - Run following command in root of your workspace
    ```
    github_changelog_generator --no-pull-request  --filter-by-milestone --future-release '6.1.xx' --exclude-labels designer,internal,wontfix,duplicate,documentation
    ```
  - git pull
  - Review/Update/Merge CHANGELOG.md (retaining old Compatibility Notes sections).
  - Commit (with `[skip ci]`) and push merged CHANGELOG.md
  - Update the new release on GitHub (https://github.com/CenturyLinkCloud/mdw/releases), copying the notes from updated CHANGELOG.md

10. Run task 1, 2, 3 & 6 and commit the files right away for the post-release snapshot (to prevent another commit from auto-publishing).
    - Mark build as "This is a pre-release" on GitHub

11. See mdw-ctl-internal build.md.

12. Create and publish Docker image
    - Log into 143 server and sudo su - mdwapp, then go to directory with cloned Git repo (/app/prod/jack/mdw/mdw).
    - git pull
    - Create docker image with following command:
        docker build --build-arg version=6.1.04 -t mdwcore/mdw:6.1.04 .   (update with actual MDW version)
    - Log into docker using the following command (use your Docker Hub credentials when it prompts you)
        docker login
    - Publish image to Docker repository with command
        docker push mdwcore/mdw:6.1.0X   (update with actual MDW version)

13. - Upgrade mdw-demo 
   - Wait until new build is searchable on Maven Central:    
     https://search.maven.org/search?q=mdw-common (> 2hrs)
   - Update mdw version in the following files:
       - mdw-demo/gradle.properties
       - mdw-demo/pom.xml
       - mdw-demo/project.yaml
   - Update the framework assets:
     ```
     cd mdw-demo
     mdw update (--snapshots)
     ```
  - Commit and push changes
