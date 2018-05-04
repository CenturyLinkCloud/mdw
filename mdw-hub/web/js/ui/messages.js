'use strict';

var MdwMessages = function() {
  this.webSocketUrl = $mdwUi.getWebSocketUrl();
  if (this.webSocketUrl) {
    const messageSocket = new WebSocket(this.webSocketUrl);
    messageSocket.addEventListener('open', function(event) {
      messageSocket.send("SystemMessage");
    });
    var mdwMessages = this;
    messageSocket.addEventListener('message', function(event) {
      var obj = JSON.parse(event.data);
      if (obj.id) {
        mdwMessages.bulletin(obj);
      }
      else {
        mdwMessages.message(obj);
      }
    });
  }   
};

MdwMessages.prototype.bulletin = function(bulletin) {
  this.clear();
  if (this.timeout) {
    clearTimeout(this.timeout);
    this.timeout = null;
  }
  if (this.currentBulletin) {
    if (this.currentBulletin.id === bulletin.id && bulletin.signal === 'Off') {
      var text = bulletin.message.message;
      if (text && text !== this.currentBulletin.message.message) {
        let msgs = this.message(bulletin.message);
        if (msgs) {
          msgs.classList.add('mdw-fade-out');
          var mdwMessages = this;
          this.timeout = setTimeout(function() { 
            mdwMessages.clear(); 
          }, 3000);
        }
      }
      this.currentBulletin = null;
    }
    else if (bulletin.message.level === 'Error' || this.currentBulletin.message.level === 'Info') {
      // display new one only if higher or same severity
      this.currentBulletin = bulletin;
    }
  }
  else {
    this.currentBulletin = bulletin;
  }
  
  if (this.currentBulletin) {
    if (this.currentBulletin.signal === 'On') {
      let msgs = this.message(this.currentBulletin.message);
      if (msgs) {
        document.body.style.overflowX = 'hidden';
        msgs.classList.add('mdw-bulletin');
      }
    }
  }
};

MdwMessages.prototype.message = function(message) {
  return this.show(message.message, message.level);
};

MdwMessages.prototype.show = function(text, level) {
  var msgs = document.getElementById('mdwMainMessages');
  if (msgs) {
    this.clear(msgs);
    if (level === 'Info') {
      msgs.classList.add('mdw-info');
    }
    else {
      msgs.classList.add('mdw-error');
    }
    msgs.innerHTML = text;
  }
  else {
    // TODO mobile toast
    if (level === 'Info') {
      console.log(text);
    }
    else {
      console.error(text);
    }
  }
  return msgs;
};

MdwMessages.prototype.clear = function(msgs) {
  document.body.style.overflowX = 'visible';
  if (!msgs) {
    msgs = document.getElementById('mdwMainMessages');
  }
  if (msgs) {
    msgs.innerHTML = '';
    msgs.classList.remove('mdw-error');
    msgs.classList.remove('mdw-info');
    msgs.classList.remove('mdw-bulletin');
    msgs.classList.remove('mdw-fade-out');
  }
  else {
    // TODO: mobile toast
    console.error('{}');
  }
  return msgs;
};