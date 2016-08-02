// Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
'use strict';
angular.module('blvWebsocket', [
  'ngAnimate',
  'ngWebSocket'
])
.factory('Messages', function($websocket) {
  var ws = $websocket('ws://localhost:8181/blvTopic');
  var collection = [];

  ws.onMessage(function(event) {
    console.log('message: ', event);
    /**
    var res;
    try {
      res = JSON.parse(event.data);
    } catch(e) {
      res = {'username': 'anonymous', 'message': event.data};
    }

    collection.push({
      username: res.username,
      content: res.message,
      timeStamp: event.timeStamp
    });
    */
  });

  ws.onError(function(event) {
    console.log('connection Error', event);
  });

  ws.onClose(function(event) {
    console.log('connection closed', event);
  });

  ws.onOpen(function() {
    console.log('connection open');
  });
  // setTimeout(function() {
  //   ws.close();
  // }, 500)

  return {
    collection: collection,
    status: function() {
      return ws.readyState;
    },
    send: function(message) {
      if (angular.isString(message)) {
        ws.send(message);
      }
      else if (angular.isObject(message)) {
        ws.send(JSON.stringify(message));
      }
    }

  };
});
