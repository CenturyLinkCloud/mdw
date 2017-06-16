'use strict'; 
try {
  var testCase = getTestCase();
  console.log('running test case:\n  ' + JSON.stringify(testCase, null, 2));
  
  var fs = require('fs');
  var postman = JSON.parse(fs.readFileSync(testCase.coll, 'utf8'));
  
  console.log('read postman test collection:\n ' + testCase.coll);
  
  var request = require('request');
  
  for (let i = 0; i < postman.item.length; i++) {
    var postmanItem = postman.item[i];
    var shouldRun = (testCase.items && testCase.items.find(function(item) {
      console.log("NAME: " + item.name);
      console.log("PI: " + postmanItem.name);
      return item.name == postmanItem.name;
    }));
    
    if (shouldRun) {
      console.log('will run: ' + JSON.stringify(postmanItem, null, 2));
    }
  }
  
  // TODO set expected result YAML
  request(postmanItem.request.url, function(error, response, body) {
    if (error) {
      console.log('error:', error);
      setTestResult({ status: 'Errored', message: error.toString() });
    }
    else {
      // TODO set actual result YAML
      console.log('statusCode:', response && response.statusCode); 
      console.log('body:', body); 
      setTestResult({ status: 'Passed', message: 'Test succeeded' });
    }
  });
}
catch (err) {
  // if not caught, VM can System.exit()
  console.log(err);
  setTestResult({ status: 'Errored', message: err.toString() });  
  
}