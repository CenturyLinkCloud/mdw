---
permalink: /docs/getting-started/cli/
title: Command Line Interface
---
## MDW CLI
  The MDW Command Line Interface has commands for common operations like initializing a workspace and updating assets. 
  - [Installation](#installation)
  - [Usage](#usage)
  - [Quick Start](#quick-start)
  - [Examples](#examples)
  
### Installation
   - Make sure Java 8 JDK is installed and on your system path (before any non-JDK JREs).
   - Download **mdw-cli.zip** from the latest MDW release on GitHub:<br>   
     <https://github.com/CenturyLinkCloud/mdw/releases>
   - Unzip anywhere on your hard drive.
   - Create an environment variable named MDW_HOME pointing to this location, and add its bin directory to your system PATH. 
   
### Usage
  `mdw [command] [command options]`<br>
  Most commands work best if you `cd` into the main project directory before executing.  The exceptions are `init` and `import`,
  which you may want to run before a project directory even exists.  When executed in a valid MDW main project directory, commands
  will find values such as asset-loc and git-remote-url from the project config files.  Otherwise you'll have to include all
  required (non-defaulted) settings via the options shown below.  
  ```
  Commands:
    help      Syntax Help
      Usage: help

    init      Initialize an MDW project
      Usage: init [options] <project>

    import      Import assets from Git (HARD RESET!)
      Usage: import [options]

    update      Update MDW assets locally via Discovery
      Usage: update [options]

    install      Install MDW
      Usage: install [options]

    run      Run MDW
      Usage: run [options]

    version      MDW CLI Version
      Usage: version

    git      Git commands
      Usage: git [args]

    status      Project status
      Usage: status [options]

    archive      Asset ref info (--show for contents)
      Usage: archive [options]
      
    General Options ("mdw help" for command-specific options):
      --asset-loc
        Asset location
        Default: assets
      --base-asset-packages
        MDW Base Asset Packages (comma-separated)
      --database-password
        DB Password
        Default: mdw
      --database-url
        JDBC URL (without credentials)
        Default: jdbc:mariadb://localhost:3308/mdw
      --database-user
        DB User
        Default: mdw
      --debug
        Display CLI debug information
        Default: false
      --discovery-url
        Asset Discovery URL
        Default: https://mdw.useast.appfog.ctl.io/mdw
      --force
        Force overwrite, even on localhost or when branch disagrees
        Default: false
      --git-branch
        Git branch
        Default: master
      --git-password
        Git password
      --git-remote-url
        Git repository URL
        Default: https://github.com/CenturyLinkCloud/mdw-demo.git
      --git-user
        Git user
        Default: anonymous
      --mdw-version
        MDW Version
      --releases-url
        MDW releases Maven repo URL
        Default: http://repo.maven.apache.org/maven2
      --services-url
        MDW service base URL
        Default: http://localhost:8080/mdw/services
      --snapshots
        Whether to include snapshot builds
        Default: false    
  ```      

### Quick Start
  The [Quick Start](../quick-start/) has step-by-step instructions on how to use the CLI to get
  running quickly with MDW.
  
### Examples
  - Install mdw.war for Tomcat on Linux
  ```
  mdw install --webapps-dir=/var/lib/tomcat8/webapps --mdw-version=6.0.08
  ```
  - MDW status
  ```
  mdw status
  ```
  - Git Status
  ```
  mdw git status
  ```
  - Git Log for Author with Grep
  ```
  mdw git log --author donaldoakes --grep cli
  ```