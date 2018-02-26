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
                  presets: [nodeLoc + '/node_modules/babel-preset-react'], 
                  plugins: [nodeLoc + '/node_modules/babel-plugin-transform-object-rest-spread']
                }
              }
            ]
          },
          {
            test: /\.css$/,
            // allows custom css files in assets (but excluding node/node_modules)
            include: function(modulePath) {
              var assetRoot = path.resolve(__dirname, '../../../..');
              return modulePath.startsWith(assetRoot) && 
                !modulePath.startsWith(assetRoot + path.sep + 'node' + path.sep + 'node_modules');
            },
            use: [nodeLoc + '/node_modules/style-loader', nodeLoc + '/node_modules/css-loader']
          },
          {
            test: /\.(png|svg|jpg|gif)$/,
            // allows custom image files in assets (but excluding node/node_modules)
            include: function(modulePath) {
              var assetRoot = path.resolve(__dirname, '../../../..');
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
      },
      plugins: input.devMode ? [] : [
        // uncomment for prod build
        new (require('webpack')).DefinePlugin({
          'process.env.NODE_ENV': JSON.stringify('production')
        })        
      ]
    }
  },
  bareStats: {
    // Add asset Information
    assets: false,
    // Sort assets by a field
    assetsSort: "field",
    // Add information about cached (not built) modules
    cached: false,
    // Show cached assets (setting this to `false` only shows emitted files)
    cachedAssets: false,
    // Add children information
    children: false,
    // Add chunk information (setting this to `false` allows for a less verbose output)
    chunks: false,
    // Add built modules information to chunk information
    chunkModules: false,
    // Add the origins of chunks and chunk merging info
    chunkOrigins: false,
    // Sort the chunks by a field
    chunksSort: "field",
    // Context directory for request shortening
    context: "../src/",
    // `webpack --colors` equivalent
    colors: false,
    // Display the distance from the entry point for each module
    depth: false,
    // Display the entry points with the corresponding bundles
    entrypoints: false,
    // Add errors
    errors: true,
    // Add details to errors (like resolving log)
    errorDetails: true,
    // Add the hash of the compilation
    hash: false,
    // Set the maximum number of modules to be shown
    maxModules: 15,
    // Add built modules information
    modules: false,
    // Sort the modules by a field
    modulesSort: "field",
    // Show dependencies and origin of warnings/errors (since webpack 2.5.0)
    moduleTrace: false,
    // Show performance hint when file size exceeds `performance.maxAssetSize`
    performance: false,
    // Show the exports of the modules
    providedExports: false,
    // Add public path information
    publicPath: false,
    // Add information about the reasons why modules are included
    reasons: false,
    // Add the source code of modules
    source: false,
    // Add timing information
    timings: true,
    // Show which exports of a module are used
    usedExports: false,
    // Add webpack version information
    version: false,
    // Add warnings
    warnings: true,
  }
};