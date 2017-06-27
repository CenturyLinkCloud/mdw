---
permalink: /docs/guides/cli/
title: MDW Command Line Interface
---
## MDW CLI
  The [MDW CLI](../cli) gives you commands for common operations like initializing your workspace and updating assets. 

### Installation
   - Download **mdw-cli.zip** from the latest MDW release on GitHub:   
     https://github.com/CenturyLinkCloud/mdw/releases
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
          --user
            Dev user
            Default: dxoakes

    update      Update MDW assets
      Usage: update [options]
        Options:
          --base-asset-packages
            MDW Base Asset Packages (comma-separated)
          --discovery-url
            Asset Discovery URL
            Default: https://mdw.useast.appfog.ctl.io/mdw
          --mdw-version
            MDW Version

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

  ```      