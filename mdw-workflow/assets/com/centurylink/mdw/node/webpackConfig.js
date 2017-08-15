'use strict';

var fs = require('fs');
var path = require('path');

var nodeLoc = path.dirname(require.resolve('./webpackConfig'));

module.exports = {
  getConfig: function(input) {
    return {
      context: nodeLoc, 
      entry: input.source,
      output: {
        path: path.dirname(input.output),
        publicPath: '/',  // logical path for accessing [name].js from index.html
        filename: path.basename(input.output)
      },
      module: {
        rules: [
          {
            test: /\.(js|jsx)$/,
            exclude: [/node_modules/],
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
            // allows custom css files in assets (but excluding node/node_modules)
            include: function(modulePath) {
              var assetRoot = path.resolve(__dirname, '..');
              return modulePath.startsWith(assetRoot) && 
                !modulePath.startsWith(assetRoot + path.sep + 'node' + path.sep + 'node_modules');
            },
            use: [nodeLoc + '/node_modules/style-loader', nodeLoc + '/node_modules/css-loader']
          },
          {
            test: /\.png$/,
            // allows custom png files in assets (but excluding node/node_modules)
            include: function(modulePath) {
              var assetRoot = path.resolve(__dirname, '..');
              return modulePath.startsWith(assetRoot) && 
                !modulePath.startsWith(assetRoot + path.sep + 'node' + path.sep + 'node_modules');
            },
            use: [
              {
                loader: nodeLoc + '/node_modules/file-loader',
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