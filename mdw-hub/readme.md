# mdw-hub

## Install
```bash
npm install hub-ui --save
```

## Usage

Instance diagram:

```js
var myApp = angular.module('myApp', ['mdw', 'util', 'constants', 'mdwWorkflow', 'mdwShape', 'mdwStep', 'mdwLink', 
  'mdwSubflow', 'mdwLabel', 'mdwNote', 'mdwMarquee', 'mdwInspector', 'mdwInspectorTabs']);

myApp.controller('myController', ['$scope',
                                  function($scope) {
  $scope.process = {
    name: 'MyProcessName',
    packageName: 'com.example.my.workflow',
    id: 81945 // instanceId (omit to display process definition)
  };
}]);
```

## HTML
```html
<!doctype html>
<html lang="en" ng-app="myApp">
<head>
  <title>My Hub-UI</title>
  <link rel="stylesheet" href="node_modules/bootstrap-css-only/css/bootstrap.css">
  <link rel="stylesheet" href="node_modules/highlightjs/styles/default.css">  
  <link rel="stylesheet" href="node_modules/hub-ui/hub-ui.css">
  <script src="node_modules/angular/angular.js"></script>
  <script src="node_modules/marked/lib/marked.js"></script>  
  <script src="node_modules/highlightjs/highlight.pack.js"></script>
  <script src="node_modules/hub-ui/hub-ui.js"></script>
  <script src="my.js"></script>
</head>
<body ng-controller="myController">
  <div>
    <mdw-workflow process="process" 
      render-state="true"
      service-base="http://localhost:8080/mdw/services">
    </mdw-workflow>
  </div>
</body>
</html>

```
