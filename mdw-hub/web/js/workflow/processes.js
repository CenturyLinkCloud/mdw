'use strict';

var processMod = angular.module('processes', ['mdw']);

processMod.controller('ProcessesController',
    ['$scope', '$http', 'mdw', 'util', 'PROCESS_STATUSES',
    function($scope, $http, mdw, util, PROCESS_STATUSES) {

  $scope.getFilter = function() {
    var procFilter = sessionStorage.getItem('processFilter');
    if (procFilter)
      procFilter = JSON.parse(procFilter);
    else
      procFilter = {};
    return procFilter;
  };

  $scope.setFilter = function(procFilter) {
    if (procFilter) {
        sessionStorage.setItem('processFilter', JSON.stringify(procFilter));
    }
  };

  $scope.defaultFilter = {
    master: true,
    status: '[Active]',
    sort: 'startDate',
    descending: true,
    values: null
  };

  $scope.resetFilter = function(defaults) {
    $scope.processFilter = Object.assign({}, $scope.defaultFilter, defaults);
    $scope.closePopover();
  };

  $scope.isDefaultFilter = function() {
    for (let key of Object.keys($scope.processFilter)) {
      if (!$scope.processFilter[key] && !$scope.defaultFilter[key])
        continue;
      if (key !== 'master' && key !== 'sort' && key !== 'descending' && key !== 'processId' && key !== 'masterRequestId' && key !== 'instanceId' &&
            $scope.processFilter[key] !== $scope.defaultFilter[key]) {
        return false;
      }
    }
    return true;
  };

  // definitionId and processSpec passed in query params
  // (from mdw-studio, for example)
  var definitionIdParam = util.urlParams().definitionId;
  var processSpecParam = util.urlParams().processSpec;
  var valuesParam = util.urlParams().values;
  var procFilter = $scope.getFilter();
  if (definitionIdParam && processSpecParam) {
    procFilter.processId = definitionIdParam;
    procFilter.master = false;
    procFilter.status = '[Any]';
    procFilter.values = null;
    $scope.setFilter(procFilter);
    if (processSpecParam.endsWith('.proc'))
      processSpecParam = processSpecParam.substring(0, processSpecParam.length - 5);
    sessionStorage.setItem('processSpec', processSpecParam);
    if (!valuesParam) {  // otherwise wait redirect after setting values
      window.location = mdw.roots.hub + '#/workflow/processes';
      return;
    }
  }
  if (valuesParam) {
    procFilter.master = false;
    procFilter.status = '[Any]';
    procFilter.values = valuesParam;
    $scope.setFilter(procFilter);
    window.location = mdw.roots.hub + '#/workflow/processes';
    return;
  }

  // two-way bound to/from directive
  $scope.processList = {};

  var processFilter = sessionStorage.getItem('processFilter');
  if (!processFilter) {
    $scope.resetFilter();
  }
  else {
    $scope.processFilter = JSON.parse(processFilter);
    // don't remember these
    $scope.processFilter.instanceId = null;
    $scope.processFilter.masterRequestId = null;
    // fix date format stored in cookieStore
    if ($scope.processFilter.startDate)
      $scope.processFilter.startDate = util.serviceDate(new Date($scope.processFilter.startDate));
  }

  // pseudo-status [Active] means non-final
  $scope.allStatuses = ['[Active]','[Any]'].concat(PROCESS_STATUSES);

  // preselected procDef
  if ($scope.processFilter.processId) {
    $scope.typeaheadMatchSelection = sessionStorage.getItem('processSpec');
  }
  else {
    sessionStorage.removeItem('processSpec');
  }

  $scope.getValueFilters = function() {
    if ($scope.processFilter.values) {
      // translate param string to object
      return util.toValuesObject($scope.processFilter.values);
    }
  };

  $scope.setValueFilter = function(name, value) {
    if (name) {
      if (value || value === '') {
        var valuesObject = util.toValuesObject($scope.processFilter.values);
        if (!valuesObject) {
          valuesObject = {};
        }
        valuesObject[name] = value;
        $scope.processFilter.values = util.toParamString(valuesObject);
      }
      else {
        $scope.removeValueFilter(name);
      }
    }
  };

  $scope.removeValueFilter = function(name) {
    if ($scope.processFilter.values) {
      var valuesObject = util.toValuesObject($scope.processFilter.values);
      delete valuesObject[name];
      $scope.processFilter.values = util.toParamString(valuesObject);
    }
  };

  $scope.$on('page-retrieved', function(event, processList) {
    // start date and end date, adjusted for db offset
    var dbDate = new Date(processList.retrieveDate);
    processList.processInstances.forEach(function(processInstance) {
      processInstance.startDate = util.formatDateTime(util.correctDbDate(new Date(processInstance.startDate), dbDate));
      if (processInstance.endDate)
        processInstance.endDate = util.formatDateTime(util.correctDbDate(new Date(processInstance.endDate), dbDate));
    });
    if ($scope.processFilter.processId) {
      if (processList.processInstances.length > 0) {
        let procSpec = processList.processInstances[0].processName;
        if (processList.processInstances[0].processVersion)
          procSpec += ' v' + processList.processInstances[0].processVersion;
        sessionStorage.setItem('processSpec', procSpec);
      }
      else {
        $http.get(mdw.roots.services + '/services/Workflow?id=' + $scope.processFilter.processId + '&summary=true&app=mdw-admin')
          .then(function(response) {
            sessionStorage.setItem('processSpec', response.data.name + ' v' + response.data.version);
          }
        );
      }
    }
    else {
      sessionStorage.removeItem('processSpec');
    }
    sessionStorage.setItem('processFilter', JSON.stringify($scope.processFilter));
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
          if (procInst.masterRequestId.toLowerCase().startsWith(typed.toLowerCase())) {
            var existMrId = matches.find(function(match) {
              return match.type === 'masterRequestId' && match.value.toLowerCase() === procInst.masterRequestId.toLowerCase();
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
    if ($scope.processFilter.instanceId)
      $scope.processFilter.instanceId = null;
    if ($scope.processFilter.masterRequestId)
      $scope.processFilter.masterRequestId = null;
    if ($scope.processFilter.processId)
      $scope.processFilter.processId = null;
    $scope.resetFilter({master: $scope.processFilter.master});
  };

  $scope.typeaheadChange = function() {
    if ($scope.typeaheadMatchSelection === null)
      $scope.clearTypeaheadFilters();
  };

  $scope.typeaheadSelect = function() {
    $scope.clearTypeaheadFilters();
    $scope.processFilter.status = null;
    $scope.processFilter.values = null;
    if ($scope.typeaheadMatchSelection.id)
      $scope.processFilter[$scope.typeaheadMatchSelection.type] = $scope.typeaheadMatchSelection.id;
    else
      $scope.processFilter[$scope.typeaheadMatchSelection.type] = $scope.typeaheadMatchSelection.value;
  };

  $scope.clearTypeahead = function() {
    $scope.typeaheadMatchSelection = null;
    $scope.clearTypeaheadFilters();
    sessionStorage.removeItem('processSpec');
  };

  $scope.goChart = function() {
    window.location = 'dashboard/processes';
  };
}]);

processMod.controller('ProcessController',
    ['$scope', '$route', '$routeParams', '$filter', '$http', 'mdw', 'util', 'uiUtil', 'Process', 'ProcessSummary', 'MilestoneGroups', 'MilestoneGroupsPersist', 'Staging', 'DOCUMENT_TYPES', 'WORKFLOW_STATUSES',
     function($scope, $route, $routeParams, $filter, $http, mdw, util, uiUtil, Process, ProcessSummary, MilestoneGroups, MilestoneGroupsPersist, Staging, DOCUMENT_TYPES, WORKFLOW_STATUSES) {

  var activity = sessionStorage.getItem('mdw-activityInstance');
  if (activity) {
    $scope.activity = activity;
    sessionStorage.removeItem('mdw-activityInstance');
  }

  $scope.retrieveProcess = function() {
    if ($routeParams.triggerId) {
      $scope.process = Process.retrieve({triggerId: $routeParams.triggerId}, function() {
        ProcessSummary.set($scope.process);
        $scope.archived = $scope.process.archived;
        if ($scope.process.hasMilestones && !sessionStorage.getItem("mdw-milestoneGroups")) {
          MilestoneGroups.retrieve({}, function(data) { MilestoneGroupsPersist.save(data); });
        }
      });
    }
    else {
      $scope.process = Process.retrieve({instanceId: $routeParams.instanceId, extra: 'summary'}, function() {
        ProcessSummary.set($scope.process);
        $scope.archived = $scope.process.archived;
        if ($scope.process.hasMilestones && !sessionStorage.getItem("mdw-milestoneGroups")) {
          MilestoneGroups.retrieve({}, function(data) { MilestoneGroupsPersist.save(data); });
        }
      });
    }
  };

  $scope.workflowStatuses = WORKFLOW_STATUSES;
  $scope.valuesEdit = false;
  $scope.editValues = function(edit) {
    $scope.valuesEdit = edit;
    if ($scope.valuesEdit)
      $scope.values = $scope.allValues;
    else
      $scope.values = $scope.populatedValues;
  };
  $scope.saveValues = function() {
    console.log('saving process values for instance: ' + $scope.process.id);
    var newValues = {};
    util.getProperties($scope.values).forEach(function(name) {
      var value = $scope.values[name];
      if (value.dirty) {
        if (value.type === 'java.util.Date' && value.value) {
            var timezoneAbbr = 'MST'; // TODO
            newValues[value.name] = $filter('date')(value.value, 'EEE MMM dd HH:mm:ss ') + timezoneAbbr + $filter('date')(value.value, ' yyyy');
        }
        else {
          newValues[value.name] = value.value === null ? '' : value.value;
        }
      }
    });
    Process.update({instanceId: $scope.process.id, extra: 'values'}, newValues,
      function(data) {
        if (data.status.code >= 300) {
          mdw.messages = data.status.message;
        }
        else {
          $route.reload();
        }
      },
      function(error) {
        mdw.messages = error.data.status.message;
      }
    );
  };

  if ($route.current.originalPath.endsWith('/values') || $routeParams.name) {
    if ($routeParams.name) {
      $scope.value = Process.retrieve({instanceId: $routeParams.instanceId, extra: 'values', valueName: $routeParams.name}, function() {
        $scope.value.name = $routeParams.name;
        if ($scope.value.type) {
          $scope.value.format = DOCUMENT_TYPES[$scope.value.type];
          if ($scope.value.type === 'java.lang.Exception')
            $scope.value.view = 'exception';
        }
      });
    }
    else {
      $scope.allValues = Process.retrieve({instanceId: $routeParams.instanceId, extra: 'values', includeEmpty: 'true'}, function() {
        $scope.populatedValues = {};
        var emptyValues = {};
        util.getProperties($scope.allValues).forEach(function(name) {
          var value = $scope.allValues[name];
          if (value.value)
            $scope.populatedValues[name] = value;
          else
            emptyValues[name] = value;
        });
        $scope.values = $scope.populatedValues;
      });
    }

    $scope.process = ProcessSummary.get();
    if (!$scope.process) {
      $scope.retrieveProcess();
    }
  }
  else {
    $scope.retrieveProcess();
  }

  $scope.isException = function(value) {
    return value.type == 'java.lang.Exception';
  };

  $scope.asException = function(value) {
    return util.asException(value);
  };

  $scope.instanceCancelAllowed = function() {
    return $scope.authUser.hasRole('Process Execution') && $scope.process &&
        ($scope.process.status == 'In Progress' || $scope.process.status == "Waiting");
  };
  $scope.confirmCancel = function() {
    var msg = 'Cancel process ' + $scope.process.id + '?  (Cannot be undone.)';
    uiUtil.confirm('Confirm Cancel', msg, function(res) {
      if (res) {
        $http.delete(mdw.roots.services + '/services/Processes/' + $scope.process.id + '?app=mdw-admin')
        .then(function(response) {
          $scope.process.status = 'Canceled';  // prevent Cancel button still showing
        }, function(response) {
          if (response.data.status)
            mdw.messages = 'Process cancel failed: ' + response.data.status.message;
        });
      }
    });
  };
  $scope.instanceEditAllowed = function() {
    return $scope.authUser.hasRole('Process Execution') && $scope.process &&
        ($scope.process.status == 'In Progress' || $scope.process.status == "Waiting");
  };
  $scope.definitionEditAllowed = function() {
    return $scope.authUser.hasRole('Process Design') && !mdw.git.tag;
  };

  $scope.stageAsset = function() {
    var assetPath = $scope.process.packageName + '/' + $scope.process.name + '.proc';
    Staging.stageAsset($scope.authUser.cuid, assetPath, function(statusCode) {
      if (statusCode < 400 || statusCode == 404)
      $scope.authUser.setActiveTab("#/staging");
    });
  };
}]);

processMod.factory('Process', ['$resource', 'mdw', function($resource, mdw) {
  return $resource(mdw.roots.services + '/Services/Processes/:instanceId/:extra/:valueName', mdw.serviceParams(), {
    retrieve: { method: 'GET', isArray: false },
    update: {method: 'PUT'}
  });
}]);

processMod.controller('ProcessDefsController', ['$scope', 'mdw', 'util', 'ProcessDef',
                                               function($scope, mdw, util, ProcessDef) {
  $scope.definitionList = ProcessDef.retrieve({}, function success() {
    var pkgs = $scope.definitionList.packages;
    pkgs.forEach(function(pkg) {
      pkg.assets.forEach(function(a) {
        a.baseName = a.name.substring(0, a.name.lastIndexOf('.'));
      });
      $scope.applyPkgCollapsedState();
    });
  });

  // TODO: copied from tests.js, consider refactoring
  $scope.collapse = function(pkg) {
    pkg.collapsed = true;
    $scope.savePkgCollapsedState();
  };
  $scope.collapseAll = function() {
    $scope.definitionList.packages.forEach(function(pkg) {
      pkg.collapsed = true;
    });
    $scope.savePkgCollapsedState();
  };
  $scope.expand = function(pkg) {
    pkg.collapsed = false;
    $scope.savePkgCollapsedState();
  };
  $scope.expandAll = function() {
    $scope.definitionList.packages.forEach(function(pkg) {
      pkg.collapsed = false;
    });
    $scope.savePkgCollapsedState();
  };
  $scope.savePkgCollapsedState = function() {
    var st = {};
    $scope.definitionList.packages.forEach(function(pkg) {
      if (pkg.collapsed)
        st[pkg.name] = true;
    });
    sessionStorage.setItem('procsPkgCollapsedState', JSON.stringify(st));
  };
  $scope.applyPkgCollapsedState = function() {
    var st = sessionStorage.getItem('procsPkgCollapsedState');
    if (st) {
      st = JSON.parse(st);
      util.getProperties(st).forEach(function(pkgName) {
        var col = st[pkgName];
        if (col === true) {
          for (var i = 0; i < $scope.definitionList.packages.length; i++) {
            if (pkgName == $scope.definitionList.packages[i].name) {
              $scope.definitionList.packages[i].collapsed = true;
              break;
            }
          }
        }
      });
    }
  };
}]);

processMod.controller('ProcessDefController',
    ['$scope', '$routeParams', '$route', '$location', '$filter', '$http', 'mdw', 'util', 'uiUtil', 'ProcessDef', 'ProcessSummary', 'ProcessVersions', 'MilestoneGroups', 'MilestoneGroupsPersist', 'Staging',
    function($scope, $routeParams, $route, $location, $filter, $http, mdw, util, uiUtil, ProcessDef, ProcessSummary, ProcessVersions, MilestoneGroups, MilestoneGroupsPersist, Staging) {

  var activity = sessionStorage.getItem('mdw-activity');
  if (activity) {
    $scope.activity = activity;  // (will be highlighted in rendering)
    sessionStorage.removeItem('mdw-activity');
  }

  $scope.process = {
    packageName: $routeParams.packageName,
    name: $routeParams.processName,
    version: $routeParams.version
  };
  if ($scope.process.version) {
    $scope.process.archived = true;
  }
  $scope.definitionId = null;  // stored at scope level due to vagaries in $scope.process

  $scope.setProcessFilter = function() {
    var procFilter = sessionStorage.getItem('processFilter');
    if (procFilter)
      procFilter = JSON.parse(procFilter);
    else
      procFilter = {};
    procFilter.processId = $scope.definitionId;
    if (procFilter.processId) {
      var procSpec = $scope.process.name;
      if ($scope.process.version)
        procSpec += ' v' + $scope.process.version;
      else if ($routeParams.version)
        procSpec += ' v' + $routeParams.version;
      procFilter.master = false;
      procFilter.status = null;
      sessionStorage.setItem('processFilter', JSON.stringify(procFilter));
      sessionStorage.setItem('processSpec', procSpec);
    }
  };

  var summary = ProcessSummary.get();
  if (summary) {
    if (!$routeParams.version || $routeParams.version.indexOf(".") < 0)
      $scope.process.id = summary.id;
    $scope.process.masterRequestId = summary.masterRequestId;
    $scope.process.status = summary.status;
    $scope.process.definitionId = summary.definitionId;
    $scope.process.template = summary.template;
    $scope.process.hasMilestones = summary.hasMilestones;
    $scope.process.archived = summary.archived;
    if ($scope.process.archived)
      $scope.process.version = summary.template ? summary.templateVersion : summary.version;
    $scope.definitionId = $scope.process.definitionId;
    if ($route.current && $route.current.templateUrl && $route.current.templateUrl.endsWith('/versions.html')) {
      ProcessVersions.retrieve({
          packageName: $scope.process.packageName,
          processName: $scope.process.name,
          withCommitInfo: $mdwConfig.processVersions.withCommitInfo,
          withInstanceCounts: $mdwConfig.processVersions.withInstanceCounts
        }, function(data) {
        $scope.versions = data.versions;
        $scope.versions.forEach(function(version) {
          if (version.commitInfo && version.commitInfo.date) {
            version.commitInfo.date = new Date(version.commitInfo.date);
          }
        });
      });
    }
    if ($scope.process.hasMilestones && !sessionStorage.getItem("mdw-milestoneGroups")) {
      MilestoneGroups.retrieve({}, function(data) { MilestoneGroupsPersist.save(data); });
    }
  }
  else {
    var defSum = ProcessDef.retrieve({packageName: $scope.process.packageName, processName: $scope.process.name, processVersion: $scope.process.version, summary: true}, function() {
        $scope.process.definitionId = defSum.id;
        $scope.definitionId = $scope.process.definitionId;
        $scope.template = $scope.process.template;
        $scope.process.hasMilestones = defSum.hasMilestones;
        $scope.process.archived = defSum.archived;
        if ($scope.process.archived)
          $scope.process.version = $scope.process.template ? defSum.templateVersion : defSum.version;
        if ($route.current && $route.current.templateUrl && $route.current.templateUrl.endsWith('/versions.html')) {
          ProcessVersions.retrieve({
              packageName: $scope.process.packageName,
              processName: $scope.process.name,
              withCommitInfo: $mdwConfig.processVersions.withCommitInfo,
              withInstanceCounts: $mdwConfig.processVersions.withInstanceCounts
            }, function(data) {
            $scope.versions = data.versions;
            $scope.versions.forEach(function(version) {
              if (version.commitInfo && version.commitInfo.date) {
                version.commitInfo.date = new Date(version.commitInfo.date);
              }
            });
          });
        }
      if ($scope.process.hasMilestones && !sessionStorage.getItem("mdw-milestoneGroups")) {
        MilestoneGroups.retrieve({}, function(data) { MilestoneGroupsPersist.save(data); });
      }
    });
  }

  $scope.isEditAllowed = function() {
    // TODO: no edit for archived or instance defs
    return $scope.authUser.hasRole('Process Design') && !mdw.git.tag;
  };

  $scope.stageAsset = function() {
    var assetPath = $scope.process.packageName + '/' + $scope.process.name + '.proc';
    Staging.stageAsset($scope.authUser.cuid, assetPath, function(statusCode) {
      if (statusCode < 400 || statusCode == 404)
      $scope.authUser.setActiveTab("#/staging");
    });
  };


  $scope.exportProcess = function(format) {
    var baseUrl = mdw.roots.hub + '/asset/' + $scope.process.packageName + '/' + $scope.process.name + '.proc?download=true';
    window.location = baseUrl + '&render=' + format;
  };
}]);

processMod.controller('ProcessLogController', ['$scope', '$http', '$routeParams', 'mdw', 'util', 'EXCEL_DOWNLOAD',
                                            function($scope, $http, $routeParams, mdw, util, EXCEL_DOWNLOAD) {
  $scope.process = {
    id: $routeParams.instanceId
  };

  $scope.title = 'Process ' + $scope.process.id + ' Log';

  $scope.logTime = 'DB Time';
  $scope.setLogTime = function(logTime) {
    $scope.logTime = logTime;
    document.getElementById('log-time-popover').click();
    util.calcLogTimes($scope.logData, logTime);
  };

  var url = mdw.roots.services + '/services/Processes/' + $scope.process.id + '/log?withActivities=true&app=mdw-admin';

  $http.get(url)
    .then(function(response) {
      $scope.logData = response.data;
      util.calcLogTimes($scope.logData, $scope.logTime);
    }
  );

  $scope.downloadExcel = function() {
    window.location = url + '&' + EXCEL_DOWNLOAD;
  };
}]);

// retains state for nav
processMod.factory('ProcessSummary', ['mdw', function(mdw) {
  return {
    set: function(process) {
      this.process = process;
    },
    get: function() {
      return this.process;
    }
  };
}]);

processMod.factory('ProcessDef', ['$resource', 'mdw', function($resource, mdw) {
  return $resource(mdw.roots.services + '/Services/Workflow/:packageName/:processName/:processVersion', mdw.serviceParams(), {
    retrieve: { method: 'GET', isArray: false }
  });
}]);

processMod.factory('ProcessVersions', ['$resource', 'mdw', function($resource, mdw) {
  return $resource(mdw.roots.services + '/Services/Versions/:packageName/:processName' + '.proc', mdw.serviceParams(), {
    retrieve: { method: 'GET', isArray: false }
  });
}]);

processMod.factory('MilestoneGroups', ['$resource', 'mdw', function($resource, mdw) {
  return $resource(mdw.roots.services + '/Services/com/centurylink/mdw/milestones/groups', mdw.serviceParams(), {
    retrieve: { method: 'GET', isArray: false }
  });
}]);

processMod.factory('MilestoneGroupsPersist', ['mdw', function(mdw) {
  return {
    save: function(data) {
      var groups = [];
      if (data.groups) {
        groups = data.groups;
        groups.sort((g1, g2) => g1.name.localeCompare(g2.name));
      }
      sessionStorage.setItem("mdw-milestoneGroups", JSON.stringify(groups));
    },
  };
}]);
