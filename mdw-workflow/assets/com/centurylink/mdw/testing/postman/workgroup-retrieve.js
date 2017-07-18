'use strict';

const wsLoc = '../../../../../../../..';
const limberest = require('limberest');

const testLoc = '../../tests/services/';
const resLoc = wsLoc + '/mdw6/mdw-workflow/testResults/com.centurylink.mdw.tests.services';
  
var env = limberest.loadEnvSync(testLoc + '/localhost.env');
var group = limberest.loadGroupSync(testLoc + '/admin-apis.postman');

var options = {
  location: testLoc,
  resultLocation: resLoc,
  debug: true,
  responseHeaders: ['content-type', 'mdw-request-id'],
};

// run one test
var test = group.getTest('GET', 'workgroups/{group-name}');

var values = Object.assign({}, env);
values['group-name'] = 'GroupA';

test.run(options, values, (error, response) => {
  // omitted info still available for programmatic access
  console.log("TIME: " + response.time);
  console.log("HEADERS: ");
  Object.keys(response.headers).forEach(hdrKey => {
    console.log(hdrKey + "=" + response.headers[hdrKey]);
  });
  var result = test.verify(values);
  
});