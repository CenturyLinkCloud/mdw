// concat/minification defeats dynamic javascript inclusion, so it's not performed
'use strict';
module.exports = function(grunt) {

  require('load-grunt-tasks')(grunt);

  var webpack = require("webpack");
  
  grunt.config.init({
    pkg: grunt.file.readJSON('package.json'),
    clean: {
      dist: ["dist"]
    },
    jshint: {
      files: ['Gruntfile.js', 'web/js/**/*.js', 'test/**/*.js'],
      options: {
        // options here to override JSHint defaults
        esnext: true,
        globalstrict: true,
        globals: {
          console: false,
          module: false,
          document: false,
          window: false,
          navigator: false,
          setTimeout: false,
          angular: false,
          require: false,
          $mdwHubRoot: false,
          $mdwServicesRoot: false,
          $mdwWebToolsRoot: false,
          $mdwHubUser: false,
          $mdwVersion: false,
          $mdwBuild: false,
          $mdwAuthMethod: false,
          $mdwAutoTestWebSocketUrl: false,
          hljs: false,
          JsDiff: false,
          marked: false,
          describe: false,
          it: false,
          before: false,
          beforeEach: false,
          after: false,
          afterEach: false,
          expect: false,
          inject: false,
          browser: false,
          _:false,
          DOMParser: false,
          iFrameResize: false,
          FileReader: false,
          Uint8Array: false,
          Image: false
        }
      }
    },
    'string-replace': {
      dist: {
        files: {
          'dist/index.html': ['web/index.html']
        },
        options: {
          replacements: [{
            pattern: /<link rel="stylesheet" href="bower_components\/(.*).css">/g,
            replacement: '<link rel="stylesheet" href="lib/$1.css">'
          }, {
            pattern: /<script src="bower_components\/(.*).js"><\/script>/g,
            replacement: '<script src="lib/$1.min.js"></script>'
          }]
        }
      }
    },
    copy: {
      dist: {
        files: [{
          expand: true,
          cwd: 'web',
          src: [ '**/*', '!index.html', '!bower_components/**', '!WEB-INF/**' ],
          dest: 'dist'
        }, {
          expand: true,
          cwd: 'web/bower_components',
          src: [ '**/*.css', '!**/*.min.css', '**/fonts/*', '**/*.min.js', '!angular-bootstrap/ui-bootstrap.min.js', '**/*.js.map' ],
          dest: 'dist/lib'
        }, {
          expand: true,
          cwd: 'web/bower_components/ace-builds/',
          src: [ 'src-min-noconflict/*'],
          dest: 'dist/lib/ace-builds/',
          rename: function(dest, src) {
            return dest + src.replace(/ace\.js/, 'ace.min.js');
          }          
        }]
      }
    },
    ngtemplates: {
      'mdw': {
        cwd: 'web',
        src: 'ui/**/*.html',
        dest: 'dist/hub-ui/templates.js'
      }      
    },
    concat: {
      'hub-ui': {
        src: ['web/lib/hub-ui.js', 'web/js/ui/**/*.js', 'web/js/mdw.js', 'web/js/util.js', 'web/js/constants.js', 'dist/hub-ui/templates.js'],
        dest: 'dist/hub-ui/src.js'
      }
    },
    cssmin: {
      'hub-ui': {
        files: {
          'lib/hub-ui.css': ['web/css/mdw-admin.css']
        }
      }
    },
    webpack: {
      'hub-ui': {
        entry: './dist/hub-ui/src.js',
        output: {
          path: '/workspaces/mdw6/mdw-hub/lib/',
          filename: 'hub-ui.js'
        },
        plugins: [
          // new webpack.optimize.UglifyJsPlugin({minimize: true})
        ]
      }
    },
    jasmine: {
      src : 'web/js/**/*.js',
      options : {
          specs : 'test/**/*.js',
          vendor: [
            "web/bower_components/angular/angular.js",
            "web/bower_components/angular-resource/angular-resource.js",
            "web/bower_components/angular-route/angular-route.js",
            "web/bower_components/angular-animate/angular-animate.js",
            "web/bower_components/angular-bootstrap/ui-bootstrap.js",
            "web/lib/ng-infinite-scroll-1.2.0.js",
            "web/bower_components/angular-mocks/angular-mocks.js"
          ]          
      }
    },
  });

  grunt.registerTask('default', ['jshint', 'string-replace', 'copy:dist']);
  grunt.registerTask('dist', ['jshint', 'string-replace', 'copy:dist', 'ngtemplates:mdw', 'concat:hub-ui', 'cssmin:hub-ui', 'webpack:hub-ui']);
  grunt.registerTask('hub-ui', ['ngtemplates:mdw', 'concat:hub-ui', 'cssmin:hub-ui', 'webpack:hub-ui']);
  grunt.registerTask('test', ['jasmine']);
};
