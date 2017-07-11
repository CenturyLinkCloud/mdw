'use strict';

// Runs multiple test items, each with their own case.

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

var testCase = null;
try {
  // TODO
  // const limberest = require('limberest');
  const limberest = require('limberest');
  const path = require('path');
  
  testCase = getTestCase();
  
  var testLoc = path.dirname(testCase.file);
  var env = limberest.env(testLoc + '/' + testCase.env);
  
  var options = {
    location: testLoc,
    resultLocation: testCase.resultDir,
    logLocation: testCase.resultDir,
    debug: true, // TODO
  };

  if (options.debug)
    console.log('running test case:\n  ' + JSON.stringify(testCase, null, 2));
    
  var group = limberest.group(testCase.file);
  
  var values = Object.assign({}, env); // TODO
  
  if (testCase.items) {
    testCase.items.forEach(item => {
      var test = group.test(item.method, item.name);
      var retain = (item.caseName != null);
      var opts = Object.assign({
        retainLog: retain, 
        retainResult: retain,
        caseName: item.caseName}, options);
      test.run(opts, values, (response, error) => {
        var itemId = item.method + ':' + item.name;
        setTestResponse(itemId, response);
        if (!item.caseName) {
          var result = test.verify(values);
          setTestResult(itemId, result);
        }
      });
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