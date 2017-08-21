'use strict';

var webpack = require('webpack');
var webpackConfig = require('./webpackConfig');

var input = {
  "source": "c:\\eclipse_4.6.3\\..\\mdw\\.temp\\start\\bugs\\Bug.js",
  "root": "c:\\mdw\\workspaces\\dons\\mdw-demo\\assets",
  "output": "c:\\eclipse_4.6.3\\..\\mdw\\.temp\\jsx\\bugs\\Bug.jsx",
};

var config = webpackConfig.getConfig(input);  
console.log('webpack config: ' + JSON.stringify(config, null, 2));

var compiler = webpack(config);

compiler.run((err, stats) => {
  if (err) {
    console.error(err);
  }
  else {
    console.log('stats:\n' + JSON.stringify(stats.toJson('minimal'), null, 2));
  }
});  
