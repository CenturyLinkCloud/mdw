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
            include: [ input.root ],
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
            include: [ input.root ],
            use: ['style-loader', 'css-loader']
          },
          {
            test: /\.png$/,
            include: [ input.root ],
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