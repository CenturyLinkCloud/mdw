'use strict';

// prevent unhandled errors from crashing the VM
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

// Transpiles JSX content.
try {
  
  var fs = require('fs');
  var path = require('path');
  var webpack = require('webpack');
  var webpackConfig = require('./webpackConfig');

  var jsxAsset = getJsxAsset();
  if (jsxAsset.debug)
    console.log('jsxAsset: ' + JSON.stringify(jsxAsset, null, 2));
  
  var config = webpackConfig.getConfig(jsxAsset);
  if (jsxAsset.debug)
    console.log('webpack config: ' + JSON.stringify(config, null, 2));

  var compiler = webpack(config);
  
  compiler.run((err, stats) => {
    if (err) {
      setWebpackResult({status: 'Failed', content: err.toString()});
    }
    else {
      var stats = JSON.stringify(stats.toJson(jsxAsset.debug ? 'normal' : 'minimal'));
      setWebpackResult({status: 'OK', content: stats});
    }
  });  
}
catch (err) {
  // if not caught, VM can crash
  if (err.stack)
    console.error(err.stack);
  else
    console.error(err);
  try {
    // try to log to file and set status
    if (testCase && testCase.logger) {
      testCase.logger.error(err);
      testCase.logger.error(err.stack);
    }
    setTestResult(null, {status: 'Errored', message: err.toString()});  
  }
  catch (e) {
    console.error(err);
    console.error(err.stack);
  }
}


