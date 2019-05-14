'use strict';

var GanttFactory = function(DC) {
  var Gantt = function() {
  };

  return Gantt;
};

if (typeof angular !== 'undefined') {
  var ganttMod = angular.module('mdwGantt', ['mdw']);
  ganttMod.factory('Gantt', ['DC', function(DC) {
    return GanttFactory(DC);
  }]);
}
else if (module) {
  module.exports = GanttFactory;
}

