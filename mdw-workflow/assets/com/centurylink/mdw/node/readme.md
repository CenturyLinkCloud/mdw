## node js for MDW
**Note:** the following two node modules have been CUSTOMIZED:
 - react-bootstrap-date-picker
 - react-select
Make sure they do not get overwritten via `npm install`

Forked modules:
- react-custom-scrollbars fork:
  https://github.com/mdw-dev/react-custom-scrollbars
   - (clone and copy in all but .git and node_modules)
   - edit package.json to replace `main: src/index.js` with `main: lib/index.js`