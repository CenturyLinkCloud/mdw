'use strict';

var jsxMod = angular.module('mdwJsx', ['mdw']);

jsxMod.directive('mdwJsx', ['$document', 'mdw', function($document, mdw) {
  return {
    restrict: 'A',
    link: function link(scope, elem, attrs, ctrls) {
      
      var addScript = function(url) {
        var script = $document[0].createElement('script');
        return script;
      };
      
      var url = mdw.roots.hub + '/' + attrs.mdwJsx;
      var head = $document[0].getElementsByTagName("head")[0];
      var script = head.querySelector("script[src*='" + url + "']");
      if (script) {
        script.remove();
      }
      script = $document[0].createElement('script');
      script.setAttribute('src', url);
      script.setAttribute('type', 'text/javascript');
      mdw.hubLoading(true);
      head.insertBefore(script, head.firstChild);
      script.onload = function() {
        mdw.hubLoading(false);
      };
    }
  };
}]);
