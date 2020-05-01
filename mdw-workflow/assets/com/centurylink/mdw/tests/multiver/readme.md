# MDW Multiver Tests
Automated tests related to multiple simultaneous asset versions.

Note: DynamicUpdate.test should only be run in isolation since it updates assets, refreshes 
the server cache, and changes package.yaml.  It's excluded from CI testing but can be executed 
by itself in MDWHub.

## Dependencies
  - [com.centurylink.mdw.base](https://github.com/CenturyLinkCloud/mdw/blob/master/mdw-workflow/assets/com/centurylink/mdw/base/readme.md)
  - [com.centurylink.mdw.testing](https://github.com/CenturyLinkCloud/mdw/blob/master/mdw-workflow/assets/com/centurylink/mdw/testing/readme.md)
  - [com.centurylink.mdw.tests.services](https://github.com/CenturyLinkCloud/mdw/blob/master/mdw-workflow/assets/com/centurylink/mdw/tests/services/readme.md)

