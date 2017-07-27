'use strict';

var fs = require('fs');
var path = require('path');

var nodeLoc = path.dirname(require.resolve('./webpackConfig'));

module.exports = {
  getConfig: function(jsxAsset) {
    return {
      context: nodeLoc, 
      entry: jsxAsset.file,
      output: {
        path: path.dirname(jsxAsset.output),
        publicPath: '/',  // logical path for accessing [name].js from index.html
        filename: path.basename(jsxAsset.output)
      },
      module: {
        rules: [
          {
            test: /\.(js|jsx)$/,
            include: [ jsxAsset.root ],
            use: [
              {
                loader: 'babel-loader',
                options: {
                  presets: [nodeLoc + '/node_modules/babel-preset-react']
                }
              }
            ]
          },      
          {
            test: /\.css$/,
            include: [ jsxAsset.root ],
            use: ['style-loader', 'css-loader']
          },
          {
            test: /\.png$/,
            include: [ jsxAsset.root ],
            use: [
              {
                loader: 'file-loader',
                options: {
                  name: 'img/[name].[ext]'
                }
              }
            ]
          } 
        ]
      }
    }
  }
};