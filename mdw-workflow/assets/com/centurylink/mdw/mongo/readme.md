## MongoDB Package

Document content can optionally be stored in a [MondgoDB](https://docs.mongodb.com/manual/administration/install-community/) database.

## Dependencies
  - [com.centurylink.mdw.base](https://github.com/CenturyLinkCloud/mdw/blob/master/mdw-workflow/assets/com/centurylink/mdw/base/readme.md)

## Setup
To enable, add something like this to your mdw.yaml:
```
mongodb:
  host: localhost
  port: 27017
``` 