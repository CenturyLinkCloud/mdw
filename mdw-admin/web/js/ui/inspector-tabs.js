// Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
'use strict';

var inspectorTabSvc = angular.module('mdwInspectorTabs', ['mdw']);

inspectorTabSvc.factory('InspectorTabs', ['mdw', function(mdw) {
  return {
    definition: {
      process: {
        Definition: {
          ID: 'id',
          Name: 'name',
          Description: 'description',
          Created: 'created' 
        },
        Variables: 'variables',
        Versions: {},
        Documentation: {},
        Attributes: 'attributes',
        Monitoring: {}
      },
      activity: {
        Definition: {
          ID: 'id',
          Name: 'name',
          Implementor: 'implementor',
          Description: 'description'
        },
        Documentation: {},
        Attributes: 'attributes',
        Monitoring: {},
        Stubbing: {}
      },
      subprocess: {
        Definition: {
          ID: 'id',
          Name: 'name',
          Description: 'description'
        },
        Documentation: {},
        Attributes: 'attributes'
      }
    }
  }
}]);
