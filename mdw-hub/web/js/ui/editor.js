'use strict';

var editorMod = angular.module('mdwEditor', ['mdw']);
editorMod.constant('editorConfig', {});
  
editorMod.directive('mdwEditor', ['editorConfig', function (editorConfig) {
  
  var setOptions = function(acee, session, opts) {
    
    // TODO hardwired options
    acee.setShowPrintMargin(false);

    if (angular.isDefined(opts.require)) {
      opts.require.forEach(function (n) {
          window.ace.require(n);
      });
    }
    
    if (angular.isDefined(opts.showGutter)) {
      acee.renderer.setShowGutter(opts.showGutter);
    }
    
    if (angular.isDefined(opts.useWrapMode)) {
      session.setUseWrapMode(opts.useWrapMode);
    }
    
    if (angular.isDefined(opts.showInvisibles)) {
      acee.renderer.setShowInvisibles(opts.showInvisibles);
    }
    
    if (angular.isDefined(opts.showIndentGuides)) {
      acee.renderer.setDisplayIndentGuides(opts.showIndentGuides);
    }
    
    if (angular.isDefined(opts.useSoftTabs)) {
      session.setUseSoftTabs(opts.useSoftTabs);
    }
    
    if (angular.isDefined(opts.showPrintMargin)) {
      acee.setShowPrintMargin(opts.showPrintMargin);
    }

    // commands
    if (angular.isDefined(opts.disableSearch) && opts.disableSearch) {
      acee.commands.addCommands([
        {
          name: 'unfind',
          bindKey: {
            win: 'Ctrl-F',
            mac: 'Command-F'
          },
          exec: function () {
            return false;
          },
          readOnly: true
        }
      ]);
    }

    if (angular.isString(opts.theme)) {
      acee.setTheme('ace/theme/' + opts.theme);
    }
    
    if (angular.isString(opts.mode)) {
      session.setMode('ace/mode/' + opts.mode);
    }
    
    // advanced options
    if (angular.isDefined(opts.firstLineNumber)) {
      if (angular.isNumber(opts.firstLineNumber)) {
        session.setOption('firstLineNumber', opts.firstLineNumber);
      } 
      else if (angular.isFunction(opts.firstLineNumber)) {
        session.setOption('firstLineNumber', opts.firstLineNumber());
      }
    }

    var key, obj;
    if (angular.isDefined(opts.advanced)) {
      for (key in opts.advanced) {
        // create a javascript object with the key and value
        obj = { name: key, value: opts.advanced[key] };
        // try to assign the option to the ace editor
        acee.setOption(obj.name, obj.value);
      }
    }

    // advanced options for the renderer
    if (angular.isDefined(opts.rendererOptions)) {
      for (key in opts.rendererOptions) {
        // create a javascript object with the key and value
        obj = { name: key, value: opts.rendererOptions[key] };
        // try to assign the option to the ace editor
        acee.renderer.setOption(obj.name, obj.value);
      }
    }

    // onLoad callbacks
    angular.forEach(opts.callbacks, function (cb) {
      if (angular.isFunction(cb)) {
        cb(acee);
      }
    });
  };

  return {
    restrict: 'EA',
    require: '?ngModel',
    link: function (scope, elem, attrs, ngModel) {
      var options = editorConfig.ace || {};
      var opts = angular.extend({}, options, scope.$eval(attrs.mdwEditor));
      var acee = window.ace.edit(elem[0]);
      var session = acee.getSession();
      var onChangeListener;
      var onBlurListener;
      var executeUserCallback = function () {
        var callback = arguments[0];
        var args = Array.prototype.slice.call(arguments, 1);
  
        if (angular.isDefined(callback)) {
          scope.$evalAsync(function () {
            if (angular.isFunction(callback)) {
              callback(args);
            }
          });
        }
      };
      var listenerFactory = {
        onChange: function (callback) {
          return function (e) {
            var newValue = session.getValue();
  
            if (ngModel && newValue !== ngModel.$viewValue &&
                // HACK make sure to only trigger the apply outside of the
                // digest loop 'cause ACE is actually using this callback
                // for any text transformation !
                !scope.$$phase && !scope.$root.$$phase) {
              scope.$evalAsync(function () {
                ngModel.$setViewValue(newValue);
              });
            }
  
            executeUserCallback(callback, e, acee);
          };
        },
        onBlur: function (callback) {
          return function () {
            executeUserCallback(callback, acee);
          };
        }
      };
  
      attrs.$observe('readonly', function (value) {
        acee.setReadOnly(!!value || value === '');
      });
  
      if (ngModel) {
        ngModel.$formatters.push(function (value) {
          if (angular.isUndefined(value) || value === null) {
            return '';
          }
          else if (angular.isObject(value) || angular.isArray(value)) {
            throw new Error('ui-ace cannot use an object or an array as a model');
          }
          return value;
        });
  
        ngModel.$render = function () {
          session.setValue(ngModel.$viewValue);
        };
      }
  
      // listen for option updates
      var updateOptions = function (current, previous) {
        if (current === previous)
          return;
        opts = angular.extend({}, options, scope.$eval(attrs.mdwEditor));
  
        opts.callbacks = [ opts.onLoad ];
        if (opts.onLoad !== options.onLoad) {
          // also call the global onLoad handler
          opts.callbacks.unshift(options.onLoad);
        }
  
        // EVENTS
  
        // unbind old change listener
        session.removeListener('change', onChangeListener);
  
        // bind new change listener
        onChangeListener = listenerFactory.onChange(opts.onChange);
        session.on('change', onChangeListener);
  
        // unbind old blur listener
        //session.removeListener('blur', onBlurListener);
        acee.removeListener('blur', onBlurListener);
  
        // bind new blur listener
        onBlurListener = listenerFactory.onBlur(opts.onBlur);
        acee.on('blur', onBlurListener);
  
        setOptions(acee, session, opts);
      };
  
      scope.$watch(attrs.mdwEditor, updateOptions, /* deep watch */ true);
  
      updateOptions(options);
  
      elem.on('$destroy', function () {
        acee.session.$stopWorker();
        acee.destroy();
      });
  
      scope.$watch(function() {
        return [elem[0].offsetWidth, elem[0].offsetHeight];
      }, function() {
        acee.resize();
        acee.renderer.updateFull();
      }, true);
    }
  };
}]);
