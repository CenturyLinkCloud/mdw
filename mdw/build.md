## Publishing an mdw6 build

1 - Edit these files to set the new build number:
    mdw/gradle.properties:
      - mdwVersion
      - mdwPrevTag
    mdw/project.yaml:
      - mdw.version
    mdw/cli/tests/quickstart.bats (line 53 -- formal builds only)
      - add skip (formal)
        (TODO: better way of handling)

2 - Run Gradle task updateMdwVerInFiles to update these files:
  - mdw-hub/package.json
  - RestApiDefinition.java
  - all package.yaml files

3 - (Brand new point-release -- eg: moving from 6.1 to 6.2):
  - Clean out schemaUpgradeQueries in mdw-common/src/META-INF/mdw/db/mysql.json and oracle.json.

4 - (Formal builds only) Clean Tag
  - Go to https://github.com/CenturyLinkCloud/mdw/tags
  - Delete SNAPSHOT release and tag
  - git pull

5 - Commit and push all the above changes to Git (normally gradle.properties and project.yaml).
  - Travis CI will run the build, tests and publish to maven-central or sonatype.
  - Compilation or testing errors will prevent the build from being published.

6 - After success, verify repository contains artifacts.
  6a. (Formal Build)
    - Formal build repo : http://repo.maven.apache.org/maven2/com/centurylink/mdw/ (10 min)
    - Assets: http://repo.maven.apache.org/maven2/com/centurylink/mdw/assets/tests-workflow/  (15 min)
  6b. (Snapshot Build)
    - Snapshot repo: https://oss.sonatype.org/content/repositories/snapshots/com/centurylink/mdw/

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
    ```
    github_changelog_generator --no-pull-request  --filter-by-milestone --future-release '6.1.xx' --exclude-labels designer,internal,wontfix,duplicate,documentation
    ```
  - git pull
  - Review/Update CHANGELOG.md and then: `git commit CHANGELOG.md -m "Release notes [skip ci]"`
  - git push (pushes generated CHANGELOG.md to GitHub)
  - Update the new release on GitHub, copy the notes from updated CHANGELOG.md

9 - Run task 1, 2 & 5 and commit the files right away for the post-release snapshot (to prevent another commit from auto-publishing).

10 - Create and publish Docker image
    - Log into 143 server and sudo su - mdwapp, then go to directory with cloned Git repo (/app/prod/jack/mdw/mdw).
    - git pull
    - Create docker image with following command:
        docker build --build-arg version=6.1.04 -t mdwcore/mdw:6.1.04 .   (update with actual MDW version)
    - Log into docker using the following command (use your Docker Hub credentials when it prompts you)
        docker login
    - Publish image to Docker repository with command
        docker push mdwcore/mdw:6.1.0X   (update with actual MDW version)

11 - Upgrade mdw-demo (Need to wait until asset zips are queryable on Maven Central):
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
