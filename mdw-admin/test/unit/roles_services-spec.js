// Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
'use strict';
describe('Testing roles Service', function() {
  var workRolesService, httpMock;

  beforeEach(function(){
  // load the roles module
    module('roles');
    
  //Instantiate WorkRoles Service
    inject(function($rootScope,$injector){
      workRolesService = $injector.get('Workroles');
    //  $scope = $rootScope.$new();
    });
  });
  
//set up the back end mock http server for workRolesService.query() to call
  beforeEach(inject(function($httpBackend) {
    httpMock = $httpBackend;
    httpMock.expectGET('/mdw/Services/Roles?app=mdw-admin')
    
    .respond(
        [
                  {"name":"ProcessAdmin",       "description":"process Administrator" },
                  {"name":"Process Execution",  "description":"execute processes" },
                  {"name":"Task Execution",     "description":"perform manual tasks" }
                  ]
    );
  }));

  it("does some networking in a service call'", function () {
    
    // service makes http request to httpMock defined above
    var roleList = workRolesService.query();
    
    //htp request handled and result sent to roleList.
    httpMock.flush();
    
    console.log("keys in roleList object " + Object.keys(roleList));
    console.log("keys in roleList.[0] object " + Object.keys(roleList[0]));
    console.log("roleList[0].name=" + roleList[0].name);
    console.log("roleList[0].description=" + roleList[0].description);
    console.log("$promise.$$state.status=" + roleList.$promise.$$state.status); //0 for pending, 1 for fulfilled, or 2 for rejected.
    console.log("$resolved=" + roleList.$resolved);
    
    expect(roleList[0].name).toContain('ProcessAdmin');
    expect(roleList[0].description).toContain('process Administrator');
    expect(roleList[2].name).toContain('Task Execution');
    expect(roleList[2].description).toContain('perform manual tasks');
    expect(roleList.length).toBe(3);
  });

});
