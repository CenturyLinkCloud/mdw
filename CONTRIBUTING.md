## Contributing to MDW
We welcome your contributions to MDW whether they be fixes/enhancements, automated tests, documentation, bug reports or feature requests.

### Issues
  - Issues must have appropriate labels and a proposed milestone.

### Pull Requests
  - All commits are against an issue, and your comment should reference the issue it addresses: `Fix #<issue number>`
  - These labels are not included in release notes
    - internal
    - wontfix
    - duplicate
    - documentation

### Developer Setup
1. Prerequisites
   - IntelliJ IDEA Community Edition:                              
     https://www.jetbrains.com/idea/download/
   - MDW Studio Plugin:
     http://centurylinkcloud.github.io/mdw/docs/guides/mdw-studio/

2. Get the Source Code
   - Command-line Git:  
     `git clone https://github.com/CenturyLinkCloud/mdw.git mdw6`

3. Set up npm (One-time step)
   - Install NodeJS:                                                                     
     https://docs.npmjs.com/getting-started/installing-node
   - Open a command prompt in the mdw-hub project directory
     ```
     npm install
     ```

4. Build in IntelliJ IDEA
   - Run IntelliJ IDEA Community Edition
   - Select Open Project and browse to mdw/mdw
   - In the Gradle tool window, execute the buildDev task.

5. Use [Embedded DB](/mdw-workflow/assets/com/centurylink/mdw/db/readme.md) or set up an external MySQL database as described in [this readme](/mdw/database/mysql/readme.txt)

6. Edit configuration files to suit local environment:
   - mdw/config/mdw.yaml (locally, use absolute paths for mdw.asset.location and mdw.git.local.path)
   - mdw/config/access.yaml (set devUser to yourself)
   - mdw/config/seed_users.json

7. Run Spring Boot Jar in IntelliJ IDEA
   - From the IntelliJ menu select Run > Edit Configurations.
   - Click **+** > Jar Application.
   - For Path to Jar, browse to mdw/deploy/app/mdw-boot-6.1.XX-SNAPSHOT.jar
   - VM Options: `-Dmdw.runtime.env=dev -Dmdw.config.location=mdw/config`
   - Save the configuration and type ctrl-alt-R to run/debug MDW.

8. MDWHub Web Development
   - To avoid having to reassemble the boot jar to test web content changes, add this to your IntelliJ run configuration:
     `-Dmdw.hub.dev.override.root=../mdw-hub/web`

9. Run the Tests
   - Access the autotest page in MDWHub:
     http://localhost:8080/mdw/#/tests
   - Use the Settings button to enable Stubbing and increase Threads to 10
   - Select all test cases, and execute

### Code Format
   - https://centurylinkcloud.github.io/mdw/docs/code/format/

### MDW Studio Development
   - https://github.com/CenturyLinkCloud/mdw-studio/blob/master/CONTRIBUTING.md

### Documentation
1. Source
   - Documentation is in the docs directory of the master branch on GitHub
   - **Important:** When editing .md files in IntelliJ, to preserve trailing whitespace characters, install the Markdown plugin:
     - https://plugins.jetbrains.com/plugin/7793-markdown-support
     Do not commit changes to markdown files that remove trailing whitespace.
2. Local GitHub Pages
   - To test doc access before pushing, and to make changes to default layouts and styles, you can build through [Jekyll](https://help.github.com/articles/about-github-pages-and-jekyll/) locally.
   - Install Ruby 2.3.3 or higher and add its bin directory to your PATH.
   - Install Bundler:
     ```
     gem install bundler
     ```
   - Download the CURL CA Certs from http://curl.haxx.se/ca/cacert.pem and save in your Ruby installation directory.
   - Set environment variable SSL_CERT_FILE to point to this this cacert.pem file location.
   - Install Ruby DevKit: https://github.com/oneclick/rubyinstaller/wiki/Development-Kit
   - Install Jekyll and all its dependencies (in the docs directory):
     ```
     bundle install
     ```
   - Build GitHub pages site locally (in the docs directory):
     ```
     bundle exec jekyll serve --incremental --watch --baseurl ''
     ```
   - Access locally in your browser:
     http://127.0.0.1:4000/
