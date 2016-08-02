// Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
'use strict';
var panelMod = angular.module('mdwPanel', ['mdw']);

// device-specific wrap
panelMod.directive('mdwPanelCollapse', [function() {
  return {
    restrict: 'E',
    templateUrl: 'ui/panel-collapse.html',
    scope: {
      isCollapsed: '=mdwCollapsed'
    }
  };
}]);
