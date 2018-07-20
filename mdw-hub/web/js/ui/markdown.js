'use strict';

var mdMod = angular.module('mdwMarkdown', ['mdw']);

mdMod.controller('MdwMarkdownController', 
    ['$scope', '$http', 'mdw', function($scope, $http, mdw) {

  $scope.init = function() {
    $scope.edits = 0;
    $http.get(mdw.roots.services + '/asset/' + $scope.markdownAsset)
    .then(function success(response) {
      $scope.markdown = response.data;
      var mde = new SimpleMDE({
        autofocus: true,
        indentWithTabs: false,
        spellChecker: false
      });
      mde.codemirror.on("change", function() {
        // first time doesn't count
        $scope.edits++;
        if ($scope.edits >= 2) {
          $scope.$apply(function() {
            $scope.valueChanged(mde.value());
          });
        }
      });      
      mde.value($scope.markdown);
    }, function error(response) {
      mdw.messages = response.statusText;
    });
  };

  $scope.valueChanged = function(newVal) {
    $scope.$parent.setDirty(true);
    $scope.markdown = newVal;
    $scope.$parent.asset.content = $scope.markdown;
  };  
}]);

mdMod.directive('mdwMarkdown', [function() {
  return {
    restrict: 'E',
    templateUrl: 'ui/markdown.html',
    scope: {
      markdownAsset: '@markdownAsset',
      editable: '@editable'
    },
    controller: 'MdwMarkdownController',
    controllerAs: 'mdwMarkdown',
    link: function link(scope, elem, attrs, ctrls) {
      scope.init();
    }
  };
}]);