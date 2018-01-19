'use strict';

var userMod = angular.module('users', ['ngResource', 'mdw', 'infinite-scroll']);

userMod.controller('UsersController', ['$scope', '$http', '$location', 'mdw', 'Users', 
                                       function($scope, $http, $location, mdw, Users) {
  $scope.users = [];
    
  $scope.busy = false;
  $scope.total = 0;
  $scope.selected = null;
  
  $scope.getNext = function() {
    if (!$scope.busy) {
      $scope.busy = true;
      
      if ($scope.selected !== null) {
        // user has been selected in typeahead
        $scope.users = [$scope.selected];
        $scope.busy = false;
      }
      else {
        // retrieve the user list
        var url = mdw.roots.services + '/Services/Users?app=mdw-admin&start=' + $scope.users.length;
        $http.get(url).error(function(data, status) {
          console.log('HTTP ' + status + ': ' + url);
          this.busy = false;
        }).success(function(data, status, headers, config) {
          $scope.total = data.total;
          $scope.users = $scope.users.concat(data.users);
          $scope.busy = false;
        });
      }
    }
  };
  
  $scope.hasMore = function() {
    return $scope.users.length === 0 || $scope.users.length < $scope.total;
  };
  
  $scope.select = function() {
    $scope.users = [$scope.selected];
  };
  
  $scope.change = function() {
    if ($scope.selected === null) {
      // repopulate list
      $scope.users = [];
      $scope.getNext();
    }
  };
  
  $scope.find = function(typed) {
    return $http.get(mdw.roots.services + '/Services/Users?app=mdw-admin&find=' + typed).then(function(response) {
      return response.data.users;
    });
  };
  
  $scope.create = false;
  $scope.setCreate = function(create) {
    $scope.create = create;
    $scope.user = {
        // blank cuid and name help w/applying error styles
        cuid: '', 
        name: '',
        // blank default attributes force empty fields to display
        attributes: { 'Email Address': '', 'Phone Number': '' }
    };
  };
  
  $scope.cancel = function() {
    $scope.setCreate(false);
  };
  
  $scope.isSaveEnabled = function() {
    return $scope.user && $scope.user.cuid && $scope.user.cuid.length > 0 &&
      $scope.user.name && $scope.user.name.length > 0;
  };
  
  $scope.save = function() {
    console.log('creating user: ' + $scope.user.cuid);
    document.getElementById("newAttr-Name").disabled = true;
    document.getElementById("newAttr-Value").disabled = true;
    Users.create({cuid: $scope.user.cuid}, $scope.user,
      function(data) {
        if (data.status.code !== 0) {
          $scope.user.message = data.status.message;
        }
        else {
          $scope.setCreate(false);
          $scope.users = [];
          $scope.total = 0;          
          $location.path('/users');
        }
      }, 
      function(error) {
        $scope.user.message = error.data.status.message;
      });
  };
  
}]);

userMod.controller('UserController', ['$scope', '$routeParams', '$location', 'Users', 'Workgroups', 'Workroles',
                                      function($scope, $routeParams, $location, Users, Workgroups, Workroles) {
  // need the workgroup list cached
  if (!Workgroups.groupList)
    Workgroups.groupList = Workgroups.get();
    
  // need the role list cached
  if (!Workroles.roleList)
    Workroles.roleList = Workroles.get();
    
  $scope.userId = $routeParams.userId;
  $scope.edit = false;
  $scope.setEdit = function(edit) {
    $scope.edit = edit;
    if (edit) // backup original for cancel
      $scope.uneditedUser = Users.shallowCopy({}, $scope.user);
  };
  
  $scope.confirm = false;
  $scope.setConfirm = function(confirm) {
    $scope.confirm = confirm;
    $scope.user.message = confirm ? 'Delete user "' + $scope.user.name + '"?' : null;
  };
  
  $scope.cancel = function() {
    if ($scope.edit)
      $scope.user = Users.shallowCopy($scope.user, $scope.uneditedUser);
    $scope.setEdit(false);
    $scope.setConfirm(false);
  };
  
  $scope.canEdit = function(user) {
    return !$scope.edit && !$scope.confirm && ($scope.authUser.hasRole('User Admin') || (user && $scope.authUser.cuid == user.cuid));  
  };
  
  $scope.isSaveEnabled = function() {
    return $scope.user && $scope.user.cuid && $scope.user.cuid.length > 0 &&
      $scope.user.name && $scope.user.name.length > 0;
  };
  
  $scope.save = function() {
    console.log('saving user: ' + $scope.user.cuid);
    Users.update({cuid: $scope.uneditedUser.cuid}, Users.shallowCopy({}, $scope.user),
      function(data) {
        if (data.status.code !== 0) {
          $scope.user.message = data.status.message;
        }
        else {
          $scope.userName = $scope.user.name;
          $scope.setEdit(false);
        }
      }, 
      function(error) {
        $scope.user.message = error.data.status.message;
      });
  };
  
  $scope.deleteUser = function() {
    console.log('deleting user: ' + $scope.user.cuid);
    
    Users.remove({cuid: $scope.user.cuid},
      function(data) {
        if (data.status.code !== 0) {
          $scope.user.message = data.status.message;
        }
        else {
          $scope.setConfirm(false);
          $location.path('/users');
        }
      }, 
      function(error) {
        $scope.user.message = error.data.status.message;
      });
  };
  
  // the workgroups user does not currently belong to
  $scope.getOtherWorkgroups = function() {
    var otherWorkgroups = [];
    
    for (var i = 0; i < Workgroups.groupList.workgroups.length; i++) {
      var workgroup = Workgroups.groupList.workgroups[i];
      if ($scope.user.workgroups.indexOf(workgroup.name) < 0)
        otherWorkgroups.push(workgroup.name);
    }
    
    return otherWorkgroups;
  };
  
  $scope.addWorkgroup = function(workgroup) {
    Users.create({cuid: $scope.user.cuid, rel: 'workgroups', relId: workgroup }, Users.shallowCopy({}, $scope.user),
        function(data) {
          if (data.status.code !== 0) {
            $scope.user.message = data.status.message;
          }
          else {
            $scope.user.workgroups.push(workgroup);
            $scope.user.workgroups.sort();
          }
        },
        function(error) {
          $scope.user.message = error.data.status.message;
        });
    
    $scope.closePopover();
  };

  $scope.removeWorkgroup = function(workgroup) {
    Users.remove({cuid: $scope.user.cuid, rel: 'workgroups', relId: workgroup }, Users.shallowCopy({}, $scope.user),
        function(data) {
          if (data.status.code !== 0) {
            $scope.user.message = data.status.message;
          }
          else {
            $scope.user.workgroups.splice($scope.user.workgroups.indexOf(workgroup), 1);
          }
        },
        function(error) {
          $scope.user.message = error.data.status.message;
        });
  };
  
  // the role user does not currently belong to
  $scope.getOtherRoles = function() {
    var otherRoles = [];
    
    for (var i = 0; i < Workroles.roleList.roles.length; i++) {
      var role = Workroles.roleList.roles[i];
      if ($scope.user.roles.indexOf(role.name) < 0)
        otherRoles.push(role.name);
    }
    
    return otherRoles;
  };
  
  $scope.addRole = function(role) {
      Users.create({cuid: $scope.user.cuid, rel: 'roles', relId: role }, Users.shallowCopy({}, $scope.user),
        function(data) {
          if (data.status.code !== 0) {
            $scope.user.message = data.status.message;
          }
          else {
            $scope.user.roles.push(role);
            $scope.user.roles.sort();
          }
        },
        function(error) {
          $scope.user.message = error.data.status.message;
        });
    
    $scope.closePopover();
  };

  $scope.removeRole = function(role) {
    Users.remove({cuid: $scope.user.cuid, rel: 'roles', relId: role }, Users.shallowCopy({}, $scope.user),
        function(data) {
          if (data.status.code !== 0) {
            $scope.user.message = data.status.message;
          }
          else {
            $scope.user.roles.splice($scope.user.roles.indexOf(role), 1);
          }
        },
        function(error) {
          $scope.user.message = error.data.status.message;
        });
  };
  
  $scope.user = Users.get({cuid: $routeParams.userId},
    function() {
      $scope.userName = $scope.user.name;
    });
  
  $scope.attributes = [];
  $scope.attribute = {
	      name: '', 
	      value: ''
	  };
  $scope.addAttribute = function () {
	  var newAttribute = $scope.attribute;
      $scope.attributes.push(newAttribute);
      $scope.attribute = {
    	      name: '', 
    	      value: ''
    	  };
  }
  $scope.del = function(i){
	    $scope.attributes.splice(i,1);
	  }
  $scope.save = function() {
	    document.getElementById("newAttr-Name").disabled = true;
	    document.getElementById("newAttr-Value").disabled = true;
  }
}]);

userMod.factory('Users', ['$resource', 'mdw', function($resource, mdw) {
  return angular.extend({}, $resource(mdw.roots.services + '/Services/Users/:cuid/:rel/:relId', mdw.serviceParams(), {
    find: { method: 'GET', isArray: false },
    query: { method: 'GET', isArray: false }, 
    create: { method: 'POST'},
    update: { method: 'PUT' },
    remove: { method: 'DELETE' }    
  }), {
    shallowCopy: function(destUser, srcUser) {
      destUser.cuid = srcUser.cuid;
      destUser.name = srcUser.name;
      destUser.attributes = {};
      for (var attr in srcUser.attributes) {
        if (srcUser.attributes.hasOwnProperty(attr))
          destUser.attributes[attr] = srcUser.attributes[attr];
      }
      return destUser;
    }
  });
}]);
