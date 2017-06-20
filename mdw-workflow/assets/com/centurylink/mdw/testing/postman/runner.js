'use strict'; 
try {
  var testCase = getTestCase();
  console.log('running test case:\n  ' + JSON.stringify(testCase, null, 2));
  
  var fs = require('fs');
  var postman = JSON.parse(fs.readFileSync(testCase.coll, 'utf8'));
  
  var jsYaml = require('js-yaml');
  
  console.log('read postman test collection:\n ' + testCase.coll);
  
  var request = require('request');
  
  var postmanItems = [];
  for (let i = 0; i < testCase.items.length; i++) {
    
    var pmItem = (testCase.items[i] && postman.item.find(function(item) {
      return item.name == testCase.items[i].name;
    }));
    
    if (pmItem) {
      postmanItems.push(pmItem);
    }
  }
  
  postmanItems.forEach(function(postmanItem) {
    console.log('will run: ' + JSON.stringify(postmanItem, null, 2));
    console.log('postmanItem.request.url: ' + postmanItem.request.url);
    
    // TODO substs in req 
    var req = {
      url: postmanItem.request.url,
      method: postmanItem.request.method,
    };
    if (postmanItem.request.header) {
      req.header = {};
      for (let i = 0; i < postmanItem.request.header.length; i++) {
        var postmanHeader = postmanItem.request.header[i];
        req.header[postmanHeader.key] = postmanHeader.value;
      }
    }
    if (postmanItem.request.body && postmanItem.request.body.raw) {
      req.body = postmanItem.request.body.raw.replace(/\r/g, '');
    }

    console.log('REQUEST JSON:\n' + JSON.stringify(req, null, 2));
    
    // save request yaml
    var reqYaml = jsYaml.safeDump(req, {noCompatMode: true});
    console.log('REQUEST YAML:\n' + reqYaml);
    
    // expected result (request and response) YAML
    request(postmanItem.request.url, function(error, response, body) {
      if (error) {
        console.log('error:', error);
        setTestResult({ status: 'Errored', message: error.toString() });
      }
      else {
        // TODO set actual result (request and response) YAML
        console.log('statusCode:', response && response.statusCode); 
        console.log('body:', body); 
        setTestResult({ status: 'Passed', message: 'Test succeeded' });
      }
    });
  });
}
catch (err) {
  // if not caught, VM can System.exit()
  console.log(err);
  setTestResult({ status: 'Errored', message: err.toString() });  
  
}