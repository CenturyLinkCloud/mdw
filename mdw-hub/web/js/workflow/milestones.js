'use strict';

var milestonesMod = angular.module('milestones', ['mdw']);

milestonesMod.controller('MilestonesController',
    ['$scope', '$http', 'mdw', 'util', 'PROCESS_STATUSES',
    function($scope, $http, mdw, util, PROCESS_STATUSES) {

  $scope.getFilter = function() {
    var filter = sessionStorage.getItem('milestonesFilter');
    if (filter)
      filter = JSON.parse(filter);
    else
      filter = {};
    return filter;
  };

  $scope.setFilter = function(filter) {
    if (filter) {
        sessionStorage.setItem('milestonesFilter', JSON.stringify(filter));
    }
  };

  $scope.resetFilter = function() {
    $scope.milestonesFilter = {
        status: '[Active]',
        sort: 'startDate',
        descending: true,
        values: null
    };
  };

  // definitionId and processSpec passed in query params
  // (from mdw-studio, for example)
  var definitionIdParam = util.urlParams().definitionId;
  var processSpecParam = util.urlParams().processSpec;
  var valuesParam = util.urlParams().values;
  var filter = $scope.getFilter();
  if (definitionIdParam && processSpecParam) {
    filter.processId = definitionIdParam;
    filter.status = '[Any]';
    filter.values = null;
    $scope.setFilter(filter);
    if (processSpecParam.endsWith('.proc'))
      processSpecParam = processSpecParam.substring(0, processSpecParam.length - 5);
    sessionStorage.setItem('milestonesProcessSpec', processSpecParam);
    if (!valuesParam) {  // otherwise wait redirect after setting values
      window.location = mdw.roots.hub + '#/workflow/milestones';
      return;
    }
  }
  if (valuesParam) {
    filter.status = '[Any]';
    filter.values = valuesParam;
    $scope.setFilter(filter);
    window.location = mdw.roots.hub + '#/workflow/milestones';
    return;
  }

  // two-way bound to/from directive
  $scope.milestonesList = {};

  var sessionFilter = sessionStorage.getItem('milestonesFilter');
  if (!sessionFilter) {
    $scope.resetFilter();
  }
  else {
    $scope.milestonesFilter = JSON.parse(sessionFilter);
    // don't remember these
    $scope.milestonesFilter.instanceId = null;
    $scope.milestonesFilter.masterRequestId = null;
  }

  // pseudo-status [Active] means non-final
  $scope.allStatuses = ['[Active]','[Any]'].concat(PROCESS_STATUSES);

  // preselected procDef
  if ($scope.milestonesFilter.processId) {
    $scope.typeaheadMatchSelection = sessionStorage.getItem('processSpec');
  }
  else {
    sessionStorage.removeItem('processSpec');
  }

  $scope.getValueFilters = function() {
    if ($scope.milestonesFilter.values) {
      // translate param string to object
      return util.toValuesObject($scope.milestonesFilter.values);
    }
  };

  $scope.setValueFilter = function(name, value) {
    if (name) {
      if (value || value === '') {
        var valuesObject = util.toValuesObject($scope.milestonesFilter.values);
        if (!valuesObject) {
          valuesObject = {};
        }
        valuesObject[name] = value;
        $scope.milestonesFilter.values = util.toParamString(valuesObject);
      }
      else {
        $scope.removeValueFilter(name);
      }
    }
  };

  $scope.removeValueFilter = function(name) {
    if ($scope.milestonesFilter.values) {
      var valuesObject = util.toValuesObject($scope.milestonesFilter.values);
      delete valuesObject[name];
      $scope.milestonesFilter.values = util.toParamString(valuesObject);
    }
  };

  $scope.$on('page-retrieved', function(event, milestonesList) {
    // start date and end date
    milestonesList.milestones.forEach(function(milestone) {
      milestone.startDate = util.formatDateTime(new Date(milestone.processInstance.start));
      if (milestone.processInstance.end)
        milestone.endDate = util.formatDateTime(new Date(milestone.processInstance.end));
    });
    if ($scope.milestonesFilter.processId) {
      if (milestonesList.milestones.length > 0) {
        let procSpec = milestonesList.milestones[0].process.name;
        if (milestonesList.milestones[0].process.version)
          procSpec += ' v' + milestonesList.milestones[0].process.version;
        sessionStorage.setItem('processSpec', procSpec);
      }
      else {
        $http.get(mdw.roots.services + '/services/Workflow?id=' + $scope.milestonesFilter.processId + '&summary=true&app=mdw-admin')
          .then(function(response) {
            sessionStorage.setItem('processSpec', response.data.name + ' v' + response.data.version);
          }
        );
      }
    }
    else {
      sessionStorage.removeItem('milestonesProcessSpec');
    }
    sessionStorage.setItem('milestonesFilter', JSON.stringify($scope.milestonesFilter));
  });

  // instanceId, masterRequestId, processName, packageName
  $scope.findTypeaheadMatches = function(typed) {
    return $http.get(mdw.roots.services + '/services/Processes' + '?app=mdw-admin&find=' + typed).then(function(response) {
      // service matches on instanceId or masterRequestId
      var procInsts = response.data.processInstances;
      if (procInsts.length > 0) {
        var matches = [];
        procInsts.forEach(function(procInst) {
          if (procInst.id.toString().startsWith(typed)) {
            var existProcInst = matches.find(function(match) {
              return match.type === 'instanceId' && match.value === procInst.id.toString();
            });
            if (!existProcInst)
              matches.push({type: 'instanceId', value: procInst.id.toString()});
          }
          if (procInst.masterRequestId.startsWith(typed)) {
            var existMrId = matches.find(function(match) {
              return match.type === 'masterRequestId' && match.value === procInst.masterRequestId;
            });
            if (!existMrId)
              matches.push({type: 'masterRequestId', value: procInst.masterRequestId});
          }
        });
        return matches;
      }
      else {
        return $http.get(mdw.roots.services + '/services/Processes/definitions' + '?app=mdw-admin&find=' + typed).then(function(response) {
          // services matches on process or package name
          if (response.data.length > 0) {
            var matches2 = [];
            response.data.forEach(function(procDef) {
              if (typed.indexOf('.') > 0) {
                matches2.push({type: 'processId', value: procDef.packageName + '/' + procDef.name + ' v' + procDef.version, id: procDef.processId});
              }
              else {
                matches2.push({type: 'processId', value: procDef.name + ' v' + procDef.version, id: procDef.processId});
              }
            });
            return matches2;
          }
        });
      }
    });
  };

  $scope.clearTypeaheadFilters = function() {
    // check if defined to avoid triggering evaluation
    if ($scope.milestonesFilter.instanceId)
      $scope.milestonesFilter.instanceId = null;
    if ($scope.milestonesFilter.masterRequestId)
      $scope.milestonesFilter.masterRequestId = null;
    if ($scope.milestonesFilter.processId)
      $scope.milestonesFilter.processId = null;
  };

  $scope.typeaheadChange = function() {
    if ($scope.typeaheadMatchSelection === null)
      $scope.clearTypeaheadFilters();
  };

  $scope.typeaheadSelect = function() {
    $scope.clearTypeaheadFilters();
    if ($scope.typeaheadMatchSelection.id)
      $scope.milestonesFilter[$scope.typeaheadMatchSelection.type] = $scope.typeaheadMatchSelection.id;
    else
      $scope.milestonesFilter[$scope.typeaheadMatchSelection.type] = $scope.typeaheadMatchSelection.value;
  };

  $scope.clearTypeahead = function() {
    $scope.typeaheadMatchSelection = null;
    $scope.clearTypeaheadFilters();
    sessionStorage.removeItem('milestonesProcessSpec');
  };

  $scope.goChart = function() {
    window.location = 'dashboard/milestones';
  };
}]);

