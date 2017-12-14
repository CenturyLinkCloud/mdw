// concat/minification defeats dynamic javascript inclusion, so it's not performed
'use strict';
module.exports = function(grunt) {

  require('load-grunt-tasks')(grunt);

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
          confirm: false,
          $mdwHubRoot: false,
          $mdwServicesRoot: false,
          $mdwWebToolsRoot: false,
          $mdwDocsRoot: false,
          $mdwHubUser: false,
          $mdwVersion: false,
          $mdwBuild: false,
          $mdwAuthMethod: false,
          $mdwWebSocketUrl: false,
          $mdwDiscoveryUrl: false,
          $mdwCustomRoutes: false,
          $mdwUi: true,
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
          clearInterval: false,
          setInterval: false,
          FileReader: false,
          Uint8Array: false,
          SwaggerUIBundle: false,
          SwaggerUIStandalonePreset: false,
          Image: false,
          WebSocket: false,
          Console: false,
          fetch: false,
          _:false,
          __dirname: false
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
    concat: {
      options: {
        banner: "'use strict';\r\n",
        process: function(src, filepath) {
          return '// source: ' + filepath + '\r\n' +
            src.replace(/(^|\r\n)[ \t]*('use strict'|"use strict");?\s*/g, '$1');
          }
      }
    },
    eslint: {
      src: [
        '../mdw-workflow/assets/com/centurylink/mdw/react/*.js',
        '../mdw-workflow/assets/com/centurylink/mdw/react/*.jsx',
        '../mdw-workflow/assets/com/centurylink/mdw/task/*.js',
        '../mdw-workflow/assets/com/centurylink/mdw/task/*.jsx'
      ],      
      options: {
        configFile: 'eslint.json'
      }
    }
  });

  grunt.registerTask('default', ['jshint', 'eslint', 'string-replace', 'copy:dist']);
  grunt.registerTask('dist', ['jshint', 'string-replace', 'copy:dist']);
};
