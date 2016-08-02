// Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
'use strict';
describe('controller: RolesController', function() {
   var   RoleController,scope;
  
  // load the roles module
  beforeEach(module('roles'));
  
//Instantiate $routeParams
  beforeEach(module(function ($provide) {
    $provide.provider('$routeParams', function () {
      this.$get = function () {
        return {
          roleName: 'Supervisor'
        };
      };
    });
  }));
//Instantiate scope and RoleController
  beforeEach(inject(function($rootScope, $controller, $routeParams) {
    scope = $rootScope.$new();
    RoleController = $controller('RoleController', {$scope: scope}, $routeParams );
  }));
 
  it('RoleController Should set the Role Name in scope that was passed in $routeParams', function() {
    console.log( "scope.edit = "+ scope.edit);
    console.log( "scope.roleName = "+ scope.roleName);
    expect(scope.roleName).toContain('Supervisor');

  });

});