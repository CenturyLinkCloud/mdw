'use strict';

var testingMod = angular.module('testing', ['ngResource', 'mdw']);

testingMod.controller('TestsController', 
    ['$scope', '$websocket', '$cookieStore', '$interval', '$timeout', 'mdw', 'util', 'AutomatedTests', 'TestsExec', 'TestsCancel', 'TestConfig',
    function($scope, $websocket, $cookieStore, $interval, $timeout, mdw, util, AutomatedTests, TestsExec, TestsCancel, TestConfig) {

  $scope.testCaseList = AutomatedTests.get({}, function success() {
    $scope.testCaseCount = 0;
    $scope.testCaseList.packages.forEach(function(pkg) {
      pkg.selected = false;
      pkg.testCases.forEach(function(tc) {
        tc.baseName = tc.name.substring(0, tc.name.lastIndexOf('.'));
        if (tc.items) {
          tc.items.forEach(function(item) {
            item.meth = item.object && item.object.request ? item.object.request.method : '';
            if (item.meth === 'DELETE')
              item.meth = 'DEL';
            else if (item.meth === 'OPTIONS')
              item.meth = 'OPT';
            item.path = item.object.name.replace('/', '~');
            if (item.meth)
              item.path = item.meth + ':' + item.path;
          });
        }
        $scope.testCaseCount++;
      });
      $scope.applyPkgCollapsedState();
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
  $scope.stopped = function() {
    return $scope.forStatus('Stopped');
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
        if (testCase.items) {
          testCase.items.forEach(function(item) {
            item.selected = pkg.selected;
          });
        }
      });
    });
  };
  $scope.togglePackage = function(pkg) {
    pkg.testCases.forEach(function(testCase) {
      testCase.selected = pkg.selected;
      if (testCase.items) {
        testCase.items.forEach(function(item) {
          item.selected = pkg.selected;
        });
      }
    });
    $scope.selectedState.all = false;
  };
  $scope.packageOff = function(pkg) {
    pkg.selected = false;
    $scope.selectedState.all = false;
  };
  
  $scope.collapse = function(pkg) {
    pkg.collapsed = true;
    $scope.savePkgCollapsedState();
  };
  $scope.collapseAll = function() {
    $scope.testCaseList.packages.forEach(function(pkg) {
      pkg.collapsed = true;
    });
    $scope.savePkgCollapsedState();
  };
  $scope.expand = function(pkg) {
    pkg.collapsed = false;
    $scope.savePkgCollapsedState();
  };
  $scope.expandAll = function() {
    $scope.testCaseList.packages.forEach(function(pkg) {
      pkg.collapsed = false;
    });
    $scope.savePkgCollapsedState();
  };
  $scope.savePkgCollapsedState = function() {
    var st = {};
    $scope.testCaseList.packages.forEach(function(pkg) {
      if (pkg.collapsed)
        st[pkg.name] = true;
    });
    $cookieStore.put('testsPkgCollapsedState', st);
  };
  $scope.applyPkgCollapsedState = function() {
    var st = $cookieStore.get('testsPkgCollapsedState');
    if (st) {
      util.getProperties(st).forEach(function(pkgName) {
        var col = st[pkgName];
        if (col === true) {
          for (var i = 0; i < $scope.testCaseList.packages.length; i++) {
            if (pkgName == $scope.testCaseList.packages[i].name) {
              $scope.testCaseList.packages[i].collapsed = true;
              break;
            }
          }
        }
      });
    }
  };
  
  $scope.runTests = function() {
    var execTestPkgs = [];
    var pkgObj;
    for (var i = 0; i < $scope.testCaseList.packages.length; i++) {
      for (var j = 0; j < $scope.testCaseList.packages[i].testCases.length; j++) {
        var tc = $scope.testCaseList.packages[i].testCases[j];
        if (tc.selected) {
          var pkgName = $scope.testCaseList.packages[i].name;
          pkgObj = null;
          for (let k = 0; k < execTestPkgs.length; k++) {
            if (execTestPkgs[k].name == pkgName) {
              pkgObj = execTestPkgs[k];
              break;
            }
          }
          if (!pkgObj) {
            pkgObj = {name: pkgName, version: $scope.testCaseList.packages[i].version, testCases: []};
            execTestPkgs.push(pkgObj);
          }
          var tcObj = {name: tc.name};
          if (tc.items) {
            tcObj.items = [];
            for (let k = 0; k < tc.items.length; k++) {
              if (tc.items[k].selected)
                tcObj.items.push({object: {name: tc.items[k].object.name, request: { method: tc.items[k].object.request.method }}});
            }
          }
          pkgObj.testCases.push(tcObj);
        }
      }
    }
    
    TestConfig.put({}, $scope.config, function success() {
      mdw.messages = null;
      TestsExec.run({}, {packages: execTestPkgs}, function(data) {
        if (data.status.code !== 0) {
          mdw.messages = data.status.message;
        }
        else {
          mdw.messages = null;
          // immediately update
          $timeout(function() {
            var newTestCaseList = AutomatedTests.get({}, function success() {
              $scope.applyUpdate(newTestCaseList);
            });
          }, 500);
        }
      }, 
      function(error) {
        mdw.messages = error.data.status.message;
      });
    },
    function(error) {
      mdw.messages = error.data.status.message;
    });
  };
  
  $scope.cancelTests = function() {
    TestsCancel.run({}, {}, function(data) {
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
  };  
  
  $scope.acceptUpdates = function() {
    $scope.dataStream = $websocket(mdw.autoTestWebSocketUrl);
    $scope.dataStream.send("AutomatedTests");
    $scope.dataStream.onMessage(function(message) {
      var newTestCaseList = JSON.parse(message.data);
      $scope.applyUpdate(newTestCaseList);
    });
  };
  
  $scope.pollForUpdates = function() {
    $scope.poller = $interval(function() {
      var newTestCaseList = AutomatedTests.get({}, function success() {
        $scope.applyUpdate(newTestCaseList);
      });
    }, 5000);
  };
  
  $scope.applyUpdate = function(newTestCaseList) {
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
          for (let j = 0; j < oldPkg.testCases.length; j++) {
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
            oldTestCase.log = newTestCase.log;
            if (newTestCase.items && oldTestCase.items) {
              for (let j = 0; j < oldTestCase.items.length; j++) {
                var oldItem = oldTestCase.items[j];
                var newItem = null;
                for (let k = 0; k < newTestCase.items.length; k++) {
                  if (oldItem.object.name == newTestCase.items[k].object.name) {
                    if (oldItem.object.request && oldItem.object.request.method) {
                      var newReq = newTestCase.items[k].object.request;
                      if (newReq && newReq.method === oldItem.object.request.method) {
                        newItem = newTestCase.items[k];
                        break;
                      }
                    }
                    else {
                      newItem = newTestCase.items[k];
                      break;
                    }
                  }
                }
                if (newItem) {
                  oldItem.status = newItem.status;
                  oldItem.start = newItem.start;
                  oldItem.end = newItem.end;
                  oldItem.message = newItem.message;
                  oldItem.expected = newItem.expected;
                  oldItem.actual = newItem.actual;
                  oldItem.log = newItem.log;
                }
              }
            }
          }
        });
      }
    });
  };
  
  if (mdw.autoTestWebSocketUrl != '${mdw.autoTestWebSocketUrl}') {
    $scope.acceptUpdates();  // substituted value should be websocket url
  }
  else {
    $scope.pollForUpdates(); // no ws configured
  }
  
  $scope.$on('$destroy', function() {
    if ($scope.poller)
      $interval.cancel($scope.poller);
  });  
}]);

testingMod.controller('TestController', ['$scope', '$routeParams', '$q', '$location', 'AutomatedTests', 'TestVcs', 'TestCase', 'TestExec',
                                         function($scope, $routeParams, $q, $location, AutomatedTests, TestVcs, TestCase, TestExec) {
  $scope.testCase = AutomatedTests.get({
    packageName: $routeParams.packageName, 
    testCaseName: $routeParams.testCaseName,
    itemName: $routeParams.itemName}, 
  function(testCaseData) {

    $scope.testCasePackage = $routeParams.packageName;
    $scope.testCaseItem = $routeParams.itemName;
    $scope.testCaseName = $routeParams.testCaseName; 

    if ($scope.testCaseItem) {
      $scope.testCase.baseName = $routeParams.testCaseName + ': ' + $scope.testCase.object.name;
      $scope.testCase.language = 'json';
    }
    else {
      var lastDot = $scope.testCase.name.lastIndexOf('.');
      $scope.testCase.baseName = $scope.testCase.name.substring(0, lastDot);
      $scope.testCase.language = $scope.testCase.name.substring(lastDot + 1);
      $scope.testCase.commitInfo = TestVcs.get({
        packageName: $scope.testCasePackage,
        testCaseName: $scope.testCaseName
      });
    }
    
    $scope.testCase.commands = TestCase.get({
      basePath: 'testCase',
      subPath: $routeParams.packageName,
      testResource: $scope.testCaseName,
      item: $scope.testCaseItem
    });
    if ($scope.testCase.expected) {
      $scope.testCase.expectedResults = TestCase.get({
        basePath: 'testCase',
        subPath: $scope.testCase.expected.substring(0, $scope.testCase.expected.indexOf('/')),
        testResource: $scope.testCase.expected.substring($scope.testCase.expected.indexOf('/') + 1),
        item: $scope.testCase.item
      });
    }
    if ($scope.testCase.actual) {
      $scope.testCase.actualResults = TestCase.get({
        basePath: 'testResult',
        subPath: $scope.testCase.actual.substring(0, $scope.testCase.actual.indexOf('/')),
        testResource: $scope.testCase.actual.substring($scope.testCase.actual.indexOf('/') + 1),
        item: $scope.testCase.item
      });
    }
    if ($scope.testCase.executeLog) {
      $scope.testCase.log = TestCase.get({
        basePath: 'testResult',
        subPath: $scope.testCase.executeLog.substring(0, $scope.testCase.executeLog.indexOf('/')),
        testResource: $scope.testCase.executeLog.substring($scope.testCase.executeLog.indexOf('/') + 1),
        item: $scope.testCase.item
      });
    }
  });
  
  $scope.runTest = function(testPkg, testName, item) {
    TestExec.run({packageName: testPkg, testCaseName: testName, itemName: item}, {}, function(data) {
      if (data.status.code !== 0) {
        $scope.testExecMessage = data.status.message;
      }
      else {
        $scope.testExecMessage = null;
        $location.path('/tests');        
      }
    }, 
    function(error) {
      $scope.testExecMessage = error.data.status.message;
    });
  };
}]);

testingMod.filter('assetLinks', function($sce) {
  return function(input, pkg) {
    if (input) {
      var start = '<span class="hljs-string">';
      var end = '</span>';
      // regex for both single and double quotes
      var regex1 = new RegExp('(.*?)(asset|process|file)\\(' + start + '\'(.*?)\'' + end + '\\)', 'g');
      var regex2 = new RegExp('(.*?)(asset|process|file)\\(' + start + '"(.*?)"' + end + '\\)', 'g');
      var output = '';
      input.getLines().forEach(function(line) {
        var regex = regex1;
        var match = regex.exec(line);
        if (match === null) {
          regex = regex2;
          match = regex.exec(line);
        }
        if (match !== null) {
          var stop;
          while (match !== null) {
            var path = match[3];
            var slash = path.lastIndexOf('/');
            if (slash < 0)
              path = pkg + '/' + path;
            if (match[2] == 'process')
              path += '.proc';
            var quot = regex == regex2 ? '"' : "'";
            output += match[1] + match[2] + '(' + start + quot + '<a href="#/asset/' + path + '">' + match[3] + '</a>' + quot + end + ')';
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

testingMod.filter('instanceLinks', function($sce) {
  return function(input) {
    if (input) {
      var start = '<span class="mdw-diff-ignored"># ';
      var end = '</span>';
      var output = '';
      input.getLines().forEach(function(line) {
        if (line.startsWith('process: ' + start)) {
          var procInstId = line.substring(start.length + 9, line.length - end.length);
          output += 'process: ' + start + '<a href="#/workflow/processes/' +  procInstId + '">' + procInstId + '</a>' + end + '\n';
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
  return function(one, two, testCaseItem) {
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
  return $resource(mdw.roots.services + '/:basePath/:subPath/:testResource/:item', mdw.hubParams(), {
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
  return $resource(mdw.roots.services + '/services/com/centurylink/mdw/testing/AutomatedTests/:packageName/:testCaseName/:itemName', mdw.serviceParams(), {
    get: { method: 'GET', isArray: false }
  });
}]);

testingMod.factory('TestVcs', ['$resource', 'mdw', function($resource, mdw) {
  return $resource(mdw.roots.services + '/services/GitVcs/:packageName/:testCaseName', mdw.serviceParams(), {
    get: { method: 'GET', isArray: false }
  });
}]);

testingMod.factory('TestsExec', ['$resource', 'mdw', function($resource, mdw) {
  return $resource(mdw.roots.services + '/services/com/centurylink/mdw/testing/AutomatedTests/exec', mdw.serviceParams(), {
    run: { method: 'POST' }
  });
}]);

testingMod.factory('TestExec', ['$resource', 'mdw', function($resource, mdw) {
  return $resource(mdw.roots.services + '/services/com/centurylink/mdw/testing/AutomatedTests/:packageName/:testCaseName/:itemName', mdw.serviceParams(), {
    run: { method: 'POST' }
  });
}]);

testingMod.factory('TestsCancel', ['$resource', 'mdw', function($resource, mdw) {
  return $resource(mdw.roots.services + '/services/com/centurylink/mdw/testing/AutomatedTests/cancel', mdw.serviceParams(), {
    run: { method: 'POST' }
  });
}]);

testingMod.factory('TestConfig', ['$resource', 'mdw', function($resource, mdw) {
  return $resource(mdw.roots.services + '/services/com/centurylink/mdw/testing/AutomatedTests/config', mdw.serviceParams(), {
    get: { method: 'GET', isArray: false },
    put: { method: 'PUT' }
  });
}]);