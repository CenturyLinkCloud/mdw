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

var testCase = null;
try {
  // TODO
  // const limberest = require('limberest');
  const limberest = require('../../../../../../../../limberest-js/index.js');
  const path = require('path');
  
  testCase = getTestCase();
  console.log('running test case:\n  ' + JSON.stringify(testCase, null, 2));
  
  var testLoc = path.dirname(testCase.file);
  var env = limberest.env(testLoc + '/' + testCase.env);
  
  var options = {
    location: testLoc,
    resultLocation: testCase.resultDir,
    logLocation: testCase.resultDir,
    debug: true // TODO
  };
  
  var group = limberest.group(testCase.file);
  
  var values = Object.assign({}, env);
  values['group-name'] = 'GroupA';

  if (testCase.items) {
    testCase.items.forEach(item => {
      var test = group.test(item.method, item.name);
      var logFile = testCase.resultDir + '/' + item.name + '.log';
      test.run(options, values, (response, error) => {
        console.log("RESP:\n" + JSON.stringify(response, null, 2));
        var result = test.verify(values);
        console.log("RES:\n" + JSON.stringify(result, null, 2));
        setTestResult(result);
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
    setTestResult({ status: 'Errored', message: err.toString() });  
  }
  catch (e) {
    console.error(err);
    console.error(err.stack);
  }
}