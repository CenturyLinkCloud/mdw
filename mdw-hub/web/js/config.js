'use strict';

/**
* Override this global to configure MDWHub client request behavior.
* Values from here are accessible in both Angular modules and React components.
*/
var $mdwConfig = {
  processVersions: {
    withCommitInfo: true,
    withInstanceCounts: true
  }
};