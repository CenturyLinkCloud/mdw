## Node.js Package
The [Node.js](https://nodejs.org/en/) package is is not intended for for direct client app use.  Rather, it's
a dependency for the MDW [React package](https://github.com/CenturyLinkCloud/mdw/blob/master/mdw-workflow/assets/com/centurylink/mdw/react/readme.md).

Includes [react-bootstrap](https://react-bootstrap.github.io/components/alerts) components.

## Dependencies
  - [com.centurylink.mdw.base](https://github.com/CenturyLinkCloud/mdw/blob/master/mdw-workflow/assets/com/centurylink/mdw/base/readme.md)

## Maintaining
Forked modules (see package.json):
  - react-bootstrap-date-picker
  - react-custom-scrollbars

To install a new package:
  1. Delete node_modules directory and node_modules.zip
  2. npm install
  3. npm install `<package>` --save
  4. Rezip node_modules.zip: 
     zip -r -X node_modules.zip node_modules -x "*.DS_Store"
  5. Delete node_modules dir again
  6. Delete temp dir to force JSX recompile
  7. Start server, and test that stuff still works (esp filepanel scrolling large files)
  8. Commit node_modules.zip.
