// Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
'use strict';

var mdwMod = angular.module('mdw', []);

mdwMod.factory('mdw', function() {
  return {
    // $mdwVersion and $mdwHubRoot are set in logical root.js -- See RootServlet.java
    version: $mdwVersion,
    build: $mdwBuild,
    autoTestWebSocketUrl: $mdwAutoTestWebSocketUrl,
    roots: {
      services: $mdwServicesRoot,
      hub: $mdwHubRoot,
      webTools: $mdwWebToolsRoot
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
      // TODO: figure out a more angular way
      var isIe = (navigator.userAgent.indexOf('MSIE') >= 0 || navigator.userAgent.indexOf('Trident') >= 0);
      setTimeout(function() {
        var logo, load;
        if (loading) {
          if (isIe) {
            logo = document.getElementById('hub_logo');
            if (logo)
              logo.src = logo.src.substring(0, logo.src.lastIndexOf('/')) + '/hub_loading.gif';
          }
          else {
            logo = document.getElementById('hub_logo');
            if (logo)
              logo.style.display = 'none';
            load = document.getElementById('hub_loading');
            if (load)
              load.style.display = 'inline';
          }
        }
        else {
          if (isIe) {
            logo = document.getElementById('hub_logo');
            if (logo)
              logo.src = logo.src.substring(0, logo.src.lastIndexOf('/')) + '/hub_logo.png';
          }
          else {
            load = document.getElementById('hub_loading');
            if (load)
              load.style.display = 'none';
            logo = document.getElementById('hub_logo');
            if (logo)
              logo.style.display = 'inline';
          }
        }
      }, 0);
    }
  };
});