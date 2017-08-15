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

// Compile using webpack.
try {
  
  var fs = require('fs');
  var path = require('path');
  var webpack = require('webpack');
  var webpackConfig = require('./webpackConfig');

  var input = getInput();
  if (input.debug)
    console.log('input: ' + JSON.stringify(input, null, 2));
  
  var config = webpackConfig.getConfig(input);
  if (input.debug)
    console.log('webpack config: ' + JSON.stringify(config, null, 2));

  var compiler = webpack(config);
  
  compiler.run((err, stats) => {
    if (err) {
      setWebpackResult({status: 'Failed', content: err.toString()});
    }
    else {
      var debug = stats.errors && stats.errors.length && input.debug;
      var stats = JSON.stringify(stats.toJson(debug ? 'normal' : 'minimal'));
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
    setWebpackResult(null, {status: 'Errored', content: err.toString()});  
  }
  catch (e) {
    console.error(e);
    if (e.stack)
      console.error(e.stack);
  }
}


