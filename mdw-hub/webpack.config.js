const path = require('path');
var HtmlWebpackPlugin = require('html-webpack-plugin');

module.exports = {
  entry: {
    app: './web/app/index.jsx'
  },
  output: {
    path: path.resolve(__dirname, 'web/dist'),  // output path for generated stuff
    publicPath: '/',  // logical path for accessing [name].js from index.html
    filename: '[name]/index.js'
  },
  module: {
    rules: [
      {
        test: /\.(js|jsx)$/,
        exclude: /node_modules/,
        use: [
          {
            loader: 'babel-loader',
            options: {
              presets: ['react', 'latest']
            }
          }
        ]
      },      
      {
        test: /\.css$/,
        exclude: /node_modules/,
        use: ['style-loader', 'css-loader']
      },
      {
        test: /\.png$/,
        exclude: /node_modules/,
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
  },
  plugins: [
    // use multiple plugins for additional index.html files
    new HtmlWebpackPlugin({
      inject: 'true',
      chunks: ['app'],
      filename: 'app/index.html',
      template: 'web/app/index.html'
    })
  ],
  devServer: {
    proxy: {
      '/services': {
        target: 'http://localhost:8080/mdw',
        secure: false
      },
      '/api': {
        target: 'http://localhost:8080/mdw',
        secure: false
      },
      '/images': {
        target: 'http://localhost:8080/mdw',
        secure: false
      }
    },
    historyApiFallback: {
      index: '/swagger/'
    }    
  }
};