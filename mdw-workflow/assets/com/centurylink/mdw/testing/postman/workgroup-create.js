'use strict';

const wsLoc = '../../../../../../../..';
const limberest = require(wsLoc + '/limberest-js/index.js');

const testLoc = '../../tests/services/';
const resLoc = wsLoc + '/mdw6/mdw-workflow/testResults/com.centurylink.mdw.tests.services';
  
var env = limberest.env(testLoc + '/localhost.env');
var group = limberest.group(testLoc + '/admin-apis.postman');

var options = {
  location: testLoc,
  resultLocation: resLoc,
  logLocation: resLoc,
  debug: true,
};

// run one test
var test = group.test('POST', 'workgroups');

var values = Object.assign({}, env);
values['group-name'] = 'GroupA';

test.run(options, values, (response, error) => {
  var result = test.verify(values);
});