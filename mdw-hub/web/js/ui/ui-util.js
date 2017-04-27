'use strict';
var uiUtil = angular.module('mdwUtil', ['mdw']);

uiUtil.config(function($httpProvider) {
  $httpProvider.defaults.useXDomain = true;
});

uiUtil.filter('highlight', function($sce) {
  return function(input, lang) {
    if (lang === 'test') {
      lang = 'groovy';
    }
    else if (lang === 'spring' || lang === 'camel') {
      lang = 'xml';
    }
    else if (lang === 'proc' || lang === 'task' || lang === 'impl' || lang === 'evth' || lang == 'pagelet') {
      if (input.trim().startsWith('{'))
        lang = 'json';
      else
        lang = 'xml';
    }
    if (lang && hljs.getLanguage(lang) && input) {
      return hljs.highlight(lang, input.removeCrs()).value;
    }
    else if (input)
      return input.replace(/&/g,'&amp;').replace(/</g,'&lt;');
    else
      return input;
  };
}).filter('unsafe', function($sce) { return $sce.trustAsHtml; });

uiUtil.filter('markdown', function($sce) {
  marked.setOptions({
    highlight: function (code) {
      return hljs.highlightAuto(code).value;
    }
  });
  
  return function(input) {
    if (input)
        return marked(input);
    else
      return input;
  };
}).filter('unsafe', function($sce) { return $sce.trustAsHtml; });

//in case js string does not supply startsWith() and endsWith()
if (typeof String.prototype.startsWith != 'function') {
  String.prototype.startsWith = function(prefix) {
    return this.indexOf(prefix) === 0;
  };
}
if (typeof String.prototype.endsWith !== 'function') {
  String.prototype.endsWith = function(suffix) {
      return this.indexOf(suffix, this.length - suffix.length) !== -1;
  };
}

// TODO: another way for these polyfill functions
// remove DOS/Windows CR characters
String.prototype.removeCrs = function() {
  return this.replace(/\r/g, '');
};
// split into lines (removing CRs first)
String.prototype.getLines = function() {
  return this.removeCrs().split(/\n/);
};
// count lines
String.prototype.lineCount = function() {
  return this.getLines().length;
};
String.prototype.replaceAll = function(target, replacement) {
  return this.split(target).join(replacement);
};
// line numbers
String.prototype.lineNumbers = function() {
  var lines = this.getLines();
  var lineNums = '';
  for (var i = 1; i < lines.length + 1; i++)
    lineNums += i + '\n';
  return lineNums;
};
