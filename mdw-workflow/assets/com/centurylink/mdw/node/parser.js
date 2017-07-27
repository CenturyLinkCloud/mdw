'use strict';

//prevent unhandled errors from crashing the VM
process.on('unhandledRejection', (err) => {
  console.error('Unhandled Rejection', err);
  if (err.stack)
    console.error(err.stack);
});
process.on('uncaughtException', (err) => {
  console.error('Uncaught Exception', err);
  if (err.stack)
    console.error(err.stack);
});

// Pre-parses javascript to catch syntax exceptions
// which would otherwise crash the Java VM
// (https://github.com/eclipsesource/J2V8/issues/217)
try {
  var runner = getRunner();
  var js = require('fs').readFileSync(runner.file, 'utf8');
  require('acorn').parse(js);
  setParseResult({ status: 'OK', message: 'Success' });  
}
catch (err) {
  console.error(err);
  if (err.stack)
    console.error(err.stack);
  setParseResult({ status: 'ERR', message: err.toString() });  
}