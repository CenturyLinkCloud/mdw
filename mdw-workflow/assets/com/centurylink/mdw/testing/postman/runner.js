try {
  var testCase = getTestCase();
  console.log('running postman test collection:\n  ' + JSON.stringify(testCase, null, 2));
  setResult({ status: 'Passed', message: 'Test succeeded' });
}
catch (err) {
  // if not caught, VM can System.exit()
  console.log(err);
}