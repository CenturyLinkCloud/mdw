'use strict';

var swaggerMod = angular.module('mdwSwagger', ['mdw']);

swaggerMod.directive('mdwSwagger', function() {
  return {
    restrict: 'E',
    scope: {
      apiUrl: '=apiUrl'
    },
    template: '<div id="swagger-ui"></div>',
    link: function link(scope, elem, attrs, ctrls) {
      const ui = SwaggerUIBundle({
        url: scope.apiUrl,
        dom_id: '#swagger-ui',
        presets: [
          SwaggerUIBundle.presets.apis,
          SwaggerUIStandalonePreset
        ],
        plugins: [
          SwaggerUIBundle.plugins.DownloadUrl
        ],
        layout: "StandaloneLayout"
      });
      window.ui = ui;      
    }    
  };
});
