//Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
'use strict';

var activityMod = angular.module('activities', ['mdw']);

activityMod.controller('ActivitiesController', ['$scope', '$http', '$uibModal', 'mdw', 'util', 'ACTIVITY_STATUSES',
                                                function($scope, $http, $uibModal, mdw, util, ACTIVITY_STATUSES) {

  // two-way bound to/from directive
  $scope.model = {};
  $scope.model.activityList = {};
  $scope.model.activityFilter = { 
      status: 'Failed',
      sort: 'startDate',
      descending: true
  };

  $scope.allStatuses = ACTIVITY_STATUSES;

  $scope.getSelectedActivities = function() {
    return $scope.model.activityList.getSelectedItems();
  };
  
  $scope.confirmAction = function(action) {
    $scope.action = action;
    $scope.closePopover(); // popover should be closed
    var modalInstance = $uibModal.open({
      scope: $scope,
      templateUrl: 'workflow/activityActionConfirm.html',
      controller: 'ActivitiesController',
      size: 'sm'
    });    
  };

  $scope.performActionOnActivities = function(action) {
    var selectedActivities = $scope.$parent.getSelectedActivities();
    $scope.closePopover(); // popover should be closed
    if (selectedActivities && selectedActivities.length > 0) {
      console.log('Performing action: ' + $scope.action + ' on ' + selectedActivities.length + ' selected activitie(s)');
      var instanceIds = [];
      selectedActivities.forEach(function(activity) {
        instanceIds.push(activity.id);
      });

      var errorHandler = function(data, status) {
        console.log('http: ' + status);
        $scope.model.activityList.reload(function(activityList) {
          $scope.updateOnActionError(data.status.message, instanceIds, activityList);
        });
      };
      var successHandler = function(data, status, headers, config) {
        if (data.status.code !== 0) {
          $scope.$parent.model.activityList.reload(function(activityList) {
            $scope.updateOnActionError(data.status.message, instanceIds, activityList);
          });
        }
        else {
          $scope.$close();
          $scope.$parent.model.activityList.reload();
        }
      };
      var actionSubUrl = '';
      if (!angular.isUndefined($scope.model.completionCode))
      {
        actionSubUrl = $scope.action + "/" + $scope.model.completionCode;
      }
      else {
        actionSubUrl = $scope.action;
      }
      for (var i = 0; i < selectedActivities.length; i++) {
        var request = $http({
          method : "post",
          url : mdw.roots.services + '/Services/Activities/' + selectedActivities[i].id + '/' + actionSubUrl + '?app=mdw-admin',
          data : {}
        });

        request.error(errorHandler).success(successHandler);
      }
    }
    else {
      mdw.messages = 'Please select Activity(s) to perform action on.';
    }
  };

  $scope.updateOnActionError = function(message, prevSelInstIds, activityList) {
    mdw.messages = message;
    $scope.$close();
    // find problem instance id if available
    var instIdIdx = message.indexOf('instance: ');
    if (instIdIdx !== -1) {
      var erroredInstId = parseInt(message.substring(instIdIdx + 10));
      if (erroredInstId) {
        var after = false;
        var newSelInstIds = [];
        prevSelInstIds.forEach(function(prevSelInstId) {
          if (prevSelInstId == erroredInstId)
            after = true;
          if (after) {
            newSelInstIds.push(prevSelInstId);
          }
        });
        activityList.forEach(function(activity) {
          if (newSelInstIds.indexOf(activity.id) >= 0) {
            activity.selected = true;
          }
        });
      }
    }
    $scope.$parent.digest(); // to show mdw.messages
  };

  $scope.model.typeaheadMatchSelection = null;

  // activity instanceId, masterRequestId, activity name
  $scope.findTypeaheadMatches = function(typed) {
    return $http.get(mdw.roots.services + '/services/Activities' + '?app=mdw-admin&find=' + typed).then(function(response) {
      // service matches on instanceId or masterRequestId
      var actInsts = response.data.activityInstances;
      console.log('role' + $scope.authUser.hasRole('Process Execution'));
      if (actInsts.length > 0) {
        var matches = [];
        actInsts.forEach(function(actInst) {
          if (actInst.id.toString().startsWith(typed))
            matches.push({type: 'instanceId', value: actInst.id.toString()});
          else
            matches.push({type: 'masterRequestId', value: actInst.masterRequestId});
        });
        return matches;
      }
      else {
        return $http.get(mdw.roots.services + '/services/Activities/definitions' + '?app=mdw-admin&find=' + typed).then(function(response) {
          // services matches on activity name
          if (response.data.activityInstances.length > 0) {
            var matches2 = [];
            var encodedactivityName;
            response.data.activityInstances.forEach(function(actDef) {
              encodedactivityName = encodeURIComponent(actDef.name);
              matches2.push({type: 'activityName', value: actDef.name + ' (' + actDef.definitionId + ') /' + actDef.processName + ' v' + actDef.processVersion, id: encodedactivityName});
            });
            return matches2;
          }
        });                     
      }
    });
  };

  $scope.clearTypeaheadFilters = function() {
    // check if defined to avoid triggering evaluation
    if ($scope.model.activityFilter.instanceId)
      $scope.model.activityFilter.instanceId = null;
    if ($scope.model.activityFilter.masterRequestId)
      $scope.model.activityFilter.masterRequestId = null;
    if ($scope.model.activityFilter.name)
      $scope.model.activityFilter.name = null;
  };

  $scope.typeaheadChange = function() {
    if ($scope.model.typeaheadMatchSelection === null)
      $scope.clearTypeaheadFilters();
  };

  $scope.typeaheadSelect = function() {
    $scope.clearTypeaheadFilters();
    if ($scope.model.typeaheadMatchSelection.id)
      $scope.model.activityFilter[$scope.model.typeaheadMatchSelection.type] = $scope.model.typeaheadMatchSelection.id;
    else
      $scope.model.activityFilter[$scope.model.typeaheadMatchSelection.type] = $scope.model.typeaheadMatchSelection.value;
  }; 
  
  $scope.cancelAction = function(){ 
    $scope.$close();
  };

  $scope.getSelectedActivitiesMessage = function() {
    if ($scope.selectedActivities) {
      var base = 'Do you want to perform the selected action on ';
      if ($scope.selectedActivities.length == 1)
        return base + 'the selected activity?';
      else
        return base + $scope.selectedActivities.length + ' activities?';
    }
  };
}]);

activityMod.controller('ActivityController', ['$scope', '$http', '$route', '$uibModal', '$routeParams', 'mdw', 'Activity',
                                            function($scope, $http, $route, $uibModal, $routeParams, mdw, Activity) {
  $scope.model = {};
  $scope.model.singalActivity = true;
  $scope.random = Math.random(); // param to force image reload

  $scope.activity = Activity.retrieve({instanceId: $routeParams.instanceId}, function() {
    $scope.activity.name = $scope.activity.name;
    $scope.item = $scope.activity; // for activityItem template
   });
   
  $scope.refreshWorkflowImage = function() {
     $route.reload();
  };   
  $scope.getSelectedActivitiesMessage = function() {
       return 'Do you want to perform the selected action on this activity?';
  };
   
  $scope.performActionOnActivities = function(action) {
     $scope.closePopover(); // popover should be closed

       var errorHandler = function(data, status) {
         console.log('http: ' + status);
       };
       var successHandler = function(data, status, headers, config) {
         if (data.status.code !== 0) {
         }
         else {
           $scope.$close();
         }
       };
       var actionSubUrl = '';
       if (!angular.isUndefined($scope.model.completionCode))
       {
         actionSubUrl = action + "/" + $scope.model.completionCode;
       }
       else {
         actionSubUrl = action;
       }
         var request = $http({
           method : "post",
           url : mdw.roots.services + '/Services/Activities/' + $scope.activity.id + '/' + actionSubUrl + '?app=mdw-admin',
           data : {}
         });
         $scope.$close();
         request.error(errorHandler).success(successHandler);
         $scope.refreshWorkflowImage();
   };
   
   $scope.cancelAction = function(){ 
     $scope.$close();
   };
   
   $scope.confirmAction = function(action) {
     $scope.action = action;
     $scope.closePopover(); // popover should be closed
     var modalInstance = $uibModal.open({
       scope: $scope,
       templateUrl: 'workflow/activityActionConfirm.html',
       controller: 'ActivityController',
       size: 'sm'
     });    
   };
   
}]);

activityMod.factory('Activity', ['$resource', 'mdw', function($resource, mdw) {
  return $resource(mdw.roots.services + '/Services/Activities/:instanceId', mdw.serviceParams(), {
    retrieve: { method: 'GET', isArray: false }
  });
}]);
