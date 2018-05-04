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
          this.currentBulletin = null;
          msgs.classList.remove('mdw-bulletin');
          msgs.classList.add('mdw-fade-out');
          var mdwMessages = this;
          this.timeout = setTimeout(function() { 
            mdwMessages.clear(true); 
          }, 4000);
        }
      }
      else {
          this.clear(true);
      }
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

// Right now returns the current bulletin text, if any.
MdwMessages.prototype.getMessageText = function() {
  if (this.currentBulletin)
    return this.currentBulletin.text;
};

MdwMessages.prototype.show = function(text, level) {
  var msgs = document.getElementById('mdwMainMessages');
  if (msgs) {
    this.clear();
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

// if bulletin is true, also clears current bulletin
MdwMessages.prototype.clear = function(bulletin) {
  if (bulletin) {
      this.currentBulletin = null;
      document.body.style.overflowX = 'visible';
  }
  var msgs = document.getElementById('mdwMainMessages');
  if (msgs) {
    if (bulletin) {
      msgs.innerHTML = '';
      msgs.classList.remove('mdw-bulletin');
      msgs.classList.remove('mdw-error');
      msgs.classList.remove('mdw-info');
    }
    else {
      msgs.innerHtml = this.currentBulletin ? this.currentBulletin.message.message : '';
    }
    msgs.classList.remove('mdw-fade-out');
  }
  else {
    // TODO: mobile toast
    console.error('{}');
  }
  return msgs;
};