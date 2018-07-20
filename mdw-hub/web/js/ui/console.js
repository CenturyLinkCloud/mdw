'use strict';

Console.CURSOR = '> ';
function Console(websocketUrl) {
  this.textarea = document.getElementById('mdw-console');
  
  if (this.textarea) {
    this.onKeydown = this.onKeydown.bind(this);
    this.run = this.run.bind(this);
    
    this.textarea.addEventListener('keydown', this.onKeydown);
    this.websocketUrl = websocketUrl;
    this.commandHistory = [];
    this.commandIndex = 0;
    this.clear();
  }
}

Console.prototype.clear = function() {
  this.text = '';
  this.appendText(Console.CURSOR);
  this.textarea.focus();
};

Console.prototype.appendText = function(text) {
  this.text += text;
  this.textarea.value = this.text;
  this.textarea.scrollTop = this.textarea.scrollHeight;
};

Console.prototype.run = function() {
  this.text = this.textarea.value;
  const lines = this.text.split(/\n/);
  const command = lines[lines.length - 1].substring(1).trim();
  this.appendText('\n');
  if (command.length === 0) {
    this.appendText(Console.CURSOR);
    return;
  }
  
  this.commandIndex = this.commandHistory.push(command);
  
  const consoleThis = this;
  if (this.websocketUrl) {
    // TODO
  }
  else {
    fetch($mdwServicesRoot + '/services/System/CLI?command=' + command, {
      credentials: 'same-origin'
    })
    .then(function(response) {
      return response.json();
    })
    .then(function(json) {
      consoleThis.appendText(json[0].sysInfos[0].value + '\n' + Console.CURSOR);
    });    
  }
};

Console.prototype.clearCommand = function() {
  const lines = this.text.split(/\n/);
  const cmd = lines[lines.length - 1].substring(1).trim();
  if (cmd.length > 0)
    this.text = this.text.substring(0, this.text.length - cmd.length);
};

Console.prototype.onKeydown = function(event) {

  if (event.which === 37 || event.which === 39) {
    // left/right arrow
    event.preventDefault();
  }
  else if (event.which === 38) {
    // up arrow
    if (this.commandIndex > 0) {
      this.clearCommand();
      this.commandIndex--;
      this.appendText(this.commandHistory[this.commandIndex]);
    }
    event.preventDefault();
  }
  else if (event.which === 40) {
    // down arrow
    if (this.commandIndex < this.commandHistory.length - 1) {
      this.clearCommand();
      this.commandIndex++;
      this.appendText(this.commandHistory[this.commandIndex]);
    }
    event.preventDefault();
  }
  else if (event.which === 13) {
    // enter key
    this.run();
    event.preventDefault();
  }
  
};
