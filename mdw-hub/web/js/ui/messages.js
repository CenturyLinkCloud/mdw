'use strict';

var $mdwMessages = {
  init: function() {
    var webSocketUrl = $mdwUi.getWebSocketUrl();
    if (webSocketUrl) {
      const alertSocket = new WebSocket(webSocketUrl);
      alertSocket.addEventListener('open', function(event) {
        alertSocket.send("SystemAlert");
      });
      alertSocket.addEventListener('message', function(event) {
        var alert = event.data;
        console.log("ALERT: " + alert);
      });
      const messageSocket = new WebSocket(webSocketUrl);
      messageSocket.addEventListener('open', function(event) {
        messageSocket.send("SystemMessage");
      });
      messageSocket.addEventListener('message', function(event) {
        var message = event.data;
        console.log("MESSAGE: " + message);
      });
    }   
  }
};