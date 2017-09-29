---
permalink: /docs/getting-started/cli/
title: MDW Command Line Interface
---
## MDW CLI
  The MDW Command Line Interface has commands for common operations like initializing a workspace and updating assets. 
  - [Installation](#installation)
  - [Usage](#usage)
  - [Quick Start](#quick-start)
  - [Examples](#examples)
  
### Installation
   - Make sure Java 8 JDK is installed and on your system path.   
   - Download **mdw-cli.zip** from the latest MDW release on GitHub:<br>   
     <https://github.com/CenturyLinkCloud/mdw/releases>
   - Unzip anywhere on your hard drive.
   - Create an environment variable named MDW_HOME pointing to this location, and add its bin directory to your system PATH. 
   
### Usage
  `mdw [command] [command options]`
  ```
  Commands:
    help      Syntax Help
      Usage: help

    init      Initialize an MDW project
      Usage: init [options] <project>
        Options:
          --asset-loc
            Asset location
            Default: assets
          --base-asset-packages
            MDW Base Asset Packages (comma-separated)
          --cloud-foundry
            Generate a Cloud Foundry manifest.yml file
            Default: false
          --discovery-url
            Asset Discovery URL
            Default: https://mdw.useast.appfog.ctl.io/mdw
          --eclipse
            Generate Eclipse workspace artifacts
            Default: true
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
          --maven
            Generate a Maven pom.xml build file
            Default: false
          --mdw-version
            MDW Version
          --releases-url
            MDW Releases Maven Repo URL
            Default: http://repo.maven.apache.org/maven2
          --snapshots
            Whether to include snapshot builds
            Default: false
          --spring-boot
            Generate Spring Boot build artifacts (currently only Gradle)
            Default: false
          --user
            Dev user
            Default: donald

    import      Import assets from Git
      Usage: import [options] <project>
        Options:
          --asset-loc
            Asset location
            Default: assets
          --base-asset-packages
            MDW Base Asset Packages (comma-separated)
          --discovery-url
            Asset Discovery URL
            Default: https://mdw.useast.appfog.ctl.io/mdw
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
            MDW Releases Maven Repo URL
            Default: http://repo.maven.apache.org/maven2

    update      Update MDW assets
      Usage: update [options]
        Options:
          --asset-loc
            Asset location
            Default: assets
          --base-asset-packages
            MDW Base Asset Packages (comma-separated)
          --discovery-url
            Asset Discovery URL
            Default: https://mdw.useast.appfog.ctl.io/mdw
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
            MDW Releases Maven Repo URL
            Default: http://repo.maven.apache.org/maven2

    install      Install MDW
      Usage: install [options]
        Options:
          --binaries-url
            MDW Binaries URL
            Default: https://github.com/CenturyLinkCloud/mdw/releases
          --mdw-version
            MDW Version

    run      Run MDW
      Usage: run [options]
        Options:
          --binaries-url
            MDW Binaries
            Default: https://github.com/CenturyLinkCloud/mdw/releases
          --vm-args
            Java VM Arguments (enclose in quotes)
            Default: [-Dmdw.runtime.env=dev, -Dmdw.config.location=config]

    version      MDW CLI Version
      Usage: version

    git      Git commands
      Usage: git [options]
        Options:
          args
            pass-thru jgit arguments
            Default: []
  ```      

### Quick Start
  The [Quick Start](../quick-start/) has step-by-step instructions on how to use the CLI to get
  running quickly with MDW.
  
### Examples
  - Git Status
  ```
  mdw git status
  ```
  - Git Log for Author with Grep
  ```
  mdw git log --author donaldoakes --grep cli
  ```