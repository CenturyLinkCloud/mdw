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
   - (copy all but .git and node_modules into mdw-workflow/assets/com/centurylink/mdw/node/node_modules/react-custom-scrollbars)
   - edit package.json to replace `main: src/index.js` with `main: lib/index.js`