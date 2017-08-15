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
            // this include allows custom css through style-loader
            include: [
              path.resolve(__dirname, '..')
            ],
            use: [nodeLoc + '/node_modules/style-loader', nodeLoc + '/node_modules/css-loader']
          },
          {
            test: /\.png$/,
            // this include allows custom pngs through file-loader
            include: [
              path.resolve(__dirname, '..')
            ],
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