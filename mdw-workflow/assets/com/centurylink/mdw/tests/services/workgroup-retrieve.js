'use strict';

const wsLoc = '../../../../../../../..';
const limberest = require('../../node/node_modules/limberest');
// const limberest = require(wsLoc + '/limberest-js/index.js');

const testLoc = '../../tests/services';
const resLoc = wsLoc + '/mdw6/mdw-workflow/testResults/com.centurylink.mdw.tests.services';
  
var env = limberest.loadValuesSync(testLoc + '/localhost.env');
var group = limberest.loadGroupSync(testLoc + '/admin-apis.postman');

var options = {
  location: testLoc,
  resultLocation: resLoc,
  debug: true,
  responseHeaders: ['content-type', 'mdw-request-id'],
  qualifyLocations: false
};

// run one test
var test = group.getTest('GET', 'workgroups/{group-name}');

var values = Object.assign({}, env);
values['group-name'] = 'GroupA';

test.run(options, values, (error, response) => {
  // omitted info still available for programmatic access
  console.log('time: ' + response.time);
  console.log('headers: ');
  Object.keys(response.headers).forEach(hdrKey => {
    console.log('  ' + hdrKey + "=" + response.headers[hdrKey]);
  });
  var result = test.verifySync(values);
  console.log('result: ' + JSON.stringify(result, null, 2));
});