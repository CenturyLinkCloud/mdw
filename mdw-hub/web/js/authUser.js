'use strict';

var userSvc = angular.module('authUser', ['mdw']);

userSvc.factory('authUser', ['$http', 'mdw', function($http, mdw) {
  
  var fillTabs = function(user) {
    var tabsJsonUrl = mdw.roots.hub + '/js/tabs.json';
    return $http.get(tabsJsonUrl).then(function(response) {
      user.tabs = [];
      var allTabs = response.data;
      for (var i = 0; i < allTabs.length; i++) {
        var tab = allTabs[i];
        if (!tab.condition || eval(tab.condition)) { // jshint ignore:line
          var userTab = {id: tab.id, label: tab.label, url: tab.url};
          if (tab.routes)
            userTab.routes = tab.routes;
          if (tab.navs)
            userTab.navs = tab.navs;
          user.tabs.push(userTab);
        }
      }
      
      // sequence tab indexes
      for (i = 0; i < user.tabs.length; i++)
        user.tabs[i].index = i + 1;
      
      return user;
    });
  };
  
  return {
    getAuthUser: function(devUser, locationHash) {
      var url;
      if (devUser.length > 0)
        url = mdw.roots.services +  '/Services/Users/' + devUser;
      else
        url = mdw.roots.hub + '/Services/AuthenticatedUser';  // use hub root for auth user

      console.log('retrieving user: ' + url);
      var promise = $http.get(url).then(function(response) {
        
        var user = response.data;
        if (!user.cuid) {
          // authentication failed -- no access
          return user;
        }
        
        user.id = user.cuid;
        
        user.hasWorkgroupsOtherThanCommon = function() {
          if (this.workgroups) {
            for (var i = 0; i < this.workgroups.length; i++) {
              if (this.workgroups[i] != 'Common')
                return true;
            }
          }
          return false;
        };
        
        user.hasRole = function(role) {
          if (this.roles) {
            for (var i = 0; i < this.roles.length; i++) {
              if (this.roles[i] == role)
                return true;
            }
          }
          if (this.workgroups) {
            for (var i = 0; i < this.workgroups.length; i++) { // jshint ignore:line
              if (role == 'Site Admin' && this.workgroups[i] == 'Site Admin')
                return true;
              var groupRoles = this.workgroups[i];
              if (this[groupRoles]) {
                for (var j = 0; j < this[groupRoles].length; j++) {
                  if (this[groupRoles][j] == role)
                    return true;
                }
              }
            }
          }
          return false;
        };
        
        user.getActiveTab = function() {
          for (var i = 0; i < this.tabs.length; i++) {
            var tab = user.tabs[i];
            if (tab.active)
              return tab;
          }          
        };

        user.setActiveTab = function(url) {
          var oneActive = false;
          for (var i = 0; i < this.tabs.length; i++) {
            var tab = user.tabs[i];
            tab.active = tab.url == url;
            if (!tab.active && tab.routes) {
              for (var j = 0; j < tab.routes.length; j++) {
                if (url.startsWith(tab.routes[j]) || url.startsWith('#' + tab.routes[j]))
                  tab.active = true;
              }
            }
            if (tab.active)
              oneActive = true;
          }
          if (!oneActive) {
            var defaultTab = this.tabs[0];
            if (defaultTab)
              defaultTab.active = 'true';
          }
          for (var i = 0; i < this.tabs.length; i++) { // jshint ignore:line
            var userTab = user.tabs[i];
            userTab.classes = [];
            if (userTab.active)
              userTab.classes.push('mdw_tab_active');
            else
              userTab.classes.push('mdw_tab_inactive');
            if (i === 0)
              userTab.classes.push('mdw_tab_first');
            else if (i == user.tabs.length - 1)
              userTab.classes.push('mdw_tab_last');
          }
        };
        
        return user;
      }).then(function(user) {
        return fillTabs(user);
      }).then(function(user) {
        return user;
      });
      return promise;
    },
    logout: function() {
      console.log('Logged out');
      document.cookie = 'JSESSIONID=; expires=Thu, 01 Jan 1970 00:00:01 GMT;';
      window.location.href = $mdwHubRoot + '/authentication/logout.jsf';
    }
  };
}]);