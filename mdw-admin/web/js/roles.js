// Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
'use strict';

var roleMod = angular.module('roles', ['ngResource', 'mdw']);

roleMod.controller('RolesController', ['$scope', '$location', 'Workroles', 
                                         function($scope, $location, Workroles) {
  Workroles.roleList = Workroles.get();
  $scope.getRoleList = function() {
    return Workroles.roleList;
  };
  
  $scope.create = false;
  $scope.setCreate = function(create) {
    $scope.create = create;
    $scope.role = { name: '' }; // blank name helps w/applying error styles
  };
  
  $scope.cancel = function() {
    $scope.setCreate(false);
  };
  
  $scope.isSaveEnabled = function() {
    return $scope.role && $scope.role.name && $scope.role.name.length > 0;
  };
  
  $scope.save = function() {
    console.log('creating role: ' + $scope.role.name);
    Workroles.create({name: $scope.role.name}, $scope.role,
      function(data) {
        if (data.status.code !== 0) {
          $scope.role.message = data.status.message;
        }
        else {
          $scope.setCreate(false);
          Workroles.roleList = Workroles.get();          
          $location.path('/roles');
        }
      }, 
      function(error) {
        $scope.role.message = error.data.status.message;
      });
  };
}]);

roleMod.controller('RoleController', ['$scope', '$routeParams', '$location', '$http', 'mdw', 'Workroles', 
									  function($scope, $routeParams, $location, $http, mdw, Workroles) {
  $scope.getRoleList = function() {
    return Workroles.roleList;
  };
  
  $scope.roleName = $routeParams.roleName;
  $scope.edit = false;
  $scope.setEdit = function(edit) {
    $scope.edit = edit;
    if (edit) // backup original for cancel
      $scope.uneditedRole = Workroles.shallowCopy({}, $scope.role);
  };

  $scope.confirm = false;
  $scope.setConfirm = function(confirm) {
    $scope.confirm = confirm;
    $scope.role.message = confirm ? 'Delete Role "' + $scope.role.name + '"?' : null;
  };
  
  $scope.cancel = function() {
    if ($scope.edit)
      $scope.role = Workroles.shallowCopy($scope.role, $scope.uneditedRole);
    $scope.setEdit(false);
  };
  
  $scope.isSaveEnabled = function() {
    return $scope.role && $scope.role.name && $scope.role.name.length > 0;
  };
  
  $scope.save = function() {
    console.log('saving role: ' + $scope.role.name);

	Workroles.update({name: $scope.uneditedRole.name}, Workroles.shallowCopy({}, $scope.role),
      function(data) {
        if (data.status.code !== 0) {
          $scope.role.message = data.status.message;
        }
        else {
          $scope.roleName = $scope.role.name;
          $scope.setEdit(false);
        }
      }, 
      function(error) {
        $scope.role.message = error.data.status.message;
      });
  };
  
  $scope.deleteRole = function() {
    console.log('deleting role: ' + $scope.role.name);
    
    Workroles.remove({name: $scope.role.name},
      function(data) {
        if (data.status.code !== 0) {
          $scope.role.message = data.status.message;
        }
        else {
          $scope.setConfirm(false);
          $location.path('/roles');
        }
      }, 
      function(error) {
        $scope.role.message = error.data.status.message;
      });
  };
  
  // for associating users to this scope's role
  $scope.selectedUser = null; // not used but required by typeahead
  $scope.addSelectedUser = function(selUser) {
    Workroles.create({name: $scope.role.name, rel: 'users', relId: selUser.cuid }, Workroles.shallowCopy({}, $scope.role),
        function(data) {
          if (data.status.code !== 0) {
            $scope.role.message = data.status.message;
          }
          else {
            $scope.role.users.push(selUser);
            $scope.role.users.sort(function(u1, u2) {
              return u1.name.localeCompare(u2.name);
            });
          }
        },
        function(error) {
          $scope.role.message = error.data.status.message;
        });

    $scope.closePopover();
  };

  $scope.findUser = function(typed) {
    return $http.get(mdw.roots.services + '/Services/Users?app=mdw-admin&find=' + typed).then(function(response) {
      return response.data.users;
    });
  };  
  
  $scope.removeUser = function(cuid) {
    Workroles.remove({name: $scope.role.name, rel: 'users', relId: cuid }, $scope.role,
        function(data) {
          if (data.status.code !== 0) {
            $scope.role.message = data.status.message;
          }
          else {
            var idx = -1;
            for (var i = 0; i < $scope.role.users.length; i++) {
              if ($scope.role.users[i].cuid === cuid) {
                idx = i;
                break;
              }
            }
            $scope.role.users.splice(idx, 1);
          }
        },
        function(error) {
          $scope.role.message = error.data.status.message;
        });
  };
  
  
  $scope.role = Workroles.get({name: $routeParams.roleName});
}]);

roleMod.factory('Workroles', ['$resource', 'mdw', function($resource, mdw) {
  return angular.extend({}, $resource(mdw.roots.services + '/Services/Roles/:name/:rel/:relId', mdw.serviceParams(), {
      create: { method: 'POST'},
      update: { method: 'PUT' },
      remove: { method: 'DELETE' }
    }), {
    shallowCopy: function(destRole, srcRole) {
      destRole.name = srcRole.name;
      destRole.description = srcRole.description;
      return destRole;
    }  
  });
}]);