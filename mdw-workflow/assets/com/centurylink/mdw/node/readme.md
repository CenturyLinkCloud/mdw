## node js for MDW
**Note:** the following two node modules have been CUSTOMIZED:
 - react-bootstrap-date-picker
 - react-select
Make sure they do not get overwritten via `npm install`

Forked modules:
- react-custom-scrollbars fork:
   - `git clone https://github.com/mdw-dev/react-custom-scrollbars.git`
   - `cd react-custom-scrollbars`
   - `npm install`
   - `npm run build`
   - (copy lib dir and top-level files into mdw-workflow/assets/com/centurylink/mdw/node/node_modules/react-custom-scrollbars)
   - edit package.json to replace `"main": "src/index.js"` with `"main": "lib/index.js"`

To install a new package:
  1. Delete node_modules directory
  2. Unzip node_modules.zip, and copy somewhere as a backup
  3. Delete node_modules.zip
  4. npm install <package> --save
  5. npm install
  6. Replace these directories into node_modules from backup:
     - react-bootstrap-date-picker
     - react-custom-scrollbares
     - react-select
  7. Rezip node_modules.zip, then remove node_modules dir
  8. Delete temp dir to force JSX recompile
  9. Start server, and test that stuff still works (esp Filepanel)
  10. Commit node_modules.zip.
