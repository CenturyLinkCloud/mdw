'use strict';
var testCase = null;
var logFile = null;
try {
  // TODO
  const limberest = require('limberest');
  //const limberest = require('../../../../../../../../limberest-js/index.js');
  const fs = require('fs-extra');
  const path = require('path');
  
  testCase = getTestCase();
  console.log('running test case:\n  ' + JSON.stringify(testCase, null, 2));
  
  var testLoc = path.dirname(testCase.file);
  var env = limberest.env(testLoc + path.sep + testCase.env);
  
  var options = {
    caseDir: testLoc,
    resultDir: testCase.resultDir,
    logDir: testCase.resultDir,
    debug: true, // TODO
    color: false
  };
  
  var group = limberest.group(testCase.file);
  
  var values = Object.assign({}, env);
  values['group-name'] = 'GroupA';

  if (testCase.items) {
    testCase.items.forEach(item => {
      var test = group.test(item.method, item.name);
      logFile = testCase.resultDir + path.sep + item.name + '.log';
      test.run(values, options, (response, result, error) => {
        console.log("RESP:\n" + JSON.stringify(response, null, 2));
        console.log("RES:\n" + JSON.stringify(result, null, 2));
        setTestResult(result);
      });
    });
  }
}
catch (err) {
  // if not caught, VM can System.exit()
  console.log(err);
  console.log(err.stack);
  try {
    // try to log to file and set status
    if (logFile) {
      const fs = require('fs-extra');
      fs.ensureFileSync(logFile);
      fs.appendFileSync(logFile, err.stack);
    }
    setTestResult({ status: 'Errored', message: err.toString() });  
  }
  catch (e) {
    console.log(err);
    console.log(err.stack);
  }
}