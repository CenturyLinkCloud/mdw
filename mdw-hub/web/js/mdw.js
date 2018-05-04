'use strict';

var mdwMod = angular.module('mdw', []);

mdwMod.factory('mdw', function() {
  return {
    // $mdwVersion and $mdwHubRoot are set in logical root.js -- See RootServlet.java
    version: $mdwVersion,
    build: $mdwBuild,
    webSocketUrl: $mdwWebSocketUrl,
    discoveryUrl: $mdwDiscoveryUrl,
    authMethod: $mdwAuthMethod,
    appId: $mdwAppId,
    hubUser: $mdwHubUser,   // Used to determine if isDevelopment() - value is only set when in Dev mode
    roots: {
      services: $mdwServicesRoot,
      hub: $mdwHubRoot,
      docs: $mdwDocsRoot,
      central: $mdwCentralRoot
    },
    serviceParams: function() {
      return { 
        app: 'mdw-admin'
      };
    },
    hubParams: function() {
      return { 
        root: this.roots.hub,
        app: 'mdw-admin'
      };
    },    
    hubLoading: function(loading) {
      setTimeout(function() {
        var logo;
        if (loading) {
          logo = document.getElementById('hub_logo');
          if (logo)
            logo.src = logo.src.substring(0, logo.src.lastIndexOf('/')) + '/hub_loading.gif';
        }
        else {
          logo = document.getElementById('hub_logo');
          if (logo)
            logo.src = logo.src.substring(0, logo.src.lastIndexOf('/')) + '/hub_logo.png';
        }
      }, 0);
    },
    getBullitin: function() {
      return $mdwMessages.currentBulletin;
    }
  };
});