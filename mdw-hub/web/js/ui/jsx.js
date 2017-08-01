var jsxMod = angular.module('mdwJsx', ['mdw']);

jsxMod.directive('mdwJsx', ['$document', 'mdw', function($document, mdw) {
  return {
    restrict: 'A',
    link: function link(scope, elem, attrs, ctrls) {
      
      var url = mdw.roots.hub + '/' + attrs.mdwJsx;
      var script = $document[0].querySelector("script[src*='" + url + "']");
      if (!script) {
        mdw.hubLoading(true);
        var heads = document.getElementsByTagName("head");
        if (heads && heads.length) {
          var head = heads[0];
          if (head) {
            script = document.createElement('script');
            script.setAttribute('src', url);
            script.setAttribute('type', 'text/javascript');
            head.appendChild(script);
            script.onload = function() {
              mdw.hubLoading(false);
            };
          }
        }
      }
      else {
        $mdwJsxHolder.renderJsx();
      }
    }
  };
}]);
