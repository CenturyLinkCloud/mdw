// Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
'use strict';
/**
 * Stores off any data that can be used by different controllers.
 */
angular.module('blvSharedData', []);

angular.module('blvSharedData').factory('blvSharedData', function() {
  return {
    data : {
      activityNode : {},
      bamEvent : {},
      masterRequestId : {},
      mainProcess : {}
    }
  // Other methods or objects can go here
  };
});
