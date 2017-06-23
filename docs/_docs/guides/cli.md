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
   - Create an environment variable named MDW_HOME pointing to this location, and add it to your system PATH. 
   
### Usage
  `mdw [command] [command options]`
  ```
    Commands:
      help      Syntax Help
        Usage: help
  
      init      Initialize an MDW project
        Usage: init [options] <project>
          Options:
            --discovery-url
              Asset Discovery URL
              Default: https://mdw.useast.appfog.ctl.io/mdw
            --mdw-version
              MDW Version
              Default: 6.0.04
            --releases-url
              MDW Releases Maven Repo URL
              Default: http://repo.maven.apache.org/maven2
  
      update      Update an MDW project
        Usage: update
  
      version      MDW CLI Version
        Usage: version
  ```      