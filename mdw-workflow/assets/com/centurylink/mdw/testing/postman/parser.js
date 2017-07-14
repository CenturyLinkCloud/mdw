'use strict';

// Pre-parses javascript to catch syntax exceptions
// which would otherwise crash the Java VM
// (https://github.com/eclipsesource/J2V8/issues/217)
try {
  // TODO runner is passed
  var js = require('fs').readFileSync(__dirname + '/runner.js', 'utf8');
  require('acorn').parse(js);
  setParseResult({ status: 'OK', message: 'Success' });  
}
catch (err) {
  console.log('ERR: ' + err)
  setParseResult({ status: 'ERR', message: err.toString() });  
}
