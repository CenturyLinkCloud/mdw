'use strict';

var requestMod = angular.module('requests', ['mdw']);

requestMod.controller('RequestsController', ['$scope', '$http', '$location', 'mdw', 'util', 'REQUEST_STATUSES',
                                             function($scope, $http, $location, mdw, util, REQUEST_STATUSES) {
  
  // is this in the context of the Workflow or Services tab?
  $scope.context = $location.path().startsWith('/service') ? 'service' : 'workflow';
  $scope.defaultType = $scope.context == 'service' ? 'inboundRequests' : 'masterRequests';
  
  // two-way bound to/from directive
  $scope.requestList = {};
  $scope.requestFilter = sessionStorage.getItem($scope.context + '_requestFilter');
  if ($scope.requestFilter)
        $scope.requestFilter = JSON.parse($scope.requestFilter);
  if (!$scope.requestFilter) {
    $scope.requestFilter = {
        status: '[Active]',
        sort: 'receivedDate',
        type: $scope.defaultType,
        descending: true
    };
  }
  else {
    // fix date format stored in cookieStore
    if ($scope.requestFilter.receivedDate)
      $scope.requestFilter.receivedDate = util.serviceDate(new Date($scope.requestFilter.receivedDate));
  }

  if ($scope.context == 'service') {
    $scope.requestTypes = {
        inboundRequests: 'Inbound',
        outboundRequests: 'Outbound',
        masterRequests: 'Master Requests'
      };
  }
  else {
    $scope.requestTypes = {
      masterRequests: 'Master Requests', 
      inboundRequests: 'Inbound', 
      outboundRequests: 'Outbound'
    };
  }
  
  // pseudo-status [Active] means non-final
  $scope.allStatuses = ['[Active]'].concat(REQUEST_STATUSES);

  if ($scope.requestFilter.path) {
      $scope.typeaheadMatchSelection = $scope.requestFilter.path;
  }

  $scope.setRequestType = function(requestType) {
    $scope.typeaheadMatchSelection = null;
    $scope.clearTypeaheadFilters();
    $scope.requestFilter.type = requestType;
  };

  $scope.$on('page-retrieved', function(event, requestList) {
    // received date and end date, adjusted for db offset
    var dbDate = new Date(requestList.retrieveDate);
    requestList.requests.forEach(function(requestInstances) {
      requestInstances.receivedDate = util.formatDateTime(util.correctDbDate(new Date(requestInstances.receivedDate), dbDate));
      if (requestInstances.endDate)
        requestInstances.endDate = util.formatDateTime(util.correctDbDate(new Date(requestInstances.endDate), dbDate));
    });
    requestList.context = $scope.context;
    sessionStorage.setItem($scope.context + '_requestFilter', JSON.stringify($scope.requestFilter));
  });
  
  // docId or masterRequestId or path
  $scope.findTypeaheadMatches = function(typed) {
    var url = mdw.roots.services + '/services/Requests' + '?app=mdw-admin&type=' + $scope.requestFilter.type + '&find=' + typed;
    return $http.get(url).then(function(response) {
      // service returns matching requests
      var reqs = response.data.requests;
      var matches = [];
      reqs.forEach(function(req) {
        if (!isNaN(typed) && $scope.requestFilter.type != 'masterRequests')
          matches.push({type: 'id', value: req.id});
        else if (req.path && req.path.startsWith(typed)) {
          var existPath = matches.find(function(match) {
            return match.type === 'path' && match.value === req.path;
          });
          if (!existPath)
            matches.push({type: 'path', value: req.path});
        }
        else if (req.masterRequestId)
          matches.push({type: 'masterRequestId', value: req.masterRequestId});
      });
      return matches;
    });
  };

  $scope.clearTypeaheadFilters = function() {
    $scope.requestFilter.masterRequestId = null;
    $scope.requestFilter.id = null;
    $scope.requestFilter.path = null;
  };
  
  $scope.typeaheadChange = function() {
    if ($scope.typeaheadMatchSelection === null)
      $scope.clearTypeaheadFilters();
  };
  
  $scope.typeaheadSelect = function() {
    $scope.clearTypeaheadFilters();
    $scope.requestFilter[$scope.typeaheadMatchSelection.type] = $scope.typeaheadMatchSelection.value;
  };

  $scope.clearTypeahead = function() {
    $scope.typeaheadMatchSelection = null;
    $scope.clearTypeaheadFilters();
  };
  
}]);

requestMod.controller('RequestController', ['$scope', '$location', '$route', '$routeParams', 'mdw', 'util', 'Request',
                                             function($scope, $location, $route, $routeParams, mdw, util, Request) {
  $scope.context = $location.path().startsWith('/service/') ? 'service' : 'workflow';
  
  $scope.view = {
      request: 'Raw',
      response: 'Raw'
  };
  
  var response = $route.current.loadedTemplateUrl.startsWith('requests/response');
  var master = false;
  var id = $routeParams.requestId;
  var masterReqId = $routeParams.masterRequestId;
  if (masterReqId) {
    master = true;
    id = masterReqId;
  }
  $scope.request = Request.retrieve({requestId: id, master: master, response: response}, function() {
    var trimmed;
    if (response) {
      trimmed = $scope.request.responseContent.trim();
      if (trimmed.startsWith('{')) {
        $scope.request.responseFormat = 'json';
        try {
          var parsedResponse = JSON.parse(trimmed);
          $scope.request.formattedResponse = JSON.stringify(parsedResponse, null, 2);
        }
        catch (ex) {
          console.log(ex);
        }
      }
      else if (trimmed.startsWith('<')) {
        $scope.request.responseFormat = 'xml';
      }
      if ($scope.request.responseMeta && $scope.request.responseMeta.headers) {
        $scope.request.responseMetaHeaders = $scope.getMetaHeaders($scope.request.responseMeta.headers);
      }
    }
    else {
      trimmed = $scope.request.content ? $scope.request.content.trim() : '';
      if (trimmed.startsWith('{')) {
        $scope.request.format = 'json';
        try {
          var parsedRequest = JSON.parse(trimmed);
          $scope.request.formattedContent = JSON.stringify(parsedRequest, null, 2);
        }
        catch (ex) {
          console.log(ex);
        }
      }
      else if (trimmed.startsWith('<')) {
        $scope.request.format = 'xml';
      }
      if ($scope.request.meta && $scope.request.meta.headers) {
        $scope.request.metaHeaders = $scope.getMetaHeaders($scope.request.meta.headers);
      }
    }
  });
  
  // returns text to represent the meta.headers
  $scope.getMetaHeaders = function(headers) {
    var metaHeaders = '';
    var propNames = util.getProperties(headers).sort();
    var maxLen = 0;
    propNames.forEach(function(propName) {
      if (propName.length > maxLen)
        maxLen = propName.length;
    });
    propNames.forEach(function(propName) {
      metaHeaders += util.padTrailing(propName + ':', maxLen + 3) + '<span class="mdw-highlight">' + headers[propName] + '</span>\n';
    });
    return metaHeaders;
  };
}]);

requestMod.factory('Request', ['$resource', 'mdw', function($resource, mdw) {
  return $resource(mdw.roots.services + '/Services/Requests/:requestId', mdw.serviceParams(), {
    retrieve: { method: 'GET', isArray: false }
  });
}]);
        


