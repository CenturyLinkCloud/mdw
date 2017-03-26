'use strict';

var groupMod = angular.module('groups', ['ngResource', 'mdw']);

groupMod.controller('GroupsController', ['$scope', '$location', 'Workgroups', 
                                         function($scope, $location, Workgroups) {
  Workgroups.groupList = Workgroups.get();
  $scope.getGroupList = function() {
    return Workgroups.groupList;
  };
  
  $scope.create = false;
  $scope.setCreate = function(create) {
    $scope.create = create;
    $scope.workgroup = { name: '' }; // blank name helps w/applying error styles
  };
  
  $scope.cancel = function() {
    $scope.setCreate(false);
  };
  
  $scope.isSaveEnabled = function() {
    return $scope.workgroup && $scope.workgroup.name && $scope.workgroup.name.length > 0;
  };
  
  $scope.save = function() {
    console.log('creating workgroup: ' + $scope.workgroup.name);
    
    Workgroups.create({name: $scope.workgroup.name}, $scope.workgroup,
      function(data) {
        if (data.status.code !== 0) {
          $scope.workgroup.message = data.status.message;
        }
        else {
          $scope.setCreate(false);
          Workgroups.groupList = Workgroups.get();          
          $location.path('/groups');          
        }
      }, 
      function(error) {
        $scope.workgroup.message = error.data.status.message;
      });
  };
}]);

groupMod.controller('GroupController', ['$scope', '$routeParams', '$location', '$http', 'mdw', 'Workgroups', 
                                        function($scope, $routeParams, $location, $http, mdw, Workgroups) {
  $scope.getGroupList = function() {
    return Workgroups.groupList;
  };
  
  $scope.groupName = $routeParams.groupName;
  $scope.edit = false;
  $scope.setEdit = function(edit) {
    $scope.edit = edit;
    if (edit) // backup original for cancel
      $scope.uneditedWorkgroup = Workgroups.shallowCopy({}, $scope.workgroup);
  };

  $scope.confirm = false;
  $scope.setConfirm = function(confirm) {
    $scope.confirm = confirm;
    $scope.workgroup.message = confirm ? 'Delete workgroup "' + $scope.workgroup.name + '"?' : null;
  };
  
  $scope.cancel = function() {
    if ($scope.edit)
      $scope.workgroup = Workgroups.shallowCopy($scope.workgroup, $scope.uneditedWorkgroup);
    $scope.setEdit(false);
    $scope.setConfirm(false);
  };
  
  $scope.isSaveEnabled = function() {
    return $scope.workgroup && $scope.workgroup.name && $scope.workgroup.name.length > 0;
  };
  
  $scope.save = function() {
    console.log('saving workgroup: ' + $scope.workgroup.name);
    
    Workgroups.update({name: $scope.uneditedWorkgroup.name}, Workgroups.shallowCopy({}, $scope.workgroup),
      function(data) {
        if (data.status.code !== 0) {
          $scope.workgroup.message = data.status.message;
        }
        else {
          $scope.groupName = $scope.workgroup.name;
          $scope.setEdit(false);
        }
      }, 
      function(error) {
        $scope.workgroup.message = error.data.status.message;
      });
  };
  
  $scope.deleteWorkgroup = function() {
    console.log('deleting workgroup: ' + $scope.workgroup.name);
    
    Workgroups.remove({name: $scope.workgroup.name},
      function(data) {
        if (data.status.code !== 0) {
          $scope.workgroup.message = data.status.message;
        }
        else {
          $scope.setConfirm(false);
          $location.path('/groups');
        }
      }, 
      function(error) {
        $scope.workgroup.message = error.data.status.message;
      });
  };
  
  // for associating users to this scope's workgroup
  $scope.selectedUser = null; // not used but required by typeahead
  $scope.addSelectedUser = function(selUser) {
    Workgroups.create({name: $scope.workgroup.name, rel: 'users', relId: selUser.cuid }, Workgroups.shallowCopy({}, $scope.workgroup),
        function(data) {
          if (data.status.code !== 0) {
            $scope.workgroup.message = data.status.message;
          }
          else {
            $scope.workgroup.users.push(selUser);
            $scope.workgroup.users.sort(function(u1, u2) {
              return u1.name.localeCompare(u2.name);
            });
          }
        },
        function(error) {
          $scope.workgroup.message = error.data.status.message;
        });

    $scope.closePopover();
  };

  $scope.findUser = function(typed) {
    return $http.get(mdw.roots.services + '/Services/Users?app=mdw-admin&find=' + typed).then(function(response) {
      return response.data.users;
    });
  };  
  
  $scope.removeUser = function(cuid) {
    Workgroups.remove({name: $scope.workgroup.name, rel: 'users', relId: cuid }, Workgroups.shallowCopy({}, $scope.workgroup),
        function(data) {
          if (data.status.code !== 0) {
            $scope.workgroup.message = data.status.message;
          }
          else {
            var idx = -1;
            for (var i = 0; i < $scope.workgroup.users.length; i++) {
              if ($scope.workgroup.users[i].cuid === cuid) {
                idx = i;
                break;
              }
            }
            $scope.workgroup.users.splice(idx, 1);
          }
        },
        function(error) {
          $scope.workgroup.message = error.data.status.message;
        });
  };
  
  
  $scope.workgroup = Workgroups.get({name: $routeParams.groupName});
}]);

groupMod.factory('Workgroups', ['$resource', 'mdw', function($resource, mdw) {
  return angular.extend({}, $resource(mdw.roots.services + '/Services/Workgroups/:name/:rel/:relId', mdw.serviceParams(), {
      create: { method: 'POST'},
      update: { method: 'PUT' },
      remove: { method: 'DELETE' }
    }), {
    shallowCopy: function(destWorkgroup, srcWorkgroup) {
      destWorkgroup.name = srcWorkgroup.name;
      destWorkgroup.description = srcWorkgroup.description;
      destWorkgroup.parent = srcWorkgroup.parent;
      return destWorkgroup;
    }
  });
}]);