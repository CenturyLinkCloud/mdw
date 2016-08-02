// Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
'use strict';
describe('Testing mdw Service', function(){
  window.$mdwHubRoot = "mdw";
  window.$mdwVersion = "6.0";
  var mdwService;
  
  beforeEach(function(){
    // load the mdw module
    module('mdw');
    //Instantiate mdw service with in the mdw module
    inject(function($injector){
      mdwService = $injector.get('mdw');
    });
  });

  it('mdw service should return App Name', function() {
    var serviceParams = mdwService.serviceParams();
    expect(serviceParams.app).toContain('mdw-admin');
  });


  it('mdw service should return Service Name', function() {
    var serviceParams = mdwService.serviceParams();
    expect(serviceParams.root).toContain('mdw');
  });

  it('mdw service should return Hub Name', function() {
    var hubParams = mdwService.hubParams();
    expect(hubParams.root).toContain('mdw');

  });

});