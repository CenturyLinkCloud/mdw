## node js for MDW
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
