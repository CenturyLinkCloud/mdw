// Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
'use strict';

var testingMod = angular.module('testing', ['ngResource', 'mdw']);

testingMod.controller('TestsController', ['$scope', '$websocket', 'mdw', 'util', 'AutomatedTests', 'TestExec', 'TestConfig',
                                         function($scope, $websocket, mdw, util, AutomatedTests, TestExec, TestConfig) {

  $scope.testCaseList = AutomatedTests.get({}, function success() {
    $scope.testCaseList.packages.forEach(function(pkg) {
      pkg.selected = false;
      pkg.testCases.forEach(function(tc) {
        tc.baseName = tc.name.substring(0, tc.name.lastIndexOf('.'));
      });
    });
  });

  $scope.config = TestConfig.get();
  
  $scope.running = function() {
    return $scope.forStatus('InProgress');
  };
  $scope.passed = function() {
    return $scope.forStatus('Passed');
  };
  $scope.failed = function() {
    return $scope.forStatus('Failed');
  };
  $scope.errored = function() {
    return $scope.forStatus('Errored');
  };
  $scope.forStatus = function(status) {
    var matched = [];
    if ($scope.testCaseList.packages) {
      $scope.testCaseList.packages.forEach(function(pkg) {
        pkg.testCases.forEach(function(testCase) {
          if (testCase.status == status)
            matched.push(testCase);
        });
      });
    }
    return matched;
  };
  
  $scope.selectedState = { all: false };
  $scope.toggleAll = function() {
    $scope.testCaseList.packages.forEach(function(pkg) {
      pkg.selected = $scope.selectedState.all;
      pkg.testCases.forEach(function(testCase) {
        testCase.selected = $scope.selectedState.all;
      });
    });
  };
  $scope.togglePackage = function(pkg) {
    pkg.testCases.forEach(function(testCase) {
      testCase.selected = pkg.selected;
    });
    $scope.selectedState.all = false;
  };
  $scope.packageOff = function(pkg) {
    pkg.selected = false;
    $scope.selectedState.all = false;
  };
  
  $scope.runTests = function() {
    var execTestPkgs = [];
    var pkgObj;
    for (var i = 0; i < $scope.testCaseList.packages.length; i++) {
      for (var j = 0; j < $scope.testCaseList.packages[i].testCases.length; j++) {
        if ($scope.testCaseList.packages[i].testCases[j].selected) {
          var pkgName = $scope.testCaseList.packages[i].name;
          pkgObj = null;
          for (var k = 0; k < execTestPkgs.length; k++) {
            if (execTestPkgs[k].name == pkgName) {
              pkgObj = execTestPkgs[k];
              break;
            }
          }
          if (!pkgObj) {
            pkgObj = {name: pkgName, version: $scope.testCaseList.packages[i].version, testCases: []};
            execTestPkgs.push(pkgObj);
          }
          pkgObj.testCases.push({name: $scope.testCaseList.packages[i].testCases[j].name});
        }
      }
    }
    
    TestConfig.put({}, $scope.config, function success() {
      TestExec.run({}, {packages: execTestPkgs}, function(data) {
        if (data.status.code !== 0) {
          $scope.testExecMessage = data.status.message;
        }
        else {
          $scope.testExecMessage = null;
        }
      }, 
      function(error) {
        $scope.testExecMessage = error.data.status.message;
      });
    });
  };
  
  $scope.acceptUpdates = function() {
    $scope.dataStream = $websocket(mdw.autoTestWebSocketUrl);
    $scope.dataStream.onMessage(function(message) {
      var newTestCaseList = JSON.parse(message.data);
      newTestCaseList.packages.forEach(function(newPkg) {
        var oldPkg = null;
        for (var i = 0; i < $scope.testCaseList.packages.length; i++) {
          if ($scope.testCaseList.packages[i].name == newPkg.name) {
            oldPkg = $scope.testCaseList.packages[i];
            break;
          }
        }
        if (oldPkg) {
          newPkg.testCases.forEach(function(newTestCase) {
            var oldTestCase = null;
            for (var j = 0; j < oldPkg.testCases.length; j++) {
              if (oldPkg.testCases[j].name == newTestCase.name) {
                oldTestCase = oldPkg.testCases[j];
                break;
              }
            }
            if (oldTestCase) {
              oldTestCase.status = newTestCase.status;
              oldTestCase.start = newTestCase.start;
              oldTestCase.end = newTestCase.end;
              oldTestCase.message = newTestCase.message;
              oldTestCase.expected = newTestCase.expected;
              oldTestCase.actual = newTestCase.actual;
            }
          });
        }
      });
    });
  };
  
  if (mdw.autoTestWebSocketUrl != '${mdw.autoTestWebSocketUrl}')
    $scope.acceptUpdates();  // substituted value should be websocket url
}]);

testingMod.controller('TestController', ['$scope', '$routeParams', '$q', 'AutomatedTests', 'TestCase',
                                         function($scope, $routeParams, $q, AutomatedTests, TestCase) {
  $scope.testCase = AutomatedTests.get({packageName: $routeParams.packageName, testCaseName: $routeParams.testCaseName}, function(testCaseData) {
    
    $scope.testCasePackage = $routeParams.packageName;
    var lastDot = $scope.testCase.name.lastIndexOf('.');
    $scope.testCase.baseName = $scope.testCase.name.substring(0, lastDot);
    $scope.testCase.language = $scope.testCase.name.substring(lastDot + 1);
    
    $scope.testCase.commands = TestCase.get({
      basePath: 'testCase',
      subPath: $routeParams.packageName,
      testResource: $scope.testCase.name
    });
    if ($scope.testCase.expected) {
      $scope.testCase.expectedResults = TestCase.get({
        basePath: 'testCase',
        subPath: $scope.testCase.expected.substring(0, $scope.testCase.expected.indexOf('/')),
        testResource: $scope.testCase.expected.substring($scope.testCase.expected.indexOf('/') + 1)
      });
    }
    if ($scope.testCase.actual) {
      $scope.testCase.actualResults = TestCase.get({
        basePath: 'testResult',
        subPath: $scope.testCase.actual.substring(0, $scope.testCase.actual.indexOf('/')),
        testResource: $scope.testCase.actual.substring($scope.testCase.actual.indexOf('/') + 1)
      });
    }
    if ($scope.testCase.executeLog) {
      $scope.testCase.log = TestCase.get({
        basePath: 'testResult',
        subPath: $scope.testCase.executeLog.substring(0, $scope.testCase.executeLog.indexOf('/')),
        testResource: $scope.testCase.executeLog.substring($scope.testCase.executeLog.indexOf('/') + 1)
      });
    }
  });
  
  $scope.selectResource = function(resource) {
  };
}]);

testingMod.filter('links', function($sce) {
  return function(input, pkg) {
    if (input) {
      var start = '<span class="hljs-string">"';
      var end = '"</span>';
      var regex = new RegExp('(.*?)(asset|process|file)\\(' + start + '(.*?)' + end + '\\)', 'g');
      var output = '';
      input.getLines().forEach(function(line) {
        var match = regex.exec(line);
        if (match !== null) {
          var stop;
          while (match !== null) {
            var path = match[3];
            var slash = path.lastIndexOf('/');
            if (slash < 0)
              path = pkg + '/' + path;
            if (match[2] == 'process')
              path += '.proc';
            output += match[1] + match[2] + '(' + start + '<a href="#/asset/' + path + '">' + match[3] + '</a>' + end + ')';
            stop = match.index + match[0].length;
            match = regex.exec(line);
          }
          output += line.substring(stop) + '\n';
        }
        else {
          output += line + '\n';
        }
      });
      return output;
    }
    
    return input;
  };
}).filter('unsafe', function($sce) { return $sce.trustAsHtml; });

testingMod.filter('yamlDiff', function($sce) {
  return function(one, two) {
    if (one) {
      one = one.replace(/&/g,'&amp;').replace(/</g,'&lt;');
      if (two) {
        two = two.replace(/&/g,'&amp;').replace(/</g,'&lt;');
        var pureOne = one.replace(/#.*$/gm, '');
        var pureTwo = two.replace(/#.*$/gm, '');
  
        var diffs = JsDiff.diffWordsWithSpace(pureOne, pureTwo);
        var hlOne = '';
        var pos = 0;
        diffs.forEach(function(diff) {
          if (diff.removed) {
            var lines = diff.value.getLines();
            if (lines.length == 1) {
              hlOne += '<span class="mdw-diff-delta">' + lines[0] + '</span>';
            }
            else {
              for (var i = 0; i < lines.length; i++) {
                if (!(i == lines.length - 1 && lines[i] === ''))
                  hlOne += '<span class="mdw-diff-delta">' + lines[i] + '</span>\n';
              }
            }
          }
          else if (!diff.added) {
            hlOne += diff.value;
          }
        });
        if (hlOne.length > 0) {
          var diffed = '';
          // add back the comments
          var diffLines = hlOne.getLines();
          var origLines = one.getLines();
          for (var j = 0; j < origLines.length; j++) {
            var origLine = origLines[j];
            var hashIdx = origLine.indexOf('#');
            if (hashIdx > 0)
              diffed += diffLines[j] + '<span class="mdw-diff-ignored">' + origLine.substring(hashIdx) + '</span>\n';
            else
              diffed += diffLines[j] + '\n';
          }
          return diffed;
        }
        else {
          return one + '\n';
        }
      }
      else {
        return one + '\n';
      }
    }
  };
}).filter('unsafe', function($sce) { return $sce.trustAsHtml; });

testingMod.factory('TestCase', ['$resource', 'mdw', function($resource, mdw) {
  return $resource(mdw.roots.services + '/:basePath/:subPath/:testResource', mdw.hubParams(), {
    get: { 
      method: 'GET', 
      transformResponse: function(data, headers) {
        var lines = data.getLines();
        var lineNums = '';
        for (var i = 1; i < lines.length + 1; i++)
          lineNums += i + '\n';
        
        var resourceData = {
            content: data,
            lineNumbers: lineNums 
        };
        return resourceData;
      }
    }
  });
}]);

testingMod.factory('AutomatedTests', ['$resource', 'mdw', function($resource, mdw) {
  return $resource(mdw.roots.services + '/services/com/centurylink/mdw/testing/AutomatedTests/:packageName/:testCaseName', mdw.serviceParams(), {
    get: { method: 'GET', isArray: false }
  });
}]);

testingMod.factory('TestExec', ['$resource', 'mdw', function($resource, mdw) {
  return $resource(mdw.roots.services + '/services/com/centurylink/mdw/testing/AutomatedTests/exec', mdw.serviceParams(), {
    run: { method: 'POST' }
  });
}]);

testingMod.factory('TestConfig', ['$resource', 'mdw', function($resource, mdw) {
  return $resource(mdw.roots.services + '/services/com/centurylink/mdw/testing/AutomatedTests/config', mdw.serviceParams(), {
    get: { method: 'GET', isArray: false },
    put: { method: 'PUT' }
  });
}]);