## Publishing an MDW Build

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
  
3. Run Gradle task exportSourceImpls to update CLI impl files with latest.

4. Comment/uncomment these CLI tests:
  - mdw/cli/tests/quickstart.bats (line 53 -- skip for formal/1st shapshot build, add back for second/subsequent snapshots)
  - mdw/cli/tests/convert.bats (line 25 -- skip for formal/1st snapshot builds, add back for second/subsequent snapshots)
    TODO: better way of handling -- this is because mdw.version is something that hasn't been published yet (formal and first snapshot)

5. (Brand new point-release -- eg: moving from 6.1 to 6.2):
  - Clean out schemaUpgradeQueries in mdw-common/src/META-INF/mdw/db/mysql.json and oracle.json.

6. (Formal builds only) Clean Tag
  - Open browser to https://github.com/CenturyLinkCloud/mdw/tags
  - Delete SNAPSHOT release and tag
  - git pull

7. Commit and push all the above changes to Git (normally gradle.properties, project.yaml, package.yamls and maybe CLI tests for formal build).
  - Travis CI will run the build, tests and publish to maven-central or sonatype.
  - Compilation or testing errors will prevent the build from being published.

8. After success:
  Manually close/release from [Nexus Repository Manager](https://oss.sonatype.org/#welcome) (don't want to automate this).
  Verify repository contains artifacts:
  8a. (Formal Build)
    - Repository: https://repo.maven.apache.org/maven2/com/centurylink/mdw/ (20-30 min)
  8b. (Snapshot Build)
    - Snapshot repo: https://oss.sonatype.org/content/repositories/snapshots/com/centurylink/mdw/

  Note : If it doesnt show up in the repository make sure the user has read /write access to nexus mdw repo

9. (Formal builds only) On GitHub:
  - Make sure all closed issues have the current milestone assigned; otherwise they will not be included in release notes.
  - Create a milestone marker for the next build. (https://github.com/CenturyLinkCloud/mdw/milestones/new)
  - Assign any undelivered issues and pull requests for the current build's milestone to the next build's milestone.
  - Close this build's milestone in GitHub.

10. Release Notes
  - git pull
  - If you are doing it first time then install ruby (https://github.com/CenturyLinkCloud/mdw#documentation) and do following in root of your workspace dir
    `gem install github_changelog_generator`
  - Set the CHANGELOG_GITHUB_TOKEN environment variable to your 40 digit token from GitHub
  - Run following command in root of your workspace
    ```
    github_changelog_generator --no-pull-request  --filter-by-milestone --future-release '6.1.xx' --exclude-labels designer,internal,wontfix,duplicate,documentation
    ```
  - Review/Update/Merge CHANGELOG.md (retaining old Compatibility Notes sections).
  - Commit (with `[skip ci]`) and push merged CHANGELOG.md
  - Update the new release on GitHub (https://github.com/CenturyLinkCloud/mdw/releases), copying the notes from updated CHANGELOG.md

11. Run task 1, 2, 4 & 7 and commit the files right away for the post-release snapshot (to prevent another commit from auto-publishing).
    - Mark snapshot release as "This is a pre-release" on GitHub

12. See mdw-ctl-internal build.md.

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
     mdw update
     ```
  - Commit and push changes
