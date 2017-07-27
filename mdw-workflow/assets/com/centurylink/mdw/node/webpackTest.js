'use strict';

var webpack = require('webpack');
var webpackConfig = require('./webpackConfig');

var jsxAsset = {
    "file": "e:\\workspaces\\dons\\mdw-demo\\assets\\bugs\\new.jsx",
    "root": "e:\\workspaces\\dons\\mdw-demo\\assets",
    "output": "e:\\eclipse_4.6.3\\..\\mdw\\.temp\\jsx\\bugs\\new.jsx"
};

var config = webpackConfig.getConfig(jsxAsset);  
console.log('webpack config: ' + JSON.stringify(config, null, 2));

var compiler = webpack(config);

compiler.run((err, stats) => {
  if (err) {
    console.error(err);
  }
  else {
    console.log('stats:\n' + JSON.stringify(stats.toJson(), null, 2));
  }
});  
