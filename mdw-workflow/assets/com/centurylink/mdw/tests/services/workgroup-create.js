'use strict';

// example of REST api testing directly through NodeJS
const wsLoc = '../../../../../../../..';
const limberest = require('../../node/node_modules/limberest');

const testLoc = '../../tests/services/';
const resLoc = wsLoc + '/mdw-workflow/testResults/com.centurylink.mdw.tests.services';
  
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
var test = group.getTest('POST', 'workgroups');

var values = Object.assign({}, env);
values['group-name'] = 'GroupA';

test.run(options, values, (error, response) => {
  test.verify(values);
});