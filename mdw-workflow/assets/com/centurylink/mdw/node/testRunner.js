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

try {
  const limberest = require('limberest');
  // const limberest = require('../../../../../../../limberest-js/index.js');
  const path = require('path');
  
  // Runs multiple test items, each with their own case.
  var testCase = getTestCase();
  var testLoc = path.dirname(testCase.file);
  
  var options = {
    location: testLoc,
    resultLocation: testCase.resultDir,
    responseHeaders: ['content-type', 'mdw-request-id']
  };

  if (testCase.items) {
    var itemIdx = 0;

    const runCaseItem = function(item) {
      var opts = Object.assign({}, options, item.options);
      if (opts.debug)
        console.log("options: " + JSON.stringify(opts, null, 2));
      // deep clone for values
      var vals = {};
      if (opts.valueFiles) {
        opts.valueFiles.forEach(valueFile => {
          vals = Object.assign(vals, limberest.loadValuesSync(valueFile), item.values);
        });
      }
      else if (require('fs').existsSync(testLoc + '/localhost.env')) {
        vals = Object.assign(vals, limberest.loadValuesSync(testLoc + '/localhost.env'), item.values);
      }
      //assign unique value to masterRequestId for every execution
      var value=vals['proc-run-json'];
      if(value){
        var radomNoBet0and99= Math.floor(Math.random() * 200);
        var uniqueMasterReqIdVal="dxoakes-20171012-"+radomNoBet0and99;
        value = value.replace("replaceMe", uniqueMasterReqIdVal);
        vals['proc-run-json']=value;
      }
      if (opts.debug)
        console.log("values: " + JSON.stringify(vals, null, 2));
      if (opts.caseName && opts.verify) {
        // verify test results only
        opts.retainResult = opts.retainLog = true;
        opts.overwriteExpected = false;
        var theCase = new (limberest.Case)(opts.caseName, opts);
        setTestResult(null, theCase.verifySync(vals));
      }
      else {
        // execute test case 
        var group = limberest.loadGroupSync(testCase.file);
        var request = group.getRequest(item.method, item.name);
        var result = { start: new Date().toISOString() };
        request.run(opts, vals, (error, response) => {
          var itemId = item.method + ':' + item.name;
          setTestResponse(itemId, response);
          if (error) {
            console.error(error);
            setTestResult(itemId, {status: 'Errored', message: error.toString()});
          }
          else {
            if (!opts.caseName) {
              result = Object.assign(result, request.verifySync(vals));
              result.end = new Date().toISOString();
              setTestResult(itemId, result);
            }
          }
          // run the next test item
          itemIdx++;
          if (itemIdx < testCase.items.length) {
            runCaseItem(testCase.items[itemIdx]);
          }
        });
      }
    };
    
    if (itemIdx < testCase.items.length) {
      runCaseItem(testCase.items[itemIdx]);
    }
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
    console.error(e);
    if (e.stack)
      console.error(e.stack);
  }
}