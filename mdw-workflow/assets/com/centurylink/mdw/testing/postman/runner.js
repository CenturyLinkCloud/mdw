try {
  var util = require('util');
  
  var fs = require('fs'), // needed to read JSON file from disk
    pretty = function (obj) { // function to neatly log the collection object to console
        return util.inspect(obj, {colors: true});
    },
    Collection = require('postman-collection').Collection,
    myCollection;
  
  // Load a collection to memory from a JSON file on disk (say, sample-collection.json)
  myCollection = new Collection(JSON.stringify(fs.readFileSync('c:/eclipse_workspace/limberest-demo/test/limberest-demo.postman').toString()));
  
  // log items at root level of the collection
  console.log(util.inspect(myCollection));
  
  
  var limberest = require('limberest');
  limberest.runTest('c:/mdw/workspaces/mdw6/mdw-workflow/assets/dons/limberest-demo.postman', 'api-docs');
}
catch (err) {
  // if not caught, VM can System.exit()
  console.log(err);
}