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
  console.log('webpackRunner devMode: ' + input.devMode);
  if (input.debug)
    console.log('input: ' + JSON.stringify(input, null, 2));
  
  var config = webpackConfig.getConfig(input);
  if (input.debug)
    console.log('webpack config: ' + JSON.stringify(config, null, 2));

  const compiler = webpack(config);
  
  if (input.devMode) {
    // watching keeps this process alive until the server is stopped
    compiler.watch({
      aggregateTimeout: 300
    }, (err, stats) => {
      if (err) {
        console.log('webpack failed: ' + err);
      }
      else {
        var st = stats.toJson(webpackConfig.bareStats);
        if (st.errors && st.errors.length > 0)
          console.log('webpack errors: ' + JSON.stringify(st.errors, null, 2));
        if (st.warnings && st.warnings.length > 0)
          console.log('webpack warnings: ' + JSON.stringify(st.warnings, null, 2));
        console.log('webpack compile time: ' + st.time + ' ms');
      }
    });
  }
  else {
    compiler.run((err, stats) => {
      if (err) {
        setWebpackResult({status: 'Failed', message: err.toString()});
      }
      else {
        var debug = stats.errors && stats.errors.length && input.debug;
        var statsStr = JSON.stringify(stats.toJson(debug ? 'normal' : 'minimal'));
        setWebpackResult({status: 'OK', message: statsStr});
      }
    });
  }
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


