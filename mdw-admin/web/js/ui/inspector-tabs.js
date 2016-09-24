// Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
'use strict';

var inspectorTabSvc = angular.module('mdwInspectorTabs', ['mdw']);

inspectorTabSvc.factory('InspectorTabs', ['mdw', function(mdw) {
  return {
    definition: {
      process: {
        Definition: {
          Name: 'name',
          Description: 'description',
          Created: 'created' 
        },
        Variables: 'variables',
        Attributes: 'attributes',
        Versions: {},
        Documentation: {},
        Monitoring: {}
      },
      activity: {
        Definition: {
          ID: 'id',
          Name: 'name',
          Implementor: 'implementor',
          Description: 'description'
        },
        Attributes: 'attributes',
        Documentation: {},
        Monitoring: {},
        Stubbing: {}
      },
      subprocess: {
        Definition: {
          ID: 'id',
          Name: 'name',
          Description: 'description'
        },
        Attributes: 'attributes',
        Documentation: {}
      }
    },
    exclusions: ['Documentation']
  }
}]);
