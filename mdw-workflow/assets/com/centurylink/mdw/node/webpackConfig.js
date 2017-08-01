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
            exclude: [/node_modules/],
            use: ['style-loader', 'css-loader']
          },
          {
            test: /\.png$/,
            exclude: [/node_modules/],
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